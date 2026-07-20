package com.praharsh.infotainmentlogconsole.domain

import kotlinx.serialization.Serializable

@Serializable
enum class LogLevel {
    INFO,
    WARN,
    ERROR
}

@Serializable
enum class LogSource {
    SIMULATED,
    REMOTE
}

@Serializable
data class SignalLogEntry(
    val id: Long,
    val timestamp: String,
    val brand: String,
    val subsystem: String,
    val signal: String,
    val value: String,
    val level: LogLevel,
    val source: LogSource = LogSource.SIMULATED
)

data class Brand(
    val key: String,
    val label: String
)

object Brands {
    val all = listOf(
        Brand(key = "nordic", label = "NordicDrive"),
        Brand(key = "urban", label = "UrbanMotion")
    )

    fun labelFor(key: String): String =
        all.firstOrNull { it.key == key }?.label ?: all.first().label
}
