"""Smoke test: /health trả 200 với status=ok."""

from __future__ import annotations

from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_ok() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["service"] == "congvan-ocr-service"
    assert "version" in body
    assert "timestamp" in body


def test_root_ok() -> None:
    response = client.get("/")
    assert response.status_code == 200
    body = response.json()
    assert body["service"] == "congvan-ocr-service"
