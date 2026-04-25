# CHANGELOG

Ghi lại mọi thay đổi so với thiết kế gốc trong `docs/` (01-architecture, 02-tech-stack, 03-legal-framework). Không sửa trực tiếp 3 tài liệu gốc.

Định dạng: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), phiên bản theo [SemVer](https://semver.org/).

## [0.10.0] — 2026-04-25 — Phase 10: Testing + CI/CD + Defense docs

### Added
- JaCoCo aggregate report ở `congvan-app` — combine exec từ tất cả modules.
  Coverage: 78.4% lines, 73.4% methods, 75.9% instructions trên 38 integration tests.
- Tài liệu bảo vệ NCKH trong `docs/defense/`:
  - `00_business_rules.md` — bảng 14 BR ↔ file/migration/test
  - `01_architecture.md` — multi-module + event-driven + patterns
  - `02_security.md` — JWT, Argon2id, RBAC, BR-12, OWASP threats
  - `03_compliance.md` — NĐ 30/2020 + Luật GDĐT 2023 mapping
  - `04_demo_script.md` — kịch bản demo 15 phút
- README tổng cập nhật với phases done, coverage, demo flow, BR matrix.

### Changed
- CI workflows trigger cho `master` (project dùng master, không develop).
- `pom.xml` parent: surefire argLine `-Dnet.bytebuddy.experimental=true` để Mockito
  hoạt động với JDK 24 (giữ Java 21 LTS làm target compile).

## [0.9.0] — 2026-04-25 — Phase 9: UI Polish + Dashboard

### Added
- Endpoint `GET /api/dashboard/stats` (counts + recent docs + my tasks +
  status breakdown) trong `congvan-audit`.
- ThemeProvider (light/dark/system) + ThemeToggle, FOUC-free init script.
- Components dùng chung: Pagination (page-number), EmptyState, Skeleton.
- Custom 404 (`/not-found`) và global error boundary (`/error`).
- Áp dụng polish vào tất cả list pages (skeleton + empty state + pagination).

## [0.8.0] — Phase 8: PostgreSQL FTS tiếng Việt

### Added
- Custom config `vietnamese` (simple + unaccent) cho `to_tsvector`.
- `documents.search_vector` + `ocr_results.search_vector` với trigger update.
- Fuzzy fallback qua pg_trgm khi FTS không có match.
- Endpoint `/api/search/documents` với filter direction/date/dept.

## [0.7.0] — Phase 7: PAdES digital signature 2-tier

### Added
- PKCS#12 certificate storage (encrypted blob).
- PAdES detached SHA256WithRSA dùng PDFBox 3 + Bouncy Castle.
- BR-06: 2 chữ ký bắt buộc với thứ tự PERSONAL → ORGANIZATION.
- `SignatureGate` interface (Optional<Bean>) decouple với outbound.
- `SignatureVerificationService` cho verify chain.

## [0.6.0] — Phase 6: OCR pipeline

### Added
- FastAPI OCR service với PaddleOCR + OpenCV (deskew + denoise).
- Auto-trigger OCR sau khi register inbound document.
- `ocr_jobs` + `ocr_results` + `ocr_extracted_fields` với async callback.
- `OcrAutoTriggerListener` với @TransactionalEventListener(AFTER_COMMIT).

## [0.5.0] — Phase 5: Workflow + Outbox + RabbitMQ

### Added
- Assignment + Notifications.
- Outbox pattern + @Scheduled publisher với exponential backoff.
- Idempotent consumer dedup theo message_id.
- Cross-module events qua @TransactionalEventListener.

## [0.4.0] — Phase 4: VB đi với versioning

### Added
- Document versioning với `content_snapshot` immutable trigger (BR-07).
- 2-tier approval: phòng → đơn vị.
- Approval append-only.
- BR-07 freeze approved version.

## [0.3.0] — Phase 3: VB đến

### Added
- MinIO storage với magic bytes validation.
- SHA-256 file integrity.
- BR-03 (confidentiality + RBAC), BR-05 (24h register), BR-09 (soft delete),
  BR-11 (recall with reason).

## [0.2.0] — Phase 2: Master data

### Added
- 29 document types theo NĐ 30/2020.
- 4 confidentiality levels, 4 priority levels.
- Document books với race-free numbering (BR-01/02): INSERT ON CONFLICT
  + SELECT FOR UPDATE counter.

## [0.1.0] — Phase 1: Auth

### Added
- Argon2id password hashing.
- JWT RS256 (Nimbus JOSE) + refresh rotation với reuse detection.
- 8 roles + ~40 permissions (RBAC).
- BR-12: 5-fail lockout 15 phút (Bucket4j).
- HttpOnly + SameSite=Strict cookie cho refresh.
- Bootstrap admin user từ env.

## [0.0.0] — Phase 0: Setup

### Added
- Khởi tạo monorepo cấu trúc `backend/`, `frontend/`, `ocr-service/`,
  `db/migration/`, `docker/`, `scripts/`, `.github/workflows/`.
- Docker Compose stack: Postgres 16, Redis 7, RabbitMQ 3.13, MinIO,
  Backend, Frontend, OCR.
- CI workflows: backend-ci, frontend-ci, ocr-ci, security-scan (Trivy).
- Lombok 1.18.38 (JDK 24 compat).
