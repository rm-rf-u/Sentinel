import structlog
from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Request

logger = structlog.get_logger()
router = APIRouter(tags=["websocket"])


@router.websocket("/ws/events")
async def ws_events(ws: WebSocket, request: Request) -> None:
    manager = request.app.state.ws_manager
    await manager.connect(ws)
    try:
        while True:
            await ws.receive_text()  # keep connection alive; we only send, not receive
    except WebSocketDisconnect:
        manager.disconnect(ws)
