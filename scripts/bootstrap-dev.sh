#!/usr/bin/env bash
# =========================================================================
# Bootstrap môi trường dev cho toàn team: copy .env, pull images,
# build ảnh Docker 1 lần đầu, khởi chạy stack.
# =========================================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [ ! -f .env ]; then
    echo "→ .env chưa có, copy từ .env.example..."
    cp .env.example .env
    echo "⚠️  Mở .env và chỉnh lại mật khẩu trước khi dùng production."
fi

echo "→ Pull base images..."
docker compose pull postgres redis rabbitmq minio minio-setup

echo "→ Build backend/frontend/ocr..."
docker compose build backend frontend ocr-service

echo "→ Start stack..."
docker compose up -d

echo ""
echo "✅ Hoàn tất. Check health:"
echo "  Backend:  curl http://localhost:8080/actuator/health"
echo "  Frontend: curl http://localhost:3000/api/health"
echo "  OCR:      curl http://localhost:5000/health"
echo "  RabbitMQ: http://localhost:15672"
echo "  MinIO:    http://localhost:9001"
