package com.praharsh.infotainmentlogconsole.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.praharsh.infotainmentlogconsole.data.LogRepository
import com.praharsh.infotainmentlogconsole.data.mergeLogs
import com.praharsh.infotainmentlogconsole.domain.Brands
import com.praharsh.infotainmentlogconsole.domain.LogFilters
import com.praharsh.infotainmentlogconsole.domain.LogLevel
import com.praharsh.infotainmentlogconsole.domain.MockSignalGenerator
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class LogUiState(
    val activeBrandKey: String = Brands.all.first().key,
    val query: String = "",
    val selectedLevel: LogLevel? = null,
    val isCollecting: Boolean = false,
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val allEntries: List<SignalLogEntry> = emptyList(),
    val sessionToken: String = "",
    val infoMessage: String = "Loading cached logs.",
    val lastExportName: String? = null
) {
    fun filteredEntries(): List<SignalLogEntry> = LogFilters.apply(
        entries = allEntries,
        activeBrandKey = activeBrandKey,
        query = query,
        level = selectedLevel
    )
}

class LogConsoleViewModel(
    private val repository: LogRepository,
    private val generator: MockSignalGenerator
) : ViewModel() {
    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    private var collectorJob: Job? = null
    private var syncJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching { repository.loadCachedLogs() }
                .onSuccess { entries ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            allEntries = entries,
                            infoMessage = if (entries.isEmpty()) {
                                "No cached logs yet. Start collection or sync from the API."
                            } else {
                                "Loaded ${entries.size} cached log entries."
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            infoMessage = "Could not load the local cache: ${safeMessage(error)}"
                        )
                    }
                }
        }
    }

    fun selectBrand(brandKey: String) {
        if (Brands.all.none { it.key == brandKey }) return
        _uiState.update { state ->
            state.copy(
                activeBrandKey = brandKey,
                infoMessage = "Active brand switched to ${Brands.labelFor(brandKey)}."
            )
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query.take(MAX_QUERY_LENGTH)) }
    }

    fun updateLevel(level: LogLevel?) {
        _uiState.update { it.copy(selectedLevel = level) }
    }

    fun updateSessionToken(token: String) {
        _uiState.update { it.copy(sessionToken = token.take(MAX_TOKEN_INPUT_LENGTH)) }
    }

    fun toggleCollection() {
        if (_uiState.value.isCollecting) {
            stopCollection("Collection paused. Cached logs remain available.")
        } else {
            startCollection()
        }
    }

    fun clearLogs() {
        collectorJob?.cancel()
        collectorJob = null
        _uiState.update { state ->
            state.copy(
                isCollecting = false,
                allEntries = emptyList(),
                infoMessage = "Logs cleared and collection stopped.",
                lastExportName = null
            )
        }
        viewModelScope.launch {
            runCatching { repository.persistLogs(emptyList()) }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(infoMessage = "Could not clear the local cache: ${safeMessage(error)}")
                    }
                }
        }
    }

    fun syncFromApi() {
        val snapshot = _uiState.value
        if (snapshot.sessionToken.isBlank()) {
            _uiState.update {
                it.copy(infoMessage = "Enter a session token in Settings before syncing.")
            }
            return
        }
        if (syncJob?.isActive == true) return

        syncJob = viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, infoMessage = "Syncing authenticated logs.") }
            runCatching {
                repository.fetchRemoteLogs(
                    brand = Brands.labelFor(snapshot.activeBrandKey),
                    bearerToken = snapshot.sessionToken
                )
            }.onSuccess { remoteEntries ->
                val merged = mergeLogs(_uiState.value.allEntries, remoteEntries)
                runCatching { repository.persistLogs(merged) }
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                isSyncing = false,
                                allEntries = merged,
                                infoMessage = "Synced ${remoteEntries.size} remote log entries."
                            )
                        }
                    }
                    .onFailure { error ->
                        _uiState.update { state ->
                            state.copy(
                                isSyncing = false,
                                infoMessage = "Remote data arrived but caching failed: ${safeMessage(error)}"
                            )
                        }
                    }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isSyncing = false,
                        infoMessage = safeMessage(error)
                    )
                }
            }
        }
    }

    suspend fun exportVisibleLogs(): File? {
        val visibleEntries = _uiState.value.filteredEntries()
        if (visibleEntries.isEmpty()) {
            _uiState.update { it.copy(infoMessage = "Nothing matches the current export filters.") }
            return null
        }

        return runCatching { repository.exportLogs(visibleEntries) }
            .onSuccess { file ->
                _uiState.update { state ->
                    state.copy(
                        infoMessage = "Exported ${visibleEntries.size} visible entries as JSON.",
                        lastExportName = file.name
                    )
                }
            }
            .onFailure { error ->
                _uiState.update { state ->
                    state.copy(infoMessage = "Export failed: ${safeMessage(error)}")
                }
            }
            .getOrNull()
    }

    private fun startCollection() {
        if (collectorJob?.isActive == true) return
        _uiState.update { state ->
            state.copy(
                isCollecting = true,
                infoMessage = "Collecting ${Brands.labelFor(state.activeBrandKey)} signals."
            )
        }

        collectorJob = viewModelScope.launch {
            while (isActive) {
                val state = _uiState.value
                val generated = generator.next(Brands.labelFor(state.activeBrandKey))
                val updatedEntries = mergeLogs(state.allEntries, listOf(generated))
                runCatching { repository.persistLogs(updatedEntries) }
                    .onSuccess {
                        _uiState.update { current -> current.copy(allEntries = updatedEntries) }
                    }
                    .onFailure { error ->
                        stopCollection("Collection stopped because caching failed: ${safeMessage(error)}")
                    }
                delay(COLLECTION_INTERVAL_MS)
            }
        }
    }

    private fun stopCollection(message: String) {
        collectorJob?.cancel()
        collectorJob = null
        _uiState.update { it.copy(isCollecting = false, infoMessage = message) }
    }

    private fun safeMessage(error: Throwable): String =
        error.message?.replace(Regex("[\\r\\n]+"), " ")?.take(MAX_ERROR_LENGTH)
            ?: "Unexpected mobile client error."

    companion object {
        private const val COLLECTION_INTERVAL_MS = 1_100L
        private const val MAX_QUERY_LENGTH = 120
        private const val MAX_TOKEN_INPUT_LENGTH = 2_048
        private const val MAX_ERROR_LENGTH = 180

        fun factory(
            repository: LogRepository,
            generator: MockSignalGenerator
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(LogConsoleViewModel::class.java))
                return LogConsoleViewModel(repository, generator) as T
            }
        }
    }
}
