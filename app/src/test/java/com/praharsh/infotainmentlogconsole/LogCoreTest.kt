package com.praharsh.infotainmentlogconsole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogCoreTest {

    @Test
    fun codecRoundTripPreservesEntries() {
        val entries = listOf(
            SignalLogEntry(
                id = 1L,
                timestamp = "2026-06-24 10:00:00",
                brand = "NordicDrive",
                subsystem = "Media",
                signal = "mediaVolume",
                value = "12",
                level = LogLevel.INFO
            ),
            SignalLogEntry(
                id = 2L,
                timestamp = "2026-06-24 10:00:01",
                brand = "NordicDrive",
                subsystem = "Navigation",
                signal = "gpsDrift",
                value = "4.6m",
                level = LogLevel.WARN
            )
        )

        val decoded = LogCodec.decode(LogCodec.encode(entries))
        assertEquals(entries, decoded)
    }

    @Test
    fun filtersRespectBrandQueryAndLevel() {
        val entries = listOf(
            SignalLogEntry(1L, "2026-06-24 10:00:00", "NordicDrive", "Media", "mediaVolume", "12", LogLevel.INFO),
            SignalLogEntry(2L, "2026-06-24 10:00:01", "NordicDrive", "Navigation", "gpsDrift", "4.6m", LogLevel.WARN),
            SignalLogEntry(3L, "2026-06-24 10:00:02", "UrbanMotion", "Vehicle", "doorOpen", "1", LogLevel.ERROR)
        )

        val filtered = LogFilters.apply(
            entries = entries,
            activeBrandKey = "nordic",
            query = "gps",
            level = LogLevel.WARN
        )

        assertEquals(1, filtered.size)
        assertEquals("gpsDrift", filtered.first().signal)
        assertTrue(filtered.all { it.brand == "NordicDrive" })
    }
}
