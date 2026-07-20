package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.LogLevel
import com.praharsh.infotainmentlogconsole.domain.LogSource
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class LogCacheEnvelope(
    val schemaVersion: Int = LogJsonCodec.SCHEMA_VERSION,
    val entries: List<SignalLogEntry>
)

object LogJsonCodec {
    const val SCHEMA_VERSION = 1

    val parser = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun encode(entries: List<SignalLogEntry>): String =
        parser.encodeToString(LogCacheEnvelope(entries = entries))

    fun decode(payload: String): List<SignalLogEntry> {
        if (payload.isBlank()) return emptyList()

        val envelope = parser.decodeFromString<LogCacheEnvelope>(payload)
        require(envelope.schemaVersion == SCHEMA_VERSION) {
            "Unsupported log cache schema ${envelope.schemaVersion}."
        }
        return envelope.entries
    }
}

internal object LegacyTsvCodec {
    fun decode(payload: String): List<SignalLogEntry> = payload.lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 7) return@mapNotNull null

            runCatching {
                SignalLogEntry(
                    id = parts[0].toLong(),
                    timestamp = parts[1],
                    brand = parts[2],
                    subsystem = parts[3],
                    signal = parts[4],
                    value = parts[5],
                    level = LogLevel.valueOf(parts[6]),
                    source = LogSource.SIMULATED
                )
            }.getOrNull()
        }
        .toList()
}
