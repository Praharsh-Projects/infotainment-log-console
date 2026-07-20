package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.LogSource
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

interface LogRemoteDataSource {
    suspend fun fetchLogs(brand: String, bearerToken: String): List<SignalLogEntry>
}

@Serializable
private data class RemoteLogPage(
    val entries: List<SignalLogEntry>
)

class HttpLogRemoteDataSource(
    baseUrl: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogRemoteDataSource {
    private val baseUrl: HttpUrl = baseUrl.toHttpUrlOrNull()
        ?: throw IllegalArgumentException("API base URL is invalid.")

    override suspend fun fetchLogs(
        brand: String,
        bearerToken: String
    ): List<SignalLogEntry> = withContext(ioDispatcher) {
        val normalizedToken = bearerToken.trim()
        if (
            normalizedToken.isBlank() ||
            normalizedToken.length > MAX_TOKEN_LENGTH ||
            normalizedToken.any { it == '\r' || it == '\n' }
        ) {
            throw LogSyncException("A valid session token is required.")
        }

        val url = baseUrl.newBuilder()
            .addPathSegments("v1/logs")
            .addQueryParameter("brand", brand)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("Authorization", "Bearer $normalizedToken")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw LogSyncException("Log sync failed with HTTP ${response.code}.")
                }
                val payload = response.body?.string()
                    ?: throw LogSyncException("Log sync returned an empty response.")
                if (payload.length > MAX_RESPONSE_CHARACTERS) {
                    throw LogSyncException("Log sync response exceeded the safety limit.")
                }

                val page = runCatching {
                    LogJsonCodec.parser.decodeFromString<RemoteLogPage>(payload)
                }.getOrElse {
                    throw LogSyncException("Log sync returned invalid JSON.", it)
                }
                if (page.entries.size > MAX_REMOTE_ENTRIES) {
                    throw LogSyncException("Log sync returned too many entries.")
                }

                page.entries.map { entry ->
                    val validated = validate(entry)
                    if (!validated.brand.equals(brand, ignoreCase = true)) {
                        throw LogSyncException("Log sync returned an entry for the wrong brand.")
                    }
                    validated.copy(source = LogSource.REMOTE)
                }
            }
        } catch (error: LogSyncException) {
            throw error
        } catch (error: IOException) {
            throw LogSyncException("Log sync could not reach the configured API.", error)
        }
    }

    private fun validate(entry: SignalLogEntry): SignalLogEntry {
        if (
            entry.id <= 0 ||
            entry.timestamp.isBlank() ||
            entry.brand.isBlank() ||
            entry.subsystem.isBlank() ||
            entry.signal.isBlank() ||
            entry.value.isBlank()
        ) {
            throw LogSyncException("Log sync returned an invalid entry.")
        }
        return entry
    }

    companion object {
        private const val MAX_TOKEN_LENGTH = 2_048
        private const val MAX_RESPONSE_CHARACTERS = 1_000_000
        private const val MAX_REMOTE_ENTRIES = 500
    }
}

class LogSyncException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
