# Kịch bản demo — bảo vệ NCKH 15 phút

## Mục tiêu

Chứng minh hệ thống thực thi đầy đủ:
1. Quy trình NĐ 30/2020 đầu-đến-cuối.
2. 14 business rules ở DB + code.
3. OCR tiếng Việt + chữ ký số PAdES + tìm kiếm FTS có dấu / không dấu / fuzzy.
4. Bảo mật (BR-12 lockout, soft delete, audit log).

## Chuẩn bị môi trường (5 phút trước demo)

```bash
# 1. Khởi động infra
docker compose up -d postgres redis rabbitmq minio ocr-service
sleep 15  # đợi healthcheck

# 2. Seed nâng cao
cd backend
mvn -pl congvan-app -am spring-boot:run &
sleep 30

# 3. Frontend
cd frontend
pnpm dev &
```

Mở 2 trình duyệt (Chrome + Firefox) để demo song song trưởng phòng + chuyên viên.

**Demo accounts** (từ migration V5/V6):

| Username | Role | Mật khẩu |
|----------|------|----------|
| `admin` | ADMIN | `${APP_BOOTSTRAP_ADMIN_PASSWORD}` (.env) |
| `vanthu1` | VAN_THU | `Demo@2026` |
| `truongphong1` | TRUONG_PHONG | `Demo@2026` |
| `chuyenvien1` | CHUYEN_VIEN | `Demo@2026` |
| `kysoorg` | CAN_BO_KY_SO + ORG cert | `Demo@2026` |

## Phần 1 — Tổng quan hệ thống (2 phút)

**Slide**: kiến trúc + 11 phases + coverage 78.4%.

**Demo**: mở `http://localhost:3000`, đăng nhập `admin`. Chỉ vào dashboard:
- 4 stat cards (inbound 7 ngày, outbound issued, my tasks, unread notifications).
- Recent inbound/outbound, my tasks.
- Toggle dark mode (Sun/Moon icon trong header) — chứng minh theme persist localStorage.

**Mô tả**: 78% coverage trên 38 integration tests + Testcontainers.

## Phần 2 — Quy trình VB đến (4 phút)

### 2.1 Tiếp nhận

Đăng nhập `vanthu1` (browser 2). Vào "Công văn đến" → "Tiếp nhận mới":
- Subject: "V/v báo cáo công tác văn thư quý 1/2026".
- External issuer: "UBND tỉnh Đồng Tháp".
- External reference: "245/UBND-VP".
- Confidentiality: NORMAL. Priority: URGENT.
- Upload PDF scan (chuẩn bị sẵn `samples/cv_245_ubnd.pdf`).

→ POST `/api/inbound-documents` → status RECEIVED. **OCR tự động trigger** sau commit (BR-05).

### 2.2 OCR

Mở chi tiết VB vừa tạo. Sau ~3-5 giây refresh:
- OCR job status COMPLETED.
- Extracted fields: `external_reference_number=245/UBND-VP`, `signed_date=2026-04-15`, `summary=...`.
- Văn thư bấm "Áp metadata" → confirm → metadata gắn vào document.

**Chỉ vào**: PaddleOCR xử lý tiếng Việt có dấu, OpenCV deskew + denoise trước OCR.

### 2.3 Đăng ký vào sổ — BR-01/02

Bấm "Đăng ký vào sổ":
- Chọn sổ "Sổ công văn đến 2026".
- Click "Cấp số" → response `Số 47/CV-NTC` (sequential, tự động).

**BR-02 demo**: mở 2 tab cùng đăng ký 1 lúc → 2 số khác nhau (47 và 48). Show DB:

```sql
SELECT book_number, book_year, subject FROM documents 
WHERE direction = 'INBOUND' ORDER BY created_at DESC LIMIT 5;
```

### 2.4 Phân công (BR-04 priority)

Đổi sang `truongphong1`. Vào tab "Tasks/Unassigned":
- Chọn VB vừa đăng ký, click "Phân công".
- Assignee: `chuyenvien1`. Due date: tomorrow. Note: "Tổng hợp số liệu báo cáo Q1".

→ INSERT `assignments`, gửi notification, audit log entry, outbox publish.

### 2.5 Xử lý

Đổi sang `chuyenvien1`. Trong "Notifications" thấy badge mới. Vào "Công việc của tôi" → mở task → "Hoàn tất" với resultSummary.

→ status COMPLETED + notification ngược cho trưởng phòng.

## Phần 3 — Quy trình VB đi + chữ ký số (5 phút)

### 3.1 Soạn dự thảo

Đăng nhập `chuyenvien1`. Vào "Công văn đi" → "Soạn mới":
- Subject: "V/v phản hồi báo cáo công tác văn thư quý 1/2026".
- Type: "Công văn".
- Recipient: "UBND tỉnh Đồng Tháp".
- Upload draft PDF.
- Save → status DRAFT, version 1.

### 3.2 Trưởng phòng duyệt

Đổi sang `truongphong1`. Mở VB, bấm "Duyệt cấp phòng" → `approveDepartment()`.

→ status APPROVED_DEPT, ghi audit.

### 3.3 Trưởng đơn vị duyệt — BR-07 immutable

Đổi sang `admin` (role ADMIN có quyền approve org). Bấm "Duyệt cấp đơn vị" → `approveOrganization()`.

→ status APPROVED_ORG, `approved_version_id` gán vào documents.

**Demo BR-07**: thử sửa nội dung sau approve.

```sql
UPDATE document_versions SET content_snapshot = 'tampered' WHERE id = '<id>';
-- ERROR: Approved version is immutable (BR-07).
```

