package com.praharsh.infotainmentlogconsole.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogFiltersTest {
    private val entries = listOf(
        SignalLogEntry(1, "2026-06-24 10:00:00", "NordicDrive", "Media", "mediaVolume", "12", LogLevel.INFO),
        SignalLogEntry(2, "2026-06-24 10:00:01", "NordicDrive", "Navigation", "gpsDrift", "4.6m", LogLevel.WARN),
        SignalLogEntry(3, "2026-06-24 10:00:02", "UrbanMotion", "Vehicle", "doorOpen", "1", LogLevel.ERROR)
    )

    @Test
    fun filtersRespectBrandQueryAndLevel() {
        val filtered = LogFilters.apply(entries, "nordic", "gps", LogLevel.WARN)

        assertEquals(1, filtered.size)
        assertEquals("gpsDrift", filtered.first().signal)
        assertTrue(filtered.all { it.brand == "NordicDrive" })
    }

    @Test
    fun blankQueryAndLevelStillRestrictBrand() {
        val filtered = LogFilters.apply(entries, "urban", "", null)

        assertEquals(listOf(3L), filtered.map { it.id })
    }
}
