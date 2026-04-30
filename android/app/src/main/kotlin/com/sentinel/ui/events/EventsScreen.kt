package com.sentinel.ui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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
import java.time.temporal.ChronoUnit

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = hPad, vertical = 16.dp)
            .widthIn(max = if (isTablet) 900.dp else Int.MAX_VALUE.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(null, filter, stringResource(R.string.filter_all)) { filter = it }
            FilterChip("zone_violation", filter, stringResource(R.string.filter_zone)) { filter = it }
            FilterChip("prone_position", filter, stringResource(R.string.filter_prone)) { filter = it }
            FilterChip("cry_detected", filter, stringResource(R.string.filter_cry)) { filter = it }
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
private fun FilterChip(value: String?, current: String?, label: String, onClick: (String?) -> Unit) {
    val selected = current == value
    Surface(
        onClick = { onClick(value) },
        shape = RoundedCornerShape(50),
        color = if (selected) Primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
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
                Text(relativeTime(event.timestamp), style = MaterialTheme.typography.bodySmall)
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

private fun relativeTime(iso: String): String {
    return runCatching {
        val then = Instant.parse(iso)
        val now = Instant.now()
        val diffS = ChronoUnit.SECONDS.between(then, now)
        when {
            diffS < 60   -> "방금 전"
            diffS < 3600 -> "${diffS / 60}분 전"
            diffS < 86400 -> "${diffS / 3600}시간 전"
            else -> DateTimeFormatter.ofPattern("M월 d일 HH:mm")
                .withZone(ZoneId.systemDefault()).format(then)
        }
    }.getOrDefault(iso)
}
