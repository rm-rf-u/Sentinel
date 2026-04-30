package com.sentinel.domain

data class SentinelEvent(
    val id: String,
    val type: String,          // zone_violation | prone_position | cry_detected
    val severity: String,      // info | warning | danger
    val timestamp: String,     // UTC ISO-8601
    val confidence: Double,
    val details: EventDetails? = null,
)

data class EventDetails(
    val zone_mode: String? = null,   // inside | outside
    val duration_ms: Int?  = null,
)

fun SentinelEvent.typeLabel(): Int = when (type) {
    "zone_violation"  -> com.sentinel.R.string.event_zone_violation
    "prone_position"  -> com.sentinel.R.string.event_prone_position
    "cry_detected"    -> com.sentinel.R.string.event_cry_detected
    else              -> com.sentinel.R.string.event_zone_violation
}

fun SentinelEvent.severityColor() = when (severity) {
    "danger"  -> com.sentinel.ui.theme.Danger
    "warning" -> com.sentinel.ui.theme.Warning
    else      -> com.sentinel.ui.theme.Accent
}