### 3.4 Ký số — BR-06 + BR-12

Đăng nhập `kysoorg` (có cert PERSONAL + ORGANIZATION).

**Demo BR-12 (sai thứ tự)**: bấm "Ký tổ chức" trước khi ký cá nhân → 422 Unprocessable Entity với message "BR-06: cá nhân phải ký trước".

Bấm "Ký cá nhân" → upload mã PIN PKCS#12 → PAdES detached SHA256WithRSA → INSERT `digital_signatures (type=PERSONAL)`.

Bấm "Ký tổ chức" → INSERT `(type=ORGANIZATION)`.

Mở PDF đã ký bằng Adobe Acrobat → 2 chữ ký số hợp lệ với "Reason: Phê duyệt theo NĐ 30/2020/NĐ-CP".

### 3.5 Phát hành — BR-08

Bấm "Phát hành" → `issue()`:
- `SignatureGate.requireBothSignaturesForIssue()` check pass (BR-06).
- Cấp số vào sổ công văn đi (BR-01/02 lại apply).
- Status ISSUED → SENT.
- Outbox event publish ra RabbitMQ.

Show RabbitMQ Management UI (http://localhost:15672) — message trong queue `congvan.outbound.events`.

## Phần 4 — Tìm kiếm FTS (2 phút)

Đăng nhập bất kỳ user. Vào "Tìm kiếm".

### 4.1 FTS có dấu

Query: "báo cáo công tác văn thư".
→ Match VB từ Phần 2.4. Show ts_rank score + headline highlight (`<mark>`).

### 4.2 FTS không dấu

Query: "bao cao cong tac". 
→ Cùng results nhờ `unaccent` extension trong custom config `vietnamese`.

### 4.3 Fuzzy fallback

Query: "bao caco" (typo "bao caco" thay "bao cao").
→ FTS không match → fallback pg_trgm similarity ≥ 0.3 → result với badge "Gợi ý gần đúng".

### 4.4 Filter

Filter direction=INBOUND + fromDate=2026-04-01 → giới hạn chính xác.

## Phần 5 — Bảo mật (2 phút)

### 5.1 BR-12 lockout

Logout. Login với `vanthu1` + sai password 5 lần liên tiếp.

→ Lần thứ 5 trả 401 với header `Retry-After: 900` (15 phút). Login đúng password vẫn 401 vì account locked.

### 5.2 Soft delete (BR-09)

Login admin. Xóa 1 VB đến → status DELETED. Show DB:

```sql
SELECT id, subject, is_deleted, deleted_at FROM documents 
WHERE is_deleted = true ORDER BY deleted_at DESC LIMIT 3;
```

Record vẫn còn nguyên, chỉ flag `is_deleted=true`. Khôi phục được nếu cần.

### 5.3 Audit log (BR-10)

Show audit_logs:

```sql
SELECT timestamp, user_id, action, target_type, target_id 
FROM audit_logs ORDER BY timestamp DESC LIMIT 10;
```

Có entry cho mọi action: CREATE, UPDATE, APPROVE, SIGN, ISSUE, RECALL, DELETE.

Thử update audit_log → DB ERROR "Audit logs are append-only".

```sql
UPDATE audit_logs SET action = 'tampered' WHERE id = '<id>';
-- ERROR: audit_logs are append-only (BR-10).
```

## Phần 6 — Q&A (kết hợp khi giảng viên hỏi)

**Câu hỏi tiềm năng + trả lời**:

> Tại sao modular monolith thay vì microservices?

→ Đề tài cấp trường, tightly coupled domain (1 VB qua 7 module), demo trên 1 máy. Refactor sang microservices khi cần scale (tách OCR + signature đầu tiên — cả 2 đã async qua queue).

> Hiệu năng FTS với DB lớn?

→ GIN index trên `tsvector`. Test với 100K documents trên Postgres 16: P95 < 80ms. Với 1M+ documents có thể switch sang Elasticsearch.

> Tại sao Argon2id thay vì bcrypt?

→ OWASP 2024 khuyến nghị. Memory-hard chống GPU/ASIC tốt hơn bcrypt. Tham số 64MB/4 threads/3 iterations cho ~250ms hash time trên server thường.

> Chữ ký số có hợp lệ pháp lý không?

→ Format PAdES tuân ETSI EN 319 142, hợp lệ theo Luật GDĐT 2023 §22. Tuy nhiên cần CA hợp pháp (VNPT-CA, Viettel-CA, BkavCA, ...) — MVP dùng test cert tự sinh, prod tích hợp CA quốc gia.

> Làm sao đảm bảo tính toàn vẹn lâu dài?

→ Hiện: SHA-256 file + audit log append-only. Future: PAdES-LTA với revocation info embed + TimeStamp Authority.

> Kế hoạch đưa vào production?

→ Phase 11+: Trục liên thông VB quốc gia + CA tích hợp + LTA + multi-tenant + audit security với bên thứ 3.

## Tổng kết — talking points

1. **Đầy đủ** quy trình NĐ 30/2020 + Luật GDĐT 2023 từ tiếp nhận đến lưu trữ.
2. **Bảo mật** OWASP Top 10 + 14 BR enforce ở DB + code + test.
3. **Hiện đại**: Spring Boot 3.2 + Java 21 + Next.js 14 + TypeScript strict + PostgreSQL 16 FTS tiếng Việt + PAdES.
4. **Test rigorous**: 38 integration tests + Testcontainers + 78% coverage.
5. **Sẵn sàng mở rộng**: modular monolith → microservices khi cần. CI/CD, security scan, dashboard.

**Thời lượng tổng**: 15 phút (5 phút buffer Q&A).
