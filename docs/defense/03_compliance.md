# Tuân thủ pháp lý

## 1. Khung pháp lý áp dụng

| Văn bản | Phạm vi | Mức độ |
|---------|---------|--------|
| **Nghị định 30/2020/NĐ-CP** về công tác văn thư | Quy trình xử lý VB, sổ đăng ký, chữ ký, lưu trữ | **Bắt buộc** |
| **Luật Giao dịch điện tử 2023** (số 20/2023/QH15) | Hiệu lực CKS, dữ liệu điện tử | **Bắt buộc** |
| **Luật An toàn thông tin mạng 2015** (số 86/2015/QH13) | Bảo mật, dữ liệu cá nhân | **Bắt buộc** |
| **Luật Lưu trữ 2011** (số 01/2011/QH13) | Lưu trữ tài liệu | Áp dụng cho retention |
| TT 27/2017/TT-BTTTT | Trục liên thông VB quốc gia | Future work |
| TT 41/2017/TT-BTTTT | Quy trình ký số | Tham chiếu |

## 2. Đối chiếu NĐ 30/2020/NĐ-CP

### Điều 5 — Số văn bản

> "Số của văn bản được đánh liên tục theo thứ tự văn bản được phát hành trong một năm, bắt đầu từ số 01 vào ngày đầu năm và kết thúc vào ngày 31 tháng 12 hàng năm."

**Implement**: BR-01 — counter per `(book_id, year)`. Migration `V8`.

### Điều 6 — Tiếp nhận văn bản đến

> "Văn bản đến từ bất kỳ nguồn nào đều phải được tập trung tại Văn thư cơ quan, tổ chức để làm thủ tục tiếp nhận, đăng ký."

**Implement**:
- Endpoint `POST /api/inbound-documents` — văn thư tạo VB đến với role `VAN_THU`.
- BR-05 — đăng ký vào sổ trong 24h (widget cảnh báo).
- BR-09 — soft delete (không xóa vật lý).

### Điều 10 — Soạn thảo văn bản

> "Văn bản đã được người có thẩm quyền ký không được sửa chữa hoặc thay đổi nội dung."

**Implement**: BR-07 — DB trigger block UPDATE `content_snapshot` khi `approval_status = 'APPROVED'`.

### Điều 11 — Thu hồi văn bản

> "Văn bản đã ký phát hành nhưng phát hiện sai sót về nội dung hoặc thể thức phải được thu hồi, sửa chữa, ký phát hành lại."

**Implement**: BR-11 — status `RECALLED` + lý do bắt buộc + audit log. Không xóa khỏi sổ.

### Điều 12 — Phân quyền văn thư

> "Người đứng đầu cơ quan quyết định phân công văn thư..."

**Implement**: 8 roles (ADMIN, VAN_THU, TRUONG_DON_VI, TRUONG_PHONG, CHUYEN_VIEN, CAN_BO_KY_SO, KIEM_TRA_VIEN, USER) + ~40 permissions với scope `OWN/DEPARTMENT/ORGANIZATION/ALL`.

### Điều 13 — Chữ ký số trên văn bản điện tử

> "Văn bản điện tử có giá trị pháp lý khi được ký bởi cả chữ ký số của người có thẩm quyền và chữ ký số của cơ quan, tổ chức..."

**Implement**: BR-06 — `digital_signatures` UNIQUE per `(version_id, type)`. Cá nhân ký trước, tổ chức ký sau (BR-12).

### Điều 16 — Quản lý văn bản đi/đến trong môi trường điện tử

> "Phải bảo đảm tính toàn vẹn, không sửa đổi của văn bản... Việc lưu trữ văn bản điện tử phải tuân thủ pháp luật về lưu trữ."

**Implement**:
- BR-10 — audit log append-only (DB trigger).
- BR-13/14 — retention metadata + ARCHIVED status (job lưu trữ là future work).
- File storage SHA-256 integrity check.

## 3. Đối chiếu Luật GDĐT 2023

### Điều 15 — Bảo mật trong giao dịch điện tử

> "Cơ quan, tổ chức cung cấp dịch vụ giao dịch điện tử có trách nhiệm: ...áp dụng các biện pháp bảo đảm an toàn thông tin..."

**Implement**:
- HTTPS bắt buộc (HSTS).
- Argon2id mật khẩu.
- BR-12 lockout sau 5 lần sai.
- Refresh token rotation + reuse detection.

