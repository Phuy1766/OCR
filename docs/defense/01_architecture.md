# Kiến trúc hệ thống

## 1. Tổng quan

Hệ thống được thiết kế theo **modular monolith** trên Spring Boot 3.2 với 12 Maven modules, dùng **event-driven architecture** trong nội bộ ứng dụng để giảm coupling giữa các nghiệp vụ.

```
┌──────────────────────────────────────────────────────────────┐
│  Next.js 14 App Router (TypeScript strict)                   │
│  - TanStack Query v5 (state) + Zustand (auth store)          │
│  - shadcn/ui + TailwindCSS + dark mode (system/light/dark)   │
│  - Refresh token rotation qua HttpOnly cookie SameSite=Strict│
└─────────────────────────┬────────────────────────────────────┘
                          │ HTTPS / JSON
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  Spring Boot 3.2 / Java 21                                   │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Layer Web/Controller — @RestController + @PreAuthorize │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ Layer Service — @Service + @Transactional              │  │
│  ├────────────────────────────────────────────────────────┤  │
│  │ Layer Repository — Spring Data JPA + native queries    │  │
│  └────────────────────────────────────────────────────────┘  │
│  Cross-cutting:                                              │
│  - JWT RS256 (Nimbus JOSE) + Argon2id (argon2-jvm)           │
│  - Outbox pattern + @Scheduled publisher → RabbitMQ          │
│  - @TransactionalEventListener(AFTER_COMMIT) cross-module    │
│  - SignatureGate (Optional<Bean>) — gateway pattern          │
└─────┬─────────────┬─────────────┬─────────────┬──────────────┘
      │             │             │             │
      ▼             ▼             ▼             ▼
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌──────────────┐
│Postgres │  │ Redis 7 │  │RabbitMQ │  │FastAPI OCR   │
│  16     │  │ token   │  │ outbox  │  │ PaddleOCR    │
│ +FTS    │  │blacklist│  │ events  │  │ +OpenCV      │
│ +pg_trgm│  │+ratelimit│ │         │  │              │
└─────────┘  └─────────┘  └─────────┘  └──────────────┘
      ▲
      │
┌─────┴───────────────┐
│ MinIO (S3-compat)   │
│ document files +    │
│ signatures          │
└─────────────────────┘
```

## 2. Multi-module design

12 Maven modules, dependency tree theo Domain-Driven Design:

```
common (no deps)
  ↑
auth (common) — JWT + RBAC + AuthPrincipal
  ↑
masterdata (common, auth) — orgs, books, document types
  ↑                ↑
inbound       outbound — VB đến/đi (parallel domains)
  │ │           │ │
  │ │           │ │
  ▼ ▼           ▼ ▼
workflow (inbound, outbound, common, auth) — assignment, notifications
  ↑
ocr (inbound, common) — auto-trigger + extraction
signature (outbound, common) — PAdES, PKCS#12
search (inbound, outbound, common) — FTS unified
audit (common, auth) — audit log + dashboard
integration (all) — cross-module event listeners

app (all + flyway + actuator + springdoc) — Spring Boot assembly
```

**Vì sao modular monolith thay vì microservices?**

- Đề tài NCKH cấp trường → cần demo được trên 1 máy.
- Nghiệp vụ tightly coupled (1 VB đi qua nhiều module).
- Transaction boundaries dễ giữ ACID.
- Refactor sang microservices khi cần scale: tách `congvan-ocr` (đã async qua queue) hoặc `congvan-signature` (CPU-intensive PAdES).

## 3. Pattern then chốt

### 3.1 Outbox Pattern (Phase 5)

**Vấn đề**: nghiệp vụ thay đổi DB + cần publish message đến RabbitMQ. Nếu DB commit OK nhưng publish thất bại (network, broker down), message mất → data drift.

**Giải pháp**: lưu message vào table `outbox_messages` cùng transaction với business write. Background scheduler quét table và publish:

