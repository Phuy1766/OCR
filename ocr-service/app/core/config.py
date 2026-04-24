"""Cấu hình OCR service qua biến môi trường (Pydantic v2 Settings)."""

from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Settings load từ env hoặc file .env."""

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
    )

    # Service metadata
    service_name: str = Field(default="congvan-ocr-service")
    service_version: str = Field(default="0.1.0")
    environment: str = Field(default="dev")

    # Server
    host: str = Field(default="0.0.0.0")  # noqa: S104 - container bind
    port: int = Field(default=5000, alias="OCR_SERVICE_PORT")

    # Auth — API key nội bộ do backend Spring Boot gửi
    internal_api_key: str = Field(default="", alias="OCR_INTERNAL_API_KEY")

    # Paddle
    paddle_lang: str = Field(default="vi", alias="OCR_PADDLE_LANG")
    paddle_use_gpu: bool = Field(default=False, alias="OCR_PADDLE_USE_GPU")

    # Logging
    log_level: str = Field(default="INFO")


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Singleton settings (cache để không parse env nhiều lần)."""
    return Settings()
