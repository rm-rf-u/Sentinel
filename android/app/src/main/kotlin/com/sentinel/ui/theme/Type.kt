package com.sentinel.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SentinelTypography = Typography(
    titleLarge  = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary),
    bodyMedium  = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 14.sp, color = TextPrimary),
    bodySmall   = TextStyle(fontWeight = FontWeight.Normal,   fontSize = 12.sp, color = TextSecondary),
    labelSmall  = TextStyle(fontWeight = FontWeight.Medium,   fontSize = 11.sp, color = TextSecondary),
)
