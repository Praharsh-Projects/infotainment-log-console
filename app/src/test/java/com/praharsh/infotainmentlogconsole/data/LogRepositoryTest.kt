package com.praharsh.infotainmentlogconsole.data

import com.praharsh.infotainmentlogconsole.domain.LogLevel
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LogRepositoryTest {
    @Test
    fun mergeDeduplicatesIdsAndKeepsLatestIncomingValue() {
        val cached = listOf(entry(1, "old"), entry(2, "keep"))
        val incoming = listOf(entry(1, "new"), entry(3, "remote"))

        val merged = mergeLogs(cached, incoming)

        assertEquals(listOf(1L, 2L, 3L), merged.map { it.id })
        assertEquals("new", merged.first { it.id == 1L }.value)
    }

    @Test
    fun mergeHonorsConfiguredLimit() {
        val merged = mergeLogs(emptyList(), (1L..10L).map { entry(it, "$it") }, limit = 3)

        assertEquals(listOf(8L, 9L, 10L), merged.map { it.id })
    }

    private fun entry(id: Long, value: String) = SignalLogEntry(
        id,
        "2026-06-24 10:00:${id.toString().padStart(2, '0')}",
        "NordicDrive",
        "Media",
        "mediaVolume",
        value,
        LogLevel.INFO
    )
}
