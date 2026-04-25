# Bảo mật hệ thống

Tuân thủ **Luật Giao dịch điện tử 2023**, **Luật An toàn thông tin mạng 2015**, OWASP Top 10, và best practices của Spring Security 6.

## 1. Xác thực (Authentication)

### 1.1 Mật khẩu — Argon2id

Dùng `argon2-jvm` (libsodium-based). Tham số phù hợp OWASP 2024:

```yaml
app.security.argon2:
  iterations: 3
  memory: 65536       # 64 MB
  parallelism: 4
  hash-length: 32
  salt-length: 16
```

So với bcrypt: Argon2id chống GPU/ASIC tốt hơn nhờ memory-hard. PHC string format `$argon2id$v=19$m=65536,t=3,p=4$<salt>$<hash>` lưu trong `users.password_hash`.

### 1.2 Token — JWT RS256

- **Access token**: 15 phút, signed RSA-256 (Nimbus JOSE).
- **Refresh token**: 7 ngày, lưu trong `refresh_tokens` table với `token_hash` (SHA-256).
- **Cookie HttpOnly + SameSite=Strict + Secure (prod)** cho refresh.
- **Refresh rotation**: mỗi lần refresh, token cũ bị revoke + cấp token mới. Phát hiện reuse → revoke toàn bộ session của user (replay protection).

```java
// Phát hiện replay
RefreshToken existing = repo.findByHash(presentedHash)
    .orElseThrow(InvalidRefreshTokenException::new);
if (existing.isRevoked()) {
    repo.revokeAllByUserId(existing.getUserId());  // panic mode
    throw new TokenReuseDetectedException();
}
```

Public key `/api/.well-known/jwks.json` cho future microservice verification.

### 1.3 BR-12 — Khóa tài khoản

`login_attempts` table ghi mỗi lần đăng nhập (success/fail) + IP + user-agent. Bucket4j per `(username, ip)`:

- 5 fail liên tiếp trong 15 phút → 401 + LOCKED 15 phút.
- Reset bucket khi login success.

Endpoint `/api/auth/login` không CSRF protect (dùng JWT stateless), nhưng có rate-limit Bucket4j 10 req/phút per IP.

## 2. Phân quyền (Authorization)

### 2.1 RBAC — 8 roles + ~40 permissions

Theo NĐ 30/2020 §12 — phân cấp quản lý văn thư:

| Role | Mô tả | Permissions chính |
|------|-------|-------------------|
| `ADMIN` | Quản trị hệ thống | tất cả |
| `VAN_THU` | Văn thư | `INBOUND:CREATE/REGISTER`, `OUTBOUND:VIEW`, `MASTERDATA:READ` |
| `TRUONG_DON_VI` | Trưởng đơn vị | `OUTBOUND:APPROVE_ORG`, `WORKFLOW:OVERSEE` |
| `TRUONG_PHONG` | Trưởng phòng | `OUTBOUND:APPROVE_DEPT`, `WORKFLOW:ASSIGN` |
| `CHUYEN_VIEN` | Chuyên viên | `INBOUND:VIEW_OWN_ASSIGNMENTS`, `WORKFLOW:HANDLE` |
| `CAN_BO_KY_SO` | Cán bộ ký số | `OUTBOUND:SIGN_PERSONAL`, `OUTBOUND:SIGN_ORG` |
| `KIEM_TRA_VIEN` | Kiểm tra viên | view-only audit + audit logs |
| `USER` | Người dùng cơ bản | `USER:VIEW_SELF`, `NOTIFICATION:VIEW_OWN` |

Permissions string format `RESOURCE:ACTION` (vd: `INBOUND:CREATE`, `OUTBOUND:APPROVE_ORG`). Spring Security `@PreAuthorize("hasAuthority('INBOUND:CREATE')")`.

### 2.2 Scope-based filtering

Permission là cần thiết nhưng không đủ — chuyên viên A không được xem assignment của chuyên viên B dù cùng có `WORKFLOW:HANDLE`.

