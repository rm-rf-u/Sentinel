package com.sentinel.data.webrtc

import android.content.Context
import com.sentinel.data.api.SentinelApi
import com.sentinel.data.api.models.WebRtcOfferRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

enum class WebRtcState { IDLE, CONNECTING, CONNECTED, DISCONNECTED }

@Singleton
class WebRtcClient @Inject constructor(
    private val context: Context,
    private val api: SentinelApi,
) {
    private val eglBase = EglBase.create()
    val eglContext: EglBase.Context get() = eglBase.eglBaseContext

    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    var videoTrack: VideoTrack? = null
        private set

    var onStateChange: ((WebRtcState) -> Unit)? = null
    var onVideoTrack: ((VideoTrack) -> Unit)? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    fun init() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions()
        )
        factory = PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
            .createPeerConnectionFactory()
    }

    fun connect() {
        onStateChange?.invoke(WebRtcState.CONNECTING)
        scope.launch { negotiate() }
    }

    fun disconnect() {
        peerConnection?.close()
        peerConnection = null
        videoTrack = null
        onStateChange?.invoke(WebRtcState.IDLE)
    }

    private suspend fun negotiate() {
        val f = factory ?: return
        val config = PeerConnection.RTCConfiguration(emptyList()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val iceComplete = CompletableDeferred<Unit>()

        val pc = f.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED ->
                        onStateChange?.invoke(WebRtcState.CONNECTED)
                    PeerConnection.IceConnectionState.DISCONNECTED,
                    PeerConnection.IceConnectionState.FAILED ->
                        onStateChange?.invoke(WebRtcState.DISCONNECTED)
                    else -> Unit
                }
            }
            override fun onTrack(transceiver: RtpTransceiver) {
                val track = transceiver.receiver.track() as? VideoTrack ?: return
                videoTrack = track
                onVideoTrack?.invoke(track)
            }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                if (state == PeerConnection.IceGatheringState.COMPLETE) iceComplete.complete(Unit)
            }
            override fun onIceConnectionReceivingChange(p0: Boolean) = Unit
            override fun onSignalingChange(p0: PeerConnection.SignalingState?) = Unit
            override fun onIceCandidate(p0: IceCandidate?) = Unit
            override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) = Unit
            override fun onAddStream(p0: MediaStream?) = Unit
            override fun onRemoveStream(p0: MediaStream?) = Unit
            override fun onDataChannel(p0: DataChannel?) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onConnectionChange(state: PeerConnection.PeerConnectionState?) = Unit
            override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) = Unit
        }) ?: return
        peerConnection = pc

        pc.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )

        val offer = createSdp(pc) { pc.createOffer(it, MediaConstraints()) } ?: return
        setLocalSdp(pc, offer)
        withTimeoutOrNull(5_000) { iceComplete.await() }

        val answer = runCatching {
            api.postOffer(WebRtcOfferRequest(sdp = pc.localDescription.description))
        }.getOrElse {
            onStateChange?.invoke(WebRtcState.DISCONNECTED)
            return
        }

        setRemoteSdp(pc, answer.sdp, answer.type)
    }

    private suspend fun createSdp(
        pc: PeerConnection,
        block: (SdpObserver) -> Unit,
    ): SessionDescription? = suspendCancellableCoroutine { cont ->
        block(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription) { cont.resume(sdp) }
            override fun onCreateFailure(s: String?) { cont.resume(null) }
            override fun onSetSuccess() = Unit
            override fun onSetFailure(s: String?) = Unit
        })
    }

    private suspend fun setLocalSdp(pc: PeerConnection, sdp: SessionDescription) =
        suspendCancellableCoroutine { cont ->
            pc.setLocalDescription(object : SdpObserver {
                override fun onSetSuccess() { cont.resume(Unit) }
                override fun onSetFailure(s: String?) { cont.resume(Unit) }
                override fun onCreateSuccess(p0: SessionDescription?) = Unit
                override fun onCreateFailure(p0: String?) = Unit
            }, sdp)
        }

    private suspend fun setRemoteSdp(pc: PeerConnection, sdp: String, type: String) =
        suspendCancellableCoroutine { cont ->
            val t = when (type) {
                "answer" -> SessionDescription.Type.ANSWER
                else     -> SessionDescription.Type.OFFER
            }
            pc.setRemoteDescription(object : SdpObserver {
                override fun onSetSuccess() { cont.resume(Unit) }
                override fun onSetFailure(s: String?) { cont.resume(Unit) }
                override fun onCreateSuccess(p0: SessionDescription?) = Unit
                override fun onCreateFailure(p0: String?) = Unit
            }, SessionDescription(t, sdp))
        }

    fun release() {
        disconnect()
        factory?.dispose()
        eglBase.release()
    }
}
