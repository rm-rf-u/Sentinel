from fastapi import APIRouter, HTTPException, Request

from sentinel.storage import config_repo
from sentinel.types import SafeZone

router = APIRouter(prefix="/api", tags=["safe-zone"])


@router.get("/safe-zone", response_model=SafeZone)
async def get_safe_zone() -> SafeZone:
    zone = await config_repo.get_safe_zone()
    if zone is None:
        raise HTTPException(status_code=404, detail="No safe zone configured")
    return zone


@router.put("/safe-zone", response_model=SafeZone)
async def upsert_safe_zone(zone: SafeZone, request: Request) -> SafeZone:
    saved = await config_repo.upsert_safe_zone(zone)
    # Hot-reload into the running event engine
    if hasattr(request.app.state, "engine"):
        request.app.state.engine.update_safe_zone(saved)
    return saved
