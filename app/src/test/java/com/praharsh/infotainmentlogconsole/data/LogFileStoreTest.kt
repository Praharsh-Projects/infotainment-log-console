package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.LogLevel
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LogFileStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun saveBoundsCacheAtFiveHundredEntries() = runTest(dispatcher) {
        val store = createStore()
        store.save((1L..510L).map(::entry))

        val loaded = store.load()
        assertEquals(500, loaded.size)
        assertEquals(11L, loaded.first().id)
        assertEquals(510L, loaded.last().id)
    }

    @Test
    fun loadMigratesLegacyTsvToJson() = runTest(dispatcher) {
        val root = temporaryFolder.newFolder("migration")
        val jsonFile = File(root, "cache.json")
        val legacyFile = File(root, "cache.tsv").apply {
            writeText("1\t2026-06-24 10:00:00\tNordicDrive\tMedia\tmediaVolume\t12\tINFO")
        }
        val store = JsonLogFileStore(
            storageFile = jsonFile,
            exportDirectory = File(root, "exports"),
            legacyStorageFile = legacyFile,
            ioDispatcher = dispatcher
        )

        assertEquals(listOf(1L), store.load().map { it.id })
        assertTrue(jsonFile.exists())
        assertFalse(jsonFile.readText().contains('\t'))
    }

    @Test
    fun exportCreatesReadableJsonFile() = runTest(dispatcher) {
        val store = createStore()

        val exported = store.export(listOf(entry(7)))

        assertTrue(exported.name.endsWith(".json"))
        assertEquals(listOf(7L), LogJsonCodec.decode(exported.readText()).map { it.id })
    }

    private fun createStore(): JsonLogFileStore {
        val root = temporaryFolder.newFolder()
        return JsonLogFileStore(
            storageFile = File(root, "cache.json"),
            exportDirectory = File(root, "exports"),
            ioDispatcher = dispatcher
        )
    }

    private fun entry(id: Long) = SignalLogEntry(
        id = id,
        timestamp = "2026-06-24 10:00:${id % 60}",
        brand = "NordicDrive",
        subsystem = "Media",
        signal = "mediaVolume",
        value = "12",
        level = LogLevel.INFO
    )
}
