package com.praharsh.infotainmentlogconsole.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    state: LogUiState,
    apiBaseUrl: String,
    viewModel: LogConsoleViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "API & session",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Environment configuration and authenticated REST sync",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Configured API base URL", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = apiBaseUrl, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Debug and release endpoints are supplied through Gradle properties.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
            value = state.sessionToken,
            onValueChange = viewModel::updateSessionToken,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Bearer session token") },
            supportingText = { Text("Kept in process memory only; never written to the log cache.") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = viewModel::syncFromApi,
            enabled = state.sessionToken.isNotBlank() && !state.isSyncing
        ) {
            Text(if (state.isSyncing) "Syncing" else "Sync selected brand")
        }
        Spacer(modifier = Modifier.height(12.dp))
        StatusMessage(state.infoMessage)
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = "Release boundary", fontWeight = FontWeight.SemiBold)
        Text(
            text = "Production credentials, signing keys, and store publication are intentionally external to the repository.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
