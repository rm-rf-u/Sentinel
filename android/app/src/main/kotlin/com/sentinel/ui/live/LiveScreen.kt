package com.sentinel.ui.live

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.sentinel.data.webrtc.WebRtcState
import com.sentinel.ui.theme.Primary
import org.webrtc.SurfaceViewRenderer

/**
 * Full-screen video feed with polygon canvas overlay.
 * All controls live in the right-side toolbar in AppContent.
 */
@Composable
fun LiveScreen(vm: LiveViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsState()
    val density = LocalDensity.current
    var rendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    LaunchedEffect(Unit) { vm.connect() }
    DisposableEffect(Unit) { onDispose { vm.disconnect() } }

    LaunchedEffect(rendererRef) {
        vm.webRtcClient.onVideoTrack = { track ->
            rendererRef?.let { track.addSink(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { size ->
                with(density) {
                    vm.onCanvasSize(size.width.toFloat(), size.height.toFloat())
                }
            }
    ) {
        // Video
        AndroidView(
            factory = { ctx ->
                SurfaceViewRenderer(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    init(vm.webRtcClient.eglContext, null)
                    rendererRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Connecting spinner
        if (ui.connectionState == WebRtcState.CONNECTING) {
            CircularProgressIndicator(
                color = Primary,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Polygon canvas overlay (always rendered; interactive when editorState != Idle)
        PolygonCanvasOverlay(
            modifier = Modifier.fillMaxSize(),
            editorState = ui.editorState,
            vertices = ui.vertices,
            onVerticesChange = vm::updateVertices,
            onStateChange = vm::updateEditorState,
        )
    }
}
