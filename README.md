# Hệ thống quản lý công văn đi/đến

> Đề tài NCKH sinh viên cấp trường — Nghiên cứu các kỹ thuật xử lý chữ ký số và nhận diện ký tự quang học OCR hiện đại, ứng dụng trong phát triển thử nghiệm hệ thống thông tin hỗ trợ quản lý công văn đi và đến của một đơn vị.

Hệ thống số hóa toàn bộ quy trình công văn theo **Nghị định 30/2020/NĐ-CP** và **Luật Giao dịch điện tử 2023**: tiếp nhận → OCR → ghi sổ → phân công → duyệt → ký số → phát hành → lưu trữ.

## Kiến trúc tổng thể

```
┌─────────────┐        ┌───────────────────┐        ┌──────────────┐
│  Next.js 14 │ <────> │  Spring Boot 3    │ <────> │ PostgreSQL 16│
│   (3000)    │        │   (8080)          │        │    (5432)    │
└─────────────┘        └───┬───────────┬───┘        └──────────────┘
                           │           │
                           │           ├────────> Redis 7 (6379)
                           │           ├────────> RabbitMQ (5672)
                           │           └────────> MinIO (9000)
                           │
                           └───> FastAPI OCR service (5000)
                                 └── PaddleOCR + OpenCV
```

## Yêu cầu môi trường

| Tool          | Phiên bản tối thiểu |
| ------------- | ------------------- |
| Docker        | 24.x                |
| Docker Compose| v2                  |
| Java JDK      | 21 LTS              |
| Maven         | 3.9+                |
| Node.js       | 20 LTS              |
| pnpm          | 9.x                 |
| Python        | 3.11+               |

## Khởi chạy nhanh (local dev)

```bash
# 1. Clone repo
git clone <repo-url> congvan-system
cd congvan-system

# 2. Cấu hình môi trường
cp .env.example .env
# Chỉnh sửa mật khẩu, khoá JWT, v.v.

# 3. Khởi chạy toàn bộ stack
docker compose up -d

# 4. Kiểm tra health
curl http://localhost:8080/actuator/health   # Backend
curl http://localhost:3000                   # Frontend
curl http://localhost:5000/health            # OCR service
```

Console quản trị:

- **RabbitMQ Management**: http://localhost:15672
- **MinIO Console**: http://localhost:9001
- **Swagger UI** (backend): http://localhost:8080/swagger-ui.html

## Cấu trúc thư mục

```
congvan-system/
├── backend/            Spring Boot 3 / Java 21 / Maven multi-module
├── frontend/           Next.js 14 App Router / TypeScript strict
├── ocr-service/        FastAPI / Python 3.11 / PaddleOCR
├── db/migration/       Flyway SQL migrations (shared)
├── docker/             Dockerfile per service + init scripts
├── docs/               Tài liệu thiết kế & pháp lý (gốc)
├── scripts/            Build / seed / deployment scripts
└── .github/workflows/  CI/CD pipelines
```

## Tài liệu thiết kế

Xem thư mục [`docs/`](./docs/). Đọc theo thứ tự:

1. [`docs/README.md`](./docs/README.md) — Mục lục
2. [`docs/architecture_congvan_full_defense (1).md`](./docs/architecture_congvan_full_defense%20(1).md) — Nghiệp vụ, ERD, API
3. [`docs/tech_stack_congvan_system.md`](./docs/tech_stack_congvan_system.md) — Tech stack, patterns
4. [`docs/website_framework_vietnam_legal.md`](./docs/website_framework_vietnam_legal.md) — Pháp lý VN (bắt buộc)

## Quy ước phát triển

- **Branch**: `main` (protected) / `develop` (default) / `feature/*` / `hotfix/*`
- **Commit**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`)
- **Code style**: Spotless (Java), Prettier (TS), Ruff (Python)
- **Test coverage**: > 80% (JaCoCo / Vitest / pytest)
- **Ngôn ngữ code**: tên biến/hàm tiếng Anh, comment tiếng Việt cho nghiệp vụ

## Roadmap

Xem [`docs/tech_stack_congvan_system.md §16`](./docs/tech_stack_congvan_system.md). 11 phase, ~5 tháng, từ Phase 0 (setup) đến Phase 10 (testing + CI/CD + tài liệu).

## Giấy phép

Học thuật — Đề tài NCKH.
