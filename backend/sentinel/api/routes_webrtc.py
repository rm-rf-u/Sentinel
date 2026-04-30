from fastapi import APIRouter, Request

from sentinel.types import WebRTCAnswer, WebRTCOffer

router = APIRouter(prefix="/api/webrtc", tags=["webrtc"])


@router.post("/offer", response_model=WebRTCAnswer)
async def offer(body: WebRTCOffer, request: Request) -> WebRTCAnswer:
    pool = request.app.state.pc_pool
    buffer = request.app.state.camera.buffer

    sdp, sdp_type = await pool.create_offer_answer(body.sdp, body.type, buffer)
    return WebRTCAnswer(sdp=sdp, type=sdp_type)
