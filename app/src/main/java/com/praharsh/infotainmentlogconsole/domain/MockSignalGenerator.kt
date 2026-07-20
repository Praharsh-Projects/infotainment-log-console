package com.praharsh.infotainmentlogconsole.domain

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class MockSignalGenerator(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val random: Random = Random.Default
) {
    private data class Template(
        val subsystem: String,
        val signal: String,
        val values: List<String>,
        val levels: List<LogLevel>
    )

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    private val templates = mapOf(
        "NordicDrive" to listOf(
            Template("Media", "mediaVolume", listOf("10", "12", "15"), listOf(LogLevel.INFO)),
            Template("Connectivity", "lteSignal", listOf("-83dBm", "-79dBm", "-91dBm"), listOf(LogLevel.INFO, LogLevel.WARN)),
            Template("Climate", "cabinTemp", listOf("19.5C", "20.0C", "21.0C"), listOf(LogLevel.INFO)),
            Template("Navigation", "gpsDrift", listOf("0.8m", "2.1m", "4.6m"), listOf(LogLevel.INFO, LogLevel.WARN))
        ),
        "UrbanMotion" to listOf(
            Template("Vehicle", "speedKph", listOf("42", "55", "68"), listOf(LogLevel.INFO)),
            Template("Vehicle", "doorOpen", listOf("0", "1"), listOf(LogLevel.INFO, LogLevel.ERROR)),
            Template("Power", "batteryLoad", listOf("38%", "43%", "51%"), listOf(LogLevel.INFO)),
            Template("Connectivity", "bluetoothState", listOf("Connected", "Pairing", "Dropped"), listOf(LogLevel.INFO, LogLevel.WARN))
        )
    )

    fun next(brand: String): SignalLogEntry {
        val selected = templates.getValue(brand).random(random)
        val value = selected.values.random(random)
        val level = when {
            selected.signal == "doorOpen" && value == "1" -> LogLevel.ERROR
            selected.signal == "bluetoothState" && value == "Dropped" -> LogLevel.WARN
            selected.signal == "gpsDrift" && value == "4.6m" -> LogLevel.WARN
            else -> selected.levels.random(random)
        }
        val now = Instant.now(clock)

        return SignalLogEntry(
            id = now.toEpochMilli() * 1_000 + random.nextInt(0, 1_000),
            timestamp = formatter.format(now),
            brand = brand,
            subsystem = selected.subsystem,
            signal = selected.signal,
            value = value,
            level = level,
            source = LogSource.SIMULATED
        )
    }
}
