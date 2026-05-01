from __future__ import annotations

import asyncio
from contextlib import asynccontextmanager
from typing import AsyncGenerator

import structlog
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from sentinel.capture.camera import CameraCapture
from sentinel.capture.microphone import MicCapture
from sentinel.config import config
from sentinel.events.dispatcher import Dispatcher
from sentinel.events.engine import EventEngine
from sentinel.events.models import AudioDetection, VisionDetection
from sentinel.inference.audio import AudioRunner
from sentinel.inference.vision import VisionRunner
from sentinel.logging import setup_logging
from sentinel.push import fcm
from sentinel.storage import config_repo, events_repo
from sentinel.storage.db import close_db, init_db
from sentinel.transport.webrtc import PeerConnectionPool
from sentinel.transport.ws import ConnectionManager

logger = structlog.get_logger()


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    setup_logging(config.log_level)
    logger.info("sentinel.startup", host=config.host, port=config.port)

    await init_db(config.db_path)
    fcm.init(config.fcm_service_account_json)

    # Hardware capture
    camera = CameraCapture(config.camera_device_index)
    camera.start()
    mic = MicCapture(config.mic_device_index)
    mic.start()

    # Shared transport
    ws_manager = ConnectionManager()
    pc_pool = PeerConnectionPool()

    # Event pipeline
    dispatcher = Dispatcher(ws_manager)
    settings = await config_repo.get_settings()
    dispatcher.update_settings(settings.sensitivity)
    engine = EventEngine(settings, dispatcher)

    zone = await config_repo.get_safe_zone()
    if zone:
        engine.update_safe_zone(zone)

    # Inference runners
    loop = asyncio.get_running_loop()

    vision_queue: asyncio.Queue[VisionDetection] = asyncio.Queue(maxsize=10)
    vision_runner = VisionRunner(config.model_dir, camera.buffer)
    vision_runner.start(loop, vision_queue)

    audio_queue: asyncio.Queue[AudioDetection] = asyncio.Queue(maxsize=20)
    audio_runner = AudioRunner(mic.buffer)
    audio_runner.start(loop, audio_queue)

    # Asyncio consumer tasks
    vision_task = asyncio.create_task(_vision_loop(vision_queue, engine), name="vision-loop")
    audio_task = asyncio.create_task(_audio_loop(audio_queue, engine), name="audio-loop")
    cleanup_task = asyncio.create_task(_cleanup_loop(), name="cleanup")

    # App state
    app.state.camera = camera
    app.state.mic = mic
    app.state.pc_pool = pc_pool
    app.state.ws_manager = ws_manager
    app.state.engine = engine

    yield

    # Shutdown
    vision_runner.stop()
    audio_runner.stop()
    vision_task.cancel()
    audio_task.cancel()
    cleanup_task.cancel()
    await asyncio.gather(vision_task, audio_task, cleanup_task, return_exceptions=True)
    await pc_pool.close_all()
    camera.stop()
    mic.stop()
    await close_db()
    logger.info("sentinel.shutdown")


async def _vision_loop(
    queue: asyncio.Queue[VisionDetection], engine: EventEngine
) -> None:
    while True:
        detection = await queue.get()
        try:
            await engine.process_vision(detection)
        except Exception:
            logger.exception("engine.vision_error")


async def _cleanup_loop() -> None:
    while True:
        deleted = await events_repo.purge_old_events(days=30)
        if deleted:
            logger.info("events.purged", count=deleted)
        await asyncio.sleep(24 * 3600)


async def _audio_loop(
    queue: asyncio.Queue[AudioDetection], engine: EventEngine
) -> None:
    while True:
        detection = await queue.get()
        try:
            await engine.process_audio(detection)
        except Exception:
            logger.exception("engine.audio_error")


def create_app() -> FastAPI:
    app = FastAPI(title="Sentinel", lifespan=lifespan)

    app.add_middleware(
        CORSMiddleware,
        allow_origins=[
            f"http://{config.tailscale_hostname}",
            f"https://{config.tailscale_hostname}",
            "http://localhost:3000",
            "http://localhost:3001",
        ],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    from sentinel.api import (
        routes_devices,
        routes_events,
        routes_health,
        routes_safe_zone,
        routes_settings,
        routes_webrtc,
        routes_ws,
    )

    app.include_router(routes_health.router)
    app.include_router(routes_events.router)
    app.include_router(routes_safe_zone.router)
    app.include_router(routes_settings.router)
    app.include_router(routes_devices.router)
    app.include_router(routes_webrtc.router)
    app.include_router(routes_ws.router)

    return app


app = create_app()
