from fastapi import APIRouter, Query

from sentinel.storage import events_repo

router = APIRouter(prefix="/api", tags=["events"])


@router.get("/events")
async def list_events(
    limit: int = Query(default=50, ge=1, le=200),
    before: str | None = Query(default=None),
) -> list[dict]:
    return await events_repo.list_events(limit=limit, before=before)
