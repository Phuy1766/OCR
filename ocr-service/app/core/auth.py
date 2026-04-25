"""Authentication: chỉ chấp nhận request từ backend với X-Internal-API-Key."""

from __future__ import annotations

from fastapi import Header, HTTPException, status

from app.core.config import get_settings


async def require_internal_api_key(
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-API-Key"),
) -> None:
    """Verify shared secret giữa backend và OCR service.

    Trong dev nếu chưa set internal_api_key thì cho qua (warning).
    """
    settings = get_settings()
    if not settings.internal_api_key:
        # Dev: chấp nhận nếu chưa cấu hình. Production luôn phải set.
        return
    if x_internal_api_key != settings.internal_api_key:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="X-Internal-API-Key invalid",
        )
