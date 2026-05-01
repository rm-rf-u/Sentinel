package com.sentinel.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sentinel.R
import com.sentinel.ui.theme.*

@Composable
fun WelcomeScreen(onSelect: (lang: String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(68.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Sentinel",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(72.dp))

        // Language buttons — labels are hardcoded in their own script
        Button(
            onClick = { onSelect("ko") },
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("한국어", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onSelect("en") },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, Primary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("English", style = MaterialTheme.typography.labelLarge)
        }
    }
}
