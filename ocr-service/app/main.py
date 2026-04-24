"""FastAPI application entry point."""

from __future__ import annotations

import logging

from fastapi import FastAPI

from app.api.health import router as health_router
from app.core.config import get_settings

settings = get_settings()

logging.basicConfig(level=settings.log_level)

app = FastAPI(
    title="Congvan OCR Service",
    version=settings.service_version,
    description=(
        "OCR tiếng Việt cho hệ thống quản lý công văn. "
        "Phase 0: skeleton + /health. Phase 6: tích hợp PaddleOCR + field extraction."
    ),
    docs_url="/docs",
    redoc_url=None,
)

app.include_router(health_router)


@app.get("/", include_in_schema=False)
async def root() -> dict[str, str]:
    return {"service": settings.service_name, "version": settings.service_version}
