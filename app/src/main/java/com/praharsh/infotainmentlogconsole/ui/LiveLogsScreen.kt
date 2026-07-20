package com.praharsh.infotainmentlogconsole.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LiveLogsScreen(state: LogUiState, viewModel: LogConsoleViewModel) {
    val visibleEntries = state.filteredEntries().takeLast(30).reversed()

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
            text = "Native Android collection, cache, and sync diagnostics",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        BrandSelector(state.activeBrandKey, viewModel::selectBrand)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = viewModel::toggleCollection,
                enabled = !state.isLoading
            ) {
                Text(if (state.isCollecting) "Pause" else "Collect")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = viewModel::syncFromApi,
                enabled = !state.isSyncing && !state.isLoading
            ) {
                Text(if (state.isSyncing) "Syncing" else "Sync API")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        StatusMessage(state.infoMessage)
        Spacer(modifier = Modifier.height(10.dp))
        SummaryCards(state.allEntries, visibleEntries.size)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Recent matching logs", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        LogEntryList(
            entries = visibleEntries,
            emptyMessage = "No matching entries. Collect locally or configure API sync.",
            modifier = Modifier.weight(1f)
        )
    }
}
