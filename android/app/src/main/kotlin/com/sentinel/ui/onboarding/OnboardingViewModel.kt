package com.sentinel.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.sentinel.data.api.SentinelApi
import com.sentinel.data.api.models.RegisterDeviceRequest
import com.sentinel.data.store.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val api: SentinelApi,
) : ViewModel() {

    val onboarded = prefs.onboarded.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null, // null = loading
    )

    fun connect(host: String, port: String) {
        val url = "http://${host.trim()}:${port.trim()}"
        viewModelScope.launch {
            prefs.setBaseUrl(url)
            registerFcmToken()
        }
    }

    fun refreshFcmToken() {
        viewModelScope.launch { registerFcmToken() }
    }

    private suspend fun registerFcmToken() {
        try {
            val token = withContext(Dispatchers.IO) {
                Tasks.await(FirebaseMessaging.getInstance().token)
            }
            api.registerDevice(RegisterDeviceRequest(fcm_token = token))
        } catch (_: Exception) { }
    }
}
