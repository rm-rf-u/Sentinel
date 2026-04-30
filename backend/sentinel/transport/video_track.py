"""aiortc VideoStreamTrack that reads from the camera FrameBuffer."""

from __future__ import annotations

import numpy as np
from av import VideoFrame
from aiortc import VideoStreamTrack

from sentinel.capture.ring_buffer import FrameBuffer

_BLANK_FRAME = np.zeros((720, 1280, 3), dtype=np.uint8)


class CameraVideoTrack(VideoStreamTrack):
    kind = "video"

    def __init__(self, buffer: FrameBuffer) -> None:
        super().__init__()
        self._buffer = buffer

    async def recv(self) -> VideoFrame:
        pts, time_base = await self.next_timestamp()

        frame_data = self._buffer.get_latest()
        if frame_data is None:
            frame_data = _BLANK_FRAME

        video_frame = VideoFrame.from_ndarray(frame_data, format="bgr24")
        video_frame.pts = pts
        video_frame.time_base = time_base
        return video_frame
