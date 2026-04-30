package com.sentinel.ui.live

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.data.api.SentinelApi
import com.sentinel.data.api.models.SafeZoneDto
import com.sentinel.data.store.AppPreferences
import com.sentinel.data.webrtc.WebRtcClient
import com.sentinel.data.webrtc.WebRtcState
import com.sentinel.data.ws.EventWebSocket
import com.sentinel.domain.SafeZone
import com.sentinel.domain.SentinelEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EditorState { Idle, Drawing, Editing }

data class LiveUiState(
    val connectionState: WebRtcState = WebRtcState.IDLE,
    val editorState: EditorState = EditorState.Idle,
    val vertices: List<CanvasVertex> = emptyList(),
    val zoneMode: String = "inside",
    val isSaving: Boolean = false,
    val recentEvent: SentinelEvent? = null,
)

@HiltViewModel
class LiveViewModel @Inject constructor(
    val webRtcClient: WebRtcClient,
    private val eventWs: EventWebSocket,
    private val api: SentinelApi,
    private val prefs: AppPreferences,
    @ApplicationContext context: Context,
) : ViewModel() {

    private val _ui = MutableStateFlow(LiveUiState())
    val ui: StateFlow<LiveUiState> = _ui.asStateFlow()

    private var canvasW = 1f
    private var canvasH = 1f

    init {
        webRtcClient.init()
        webRtcClient.onStateChange = { state ->
            _ui.update { it.copy(connectionState = state) }
        }

        viewModelScope.launch { loadSafeZone() }
        viewModelScope.launch {
            prefs.baseUrl.collectLatest { url ->
                eventWs.connect(url)
            }
        }
        viewModelScope.launch {
            eventWs.events.collect { event ->
                _ui.update { it.copy(recentEvent = event) }
            }
        }
    }

    fun onCanvasSize(w: Float, h: Float) {
        canvasW = w
        canvasH = h
    }

    fun connect() = webRtcClient.connect()
    fun disconnect() = webRtcClient.disconnect()

    fun startDrawing() = _ui.update { it.copy(editorState = EditorState.Drawing, vertices = emptyList()) }
    fun cancelDrawing() = _ui.update { it.copy(editorState = if (it.vertices.size >= 3) EditorState.Editing else EditorState.Idle) }
    fun clearZone()     = _ui.update { it.copy(editorState = EditorState.Idle, vertices = emptyList()) }
    fun finishDrawing() {
        if (_ui.value.vertices.size >= 3) _ui.update { it.copy(editorState = EditorState.Editing) }
    }
    fun updateVertices(v: List<CanvasVertex>) = _ui.update { it.copy(vertices = v) }
    fun updateEditorState(s: EditorState) = _ui.update { it.copy(editorState = s) }
    fun setMode(mode: String) = _ui.update { it.copy(zoneMode = mode) }

    fun saveZone() {
        val st = _ui.value
        if (st.vertices.size < 3) return
        _ui.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                val polygon = st.vertices.map { it.toNorm(canvasW, canvasH) }
                api.putSafeZone(SafeZoneDto(polygon = polygon, mode = st.zoneMode))
            }
            _ui.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun loadSafeZone() {
        runCatching {
            val zone = api.getSafeZone()
            val vertices = zone.polygon.map { it.toCanvas(canvasW, canvasH) }
            _ui.update { it.copy(vertices = vertices, zoneMode = zone.mode, editorState = EditorState.Editing) }
        }
    }

    override fun onCleared() {
        webRtcClient.release()
        eventWs.disconnect()
    }
}
