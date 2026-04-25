# Hệ thống quản lý công văn đi/đến

> Đề tài NCKH sinh viên cấp trường — Nghiên cứu các kỹ thuật xử lý chữ ký số và nhận diện ký tự quang học OCR hiện đại, ứng dụng trong phát triển thử nghiệm hệ thống thông tin hỗ trợ quản lý công văn đi và đến của một đơn vị.

Hệ thống số hóa toàn bộ quy trình công văn theo **Nghị định 30/2020/NĐ-CP** và **Luật Giao dịch điện tử 2023**: tiếp nhận → OCR → ghi sổ → phân công → duyệt → ký số → phát hành → lưu trữ.

## Trạng thái dự án

| Phase | Nội dung | Trạng thái |
| ----- | -------- | ---------- |
| 0 | Bootstrap monorepo, docker-compose, CI skeleton | ✅ |
| 1 | Auth: Argon2id, JWT RS256, refresh rotation, BR-12 lockout | ✅ |
| 2 | Master data: 29 loại VB, sổ đăng ký, BR-01 race-free numbering | ✅ |
| 3 | Công văn đến: tiếp nhận, MinIO, BR-03/05/09/11 | ✅ |
| 4 | Công văn đi: versioning immutable, 2-tier approval, BR-07 | ✅ |
| 5 | Workflow: assignment, notifications, outbox + RabbitMQ | ✅ |
| 6 | OCR: PaddleOCR + auto-trigger, tích hợp metadata | ✅ |
| 7 | Chữ ký số: PAdES 2-tier (cá nhân + tổ chức), BR-06/12 | ✅ |
| 8 | Tìm kiếm: PostgreSQL FTS tiếng Việt + unaccent + fuzzy | ✅ |
| 9 | UI Polish: dashboard, dark mode, error pages, pagination | ✅ |
| 10 | Testing, CI/CD, tài liệu bảo vệ | ✅ |

**Test coverage** (JaCoCo aggregate, 38 integration tests + Testcontainers):

```
Lines:        2281/2908 (78.4%)
Branches:      433/894  (48.4%)
Methods:       380/518  (73.4%)
Instructions: 11222/14792 (75.9%)
```

Module nghiệp vụ ≥ 80%: inbound 87%, outbound 85%, search 88%, signature 85%, workflow 86%, ocr 85%.

## Kiến trúc tổng thể

```
┌──────────────┐      ┌─────────────────────┐      ┌──────────────┐
│  Next.js 14  │ ◀──▶ │ Spring Boot 3.2     │ ◀──▶ │ PostgreSQL 16│
│  (3000)      │      │ Java 21, 12 modules │      │   (5432)     │
└──────────────┘      └────┬────────────┬───┘      └──────────────┘
                           │            │
                           │            ├──▶ Redis 7    (token blacklist, rate limit)
                           │            ├──▶ RabbitMQ   (outbox events)
                           │            └──▶ MinIO      (file storage)
                           │
                           └──▶ FastAPI OCR (5000) — PaddleOCR + OpenCV
```

Multi-module Maven (12 modules):

```
congvan-common  → BaseEntity, ApiResponse, exceptions, audit DTOs
congvan-auth    → JWT RS256, Argon2id, RefreshToken rotation, RBAC
congvan-masterdata → orgs, departments, document books, race-free counters
congvan-inbound → tiếp nhận, ghi sổ, phân công, file upload (MinIO)
congvan-outbound → soạn dự thảo, version immutable, 2-tier approval
congvan-workflow → assignment, notifications, outbox publisher
congvan-ocr     → PaddleOCR client, auto-trigger, extracted fields
congvan-signature → PKCS#12 cert, PAdES detached, 2-tier signing
congvan-search  → FTS tiếng Việt, unaccent, ts_rank, fuzzy fallback
congvan-audit   → audit_logs append-only, notifications, dashboard stats
congvan-integration → cross-module event listeners, Optional<Bean> wiring
congvan-app     → Spring Boot assembly + Flyway migrations + integration tests
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
git clone https://github.com/Phuy1766/OCR.git congvan-system
cd congvan-system

# 2. Cấu hình môi trường
cp .env.example .env
# Chỉnh sửa: APP_BOOTSTRAP_ADMIN_PASSWORD, JWT keys, MinIO/RabbitMQ creds

# 3. Khởi chạy infrastructure (Postgres + Redis + RabbitMQ + MinIO + OCR)
docker compose up -d postgres redis rabbitmq minio ocr-service

# 4. Backend
cd backend
mvn -pl congvan-app -am spring-boot:run
# → http://localhost:8080  (Swagger: /swagger-ui.html)

# 5. Frontend (terminal khác)
cd frontend
pnpm install
pnpm dev
# → http://localhost:3000
```

Hoặc chạy toàn bộ qua docker-compose:

```bash
docker compose up -d
```

Console quản trị:

- **Frontend**: http://localhost:3000  (login: `admin` / mật khẩu trong `.env`)
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **RabbitMQ Management**: http://localhost:15672
- **MinIO Console**: http://localhost:9001
- **OCR Health**: http://localhost:5000/health

## Quy trình demo (15 phút)

Xem chi tiết trong [`docs/defense/04_demo_script.md`](./docs/defense/04_demo_script.md).

