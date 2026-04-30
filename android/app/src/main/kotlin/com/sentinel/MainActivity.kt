package com.sentinel

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sentinel.data.store.AppPreferences
import com.sentinel.data.webrtc.WebRtcState
import com.sentinel.ui.events.EventsScreen
import com.sentinel.ui.live.LiveScreen
import com.sentinel.ui.live.LiveViewModel
import com.sentinel.ui.live.EditorState
import com.sentinel.ui.onboarding.OnboardingScreen
import com.sentinel.ui.onboarding.WelcomeScreen
import com.sentinel.ui.settings.SettingsScreen
import com.sentinel.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

private const val LANG_PREFS = "sentinel_lang_prefs"
private const val LANG_KEY   = "lang"
private const val LANG_CHOSEN_KEY = "lang_chosen"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* handled silently */ }

    override fun attachBaseContext(newBase: Context) {
        val lang = newBase
            .getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
            .getString(LANG_KEY, "ko") ?: "ko"
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale(lang))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            com.sentinel.ui.theme.SentinelTheme {
                AppContent()
            }
        }
    }
}

private val TOOLBAR_WIDTH = 64.dp
private val ZONE_PANEL_WIDTH = 200.dp

@Composable
private fun AppContent() {
    val onboardingVm: com.sentinel.ui.onboarding.OnboardingViewModel =
        androidx.hilt.navigation.compose.hiltViewModel()
    val onboarded by onboardingVm.onboarded.collectAsStateWithLifecycle()

    // Show nothing until DataStore resolves (avoids flicker)
    if (onboarded == null) return

    if (onboarded == false) {
        val activity = LocalContext.current as Activity
        LaunchedEffect(Unit) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        val langChosen = remember {
            activity.getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
                .getBoolean(LANG_CHOSEN_KEY, false)
        }
        com.sentinel.ui.theme.SentinelTheme {
            if (!langChosen) {
                WelcomeScreen(onSelect = { lang ->
                    activity.getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putString(LANG_KEY, lang)
                        .putBoolean(LANG_CHOSEN_KEY, true)
                        .apply()
                    activity.recreate()
                })
            } else {
                OnboardingScreen(onConnect = onboardingVm::connect)
            }
        }
        return
    }

    MainContent()
}

