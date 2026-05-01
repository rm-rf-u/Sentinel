import structlog
from fastapi import APIRouter, WebSocket, WebSocketDisconnect

logger = structlog.get_logger()
router = APIRouter(tags=["websocket"])


@router.websocket("/ws/events")
async def ws_events(ws: WebSocket) -> None:
    manager = ws.app.state.ws_manager
    await manager.connect(ws)
    try:
        while True:
            await ws.receive_text()  # keep connection alive; we only send, not receive
    except WebSocketDisconnect:
        manager.disconnect(ws)