```java
@Transactional
public void registerInbound(...) {
    documentRepo.save(doc);        // 1. business write
    outboxRepo.save(new OutboxMessage(  // 2. message in same TX
        "inbound.registered", payload));
}                                  // 3. commit hoặc rollback together

@Scheduled(fixedDelayString = "${app.outbox.poll-interval:2000}")
public void publishPendingMessages() {  // 4. async publisher
    outboxRepo.findUnpublished(50).forEach(msg -> {
        rabbitTemplate.convertAndSend(...);
        msg.markPublished();
    });
}
```

Exponential backoff khi RabbitMQ unreachable. Idempotent consumer (deduplication theo `message_id`).

### 3.2 SignatureGate — Optional<Bean> để decouple module

**Vấn đề**: `congvan-outbound` cần check chữ ký trước khi `issue()`. Nhưng outbound không thể depend on signature module (đảo ngược).

**Giải pháp**: định nghĩa `SignatureGate` interface trong common, signature module implement:

```java
// common module
public interface SignatureGate {
    void requireBothSignaturesForIssue(UUID documentVersionId);
}

// outbound module
@Service
public class OutboundService {
    private final Optional<SignatureGate> signatureGate;
    
    public void issue(UUID id) {
        signatureGate.ifPresent(g -> 
            g.requireBothSignaturesForIssue(...));
        // proceed with issue
    }
}

// signature module
@Service
@ConditionalOnProperty(value = "app.signature.gate-enabled", 
                       havingValue = "true", matchIfMissing = true)
public class SignatureGateImpl implements SignatureGate { ... }
```

Test isolation: `application-test.yml` set `app.signature.gate-enabled=false` cho các test không liên quan signature, override `=true` trong `SignatureFlowIntegrationTest`.

### 3.3 Cross-module events qua @TransactionalEventListener

**Vấn đề**: khi VB đến đăng ký xong → cần trigger OCR. OCR module không thể tightly coupled với inbound.

**Giải pháp**: inbound publish event sau commit, OCR module listen async:

```java
// inbound
applicationEventPublisher.publishEvent(
    new InboundRegisteredEvent(documentId));

// ocr module
@Async
@TransactionalEventListener(phase = AFTER_COMMIT)
public void onInboundRegistered(InboundRegisteredEvent event) {
    ocrService.submitJob(event.documentId());
}
```

`AFTER_COMMIT` đảm bảo OCR chỉ chạy sau khi business TX đã commit (tránh race condition với reader).

### 3.4 Race-free document numbering (BR-01, BR-02)

**Vấn đề**: cấp số VB phải unique, không phụ thuộc đếm lại từ MAX(number) + 1 (race khi concurrent).

**Giải pháp 2 lớp**:

1. Counter row per `(book_id, year)` với `INSERT ... ON CONFLICT DO NOTHING`.
2. `SELECT FOR UPDATE` lock row, `UPDATE last_number = last_number + 1 RETURNING last_number`.
3. UNIQUE constraint `(book_id, year, number)` là last line of defense.

```sql
INSERT INTO document_book_counters (book_id, year, last_number)
VALUES (?, ?, 0)
ON CONFLICT (book_id, year) DO NOTHING;

SELECT last_number FROM document_book_counters
WHERE book_id = ? AND year = ? FOR UPDATE;
-- Application increments and persists.
```

### 3.5 Immutable versioning (BR-07)

**Vấn đề**: VB đi đã duyệt không được phép sửa nội dung → đảm bảo ở DB, không tin code.

**Giải pháp**: trigger PostgreSQL block UPDATE `content_snapshot` của `document_versions` mà có `approval_status = 'APPROVED'`.

```sql
CREATE TRIGGER prevent_approved_version_update
BEFORE UPDATE ON document_versions
FOR EACH ROW WHEN (OLD.approval_status = 'APPROVED')
EXECUTE FUNCTION raise_immutable_violation();
```

### 3.6 Append-only audit log (BR-10)

Trigger block UPDATE/DELETE trên `audit_logs`. Chỉ INSERT. Mọi business mutation publish event để ghi audit:

```java
@TransactionalEventListener(AFTER_COMMIT)
public void onAnyMutation(AuditEvent e) {
    auditRepo.save(new AuditLog(...));  // INSERT only
}
```

