package com.praharsh.infotainmentlogconsole.domain

object LogFilters {
    fun apply(
        entries: List<SignalLogEntry>,
        activeBrandKey: String,
        query: String,
        level: LogLevel?
    ): List<SignalLogEntry> {
        val normalizedQuery = query.trim().lowercase()
        val activeBrand = Brands.labelFor(activeBrandKey)

        return entries.filter { entry ->
            val brandMatches = entry.brand.equals(activeBrand, ignoreCase = true)
            val levelMatches = level == null || entry.level == level
            val queryMatches = normalizedQuery.isBlank() || listOf(
                entry.subsystem,
                entry.signal,
                entry.value,
                entry.level.name,
                entry.source.name,
                entry.timestamp
            ).any { token -> token.lowercase().contains(normalizedQuery) }

            brandMatches && levelMatches && queryMatches
        }
    }
}
