package com.praharsh.infotainmentlogconsole.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun SavedLogsScreen(state: LogUiState, viewModel: LogConsoleViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val visibleEntries = state.filteredEntries().reversed()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved logs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Search, filter, and export the bounded JSON cache",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search subsystem, signal, value, source, or time") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        LevelFilters(state.selectedLevel, viewModel::updateLevel)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        viewModel.exportVisibleLogs()?.let { file ->
                            shareJsonExport(context, file)
                        }
                    }
                },
                enabled = visibleEntries.isNotEmpty()
            ) {
                Text("Export JSON")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = viewModel::clearLogs,
                enabled = state.allEntries.isNotEmpty()
            ) {
                Text("Clear cache")
            }
        }
        state.lastExportName?.let { name ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last export: $name",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LogEntryList(
            entries = visibleEntries,
            emptyMessage = "No entries match the saved-log filters.",
            modifier = Modifier.weight(1f)
        )
    }
}
