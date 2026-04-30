package com.sentinel.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentinel.data.api.SentinelApi
import com.sentinel.data.api.models.EventDto
import com.sentinel.data.ws.EventWebSocket
import com.sentinel.domain.EventDetails
import com.sentinel.domain.SentinelEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val api: SentinelApi,
    private val ws: EventWebSocket,
) : ViewModel() {

    private val _events = MutableStateFlow<List<SentinelEvent>>(emptyList())
    val events: StateFlow<List<SentinelEvent>> = _events.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var oldestTimestamp: String? = null

    init {
        loadInitial()
        viewModelScope.launch {
            ws.events.collect { event ->
                _events.update { listOf(event) + it }
            }
        }
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _isLoading.value = true
            runCatching {
                val data = api.getEvents(limit = 50).map { it.toDomain() }
                _events.value = data
                _hasMore.value = data.size == 50
                oldestTimestamp = data.lastOrNull()?.timestamp
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        val before = oldestTimestamp ?: return
        viewModelScope.launch {
            runCatching {
                val more = api.getEvents(limit = 50, before = before).map { it.toDomain() }
                _events.update { it + more }
                _hasMore.value = more.size == 50
                oldestTimestamp = more.lastOrNull()?.timestamp
            }
        }
    }
}

private fun EventDto.toDomain() = SentinelEvent(
    id = id,
    type = type,
    severity = severity,
    timestamp = timestamp,
    confidence = confidence,
    details = details?.let { EventDetails(zone_mode = it.zone_mode, duration_ms = it.duration_ms) },
)
