package com.praharsh.infotainmentlogconsole.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praharsh.infotainmentlogconsole.domain.Brands
import com.praharsh.infotainmentlogconsole.domain.LogLevel
import com.praharsh.infotainmentlogconsole.domain.SignalLogEntry

@Composable
fun BrandSelector(
    activeBrandKey: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Brands.all.forEach { brand ->
            FilterChip(
                selected = activeBrandKey == brand.key,
                onClick = { onSelect(brand.key) },
                label = { Text(brand.label) }
            )
        }
    }
}

@Composable
fun LevelFilters(
    selectedLevel: LogLevel?,
    onSelect: (LogLevel?) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilterChip(
            selected = selectedLevel == null,
            onClick = { onSelect(null) },
            label = { Text("All") }
        )
        LogLevel.entries.forEach { level ->
            FilterChip(
                selected = selectedLevel == level,
                onClick = { onSelect(level) },
                label = { Text(level.name) }
            )
        }
    }
}

@Composable
fun StatusMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SummaryCards(entries: List<SignalLogEntry>, visibleCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Stored",
            value = entries.size.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Visible",
            value = visibleCount.toString()
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            label = "Errors",
            value = entries.count { it.level == LogLevel.ERROR }.toString()
        )
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LogEntryList(
    entries: List<SignalLogEntry>,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = emptyMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                SignalEntryCard(entry)
            }
        }
    }
}

@Composable
private fun SignalEntryCard(entry: SignalLogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARN -> Color(0xFFFFB74D)
        LogLevel.ERROR -> Color(0xFFE57373)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = entry.signal, fontWeight = FontWeight.SemiBold)
                Text(text = entry.level.name, color = levelColor, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${entry.brand} - ${entry.subsystem} - ${entry.source.name}",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 13.sp
            )
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