## 4. Data flow tiêu biểu — VB đến từ tiếp nhận đến lưu trữ

```
1. POST /api/inbound-documents (multipart: data + files)
   ├─ DocumentService.create() → INSERT documents (status=RECEIVED)
   ├─ MinioService.upload() → file stored
   ├─ DocumentFileService.save() → INSERT document_files (sha256, magic_bytes)
   ├─ AuditEvent published
   └─ InboundReceivedEvent published

2. @TransactionalEventListener(AFTER_COMMIT) async
   └─ OcrService.submitJob() → INSERT ocr_jobs (status=PENDING)
      └─ OcrClient.submit(file) → FastAPI PaddleOCR

3. FastAPI callback /api/ocr-jobs/{id}/callback
   ├─ INSERT ocr_results + ocr_extracted_fields
   └─ Job status → COMPLETED

4. Văn thư accept OCR → metadata áp vào document

5. POST /api/inbound-documents/{id}/register
   ├─ MasterDataService.allocateNextNumber() → BR-01/02
   ├─ INSERT document_book_entries (sequential number)
   ├─ documents.book_number, book_year
   └─ status → REGISTERED

6. POST /api/workflow/assign
   ├─ INSERT assignments
   ├─ INSERT notifications (recipient: chuyên viên)
   └─ outbox.publish('workflow.assigned')

7. POST /api/assignments/{id}/complete
   ├─ assignments.status = COMPLETED
   ├─ INSERT notifications (recipient: trưởng phòng)
   └─ status update
```

## 5. Frontend architecture

```
app/
├── (auth)/login/      Public route — login form
├── dashboard/
│   ├── layout.tsx     Auth-gated nav + theme toggle
│   ├── page.tsx       Stats + recent docs + my tasks
│   ├── inbound/       List + detail + new
│   ├── outbound/      List + detail + new + approve + sign + issue
│   ├── tasks/         My assignments
│   ├── search/        FTS với fuzzy fallback
│   └── admin/         Master data management
├── error.tsx          Global error boundary
└── not-found.tsx      Custom 404

components/
├── ui/                shadcn primitives (button, card, input, ...)
├── theme-provider.tsx light/dark/system + FOUC-free init script
└── notification-bell.tsx + pagination + empty-state + skeleton

hooks/
├── use-auth.ts        useMe, useLogin, useRefresh, useLogout
├── use-inbound.ts     CRUD + invalidation
├── use-outbound.ts    + approve/reject/sign/issue mutations
├── use-workflow.ts    assignments + notifications
├── use-search.ts      FTS query với debounce
└── use-dashboard.ts   stats với staleTime 30s

lib/
├── api-client.ts      Axios instance + 401 interceptor → auto-refresh
└── utils.ts           cn() từ tailwind-merge

stores/
└── auth-store.ts      Zustand: accessToken in-memory, user, hasPermission
```

## 6. Database schema

18 Flyway migrations, ~30 tables. Xem [`backend/congvan-app/src/main/resources/db/migration/`](../../backend/congvan-app/src/main/resources/db/migration/).

Highlights:
- Extensions: `unaccent`, `pg_trgm`, `pgcrypto`, `btree_gin`, `uuid-ossp`.
- Custom FTS config `vietnamese` (simple + unaccent stop-words handling).
- `documents.search_vector` + `ocr_results.search_vector` — tsvector tự cập nhật qua trigger.
- GIN indexes cho fulltext + pg_trgm cho fuzzy.
- Triggers: prevent immutable updates (audit_logs, document_versions when approved).

## 7. Test strategy

- **Unit tests** (per module): logic thuần, không Spring context.
- **Integration tests** (congvan-app): full Spring context + Testcontainers (Postgres, Redis, MinIO, RabbitMQ).
- **Contract**: 38 integration tests covering happy path + edge cases cho mỗi business rule.
- **Coverage aggregate**: 78.4% lines, 73.4% methods.
- **CI**: GitHub Actions chạy `mvn verify` + `pnpm build` + Trivy security scan trên PR/push.
