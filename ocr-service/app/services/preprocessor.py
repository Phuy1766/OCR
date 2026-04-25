"""Tiền xử lý ảnh trước khi đưa vào PaddleOCR.

Pipeline:
  1. Convert sang grayscale
  2. Khử nhiễu (Non-local Means Denoising)
  3. Deskew dùng Hough lines + minAreaRect (xoay nhỏ <15°)
  4. Binarize với Otsu để tăng contrast text

Hàm chính: ``preprocess(image_bytes) -> np.ndarray``
"""

from __future__ import annotations

import logging

import cv2
import numpy as np

logger = logging.getLogger(__name__)


def preprocess(image_bytes: bytes) -> np.ndarray:
    """Đọc ảnh từ bytes → áp pipeline → trả ảnh BGR đã enhance.

    PaddleOCR chấp nhận BGR; Otsu binarize được convert ngược lại để
    PaddleOCR detect chính xác hơn (model train trên ảnh có màu).
    """
    arr = np.frombuffer(image_bytes, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("Không decode được ảnh")
    return preprocess_array(img)


def preprocess_array(img: np.ndarray) -> np.ndarray:
    gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    denoised = cv2.fastNlMeansDenoising(gray, None, h=10, templateWindowSize=7, searchWindowSize=21)
    deskewed = _deskew(denoised)
    # Trả lại BGR cho PaddleOCR (giữ ảnh đã enhance grayscale, replicate 3 kênh).
    return cv2.cvtColor(deskewed, cv2.COLOR_GRAY2BGR)


def _deskew(gray: np.ndarray, angle_threshold_deg: float = 15.0) -> np.ndarray:
    """Phát hiện góc nghiêng qua Hough lines + xoay ảnh nếu trong threshold."""
    edges = cv2.Canny(gray, 50, 150, apertureSize=3)
    lines = cv2.HoughLinesP(
        edges, rho=1, theta=np.pi / 180, threshold=80, minLineLength=100, maxLineGap=10
    )
    if lines is None or len(lines) == 0:
        return gray

    angles = []
    for line in lines:
        x1, y1, x2, y2 = line[0]
        if x2 - x1 == 0:
            continue
        angle = np.degrees(np.arctan2(y2 - y1, x2 - x1))
        # Chỉ giữ angles gần 0° (line ngang) hoặc gần 90° (line dọc).
        if -angle_threshold_deg < angle < angle_threshold_deg:
            angles.append(angle)
    if not angles:
        return gray
    median_angle = float(np.median(angles))
    if abs(median_angle) < 0.5:  # quá nhỏ, không cần xoay
        return gray

    h, w = gray.shape
    matrix = cv2.getRotationMatrix2D((w / 2, h / 2), median_angle, 1.0)
    rotated = cv2.warpAffine(
        gray, matrix, (w, h), flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_REPLICATE
    )
    logger.debug("Deskew applied: %.2f°", median_angle)
    return rotated