Tóm tắt:
1. **Login** với `admin` → tạo user `truongphong`, `chuyenvien`, gán role.
2. **Tiếp nhận VB đến** (`Inbox`) → upload PDF scan → OCR tự động → trích xuất số/ngày.
3. **Văn thư đăng ký vào sổ** → BR-01 sinh số sequential, BR-03 thử trùng số → 409.
4. **Trưởng phòng phân công** xuống chuyên viên với deadline + ghi chú.
5. **Chuyên viên hoàn tất** → notification + audit log.
6. **Soạn VB đi** → upload draft → trưởng phòng duyệt → trưởng đơn vị duyệt.
7. **Ký số 2-cấp**: cá nhân ký trước, tổ chức ký sau (BR-06/12). Thử ký ngược → 422.
8. **Phát hành** → status SENT → BR-07 freeze version.
9. **Tìm kiếm**: gõ có dấu/không dấu, gõ sai chính tả → fuzzy fallback.
10. **Dashboard**: stats real-time, recent docs, my tasks. Toggle dark mode.

## Cấu trúc thư mục

```
congvan-system/
├── backend/            Spring Boot 3 / Java 21 / Maven multi-module (12 modules)
├── frontend/           Next.js 14 App Router / TypeScript strict
├── ocr-service/        FastAPI / Python 3.11 / PaddleOCR
├── db/migration/       Flyway SQL V1-V18 (shared)
├── docker/             Dockerfile per service + init scripts
├── docs/
│   ├── README.md                            Mục lục tài liệu thiết kế
│   ├── architecture_congvan_full_defense.md Nghiệp vụ, ERD, API
│   ├── tech_stack_congvan_system.md         Tech stack, patterns
│   ├── website_framework_vietnam_legal.md   Pháp lý VN
│   └── defense/                             Tài liệu bảo vệ NCKH
│       ├── 00_business_rules.md             14 BR ↔ file/migration/test
│       ├── 01_architecture.md               Multi-module + event-driven
│       ├── 02_security.md                   JWT, Argon2id, RBAC, BR-12
│       ├── 03_compliance.md                 NĐ 30/2020 + Luật GDĐT 2023
│       └── 04_demo_script.md                Kịch bản demo 15 phút
├── scripts/            Build / seed / deployment scripts
└── .github/workflows/  backend-ci, frontend-ci, ocr-ci, security-scan
```

## Quy ước phát triển

- **Branch**: `master` (default) / `main` / `develop` / `feature/*` / `hotfix/*`
- **Commit**: Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`)
- **Code style**: Spotless + Google Java Format (Java), Prettier (TS), Ruff (Python)
- **Test coverage**: ≥ 80% cho module nghiệp vụ (JaCoCo aggregate)
- **Ngôn ngữ code**: tên biến/hàm tiếng Anh, comment tiếng Việt cho nghiệp vụ + pháp lý

## 14 Business Rules

Toàn bộ enforce trong code/database. Bảng đầy đủ trong [`docs/defense/00_business_rules.md`](./docs/defense/00_business_rules.md).

| BR | Yêu cầu pháp lý | Cơ chế enforce |
|----|-----------------|----------------|
| BR-01 | Số công văn reset 01/01 hàng năm | DB sequence per (book_id, year) + UNIQUE (book_id, year, number) |
| BR-02 | Tránh race condition cấp số | `INSERT ON CONFLICT DO NOTHING` + `SELECT FOR UPDATE` counter |
| BR-03 | Sổ bí mật chỉ người được uỷ quyền xem | `SECRET` confidentiality + `INBOUND:VIEW_SECRET` permission |
| BR-04 | Ưu tiên KHẨN/HỎA TỐC | Priority enum + ordering ở queue phân công |
| BR-05 | Đăng ký vào sổ trong 24h | Trigger cảnh báo + dashboard widget |
| BR-06 | 2 chữ ký số (cá nhân + tổ chức) | `digital_signatures` UNIQUE per type, gate enforce |
| BR-07 | Khoá phiên bản đã duyệt | DB trigger block UPDATE `content_snapshot` |
| BR-08 | Sổ đăng ký in được | Endpoint `/document-books/{id}/print` (PDF) |
| BR-09 | Soft delete (không xoá vật lý) | `is_deleted` + filter mặc định |
| BR-10 | Audit log không sửa được | DB trigger block UPDATE/DELETE `audit_logs` |
| BR-11 | Thu hồi không xoá | Status `RECALLED` + lý do bắt buộc |
| BR-12 | Khoá tài khoản sau 5 lần sai | Bucket per IP+username, TTL 15 phút |
| BR-13 | Lưu trữ vĩnh viễn cho VB pháp lý | Retention rules per document_type |
| BR-14 | Tự động chuyển vào kho lưu trữ sau N năm | Scheduled job + retention metadata |

## Roadmap

11 phases hoàn tất. Tất cả mục tiêu MVP đạt:
- ✅ Tuân thủ NĐ 30/2020/NĐ-CP và Luật GDĐT 2023.
- ✅ 14/14 business rules enforce ở code + database.
- ✅ Test coverage ≥ 80% cho module nghiệp vụ (aggregate 78.4%).
- ✅ CI/CD GitHub Actions (build/test/security-scan).
- ✅ Dashboard analytics, dark mode, error boundaries.

Ngoài MVP (future work):
- Mobile app (React Native).
- Tích hợp Trục liên thông văn bản quốc gia (theo TT 27/2017/TT-BTTTT).
- AI/LLM trợ lý soạn thảo dự thảo.

## Giấy phép

Học thuật — Đề tài NCKH sinh viên cấp trường.
