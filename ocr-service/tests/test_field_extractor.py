"""Test trích xuất trường metadata từ raw_text."""

from __future__ import annotations

from app.services.field_extractor import extract_fields


def _by_name(fields, name):
    for f in fields:
        if f.field_name == name:
            return f
    return None


def test_extract_reference_number_simple():
    text = """
    BỘ NỘI VỤ
    Số: 15/QĐ-BNV
    Hà Nội, ngày 15 tháng 8 năm 2025
    """
    fields = extract_fields(text)
    f = _by_name(fields, "external_reference_number")
    assert f is not None
    assert f.field_value == "15/QĐ-BNV"


def test_extract_issued_date_long_format():
    text = "Hà Nội, ngày 15 tháng 8 năm 2025"
    fields = extract_fields(text)
    f = _by_name(fields, "external_issued_date")
    assert f is not None
    assert f.field_value == "2025-08-15"


def test_extract_issued_date_short_format():
    text = "Ngày phát hành: 30/9/2025"
    fields = extract_fields(text)
    f = _by_name(fields, "external_issued_date")
    assert f is not None
    assert f.field_value == "2025-09-30"


def test_extract_issuer_uppercase_header():
    text = """
    BỘ GIÁO DỤC VÀ ĐÀO TẠO
    Số: 100/CV-BGDĐT
    """
    fields = extract_fields(text)
    f = _by_name(fields, "external_issuer")
    assert f is not None
    assert "BỘ GIÁO DỤC" in f.field_value


def test_extract_issuer_skips_slogan():
    text = """
    CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
    Độc lập - Tự do - Hạnh phúc
    BỘ NỘI VỤ
    """
    fields = extract_fields(text)
    f = _by_name(fields, "external_issuer")
    assert f is not None
    assert "BỘ NỘI VỤ" in f.field_value
    assert "CỘNG HÒA" not in f.field_value


def test_extract_subject_after_vv():
    text = """
    Số: 15/QĐ-UBND
    V/v phê duyệt kế hoạch công tác năm 2025
    """
    fields = extract_fields(text)
    f = _by_name(fields, "subject")
    assert f is not None
    assert "phê duyệt" in f.field_value.lower()


def test_extract_subject_after_ve_viec():
    text = """
    Quyết định
    Về việc bổ nhiệm cán bộ
    """
    fields = extract_fields(text)
    f = _by_name(fields, "subject")
    assert f is not None
    assert "bổ nhiệm" in f.field_value.lower()


def test_invalid_date_is_ignored():
    text = "ngày 99 tháng 13 năm 2025"
    fields = extract_fields(text)
    f = _by_name(fields, "external_issued_date")
    assert f is None


def test_empty_text_returns_empty():
    assert extract_fields("") == []
    assert extract_fields("   \n  \n  ") == []
