"""WebRTC peer connection lifecycle management."""

from __future__ import annotations

import structlog
from aiortc import RTCPeerConnection, RTCSessionDescription

from sentinel.capture.ring_buffer import FrameBuffer
from sentinel.transport.video_track import CameraVideoTrack

logger = structlog.get_logger()


class PeerConnectionPool:
    def __init__(self) -> None:
        self._pcs: set[RTCPeerConnection] = set()

    async def create_offer_answer(
        self, sdp: str, sdp_type: str, buffer: FrameBuffer
    ) -> tuple[str, str]:
        pc = RTCPeerConnection()
        self._pcs.add(pc)

        @pc.on("connectionstatechange")  # type: ignore[misc]
        async def on_state() -> None:
            logger.info("webrtc.state", state=pc.connectionState)
            if pc.connectionState in ("failed", "closed"):
                await pc.close()
                self._pcs.discard(pc)

        pc.addTrack(CameraVideoTrack(buffer))

        await pc.setRemoteDescription(RTCSessionDescription(sdp=sdp, type=sdp_type))
        answer = await pc.createAnswer()
        await pc.setLocalDescription(answer)

        return pc.localDescription.sdp, pc.localDescription.type

    async def close_all(self) -> None:
        for pc in list(self._pcs):
            await pc.close()
        self._pcs.clear()
