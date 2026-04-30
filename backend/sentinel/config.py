from pathlib import Path

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Config(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="SENTINEL_",
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
        populate_by_name=True,
    )

    host: str = "0.0.0.0"
    port: int = 8000
    db_path: Path = Path("./data/sentinel.db")
    model_dir: Path = Path("./data/models")
    log_level: str = "INFO"

    tailscale_hostname: str = Field(default="localhost", alias="TAILSCALE_HOSTNAME")
    fcm_service_account_json: Path = Field(
        default=Path("./data/fcm-sa.json"), alias="FCM_SERVICE_ACCOUNT_JSON"
    )

    camera_device_index: int = Field(default=0, alias="CAMERA_DEVICE_INDEX")
    mic_device_index: int = Field(default=-1, alias="MIC_DEVICE_INDEX")


config = Config()
