package com.praharsh.infotainmentlogconsole

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appContext = applicationContext
            val consoleViewModel: LogConsoleViewModel = viewModel(
                factory = LogConsoleViewModel.factory(appContext)
            )
            InfotainmentLogConsoleApp(consoleViewModel)
        }
    }
}

private data class BrandProfile(
    val key: String,
    val label: String,
    val primary: Color,
    val secondary: Color
)

enum class LogLevel {
    INFO,
    WARN,
    ERROR
}

data class SignalLogEntry(
    val id: Long,
    val timestamp: String,
    val brand: String,
    val subsystem: String,
    val signal: String,
    val value: String,
    val level: LogLevel
)

private data class LogUiState(
    val activeBrandKey: String = BRAND_PROFILES.first().key,
    val query: String = "",
    val selectedLevel: LogLevel? = null,
    val isCollecting: Boolean = false,
    val allEntries: List<SignalLogEntry> = emptyList(),
    val infoMessage: String = "Load existing logs or start collection.",
    val lastExportName: String? = null
) {
    fun filteredEntries(): List<SignalLogEntry> {
        return LogFilters.apply(allEntries, activeBrandKey, query, selectedLevel)
    }
}

object LogFilters {
    fun apply(
        entries: List<SignalLogEntry>,
        activeBrandKey: String,
        query: String,
        level: LogLevel?
    ): List<SignalLogEntry> {
        val normalizedQuery = query.trim().lowercase()
        return entries.filter { entry ->
            val brandMatches = entry.brand.equals(brandLabel(activeBrandKey), ignoreCase = true)
            val levelMatches = level == null || entry.level == level
            val queryMatches = normalizedQuery.isBlank() || listOf(
                entry.subsystem,
                entry.signal,
                entry.value,
                entry.level.name,
                entry.timestamp
            ).any { token -> token.lowercase().contains(normalizedQuery) }
            brandMatches && levelMatches && queryMatches
        }
    }
}

object LogCodec {
    fun encode(entries: List<SignalLogEntry>): String {
        return entries.joinToString(separator = "\n") { entry ->
            listOf(
                entry.id.toString(),
                entry.timestamp,
                entry.brand,
                entry.subsystem,
                entry.signal,
                entry.value,
                entry.level.name
            ).joinToString(separator = "\t")
        }
    }

    fun decode(payload: String): List<SignalLogEntry> {
        if (payload.isBlank()) {
            return emptyList()
        }

        return payload.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size != 7) {
                    return@mapNotNull null
                }

                runCatching {
                    SignalLogEntry(
                        id = parts[0].toLong(),
                        timestamp = parts[1],
                        brand = parts[2],
                        subsystem = parts[3],
                        signal = parts[4],
                        value = parts[5],
                        level = LogLevel.valueOf(parts[6])
                    )
                }.getOrNull()
            }
            .toList()
    }
}

private object MockSignalGenerator {
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
        val selected = templates.getValue(brand).random()
        val value = selected.values.random()
        val level = when {
            selected.signal == "doorOpen" && value == "1" -> LogLevel.ERROR
            selected.signal == "bluetoothState" && value == "Dropped" -> LogLevel.WARN
            selected.signal == "gpsDrift" && value == "4.6m" -> LogLevel.WARN
            else -> selected.levels.random()
        }

        return SignalLogEntry(
            id = System.currentTimeMillis() + Random.nextLong(0, 999),
            timestamp = formatter.format(Instant.now()),
            brand = brand,
            subsystem = selected.subsystem,
            signal = selected.signal,
            value = value,
            level = level
        )
    }
}

private class LogFileStore(private val context: Context) {
    private val storageFile = File(context.filesDir, "infotainment-log-console.tsv")

    suspend fun load(): List<SignalLogEntry> = withContext(Dispatchers.IO) {
        if (!storageFile.exists()) {
            emptyList()
        } else {
            LogCodec.decode(storageFile.readText())
        }
    }

    suspend fun persist(entries: List<SignalLogEntry>) = withContext(Dispatchers.IO) {
        storageFile.parentFile?.mkdirs()
        storageFile.writeText(LogCodec.encode(entries))
    }

    suspend fun exportVisibleLogs(entries: List<SignalLogEntry>): Pair<Uri, String> = withContext(Dispatchers.IO) {
        val exportDir = File(context.cacheDir, "exports")
        exportDir.mkdirs()
        val fileName = "infotainment-log-export-${System.currentTimeMillis()}.txt"
        val exportFile = File(exportDir, fileName)
        exportFile.writeText(LogCodec.encode(entries))
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
        uri to fileName
    }
}

