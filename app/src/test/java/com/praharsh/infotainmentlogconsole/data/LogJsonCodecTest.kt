package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.LogLevel
import com.praharsh.infotainmentlogconsole.domain.LogSource
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LogJsonCodecTest {
    @Test
    fun roundTripPreservesTypedEntries() {
        val entries = listOf(sampleEntry(source = LogSource.REMOTE))

        assertEquals(entries, LogJsonCodec.decode(LogJsonCodec.encode(entries)))
    }

    @Test
    fun decoderIgnoresForwardCompatibleFields() {
        val payload = """
            {"schemaVersion":1,"futureField":"safe","entries":[]}
        """.trimIndent()

        assertEquals(emptyList<SignalLogEntry>(), LogJsonCodec.decode(payload))
    }

    @Test
    fun decoderRejectsUnknownSchemaVersion() {
        val payload = """{"schemaVersion":99,"entries":[]}"""

        assertThrows(IllegalArgumentException::class.java) {
            LogJsonCodec.decode(payload)
        }
    }

    @Test
    fun legacyDecoderSkipsMalformedRows() {
        val payload = "1\t2026-06-24 10:00:00\tNordicDrive\tMedia\tmediaVolume\t12\tINFO\nmalformed"

        assertEquals(listOf(1L), LegacyTsvCodec.decode(payload).map { it.id })
    }

    private fun sampleEntry(source: LogSource) = SignalLogEntry(
        id = 1,
        timestamp = "2026-06-24 10:00:00",
        brand = "NordicDrive",
        subsystem = "Media",
        signal = "mediaVolume",
        value = "12",
        level = LogLevel.INFO,
        source = source
    )
}
