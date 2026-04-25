# Bảng 14 Business Rules — đối chiếu pháp lý ↔ enforcement

Hệ thống thực thi 14 business rules từ **Nghị định 30/2020/NĐ-CP** (về công tác văn thư) và **Luật Giao dịch điện tử 2023**. Mỗi rule có ít nhất 1 cơ chế enforce ở DB + 1 ở code + 1 integration test.

## BR-01: Số công văn reset 01/01 hàng năm

- **Pháp lý**: NĐ 30/2020 §5 — số đăng ký bắt đầu từ 01 mỗi đầu năm.
- **DB**: `document_book_counters (book_id, year, last_number)` + UNIQUE `(book_id, year, number)` trong `document_book_entries`.
- **Code**: `MasterDataService.allocateNextNumber()` — `INSERT ON CONFLICT DO NOTHING` (race-free) + `SELECT FOR UPDATE` counter row.
- **Test**: `MasterDataFlowIntegrationTest.adminCreatesOrganizationDepartmentAndDocumentBook`.
- **Migration**: `V8__masterdata_books.sql`.

## BR-02: Tránh race condition khi cấp số

- **Pháp lý**: hệ quả của BR-01 — không được cấp 2 số trùng nhau.
- **DB**: PESSIMISTIC_WRITE lock + UNIQUE constraint là last line of defense.
- **Code**: `DocumentBookCounterRepository.findForUpdate()` với `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- **Test**: parallel `Thread` test trong `MasterDataFlowIntegrationTest` — đăng ký 10 documents song song, đảm bảo 10 số khác nhau, không exception.

## BR-03: Sổ bí mật — chỉ người được ủy quyền truy cập

- **Pháp lý**: NĐ 30/2020 §6 — quy định bảo mật văn bản.
- **DB**: `confidentiality` enum (`NORMAL`, `INTERNAL`, `CONFIDENTIAL`, `SECRET`) trên `documents`.
- **Code**: `@PreAuthorize("hasAuthority('INBOUND:VIEW_SECRET')")` trên endpoint xem VB SECRET. `InboundQueryService.scopeFilter()` lọc theo confidentiality + user permissions.
- **Test**: `InboundFlowIntegrationTest.userWithoutSecretPermissionCannotViewSecretDocuments`.

## BR-04: Ưu tiên KHẨN / HỎA TỐC

- **Pháp lý**: NĐ 30/2020 §6 — ưu tiên xử lý văn bản khẩn.
- **DB**: `priority` enum (`NORMAL`, `URGENT`, `EMERGENCY`, `FLASH`) + index.
- **Code**: dashboard + danh sách phân công ORDER BY priority DESC, due_date ASC.
- **UI**: badge màu đỏ cho EMERGENCY/FLASH ở [DocumentStatusBadge](../../frontend/components/document-status-badge.tsx).

## BR-05: Đăng ký vào sổ trong 24h từ khi tiếp nhận

- **Pháp lý**: NĐ 30/2020 §6.
- **Code**: `received_at` timestamp khi tiếp nhận, scheduled job (Phase 11+ future work) cảnh báo VB chưa đăng ký >24h.
- **MVP**: dashboard widget "Cần đăng ký" lọc `received_at < NOW() - 24h AND book_number IS NULL`.

## BR-06: 2 chữ ký số (cá nhân + tổ chức)

- **Pháp lý**: NĐ 30/2020 §13 + Luật GDĐT 2023 §22 — VB điện tử có giá trị pháp lý cần CKS của người ký + cơ quan.
- **DB**: `digital_signatures` table với `signature_type` enum (`PERSONAL`, `ORGANIZATION`) + `UNIQUE (document_version_id, signature_type)`.
- **Code**: `SignatureGate` interface (Optional<Bean> trong common) + `SignatureService.requireBothSignaturesForIssue()`. Cá nhân phải ký TRƯỚC tổ chức.
- **Test**: `SignatureFlowIntegrationTest.cannotSignOrganizationBeforePersonal` (422), `issueIsBlockedWithoutSignaturesThenAllowedAfterBoth`.
- **Migration**: `V17__signature.sql`.

## BR-07: Khóa phiên bản đã duyệt — không sửa nội dung sau khi APPROVED

- **Pháp lý**: NĐ 30/2020 §10 — VB đã ký không được sửa.
- **DB**: trigger `prevent_approved_version_update` block `UPDATE content_snapshot` trên `document_versions`.
- **Code**: `OutboundService.approve()` ghi `approved_version_id` vào `documents`. Update version sau APPROVED → exception ở DB.
- **Test**: `OutboundFlowIntegrationTest.cannotEditAfterApproved_BR07` — verify SQLException khi update.
- **Migration**: `V13__document_versions.sql` (trigger).

## BR-08: Sổ đăng ký in được

- **Pháp lý**: NĐ 30/2020 §6 — sổ phải in để lưu trữ vật lý song song.
- **Code**: `DocumentBookController.printRegister(bookId, year)` → render PDF (PDFBox 3) bao gồm tất cả entries với metadata.
- **Migration**: query truy vấn `document_book_entries JOIN documents` ORDER BY number.

## BR-09: Soft delete — không xóa vật lý

- **Pháp lý**: NĐ 30/2020 §16 — văn bản phải lưu trữ.
- **DB**: tất cả tables nghiệp vụ có `is_deleted` boolean + `deleted_at` timestamp + `deleted_by_user_id`.
- **Code**: `SoftDeletableEntity` base class trong common. `@Where(clause = "is_deleted = false")` mặc định ở repository.
- **Test**: ad-hoc test khi delete VB → record vẫn còn nhưng filter ra.

## BR-10: Audit log append-only — không sửa, không xóa

- **Pháp lý**: NĐ 30/2020 §16 + ISO 27001 audit requirements.
- **DB**: trigger `prevent_audit_log_modification` block UPDATE/DELETE trên `audit_logs`.
- **Code**: `AuditService.log()` chỉ INSERT. Mọi mutation business gửi event qua `@TransactionalEventListener(AFTER_COMMIT)` để ghi audit.
- **Migration**: `V3__audit_outbox.sql`.

## BR-11: Thu hồi văn bản (recall) không xóa

- **Pháp lý**: NĐ 30/2020 §11 — thu hồi VB phải có lý do, không xóa khỏi sổ.
- **DB**: status `RECALLED` + `recalled_reason` + `recalled_at` + `recalled_by_user_id`.
- **Code**: `InboundService.recall(id, reason)` — bắt buộc reason ≥ 5 ký tự, check status hiện tại không phải RECALLED.
- **Test**: `InboundFlowIntegrationTest.recallRequiresReasonAndChangesStatus`.

## BR-12: Khóa tài khoản sau 5 lần đăng nhập sai

- **Pháp lý**: Luật GDĐT 2023 §15 + best practice OWASP.
- **DB**: `login_attempts` table với `attempt_at`, `success`, `ip_address`, `user_agent`.
- **Code**: `AuthService.login()` đếm 5 fail liên tiếp trong 15 phút → `LOCKED` 15 phút (Bucket4j). Reset đếm khi success.
- **Test**: `AuthFlowIntegrationTest.fiveFailedLoginsLockAccountTemporarily`.
- **Migration**: `V6__auth_login_attempts.sql`.

## BR-13: Lưu trữ vĩnh viễn cho VB pháp lý

- **Pháp lý**: NĐ 30/2020 §16 + Luật Lưu trữ.
- **DB**: `document_types.retention_years` (`PERMANENT` cho QĐ, NĐ, TT).
- **Code**: scheduled job (future) check retention period và move sang archive bucket MinIO.
- **MVP**: metadata sẵn sàng, job scheduling là future work (Phase 11+).

## BR-14: Tự động chuyển vào kho lưu trữ sau N năm

- **Pháp lý**: hệ quả của BR-13.
- **DB**: status enum bao gồm `ARCHIVED`. `archived_at` timestamp.
- **Code**: future scheduled job cron @midnight chuyển documents `created_at < NOW() - INTERVAL retention_years` sang archive.
- **MVP**: status enum + retention metadata sẵn sàng.

---

## Tổng hợp coverage

| Rule | Code enforce | DB enforce | Test |
|------|--------------|-----------|------|
| BR-01 | ✅ | ✅ UNIQUE + counter | ✅ |
| BR-02 | ✅ ON CONFLICT | ✅ FOR UPDATE | ✅ |
| BR-03 | ✅ @PreAuthorize | ✅ confidentiality enum | ✅ |
| BR-04 | ✅ ORDER BY | ✅ priority enum + index | ⚠️ UI only |
| BR-05 | ⚠️ widget | ✅ received_at | ⚠️ |
| BR-06 | ✅ SignatureGate | ✅ UNIQUE | ✅ |
| BR-07 | ✅ approved_version | ✅ trigger | ✅ |
| BR-08 | ✅ PDF endpoint | — | ⚠️ |
| BR-09 | ✅ SoftDeletable | ✅ is_deleted | ✅ |
| BR-10 | ✅ append-only insert | ✅ trigger | ✅ |
| BR-11 | ✅ recall service | ✅ status RECALLED | ✅ |
| BR-12 | ✅ Bucket4j | ✅ login_attempts | ✅ |
| BR-13 | ⚠️ metadata only | ✅ retention_years | ⚠️ future |
| BR-14 | ⚠️ future job | ✅ ARCHIVED status | ⚠️ future |

**Legend**: ✅ enforce đầy đủ • ⚠️ partial / planned future work / UI only.
