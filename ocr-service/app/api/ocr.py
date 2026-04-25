"""OCR processing endpoint."""

from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile, status

from app.core.auth import require_internal_api_key
from app.schemas.ocr import BoundingBox, ExtractedField, OcrResponse
from app.services import field_extractor, ocr_engine

logger = logging.getLogger(__name__)
router = APIRouter(tags=["ocr"], dependencies=[Depends(require_internal_api_key)])

ALLOWED_MIME = {
    "application/pdf",
    "image/jpeg",
    "image/png",
}

MAX_BYTES = 50 * 1024 * 1024  # 50MB


@router.post("/ocr/process", response_model=OcrResponse)
async def process(file: UploadFile = File(...)) -> OcrResponse:
    """Nhận 1 file (PDF / JPEG / PNG), chạy OCR + extract fields, trả kết quả."""
    if file.content_type not in ALLOWED_MIME:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Định dạng không hỗ trợ: {file.content_type}",
        )
    content = await file.read()
    if len(content) == 0:
        raise HTTPException(status_code=400, detail="File rỗng")
    if len(content) > MAX_BYTES:
        raise HTTPException(status_code=413, detail="File quá lớn (>50MB)")

    # Convert PDF → list ảnh PNG. Image → list 1 phần tử.
    if file.content_type == "application/pdf":
        try:
            pages = ocr_engine.split_pdf_to_images(content)
        except Exception as e:
            logger.exception("Không split được PDF")
            raise HTTPException(status_code=400, detail=f"PDF không đọc được: {e}")
    else:
        pages = [content]

    if not pages:
        raise HTTPException(status_code=400, detail="Không có trang nào trong file")

    try:
        result = ocr_engine.run_ocr(pages)
    except Exception as e:
        logger.exception("OCR engine failed")
        raise HTTPException(status_code=500, detail=f"OCR engine error: {e}")

    fields = field_extractor.extract_fields(result.raw_text)

    return OcrResponse(
        raw_text=result.raw_text,
        confidence_avg=result.confidence_avg,
        processing_ms=result.processing_ms,
        page_count=result.page_count,
        engine_version=result.engine_version,
        fields=fields,
        extra={"line_count": len(result.lines)},
    )


@router.post("/ocr/process-text", response_model=OcrResponse, include_in_schema=False)
async def process_text_only(payload: dict[str, str]) -> OcrResponse:
    """Endpoint test/debug: nhận raw_text → chỉ chạy field extractor.

    Hữu ích cho integration test backend không cần PaddleOCR thật.
    """
    text = payload.get("raw_text", "")
    fields = field_extractor.extract_fields(text)
    return OcrResponse(
        raw_text=text,
        confidence_avg=1.0,
        processing_ms=0,
        page_count=1,
        engine_version="text-only-stub",
        fields=fields,
        extra={},
    )


@router.get("/ocr/_unused")
async def _placeholder_for_bbox_import() -> BoundingBox:
    """Giữ import BoundingBox không bị strip; không expose thực tế."""
    return BoundingBox(x=0, y=0, w=0, h=0)
