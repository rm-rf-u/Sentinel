package com.sentinel.domain

data class SafeZone(
    val polygon: List<List<Double>>,   // [[x, y], ...] normalized [0,1]
    val mode: String,                  // inside | outside
    val updated_at: String? = null,
)