@Composable
private fun MainContent() {
    val activity = LocalContext.current as Activity
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route
    val isLive = currentRoute == "live" || currentRoute == null

    // Switch orientation based on current screen
    LaunchedEffect(currentRoute) {
        activity.requestedOrientation = when (currentRoute) {
            "events", "settings" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else                 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
    }

    // Immersive mode only in landscape (Live screen)
    if (isLive) ImmersiveMode()

    // Activity-scoped LiveViewModel so toolbar and LiveScreen share the same instance
    val liveVm: LiveViewModel = viewModel(viewModelStoreOwner = activity as ComponentActivity)
    val ui by liveVm.ui.collectAsState()

    var zoneOpen by remember { mutableStateOf(false) }

    if (isLive) {
        // ── Landscape layout: full-screen video with overlay toolbar ────────
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            Row(Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "live",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    composable("live")     { LiveScreen(liveVm) }
                    composable("events")   { EventsScreen() }
                    composable("settings") { SettingsScreen() }
                }
                Spacer(Modifier.width(TOOLBAR_WIDTH))
            }

            // Zone panel slides in from right, overlays video
            AnimatedVisibility(
                visible = zoneOpen,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = TOOLBAR_WIDTH),
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            ) {
                ZonePanel(
                    ui = ui,
                    onStartDrawing = { liveVm.startDrawing() },
                    onFinish    = liveVm::finishDrawing,
                    onCancel    = liveVm::cancelDrawing,
                    onClear     = {
                        liveVm.clearZone()
                        zoneOpen = false
                    },
                    onModeChange = liveVm::setMode,
                    onSave = {
                        liveVm.saveZone()
                        zoneOpen = false
                    },
                    onClose = { zoneOpen = false },
                )
            }

            RightToolbar(
                modifier = Modifier.align(Alignment.CenterEnd),
                connectionState = ui.connectionState,
                zoneOpen = zoneOpen,
                hasZone = ui.vertices.isNotEmpty(),
                onReconnect = liveVm::connect,
                onZoneToggle = { zoneOpen = !zoneOpen },
                onNavigate = { route ->
                    zoneOpen = false
                    navController.navigate(route) {
                        popUpTo("live") { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    } else {
        // ── Portrait layout: normal column with back-to-live header ─────────
        com.sentinel.ui.theme.SentinelTheme {
            Column(Modifier.fillMaxSize().navigationBarsPadding()) {
                // Sticky header — surface extends behind status bar, content sits below it
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            navController.navigate("live") {
                                popUpTo("live") { inclusive = true }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = stringResource(R.string.nav_live),
                            )
                        }
                        Text(
                            text = if (currentRoute == "events")
                                stringResource(R.string.nav_events)
                            else
                                stringResource(R.string.nav_settings),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                // Screen content — NavHost still powers navigation state
                NavHost(
                    navController = navController,
                    startDestination = "live",
                    modifier = Modifier.weight(1f),
                ) {
                    composable("live")     { LiveScreen(liveVm) }
                    composable("events")   { EventsScreen() }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }
}

// ── Right toolbar ─────────────────────────────────────────────────────────────

@Composable
private fun RightToolbar(
    modifier: Modifier = Modifier,
    connectionState: WebRtcState,
    zoneOpen: Boolean,
    hasZone: Boolean,
    onReconnect: () -> Unit,
    onZoneToggle: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .width(TOOLBAR_WIDTH)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.55f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        // Connection status dot
        val dotColor = when (connectionState) {
            WebRtcState.CONNECTED    -> Accent
            WebRtcState.CONNECTING   -> Warning
            WebRtcState.DISCONNECTED -> Danger
            WebRtcState.IDLE         -> TextSecondary
        }
        Box(
            Modifier
                .size(10.dp)
                .background(dotColor, CircleShape)
        )

        // Reconnect button — only when disconnected
        if (connectionState == WebRtcState.DISCONNECTED) {
            ToolbarIconButton(
                icon = Icons.Default.Refresh,
                label = stringResource(R.string.live_reconnect),
                tint = Danger,
                onClick = onReconnect,
            )
        }

        Spacer(Modifier.weight(1f))

        // Zone / polygon button
        ToolbarIconButton(
            icon = Icons.Default.CropFree,
            label = stringResource(R.string.live_draw_zone),
            tint = if (zoneOpen) Primary else if (hasZone) Accent else Color.White,
            selected = zoneOpen,
            onClick = onZoneToggle,
        )

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(8.dp))

        // Events nav
        ToolbarIconButton(
            icon = Icons.Default.List,
            label = stringResource(R.string.nav_events),
            tint = Color.White,
            onClick = { onNavigate("events") },
        )

        // Settings nav
        ToolbarIconButton(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.nav_settings),
            tint = Color.White,
            onClick = { onNavigate("settings") },
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ToolbarIconButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val bg = if (selected) Primary.copy(alpha = 0.25f) else Color.Transparent
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .background(bg, RoundedCornerShape(12.dp)),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// ── Zone control panel ────────────────────────────────────────────────────────

@Composable
private fun ZonePanel(
    ui: com.sentinel.ui.live.LiveUiState,
    onStartDrawing: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
    onModeChange: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
        color = Color.Black.copy(alpha = 0.80f),
        modifier = Modifier
            .width(ZONE_PANEL_WIDTH)
            .fillMaxHeight(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.live_draw_zone),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            // Mode toggle
            Text(stringResource(R.string.live_draw_zone), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("inside" to R.string.zone_mode_inside, "outside" to R.string.zone_mode_outside).forEach { (m, res) ->
                    val sel = ui.zoneMode == m
                    Surface(
                        onClick = { onModeChange(m) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (sel) Primary else Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            stringResource(res),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(6.dp),
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            // Drawing controls
            when (ui.editorState) {
                EditorState.Idle -> {
                    ZonePanelButton(stringResource(R.string.live_draw_zone), Primary) { onStartDrawing() }
                }
                EditorState.Drawing -> {
                    Text(
                        when {
                            ui.vertices.isEmpty() -> stringResource(R.string.live_hint_draw)
                            ui.vertices.size < 3  -> stringResource(R.string.live_hint_min_vertices, ui.vertices.size)
                            else                  -> stringResource(R.string.live_hint_close)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    if (ui.vertices.size >= 3) {
                        ZonePanelButton(stringResource(R.string.live_finish), Accent) { onFinish() }
                    }
                    ZonePanelButton(stringResource(R.string.live_cancel), Color.White.copy(alpha = 0.2f)) { onCancel() }
                }
                EditorState.Editing -> {
                    ZonePanelButton(stringResource(R.string.live_save_zone), Primary) { onSave() }
                    ZonePanelButton(stringResource(R.string.live_redraw), Color.White.copy(alpha = 0.2f)) { onStartDrawing() }
                    ZonePanelButton(stringResource(R.string.live_clear_zone), Danger.copy(alpha = 0.7f)) { onClear() }
                }
            }

            Spacer(Modifier.weight(1f))

            ZonePanelButton(stringResource(R.string.live_cancel), Color.White.copy(alpha = 0.1f)) { onClose() }
        }
    }
}

@Composable
private fun ZonePanelButton(label: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = color,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

// ── Immersive mode (always on — app is landscape only) ────────────────────────

@Composable
private fun ImmersiveMode() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
