# congvan-ocr-service

OCR service tiếng Việt — FastAPI + Pydantic v2 + PaddleOCR.

## Cấu trúc

```
app/
  api/
    health.py          # GET /health
  core/
    config.py          # Pydantic settings từ env
  services/            # Phase 6: Preprocessor, OCR engine, FieldExtractor
  schemas/             # Phase 6: Pydantic request/response
  main.py              # FastAPI app
tests/
  test_health.py       # Phase 0 smoke
```

## Chạy local (Phase 0 — chưa load PaddleOCR model)

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload

curl localhost:5000/health
```

## Test

```bash
pip install -e ".[dev]"
pytest -v
```

## Phase 6 roadmap

- Endpoint `POST /ocr/process` nhận file (PDF/ảnh) → preprocess → PaddleOCR → field extraction.
- Authentication: header `X-Internal-API-Key` do backend Spring Boot gửi (OCR_INTERNAL_API_KEY).
- Async processing qua task queue (background tasks hoặc Celery).