private class LogConsoleViewModel(
    private val store: LogFileStore
) : ViewModel() {

    var uiState by mutableStateOf(LogUiState())
        private set

    private var collectorJob: Job? = null

    init {
        viewModelScope.launch {
            val entries = store.load()
            uiState = uiState.copy(
                allEntries = entries,
                infoMessage = if (entries.isEmpty()) {
                    "No persisted logs yet. Start collection to generate signal traffic."
                } else {
                    "Loaded ${entries.size} persisted log entries."
                }
            )
        }
    }

    fun selectBrand(brandKey: String) {
        uiState = uiState.copy(
            activeBrandKey = brandKey,
            infoMessage = "Active brand switched to ${brandLabel(brandKey)}."
        )
    }

    fun updateQuery(query: String) {
        uiState = uiState.copy(query = query)
    }

    fun updateLevel(level: LogLevel?) {
        uiState = uiState.copy(selectedLevel = level)
    }

    fun toggleCollection() {
        if (uiState.isCollecting) {
            stopCollection("Collection paused. Existing logs remain stored locally.")
        } else {
            startCollection()
        }
    }

    fun clearLogs() {
        collectorJob?.cancel()
        collectorJob = null
        uiState = uiState.copy(
            isCollecting = false,
            allEntries = emptyList(),
            infoMessage = "Logs cleared. Collection is stopped.",
            lastExportName = null
        )
        viewModelScope.launch {
            store.persist(emptyList())
        }
    }

    suspend fun exportVisibleLogs(context: Context) {
        val visibleEntries = uiState.filteredEntries()
        if (visibleEntries.isEmpty()) {
            uiState = uiState.copy(infoMessage = "Nothing to export for the current filters.")
            return
        }

        val (uri, fileName) = store.exportVisibleLogs(visibleEntries)
        shareUri(context, uri)
        uiState = uiState.copy(
            infoMessage = "Exported ${visibleEntries.size} visible entries.",
            lastExportName = fileName
        )
    }

    private fun startCollection() {
        uiState = uiState.copy(
            isCollecting = true,
            infoMessage = "Collecting ${brandLabel(uiState.activeBrandKey)} signals in the background."
        )

        collectorJob?.cancel()
        collectorJob = viewModelScope.launch {
            while (isActive) {
                val generated = MockSignalGenerator.next(brandLabel(uiState.activeBrandKey))
                val updatedEntries = (uiState.allEntries + generated).takeLast(250)
                uiState = uiState.copy(allEntries = updatedEntries)
                store.persist(updatedEntries)
                delay(1100)
            }
        }
    }

    private fun stopCollection(message: String) {
        collectorJob?.cancel()
        collectorJob = null
        uiState = uiState.copy(
            isCollecting = false,
            infoMessage = message
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LogConsoleViewModel(LogFileStore(context)) as T
                }
            }
        }

        private fun shareUri(context: Context, uri: Uri) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share log export").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
private fun InfotainmentLogConsoleApp(viewModel: LogConsoleViewModel) {
    val context = LocalContext.current
    val activeBrand = BRAND_PROFILES.first { it.key == viewModel.uiState.activeBrandKey }
    MaterialTheme(
        colorScheme = brandColorScheme(activeBrand)
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LogConsoleScreen(viewModel, activeBrand, context)
        }
    }
}

@Composable
private fun LogConsoleScreen(
    viewModel: LogConsoleViewModel,
    activeBrand: BrandProfile,
    context: Context
) {
    val state = viewModel.uiState
    val scope = rememberCoroutineScope()
    val visibleEntries = state.filteredEntries()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Infotainment Log Console",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Android demo for signal logging, filtering, and export.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BRAND_PROFILES.forEach { profile ->
                FilterChip(
                    selected = state.activeBrandKey == profile.key,
                    onClick = { viewModel.selectBrand(profile.key) },
                    label = { Text(profile.label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Collection Controls",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.toggleCollection() }
                ) {
                    Text(if (state.isCollecting) "Pause collection" else "Start collection")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.clearLogs() }
                ) {
                    Text("Clear logs")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            viewModel.exportVisibleLogs(context)
                        }
                    },
                    enabled = visibleEntries.isNotEmpty()
                ) {
                    Text("Export visible logs")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.infoMessage,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.lastExportName?.let { fileName ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Last export: $fileName",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = "Stored",
                value = state.allEntries.size.toString()
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = "Visible",
                value = visibleEntries.size.toString()
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                label = "Errors",
                value = visibleEntries.count { it.level == LogLevel.ERROR }.toString()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search subsystem, signal, value, or timestamp") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.selectedLevel == null,
                onClick = { viewModel.updateLevel(null) },
                label = { Text("All") }
            )
            LogLevel.entries.forEach { level ->
                FilterChip(
                    selected = state.selectedLevel == level,
                    onClick = { viewModel.updateLevel(level) },
                    label = { Text(level.name) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (visibleEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No entries match the current filters.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(visibleEntries, key = { it.id }) { entry ->
                    SignalEntryCard(entry = entry, brand = activeBrand)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SignalEntryCard(
    entry: SignalLogEntry,
    brand: BrandProfile
) {
    val levelColor = when (entry.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> Color(0xFFFFB74D)
        LogLevel.ERROR -> Color(0xFFE57373)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.signal,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = entry.level.name,
                    color = levelColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${entry.brand} · ${entry.subsystem}", color = brand.secondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Value: ${entry.value}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = entry.timestamp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

private fun brandLabel(brandKey: String): String {
    return BRAND_PROFILES.firstOrNull { it.key == brandKey }?.label ?: BRAND_PROFILES.first().label
}

private fun brandColorScheme(brand: BrandProfile): ColorScheme {
    return darkColorScheme(
        primary = brand.primary,
        secondary = brand.secondary,
        tertiary = brand.primary.copy(alpha = 0.75f),
        background = Color(0xFF08111F),
        surface = Color(0xFF101B2F),
        surfaceVariant = Color(0xFF162338),
        onPrimary = Color(0xFF08111F),
        onSecondary = Color(0xFF08111F)
    )
}

private val BRAND_PROFILES = listOf(
    BrandProfile("nordic", "NordicDrive", Color(0xFF3ED39F), Color(0xFF7DD7BA)),
    BrandProfile("urban", "UrbanMotion", Color(0xFF7AA5FF), Color(0xFFA9C0FF))
)
