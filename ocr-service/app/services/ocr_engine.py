"""Wrapper cho PaddleOCR — lazy load model lần gọi đầu tiên."""

from __future__ import annotations

import io
import logging
import time
from dataclasses import dataclass
from threading import Lock
from typing import Any

import cv2
import numpy as np

from app.core.config import get_settings
from app.services.preprocessor import preprocess_array

logger = logging.getLogger(__name__)


@dataclass(slots=True)
class OcrLine:
    """1 dòng text được OCR detect."""

    text: str
    confidence: float
    bbox: tuple[int, int, int, int]  # x, y, w, h
    page_number: int


@dataclass(slots=True)
class OcrPipelineResult:
    raw_text: str
    confidence_avg: float
    processing_ms: int
    page_count: int
    engine_version: str
    lines: list[OcrLine]


class _LazyPaddleOCR:
    """Singleton lazy loader cho PaddleOCR — model nặng, chỉ load khi cần."""

    _instance = None
    _lock = Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._engine = None
        return cls._instance

    def get(self) -> Any:
        if self._engine is None:  # type: ignore[attr-defined]
            with self._lock:
                if self._engine is None:  # type: ignore[attr-defined]
                    settings = get_settings()
                    logger.info(
                        "Loading PaddleOCR model lang=%s use_gpu=%s ...",
                        settings.paddle_lang,
                        settings.paddle_use_gpu,
                    )
                    # Import lazy để skip khi test/dev không có model.
                    from paddleocr import PaddleOCR  # type: ignore

                    self._engine = PaddleOCR(  # type: ignore[attr-defined]
                        use_angle_cls=True,
                        lang=settings.paddle_lang,
                        use_gpu=settings.paddle_use_gpu,
                        show_log=False,
                    )
                    logger.info("PaddleOCR model loaded.")
        return self._engine  # type: ignore[attr-defined]


def run_ocr(image_bytes_list: list[bytes]) -> OcrPipelineResult:
    """Chạy OCR trên danh sách trang (1 trang/file). Áp preprocess trước.

    Trả ``OcrPipelineResult`` với raw_text concat từ tất cả trang.
    """
    started = time.time()
    engine = _LazyPaddleOCR().get()
    lines: list[OcrLine] = []
    confs: list[float] = []
    raw_pages: list[str] = []

    for page_num, raw_bytes in enumerate(image_bytes_list, start=1):
        arr = np.frombuffer(raw_bytes, dtype=np.uint8)
        img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
        if img is None:
            logger.warning("Skipping unreadable page %d", page_num)
            continue
        img = preprocess_array(img)
        result = engine.ocr(img, cls=True)
        if not result or not result[0]:
            continue
        page_lines = []
        for entry in result[0]:
            box = entry[0]  # 4 corners polygon
            text, conf = entry[1]
            xs = [int(p[0]) for p in box]
            ys = [int(p[1]) for p in box]
            x, y = min(xs), min(ys)
            w, h = max(xs) - x, max(ys) - y
            line = OcrLine(text=text, confidence=float(conf), bbox=(x, y, w, h),
                           page_number=page_num)
            page_lines.append(line)
            confs.append(float(conf))
        # Sort top-to-bottom rồi left-to-right để raw_text đúng thứ tự đọc
        page_lines.sort(key=lambda ln: (ln.bbox[1], ln.bbox[0]))
        lines.extend(page_lines)
        raw_pages.append("\n".join(ln.text for ln in page_lines))

    raw_text = "\n\n".join(raw_pages)
    avg = float(np.mean(confs)) if confs else 0.0
    elapsed_ms = int((time.time() - started) * 1000)
    return OcrPipelineResult(
        raw_text=raw_text,
        confidence_avg=round(avg, 3),
        processing_ms=elapsed_ms,
        page_count=len(image_bytes_list),
        engine_version="PaddleOCR-vi",
        lines=lines,
    )


def split_pdf_to_images(pdf_bytes: bytes, dpi: int = 200) -> list[bytes]:
    """Convert PDF bytes → list ảnh PNG bytes (1 ảnh/trang) dùng pdf2image."""
    from pdf2image import convert_from_bytes  # lazy import

    images = convert_from_bytes(pdf_bytes, dpi=dpi)
    out: list[bytes] = []
    for img in images:
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        out.append(buf.getvalue())
    return out