### Điều 22 — Hiệu lực pháp lý của chữ ký số

> "Chữ ký số có hiệu lực pháp lý khi: a) Được tạo bởi khóa bí mật của chủ thể ký...; b) Được kiểm tra bằng chứng thư số tương ứng..."

**Implement**: PAdES detached SHA256WithRSA + verification chain. Future: tích hợp CA quốc gia (VNPT-CA, Viettel-CA, BkavCA).

### Điều 23 — Lưu trữ thông điệp dữ liệu

> "Thông điệp dữ liệu được lưu trữ phải đáp ứng: a) Có thể truy cập, sử dụng để tham chiếu khi cần; b) Có thể duy trì format gốc..."

**Implement**:
- MinIO lưu file gốc bất biến.
- `document_versions.content_snapshot` lưu metadata snapshot tại thời điểm duyệt.
- BR-10 audit log append-only.

## 4. Đối chiếu Luật ATTTM 2015

### Điều 17 — Bảo vệ thông tin cá nhân

**Implement**:
- Password hash Argon2id (không lưu plain text).
- Audit log redact các field `password`, `tokenHash` trước khi ghi.
- RBAC + scope filter để mỗi user chỉ xem thông tin được phép.

### Điều 21 — Phòng chống tấn công mạng

**Implement**:
- Rate limit Bucket4j 10 req/phút per IP cho auth endpoints.
- BR-12 lockout sau brute force.
- Trivy security scan tự động trên CI.
- OWASP Top 10 mitigation (xem `02_security.md`).

## 5. Bảng tóm tắt enforcement

| Điều luật | BR liên quan | File / Migration |
|-----------|--------------|------------------|
| NĐ 30/2020 §5 | BR-01, BR-02 | `V8__masterdata_books.sql`, `MasterDataService.java` |
| NĐ 30/2020 §6 | BR-03, BR-05, BR-09 | `V11__documents.sql`, `InboundService.java` |
| NĐ 30/2020 §10 | BR-07 | `V13__document_versions.sql` (trigger) |
| NĐ 30/2020 §11 | BR-11 | `InboundService.recall()` |
| NĐ 30/2020 §12 | RBAC | `V5__auth_seed.sql` (8 roles) |
| NĐ 30/2020 §13 | BR-06 | `V17__signature.sql`, `SignatureGate.java` |
| NĐ 30/2020 §16 | BR-10, BR-13, BR-14 | `V3__audit_outbox.sql` (trigger), retention metadata |
| Luật GDĐT 2023 §15 | BR-12 | `V6__auth_login_attempts.sql`, `AuthService.java` |
| Luật GDĐT 2023 §22 | BR-06 | `SignatureService.java` (PAdES) |
| Luật GDĐT 2023 §23 | BR-07, BR-10 | versioning + audit + MinIO |
| Luật ATTTM 2015 §17 | — | Argon2id + audit redact |
| Luật ATTTM 2015 §21 | BR-12 | Bucket4j + Trivy CI |

## 6. Tiêu chuẩn quốc tế tham chiếu

| Tiêu chuẩn | Áp dụng |
|-----------|---------|
| ISO/IEC 27001:2022 | Audit log append-only, RBAC, change management |
| ETSI EN 319 142 (PAdES) | Định dạng chữ ký số PDF |
| RFC 5652 (CMS) | Cryptographic Message Syntax |
| NIST SP 800-63B | Mật khẩu (Argon2id, length, complexity) |
| OWASP ASVS Level 2 | Mục tiêu cho production |

## 7. Future compliance work (Phase 11+)

1. **Tích hợp Trục liên thông VB quốc gia** theo TT 27/2017/TT-BTTTT — gửi/nhận VB tự động giữa cơ quan nhà nước.
2. **Tích hợp CA quốc gia** (VNPT-CA, Viettel-CA, BkavCA, FPT-CA, NewCA) — chứng thư số hợp lệ.
3. **TimeStamp Authority (TSA)** — gắn timestamp signed cho chữ ký để chứng minh thời điểm ký.
4. **Long-term archive (LTA)** — định dạng PAdES-LTA với revocation info embed, đảm bảo verify được sau N năm khi cert hết hạn.
5. **Đáp ứng Quyết định 749/QĐ-TTg** về Chương trình Chuyển đổi số quốc gia.
