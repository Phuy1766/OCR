"""Trích xuất các trường metadata từ text OCR.

Phase 6: dùng regex + heuristics cho VB hành chính tiếng Việt theo
NĐ 30/2020 Phụ lục VI:
  - external_reference_number  vd "15/QĐ-UBND"
  - external_issuer            cơ quan ban hành (header lines)
  - external_issued_date       ngày ban hành
  - subject                    trích yếu (sau "V/v" hoặc "Về việc")

Phase 9 sẽ dùng template engine theo loại VB. Phase 6 chỉ best-effort
cho 80% case phổ biến.
"""

from __future__ import annotations

import re
from datetime import date
from typing import Iterable

from app.schemas.ocr import ExtractedField

# ---- Regex patterns ----

# Số/ký hiệu: "15/QĐ-UBND", "237/BC-TCKT-2025"
# Pattern: số + slash + ký hiệu chữ hoa (có thể có Đ Ư Ơ unicode) + tùy chọn dash + chữ
RE_REFERENCE_NUMBER = re.compile(
    r"\b(\d{1,5})/([A-ZĐƯƠ]{1,5}(?:-[A-ZĐƯƠ.\d]+)*)\b"
)

# Ngày: "ngày 15 tháng 8 năm 2025" hoặc "15/8/2025"
RE_DATE_VN_LONG = re.compile(
    r"ngày\s+(\d{1,2})\s+tháng\s+(\d{1,2})\s+năm\s+(\d{4})",
    re.IGNORECASE,
)
RE_DATE_VN_SHORT = re.compile(r"\b(\d{1,2})/(\d{1,2})/(\d{4})\b")

# Trích yếu: "V/v" hoặc "Về việc"
RE_SUBJECT = re.compile(
    r"(?:V/v|Về việc|VỀ VIỆC|V\\.v)\s*[:.\s]*([^\n]{5,500})",
    re.IGNORECASE,
)


def extract_fields(raw_text: str) -> list[ExtractedField]:
    """Áp regex + heuristic header detection lên raw_text → list ExtractedField."""
    if not raw_text or not raw_text.strip():
        return []

    fields: list[ExtractedField] = []
    lines = [ln.strip() for ln in raw_text.splitlines() if ln.strip()]

    # Reference number — lấy match ĐẦU TIÊN (header thường nằm trên cùng)
    ref = _first_match(RE_REFERENCE_NUMBER, raw_text)
    if ref:
        fields.append(ExtractedField(
            field_name="external_reference_number",
            field_value=f"{ref.group(1)}/{ref.group(2)}",
            confidence=0.85,
        ))

    # Issued date — ưu tiên dạng dài "ngày X tháng Y năm Z"
    date_match = RE_DATE_VN_LONG.search(raw_text)
    if date_match:
        d, m, y = (int(g) for g in date_match.groups())
        try:
            iso = date(y, m, d).isoformat()
            fields.append(ExtractedField(
                field_name="external_issued_date",
                field_value=iso,
                confidence=0.9,
            ))
        except ValueError:
            pass
    else:
        date_match = RE_DATE_VN_SHORT.search(raw_text)
        if date_match:
            d, m, y = (int(g) for g in date_match.groups())
            try:
                iso = date(y, m, d).isoformat()
                fields.append(ExtractedField(
                    field_name="external_issued_date",
                    field_value=iso,
                    confidence=0.7,
                ))
            except ValueError:
                pass

    # Issuer — heuristic: top 3 lines (header) có chứa keywords
    issuer = _detect_issuer(lines[:6])
    if issuer:
        fields.append(ExtractedField(
            field_name="external_issuer",
            field_value=issuer,
            confidence=0.6,
        ))

    # Subject (trích yếu)
    subj = RE_SUBJECT.search(raw_text)
    if subj:
        text = _clean_inline(subj.group(1))
        if 5 <= len(text) <= 500:
            fields.append(ExtractedField(
                field_name="subject",
                field_value=text,
                confidence=0.75,
            ))

    return fields


_ISSUER_KEYWORDS = (
    "BỘ", "ỦY BAN NHÂN DÂN", "CỤC", "VỤ", "SỞ", "PHÒNG", "TRƯỜNG", "VIỆN", "CÔNG TY",
    "TỔNG CỤC", "BAN", "ỦY BAN",
)


def _detect_issuer(top_lines: Iterable[str]) -> str | None:
    """Tìm dòng trông giống header tên cơ quan (chữ hoa, không có dấu chấm câu cuối)."""
    candidates: list[tuple[int, str]] = []
    for ln in top_lines:
        upper_ratio = sum(1 for c in ln if c.isupper()) / max(len(ln), 1)
        if upper_ratio < 0.5:
            continue
        if not any(kw in ln.upper() for kw in _ISSUER_KEYWORDS):
            continue
        # Loại bỏ "CỘNG HÒA XÃ HỘI..." (slogan VN, không phải tên cơ quan)
        if "CỘNG HÒA" in ln.upper() or "ĐỘC LẬP" in ln.upper():
            continue
        candidates.append((len(ln), ln))
    if not candidates:
        return None
    # Ưu tiên dòng dài hơn (tên đầy đủ) nhưng không quá dài (>200 = noise)
    candidates.sort(key=lambda x: (x[0] <= 200, x[0]), reverse=True)
    return candidates[0][1]


def _first_match(pattern: re.Pattern[str], text: str) -> re.Match[str] | None:
    return pattern.search(text)


def _clean_inline(s: str) -> str:
    return re.sub(r"\s+", " ", s).strip(" .;:")
