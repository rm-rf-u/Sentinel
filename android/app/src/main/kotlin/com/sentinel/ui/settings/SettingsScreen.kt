package com.sentinel.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sentinel.R
import com.sentinel.data.api.models.*
import com.sentinel.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val s = ui.settings
    val config = LocalConfiguration.current
    val isTablet = minOf(config.screenWidthDp, config.screenHeightDp) >= 600
    val hPad = if (isTablet) 32.dp else 16.dp

    val saved = remember(ui.savedAt) { ui.savedAt > 0 && (System.currentTimeMillis() - ui.savedAt) < 2000 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = hPad, vertical = 16.dp)
            .widthIn(max = if (isTablet) 700.dp else Int.MAX_VALUE.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Server connection
        SettingsSection(stringResource(R.string.settings_server)) {
            OutlinedTextField(
                value = ui.serverHost,
                onValueChange = vm::setServerHost,
                label = { Text(stringResource(R.string.settings_server_address), style = MaterialTheme.typography.labelSmall) },
                placeholder = { Text(stringResource(R.string.settings_server_address_hint), style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = ui.serverPort,
                onValueChange = vm::setServerPort,
                label = { Text(stringResource(R.string.settings_server_port), style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.width(120.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }

        // Sensitivity
        SettingsSection(stringResource(R.string.settings_sensitivity)) {
            SliderRow(
                label = stringResource(R.string.settings_zone_seconds),
                value = s.sensitivity.zone_violation_seconds.toFloat(),
                min = 0.5f, max = 30f, step = 0.5f,
                display = { "${it.roundToDecimal(1)}${stringResource(R.string.settings_seconds_unit)}" },
            ) { v -> vm.setSettings(s.copy(sensitivity = s.sensitivity.copy(zone_violation_seconds = v.toDouble()))) }

            SliderRow(
                label = stringResource(R.string.settings_prone_seconds),
                value = s.sensitivity.prone_position_seconds.toFloat(),
                min = 1f, max = 60f, step = 1f,
                display = { "${it.toInt()}${stringResource(R.string.settings_seconds_unit)}" },
            ) { v -> vm.setSettings(s.copy(sensitivity = s.sensitivity.copy(prone_position_seconds = v.toDouble()))) }

            SliderRow(
                label = stringResource(R.string.settings_cry_threshold),
                value = s.sensitivity.cry_score_threshold.toFloat(),
                min = 0.1f, max = 1f, step = 0.05f,
                display = { "${(it * 100).roundToInt()}%" },
            ) { v -> vm.setSettings(s.copy(sensitivity = s.sensitivity.copy(cry_score_threshold = v.toDouble()))) }

            SliderRow(
                label = stringResource(R.string.settings_cry_window),
                value = s.sensitivity.cry_window_seconds.toFloat(),
                min = 1f, max = 10f, step = 0.5f,
                display = { "${it.roundToDecimal(1)}${stringResource(R.string.settings_seconds_unit)}" },
            ) { v -> vm.setSettings(s.copy(sensitivity = s.sensitivity.copy(cry_window_seconds = v.toDouble()))) }
        }

        // Quiet hours
        SettingsSection(stringResource(R.string.settings_quiet_hours)) {
            ToggleRow(
                label = stringResource(R.string.settings_quiet_hours_enable),
                checked = s.quiet_hours.enabled,
            ) { v -> vm.setSettings(s.copy(quiet_hours = s.quiet_hours.copy(enabled = v))) }

            if (s.quiet_hours.enabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    TimeField(
                        label = stringResource(R.string.settings_quiet_start),
                        value = s.quiet_hours.start,
                        modifier = Modifier.weight(1f),
                    ) { v -> vm.setSettings(s.copy(quiet_hours = s.quiet_hours.copy(start = v))) }
                    TimeField(
                        label = stringResource(R.string.settings_quiet_end),
                        value = s.quiet_hours.end,
                        modifier = Modifier.weight(1f),
                    ) { v -> vm.setSettings(s.copy(quiet_hours = s.quiet_hours.copy(end = v))) }
                }
            }
        }

        // Notifications
        SettingsSection(stringResource(R.string.settings_notifications)) {
            ToggleRow(
                label = stringResource(R.string.settings_fcm_enabled),
                checked = s.notifications.fcm_enabled,
            ) { v -> vm.setSettings(s.copy(notifications = NotificationsDto(fcm_enabled = v))) }
        }

        // Save
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = vm::save,
                enabled = !ui.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
            ) { Text(if (ui.isSaving) "저장 중…" else stringResource(R.string.settings_save)) }

            if (saved) {
                Text(stringResource(R.string.settings_saved) + " ✓", color = Accent,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            content()
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    step: Float,
    display: @Composable (Float) -> String,
    onChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(display(value), style = MaterialTheme.typography.bodySmall, color = PrimaryDeep)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            steps = ((max - min) / step).toInt() - 1,
            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary),
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary),
        )
    }
}

@Composable
private fun TimeField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
    }
}

private fun Float.roundToDecimal(decimals: Int): String {
    val factor = Math.pow(10.0, decimals.toDouble())
    return ((this * factor).roundToInt() / factor).toString()
}
