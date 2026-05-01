package com.sentinel.data.api.models

data class SafeZoneDto(
    val polygon: List<List<Double>>,
    val mode: String,
    val updated_at: String? = null,
)

data class EventDto(
    val id: String,
    val type: String,
    val severity: String,
    val timestamp: String,
    val confidence: Double,
    val details: EventDetailsDto? = null,
)

data class EventDetailsDto(
    val zone_mode: String? = null,
    val duration_ms: Int? = null,
)

data class SensitivityDto(
    val zone_violation_seconds: Double = 2.0,
    val prone_position_seconds: Double = 5.0,
    val cry_score_threshold: Double = 0.6,
    val cry_window_seconds: Double = 3.0,
    val zone_violation_cooldown_seconds: Double = 0.0,
    val prone_position_cooldown_seconds: Double = 0.0,
    val cry_detected_cooldown_seconds: Double = 0.0,
)

data class QuietHoursDto(
    val enabled: Boolean = false,
    val start: String = "22:00",
    val end: String = "07:00",
)

data class NotificationsDto(val fcm_enabled: Boolean = true)

data class SettingsDto(
    val sensitivity: SensitivityDto = SensitivityDto(),
    val quiet_hours: QuietHoursDto = QuietHoursDto(),
    val notifications: NotificationsDto = NotificationsDto(),
)

data class RegisterDeviceRequest(val fcm_token: String)
data class RegisterDeviceResponse(val device_id: String)

data class WebRtcOfferRequest(val sdp: String, val type: String = "offer")
data class WebRtcAnswerResponse(val sdp: String, val type: String)
