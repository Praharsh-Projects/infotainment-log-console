package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface LogLocalDataSource {
    suspend fun load(): List<SignalLogEntry>
    suspend fun save(entries: List<SignalLogEntry>)
    suspend fun export(entries: List<SignalLogEntry>): File
}

class JsonLogFileStore(
    private val storageFile: File,
    private val exportDirectory: File,
    private val legacyStorageFile: File? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : LogLocalDataSource {

    override suspend fun load(): List<SignalLogEntry> = withContext(ioDispatcher) {
        when {
            storageFile.exists() -> LogJsonCodec.decode(storageFile.readText())
            legacyStorageFile?.exists() == true -> {
                val migrated = LegacyTsvCodec.decode(legacyStorageFile.readText())
                    .takeLast(MAX_STORED_ENTRIES)
                writeCache(migrated)
                migrated
            }
            else -> emptyList()
        }
    }

    override suspend fun save(entries: List<SignalLogEntry>) = withContext(ioDispatcher) {
        writeCache(entries.takeLast(MAX_STORED_ENTRIES))
    }

    override suspend fun export(entries: List<SignalLogEntry>): File = withContext(ioDispatcher) {
        require(entries.isNotEmpty()) { "Cannot export an empty log selection." }
        exportDirectory.mkdirs()
        File(exportDirectory, "infotainment-log-export-${System.currentTimeMillis()}.json").apply {
            writeText(LogJsonCodec.encode(entries))
        }
    }

    private fun writeCache(entries: List<SignalLogEntry>) {
        storageFile.parentFile?.mkdirs()
        val temporaryFile = File(storageFile.parentFile, "${storageFile.name}.tmp")
        temporaryFile.writeText(LogJsonCodec.encode(entries))
        temporaryFile.copyTo(storageFile, overwrite = true)
        check(temporaryFile.delete()) { "Could not remove temporary cache file." }
    }

    companion object {
        const val MAX_STORED_ENTRIES = 500
    }
}
