package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import java.io.File

interface LogRepository {
    suspend fun loadCachedLogs(): List<SignalLogEntry>
    suspend fun persistLogs(entries: List<SignalLogEntry>)
    suspend fun fetchRemoteLogs(brand: String, bearerToken: String): List<SignalLogEntry>
    suspend fun exportLogs(entries: List<SignalLogEntry>): File
}

class DefaultLogRepository(
    private val local: LogLocalDataSource,
    private val remote: LogRemoteDataSource
) : LogRepository {
    override suspend fun loadCachedLogs(): List<SignalLogEntry> = local.load()

    override suspend fun persistLogs(entries: List<SignalLogEntry>) {
        local.save(entries)
    }

    override suspend fun fetchRemoteLogs(
        brand: String,
        bearerToken: String
    ): List<SignalLogEntry> = remote.fetchLogs(brand, bearerToken)

    override suspend fun exportLogs(entries: List<SignalLogEntry>): File =
        local.export(entries)
}

fun mergeLogs(
    cached: List<SignalLogEntry>,
    incoming: List<SignalLogEntry>,
    limit: Int = JsonLogFileStore.MAX_STORED_ENTRIES
): List<SignalLogEntry> = (cached + incoming)
    .associateBy { it.id }
    .values
    .sortedBy { it.timestamp }
    .takeLast(limit)
