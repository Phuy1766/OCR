"""Pydantic schemas cho OCR API."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel, Field


class BoundingBox(BaseModel):
    """Tọa độ box (pixel) trên ảnh/page."""

    x: int
    y: int
    w: int
    h: int


class ExtractedField(BaseModel):
    """1 trường được trích xuất từ text."""

    field_name: str = Field(..., description="external_reference_number, ...")
    field_value: str | None = None
    confidence: float | None = None
    bbox: BoundingBox | None = None
    page_number: int | None = None


class OcrResponse(BaseModel):
    """Kết quả gọi POST /ocr/process."""

    raw_text: str
    confidence_avg: float
    processing_ms: int
    page_count: int
    engine_version: str
    fields: list[ExtractedField] = Field(default_factory=list)
    extra: dict[str, Any] = Field(default_factory=dict)
