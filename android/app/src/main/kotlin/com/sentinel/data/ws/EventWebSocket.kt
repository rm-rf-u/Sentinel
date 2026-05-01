package com.sentinel.data.ws

import com.google.gson.Gson
import com.sentinel.domain.SentinelEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventWebSocket @Inject constructor(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
) {
    private val _events = MutableSharedFlow<SentinelEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SentinelEvent> = _events

    private var ws: WebSocket? = null

    fun connect(baseUrl: String) {
        ws?.cancel()
        runCatching {
            val wsUrl = baseUrl.replace(Regex("^http"), "ws") + "/ws/events"
            val request = Request.Builder().url(wsUrl).build()
            ws = httpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    runCatching {
                        val event = gson.fromJson(text, SentinelEvent::class.java)
                        _events.tryEmit(event)
                    }
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    ws = null
                }
            })
        }
    }

    fun disconnect() {
        ws?.cancel()
        ws = null
    }
}
