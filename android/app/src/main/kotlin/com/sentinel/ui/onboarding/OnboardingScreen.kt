package com.sentinel.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sentinel.R
import com.sentinel.ui.theme.*

@Composable
fun OnboardingScreen(onConnect: (host: String, port: String) -> Unit) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8000") }
    val portValid = port.toIntOrNull()?.let { it in 1..65535 } ?: false
    val canConnect = host.isNotBlank() && portValid
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Disclaimer
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Warning.copy(alpha = 0.10f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = Warning,
                )
                Text(
                    stringResource(R.string.disclaimer_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Server settings card
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.settings_server),
                    style = MaterialTheme.typography.labelMedium,
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.onboarding_host_label), style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        placeholder = {
                            Text(
                                stringResource(R.string.onboarding_host_hint),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.onboarding_port_label), style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (canConnect) onConnect(host, port) }),
                        modifier = Modifier.width(120.dp),
                    )
                }
            }
        }

        Button(
            onClick = { onConnect(host, port) },
            enabled = canConnect,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.onboarding_connect), style = MaterialTheme.typography.labelLarge)
        }
    }
}