```java
@PreAuthorize("hasAuthority('INBOUND:VIEW_OWN_ASSIGNMENTS')")
@GetMapping("/assignments")
public Page<Assignment> myAssignments(@AuthenticationPrincipal AuthPrincipal actor) {
    return service.findByAssignedToUserId(actor.userId(), ...);
}
```

Xem permission scope: `OWN`, `DEPARTMENT`, `ORGANIZATION`, `ALL`.

### 2.3 Method-level security

`@EnableMethodSecurity(prePostEnabled = true)` bật `@PreAuthorize` SpEL:

```java
@PreAuthorize("hasAuthority('OUTBOUND:APPROVE_DEPT') and " +
              "@securityService.userIsHeadOfDepartmentOf(#documentId, principal.userId)")
public void approveDepartment(UUID documentId) { ... }
```

## 3. Bảo mật dữ liệu

### 3.1 SQL injection

Bắt buộc **parameterized queries** ở mọi tầng:
- Spring Data JPA `@Query` + named params.
- Native query: `JdbcTemplate.queryForObject(sql, params)`.
- Lint check: spotless cấm `String.format` ghép vào SQL.

### 3.2 Magic bytes validation

Khi upload file, không tin extension. Dùng Apache Tika để xác định MIME từ magic bytes:

```java
String detected = TikaMimeDetector.detect(file.getBytes());
if (!ALLOWED_MIME.contains(detected)) throw new InvalidFileException();
```

Allowed: `application/pdf`, `image/png`, `image/jpeg`, `image/tiff`. Reject `application/x-msdownload` etc.

### 3.3 SHA-256 file integrity

Mỗi file uploaded → tính SHA-256, lưu vào `document_files.sha256`. Khi download, có thể verify lại.

### 3.4 MinIO bucket policies

- `congvan-files` private bucket (default deny).
- Pre-signed URL TTL 5 phút cho download.
- Server-side encryption SSE-S3 với master key per bucket.

### 3.5 PII trong audit log

Audit log không lưu password/PII nguyên văn. Mọi request body được redact các field nhạy cảm (`password`, `tokenHash`) trước khi ghi.

## 4. Chữ ký số — PAdES

### 4.1 PKCS#12 certificate storage

Certificate user/org lưu trong `certificates` table dưới dạng PKCS#12 blob mã hóa với master key (KMS-style — environment variable trong MVP, AWS KMS / HashiCorp Vault trong prod).

```sql
CREATE TABLE certificates (
    id UUID PRIMARY KEY,
    owner_user_id UUID,
    owner_org_id UUID,
    certificate_type VARCHAR(20),      -- PERSONAL, ORGANIZATION
    p12_blob BYTEA NOT NULL,           -- encrypted PKCS#12
    valid_from TIMESTAMP,
    valid_to TIMESTAMP,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP
);
```

### 4.2 PAdES detached signature

Dùng PDFBox 3 + Bouncy Castle (BC provider). Signature là CMS detached SHA256WithRSA, embed vào PDF dưới dạng `/Sig` annotation.

```java
PDDocument doc = PDDocument.load(originalPdf);
PDSignature sig = new PDSignature();
sig.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
sig.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
sig.setName(signerName);
sig.setLocation(orgLocation);
sig.setReason("Phê duyệt theo NĐ 30/2020/NĐ-CP");
sig.setSignDate(Calendar.getInstance());

doc.addSignature(sig, signatureInterface);
doc.saveIncremental(outputStream);
```

### 4.3 BR-06 — 2 chữ ký với thứ tự cố định

Cá nhân ký TRƯỚC, sau đó tổ chức ký bao bọc (cover signature). Thử ký tổ chức trước → 422 Unprocessable Entity.

```java
public void signAsOrganization(UUID versionId, ...) {
    DigitalSignature personal = sigRepo.findOneByVersionAndType(
        versionId, SignatureType.PERSONAL).orElseThrow(
            () -> new BusinessException("BR-06: cá nhân phải ký trước tổ chức"));
    // proceed
}
```

