from fastapi import APIRouter, HTTPException
from pydantic import BaseModel

from sentinel.storage import devices_repo

router = APIRouter(prefix="/api/devices", tags=["devices"])


class RegisterRequest(BaseModel):
    fcm_token: str


class RegisterResponse(BaseModel):
    device_id: str


@router.post("/register", response_model=RegisterResponse)
async def register(body: RegisterRequest) -> RegisterResponse:
    device_id = await devices_repo.register_device(body.fcm_token)
    return RegisterResponse(device_id=device_id)


@router.delete("/{device_id}")
async def unregister(device_id: str) -> dict:
    deleted = await devices_repo.unregister_device(device_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Device not found")
    return {"ok": True}
