package com.sentinel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.data.api.SentinelApi
import com.sentinel.data.api.models.SettingsDto
import com.sentinel.data.store.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.URI
import javax.inject.Inject

data class SettingsUiState(
    val serverHost: String = "",
    val serverPort: String = "8000",
    val settings: SettingsDto = SettingsDto(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val savedAt: Long = 0L,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: SentinelApi,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val ui: StateFlow<SettingsUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.baseUrl.collectLatest { url ->
                val (host, port) = parseHostPort(url)
                _ui.update { it.copy(serverHost = host, serverPort = port) }
            }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            runCatching {
                val settings = api.getSettings()
                _ui.update { it.copy(settings = settings, isLoading = false) }
            }.onFailure {
                _ui.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setServerHost(host: String) = _ui.update { it.copy(serverHost = host) }
    fun setServerPort(port: String) = _ui.update { it.copy(serverPort = port) }

    fun setSettings(settings: SettingsDto) = _ui.update { it.copy(settings = settings) }

    fun save() {
        _ui.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                val url = "http://${_ui.value.serverHost.trim()}:${_ui.value.serverPort.trim()}"
                prefs.setBaseUrl(url)
                api.putSettings(_ui.value.settings)
            }
            _ui.update { it.copy(isSaving = false, savedAt = System.currentTimeMillis()) }
        }
    }

    private fun parseHostPort(url: String): Pair<String, String> =
        runCatching {
            val uri = URI(url)
            val host = uri.host ?: ""
            val port = if (uri.port != -1) uri.port.toString() else "8000"
            host to port
        }.getOrDefault("" to "8000")
}