### 4.4 Verification

`SignatureVerificationService.verify(pdf)` → check:
1. CMS signature valid (cryptographic).
2. Certificate trong validity period.
3. Certificate chain (future: tích hợp CA Việt Nam — VNPT-CA, Viettel-CA).
4. Hash của content khớp.

## 5. Network security

### 5.1 CORS

Whitelist origins ở `application.yml`:

```yaml
app.security.cors.allowed-origins: 
  - https://app.congvan.gov.vn
  - http://localhost:3000  # dev only
```

Spring Security `CorsConfigurationSource` set `allowCredentials=true` (cần cho HttpOnly refresh cookie).

### 5.2 CSRF

Dùng JWT stateless → CSRF disable. Refresh token qua HttpOnly cookie `SameSite=Strict` đủ chống CSRF cho refresh endpoint.

### 5.3 HTTPS bắt buộc (prod)

`server.ssl.enabled=true` + `server.forward-headers-strategy=NATIVE` (đứng sau reverse proxy nginx/traefik).

HSTS header `Strict-Transport-Security: max-age=31536000; includeSubDomains`.

### 5.4 Security headers

Mặc định Spring Security 6:
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 0`
- `Cache-Control: no-cache, no-store, max-age=0`
- `X-Frame-Options: DENY`

## 6. Logging và monitoring

- **Audit log** (BR-10): mọi mutation → `audit_logs` table (append-only).
- **Application log**: SLF4J + Logback với MDC `requestId` + `userId`.
- **Sensitive data** không log: password, JWT secret, PKCS#12 blob.
- **Health checks**: `/actuator/health` (public, basic), `/actuator/health/full` (admin only).

## 7. Vulnerability scanning

CI workflow `security-scan.yml`:
- **Trivy** filesystem scan (HIGH/CRITICAL) — chạy mỗi PR + cron Thứ 2 hàng tuần.
- **Dependabot** (GitHub native) — auto-PR khi có CVE trong dependencies.
- **OWASP Dependency-Check** (planned future) cho thư viện Java.

## 8. Compliance checklist

| Yêu cầu | Cơ chế |
|---------|--------|
| Luật GDĐT 2023 §15 — xác thực mạnh | JWT RS256 + refresh rotation |
| Luật GDĐT 2023 §22 — chữ ký số có hiệu lực | PAdES + cert hợp lệ |
| Luật ATTTM 2015 §17 — bảo mật thông tin cá nhân | Argon2id + audit log redact |
| NĐ 30/2020 §6 — bảo mật văn bản | confidentiality enum + RBAC |
| NĐ 30/2020 §16 — lưu trữ điện tử | append-only audit + soft delete |
| OWASP A01 — broken access control | @PreAuthorize + scope-filter |
| OWASP A02 — crypto failures | Argon2id, RS256, SHA-256, BC FIPS-ready |
| OWASP A03 — injection | parameterized queries, magic bytes |
| OWASP A07 — identification failures | BR-12 lockout + refresh rotation |

## 9. Threat model — top risks và mitigation

| Threat | Mitigation |
|--------|-----------|
| Đánh cắp refresh token | HttpOnly + Secure + SameSite=Strict + rotation + reuse detection |
| Brute force login | BR-12 lockout + Bucket4j rate limit |
| File upload malware | Magic bytes validation + size limit + virus scanner (future) |
| SQL injection qua search query | websearch_to_tsquery (Postgres native) — không string concat |
| Privilege escalation | RBAC + scope filter + audit log |
| Tampering với VB đã duyệt | DB trigger immutable (BR-07) |
| Audit log tampering | DB trigger append-only (BR-10) |
| Replay refresh token | rotation + reuse detection → revoke session |
| CSRF | SameSite=Strict cookie + JWT stateless |
| Man-in-the-middle | HTTPS bắt buộc + HSTS prod |
