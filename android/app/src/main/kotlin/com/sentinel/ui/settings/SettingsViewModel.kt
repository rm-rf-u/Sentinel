package com.sentinel.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.data.api.SentinelApi
import com.sentinel.data.api.models.SettingsDto
import com.sentinel.data.store.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val baseUrl: String = "",
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
                _ui.update { it.copy(baseUrl = url) }
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

    fun setBaseUrl(url: String) = _ui.update { it.copy(baseUrl = url) }

    fun setSettings(settings: SettingsDto) = _ui.update { it.copy(settings = settings) }

    fun save() {
        _ui.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                prefs.setBaseUrl(_ui.value.baseUrl)
                api.putSettings(_ui.value.settings)
            }
            _ui.update { it.copy(isSaving = false, savedAt = System.currentTimeMillis()) }
        }
    }
}
