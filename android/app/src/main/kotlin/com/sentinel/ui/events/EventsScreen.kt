package com.sentinel.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sentinel.R
import com.sentinel.domain.SentinelEvent
import com.sentinel.domain.severityColor
import com.sentinel.domain.typeLabel
import com.sentinel.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EventsScreen(vm: EventsViewModel = hiltViewModel()) {
    val events by vm.events.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val hasMore by vm.hasMore.collectAsState()
    val config = LocalConfiguration.current
    val isTablet = minOf(config.screenWidthDp, config.screenHeightDp) >= 600
    val hPad = if (isTablet) 32.dp else 16.dp

    var filter by remember { mutableStateOf<String?>(null) }
    val filtered = if (filter == null) events else events.filter { it.type == filter }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.events_clear_title)) },
            text = { Text(stringResource(R.string.events_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearLogs()
                    showClearDialog = false
                }) { Text(stringResource(R.string.events_clear_ok), color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.events_clear_cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = hPad, vertical = 16.dp)
            .widthIn(max = if (isTablet) 900.dp else Int.MAX_VALUE.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Title + clear button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.nav_events),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = { showClearDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.events_clear_title), tint = Danger)
            }
        }

        // Filter bar — full-width boxed chips
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(null, filter, stringResource(R.string.filter_all), Modifier.weight(1f)) { filter = it }
                FilterChip("zone_violation", filter, stringResource(R.string.filter_zone), Modifier.weight(1f)) { filter = it }
                FilterChip("prone_position", filter, stringResource(R.string.filter_prone), Modifier.weight(1f)) { filter = it }
                FilterChip("cry_detected", filter, stringResource(R.string.filter_cry), Modifier.weight(1f)) { filter = it }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.padding(32.dp))
            }
        } else if (filtered.isEmpty()) {
            EmptyState()
        } else {
            val listState = rememberLazyListState()
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { EventCard(it) }
                if (hasMore) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = vm::loadMore) {
                                Text(stringResource(R.string.events_load_more), color = Primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    value: String?,
    current: String?,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (String?) -> Unit,
) {
    val selected = current == value
    Surface(
        onClick = { onClick(value) },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Primary else Color.Transparent,
    ) {
        Text(
            label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (selected) Color.White else TextSecondary,
        )
    }
}

@Composable
fun EventCard(event: SentinelEvent) {
    val color = event.severityColor()
    val confidence = (event.confidence * 100).toInt()

    Surface(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Severity bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(event.typeLabel()), style = MaterialTheme.typography.bodyMedium)
                    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
                        Text(
                            stringResource(R.string.event_confidence, confidence),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                        )
                    }
                }
                Text(formatTimestamp(event.timestamp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.events_empty), style = MaterialTheme.typography.bodyMedium)
    }
}

private fun formatTimestamp(iso: String): String = runCatching {
    val instant = Instant.parse(iso)
    DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss")
        .withZone(ZoneId.systemDefault())
        .format(instant)
}.getOrDefault(iso)
