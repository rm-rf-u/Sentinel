from fastapi import APIRouter, Request

from sentinel.storage import config_repo
from sentinel.types import Settings

router = APIRouter(prefix="/api", tags=["settings"])


@router.get("/settings", response_model=Settings)
async def get_settings() -> Settings:
    return await config_repo.get_settings()


@router.put("/settings", response_model=Settings)
async def update_settings(settings: Settings, request: Request) -> Settings:
    saved = await config_repo.upsert_settings(settings)
    if hasattr(request.app.state, "engine"):
        request.app.state.engine.update_settings(saved)
    return saved
