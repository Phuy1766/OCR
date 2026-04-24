# Bộ Prompts Triển khai Code — Hệ thống Quản lý Công văn

> **Dùng cho:** VS Code + AI extension (GitHub Copilot Chat / Cursor / Continue / Cody).
> **Cách dùng:** Dán **MASTER CONTEXT** (Phần 1) ở đầu mỗi conversation mới, rồi dán **TASK PROMPT** tương ứng với việc đang làm.

---

# 🗺 MỤC LỤC

- [Phần 1: MASTER CONTEXT (bắt buộc dán đầu tiên)](#phần-1-master-context)
- [Phần 2: Phase 0 — Setup dự án](#phần-2-phase-0--setup-dự-án)
- [Phần 3: Phase 1 — Auth & User](#phần-3-phase-1--auth--user)
- [Phần 4: Phase 2 — Master Data](#phần-4-phase-2--master-data)
- [Phần 5: Phase 3 — Inbound Documents (VB đến)](#phần-5-phase-3--inbound-documents)
- [Phần 6: Phase 4 — Outbound Documents (VB đi)](#phần-6-phase-4--outbound-documents)
- [Phần 7: Phase 5 — Workflow & Approval](#phần-7-phase-5--workflow--approval)
- [Phần 8: Phase 6 — OCR Service](#phần-8-phase-6--ocr-service)
- [Phần 9: Phase 7 — Digital Signature](#phần-9-phase-7--digital-signature)
- [Phần 10: Phase 8 — Search](#phần-10-phase-8--search)
- [Phần 11: Phase 9 — Frontend UI](#phần-11-phase-9--frontend-ui)
- [Phần 12: Phase 10 — Testing](#phần-12-phase-10--testing)
- [Phần 13: Template tái sử dụng](#phần-13-template-tái-sử-dụng)
- [Phần 14: Tips dùng prompt hiệu quả](#phần-14-tips-dùng-prompt-hiệu-quả)

---

# PHẦN 1: MASTER CONTEXT

> **Mỗi khi mở conversation mới với AI** (bất kể Copilot Chat, Cursor, hay Claude.ai), **dán prompt này đầu tiên**. Sau đó mới dán task prompt cụ thể. Điều này đảm bảo AI không suy luận sai stack hoặc đề xuất công nghệ khác.

## 🟨 COPY BẮT ĐẦU 🟨

```
Bạn là Senior Full-stack Developer đang làm dự án NCKH sinh viên "Hệ thống
quản lý công văn đi/đến tích hợp OCR và chữ ký số".

## TECH STACK (ĐÃ CHỐT — KHÔNG ĐỀ XUẤT STACK KHÁC)

Backend:
- Spring Boot 3.2+ với Java 21 LTS
- Maven multi-module
- Spring Data JPA + Hibernate
- Spring Security 6 + JWT (RS256)
- Flyway cho DB migration
- MapStruct cho DTO mapping
- Bean Validation (JSR-380)
- Apache PDFBox 3.x + Bouncy Castle (ký số)
- MinIO client (file storage)
- Spring AMQP + RabbitMQ
- Springdoc OpenAPI (Swagger)

Frontend:
- Next.js 14+ App Router + TypeScript strict
- TailwindCSS + shadcn/ui
- TanStack Query (React Query) v5
- Zustand cho state
- React Hook Form + Zod
- Axios cho HTTP

OCR Service (riêng biệt):
- Python 3.11+ + FastAPI
- PaddleOCR (hỗ trợ tiếng Việt)
- OpenCV cho preprocessing

Database:
- PostgreSQL 16 với extensions: unaccent, pg_trgm, pgcrypto
- Redis 7 (cache + session)
- RabbitMQ (message queue)

## NGUYÊN TẮC CODE (BẮT BUỘC TUÂN THỦ)

1. TRANSACTION:
   - Mọi thao tác đổi trạng thái phải trong @Transactional
   - Cấp số văn bản dùng SELECT ... FOR UPDATE trên counter
   - Ghi audit_log trong cùng transaction

2. OUTBOX PATTERN:
   - Không gọi MQ/email trực tiếp trong transaction nghiệp vụ
   - Lưu message vào bảng outbox_messages, scheduled job publish sau

3. VERSION LOCK:
   - Công văn đi có document_versions (immutable)
   - Khi duyệt cuối → chốt approved_version_id
   - File ký số phải thuộc đúng approved_version_id

4. AUDIT LOG:
   - Bảng audit_logs là append-only
   - Ghi user_id, action, entity_type, entity_id, old_value, new_value,
     ip_address, created_at

5. SOFT DELETE:
   - documents, document_files, document_versions không xóa cứng
   - Chỉ is_deleted = true + deleted_at + deleted_by

6. SECURITY:
   - Password: Argon2id (không dùng BCrypt)
   - JWT: RS256, access 30m, refresh 7d HttpOnly cookie
   - File upload: check magic bytes, whitelist MIME, max 50MB
   - Mọi endpoint có @PreAuthorize phân quyền cụ thể

7. VALIDATION:
   - Controller: @Valid trên request DTO
   - Service: business rule validation riêng
   - Trả về 400 với error code + field cụ thể

## PHÁP LÝ VIỆT NAM (TUÂN THỦ)

- Nghị định 30/2020/NĐ-CP về công tác văn thư
- Luật Giao dịch điện tử 2023 (Luật 20/2023/QH15)
- Luật Bảo vệ bí mật nhà nước 2018
- 29 loại văn bản hành chính theo Phụ lục III NĐ 30/2020
- 3 mức độ mật: Mật, Tối mật, Tuyệt mật (Luật 29/2018)
- 3 mức độ khẩn: Khẩn, Thượng khẩn, Hỏa tốc
- Số văn bản bắt đầu 01 từ 01/01, reset theo năm
- Văn bản điện tử cần 2 chữ ký số (cá nhân + cơ quan)
- Sổ văn bản mật tách riêng với sổ thường

## CODE STYLE

Java:
- Dùng Records cho DTO (Java 21)
- Pattern matching, sealed classes khi phù hợp
- @Service annotation rõ ràng
- Constructor injection (không @Autowired field)
- Tên tiếng Anh (code), comment tiếng Việt cho nghiệp vụ

TypeScript:
- strict: true trong tsconfig
- Không dùng any
- Type hoặc interface cho props, không dùng React.FC
- Zod schema làm single source of truth

Python:
- Type hints bắt buộc
- Pydantic v2 cho models
- async/await cho I/O
- PEP 8 strict

## REPONSE FORMAT

Khi tôi yêu cầu code, trả về:
1. Cấu trúc thư mục nếu tạo nhiều file
2. Full code mỗi file (không cắt ngắn, không "// ... rest")
3. Giải thích ngắn gọn (5-10 dòng) sau code
4. Ghi chú các chỗ cần TODO sau

ĐỪNG tự ý đổi stack, đừng đề xuất cải tiến kiến trúc nếu không được hỏi.
Chỉ làm đúng việc tôi yêu cầu, theo đúng patterns trên.

Tôi sẽ gửi task cụ thể ở message tiếp theo. Xác nhận bạn đã hiểu context.
```

## 🟨 COPY KẾT THÚC 🟨

---

# PHẦN 2: PHASE 0 — SETUP DỰ ÁN

## Prompt 0.1 — Tạo cấu trúc monorepo

```
Tạo cấu trúc thư mục đầy đủ cho monorepo "congvan-system" với 3 service:
- backend/ (Spring Boot multi-module Maven)
- frontend/ (Next.js 14 App Router)
- ocr-service/ (FastAPI Python)

Backend có các module:
congvan-app, congvan-common, congvan-auth, congvan-masterdata,
congvan-inbound, congvan-outbound, congvan-workflow, congvan-ocr,
congvan-signature, congvan-search, congvan-audit, congvan-integration

Yêu cầu:
1. Output cây thư mục dạng tree
2. File pom.xml root có packaging=pom và liệt kê tất cả modules
3. Mỗi module con có pom.xml riêng kế thừa từ root
4. .gitignore phù hợp cho Java/Node/Python
5. File README.md root có hướng dẫn setup và run

Tạo đồng thời:
- docker/docker-compose.dev.yml (postgres, redis, rabbitmq, minio)
- .env.example với tất cả env vars cần thiết
- .editorconfig
- LICENSE (MIT)
```

## Prompt 0.2 — Docker Compose dev environment

```
Tạo docker-compose.dev.yml cho development environment với:
- postgres:16-alpine (port 5432, có healthcheck, volume persist)
- redis:7-alpine (port 6379, có password)
- rabbitmq:3.13-management-alpine (port 5672 + 15672 management UI)
- minio:latest (port 9000 + 9001 console)

Yêu cầu:
1. Tất cả services dùng env từ .env file
2. Có healthcheck
3. Có named volumes để persist data
4. Network riêng "congvan-net"
5. Container name prefix "congvan-"
6. Healthcheck command phù hợp cho từng service

Tạo thêm:
- db/init/01-extensions.sql tạo các extensions: unaccent, pg_trgm, pgcrypto, uuid-ossp
- db/init/02-vietnamese-fts.sql tạo text search configuration 'vietnamese'
- scripts/dev-up.sh và dev-down.sh để start/stop env
```

## Prompt 0.3 — Flyway migrations ban đầu

```
Tạo các file Flyway migration cho PostgreSQL 16, đặt trong
backend/congvan-app/src/main/resources/db/migration/:

V1__enable_extensions.sql:
- Enable các extensions: unaccent, pg_trgm, pgcrypto, uuid-ossp

V2__create_vietnamese_fts.sql:
- Tạo text search configuration 'vietnamese' dựa trên simple + unaccent
- Alter mapping cho hword, hword_part, word

V3__create_users_and_auth.sql:
- Bảng users: id BIGSERIAL, username UNIQUE, password_hash (Argon2),
  full_name, email, department_id, is_active, is_locked,
  failed_login_attempts, last_login_at, created_at, updated_at
- Bảng roles: id, code (ADMIN, VAN_THU_CQ, LANH_DAO, ...), name, description
- Bảng user_roles: user_id, role_id (composite PK)
- Bảng departments: id, code, name, parent_department_id, manager_user_id,
  created_at, updated_at
- Tất cả bảng có soft delete: is_deleted, deleted_at, deleted_by
- Indexes phù hợp

V4__create_master_data.sql:
- organizations (cơ quan gửi/nhận bên ngoài)
- document_types (29 loại theo Phụ lục III NĐ 30/2020)
- confidentiality_levels (Thường, Mật, Tối mật, Tuyệt mật)
- priority_levels (Thường, Khẩn, Thượng khẩn, Hỏa tốc)
- document_books (sổ công văn, có level 1/2)
- document_book_counters (bộ đếm cấp số theo năm)
- Seed 29 document_types và 4 mức mật/khẩn

Yêu cầu:
- Tất cả FK có ON DELETE behavior rõ ràng
- Comment tiếng Việt cho mỗi bảng và cột quan trọng
- CHECK constraint cho enum values
- Index GIN cho các cột search_vector sau này
```

## Prompt 0.4 — Spring Boot skeleton

```
Tạo skeleton cho backend Spring Boot:

1. backend/pom.xml (parent POM):
- Java 21, Spring Boot 3.2+
- Dependency management cho tất cả thư viện trong MASTER CONTEXT
- Module list đầy đủ

2. backend/congvan-app/:
- CongvanApplication.java (main class)
- application.yml với profiles: dev, docker, prod
- application-dev.yml dùng localhost + .env
- Health check endpoint qua Actuator

3. backend/congvan-common/:
- BaseEntity.java (id, createdAt, updatedAt, createdBy, updatedBy)
- SoftDeletableEntity.java (kế thừa BaseEntity + isDeleted, deletedAt, deletedBy)
- BaseResponse record cho API response chuẩn
- ErrorResponse record
- PageResponse<T> record
- GlobalExceptionHandler với @RestControllerAdvice
- Custom exceptions: BusinessException, ValidationException,
  ResourceNotFoundException, UnauthorizedException

4. Cấu hình Swagger UI tại /swagger-ui.html

5. Logback config với:
- Console appender cho dev
- JSON appender cho production (structured logging)
- MDC correlation ID cho mỗi request

Viết đầy đủ code, không bỏ file nào.
```

## Prompt 0.5 — Next.js skeleton

```
Setup frontend Next.js 14 với App Router trong folder frontend/:

1. Init với: pnpm create next-app với TypeScript, TailwindCSS, App Router, src/ directory

2. Install dependencies:
- shadcn/ui (init với preset "default" + Tailwind)
- @tanstack/react-query v5
- zustand
- react-hook-form, @hookform/resolvers, zod
- axios
- lucide-react
- date-fns với locale vi
- sonner (toast)

3. Tạo cấu trúc thư mục:
src/
├── app/
│   ├── (auth)/login/page.tsx
│   ├── (dashboard)/layout.tsx
│   ├── (dashboard)/page.tsx (dashboard chính)
│   ├── layout.tsx (root)
│   └── globals.css
├── components/ui/ (shadcn)
├── components/layout/ (Header, Sidebar, Footer)
├── lib/
│   ├── api-client.ts (axios instance với interceptor)
│   ├── query-client.ts (TanStack Query config)
│   └── utils.ts
├── hooks/
├── schemas/
├── stores/auth-store.ts (Zustand)
├── types/api.types.ts
└── middleware.ts (auth check)

4. Config:
- next.config.js với rewrites API đến backend localhost:8080
- tsconfig strict mode
- eslint + prettier config
- .env.local.example

5. Components ban đầu:
- RootLayout với providers (QueryClient, Theme)
- DashboardLayout với Sidebar + Header
- Login page form với react-hook-form + zod
- Middleware bảo vệ routes (redirect nếu chưa auth)

Viết đầy đủ code, ngôn ngữ giao diện tiếng Việt.
```

## Prompt 0.6 — FastAPI OCR service skeleton

```
Setup OCR service tại ocr-service/ với FastAPI + PaddleOCR:

1. requirements.txt đầy đủ (FastAPI, uvicorn, pydantic v2, paddleocr,
   paddlepaddle, opencv-python-headless, pillow, pypdf, pdf2image,
   prometheus-client, pytest, httpx)

2. Cấu trúc:
ocr-service/
├── app/
│   ├── __init__.py
│   ├── main.py (FastAPI app, include routers)
│   ├── api/
│   │   ├── ocr.py (POST /ocr/process, GET /ocr/jobs/{id})
│   │   └── health.py
│   ├── core/
│   │   ├── config.py (Pydantic Settings)
│   │   ├── security.py (verify internal API key)
│   │   └── logging.py
│   ├── services/
│   │   ├── preprocessor.py (deskew, denoise với OpenCV)
│   │   ├── ocr_engine.py (PaddleOCR wrapper, singleton)
│   │   ├── field_extractor.py (regex extractor)
│   │   └── pdf_handler.py (PDF → images)
│   └── schemas/
│       ├── ocr.py (OCRRequest, OCRResponse, ExtractedField)
│       └── common.py
├── tests/
├── Dockerfile (multi-stage, optimized)
├── .env.example
└── README.md

3. Yêu cầu:
- Config qua env: INTERNAL_API_KEY, MAX_WORKERS, OCR_TIMEOUT_SECONDS
- Endpoint /ocr/process nhận file binary + metadata, trả về
  {raw_text, confidence, fields: [{name, value, confidence}]}
- Middleware check X-Internal-API-Key header
- Prometheus metrics endpoint /metrics
- Error handling đầy đủ (timeout, file corrupt, engine error)
- PaddleOCR init 1 lần global (tốn 3-5s)

4. Regex cho field extraction:
- code_number: pattern số văn bản VN (vd: 123/QĐ-BNV)
- issue_date: "ngày X tháng Y năm Z" hoặc DD/MM/YYYY
- title_summary: sau "V/v:" hoặc sau tên loại văn bản
```

---

# PHẦN 3: PHASE 1 — AUTH & USER

## Prompt 1.1 — Module Auth: Entity & Repository

```
Hoàn thiện module congvan-auth với entities và repositories.

Tạo trong congvan-auth/src/main/java/vn/edu/congvan/auth/domain/:

1. User entity (@Entity, table "users"):
- Kế thừa SoftDeletableEntity
- Fields: username, passwordHash, fullName, email, department (ManyToOne),
  isActive, isLocked, failedLoginAttempts, lastLoginAt
- @ManyToMany với Role qua bảng user_roles
- Helper methods: hasRole(String code), isAccountNonLocked()

2. Role entity:
- Fields: code (unique), name, description
- Không soft delete (master data cố định)

3. Department entity:
- Kế thừa SoftDeletableEntity
- Self-reference cho parentDepartment
- OneToMany children
- ManyToOne manager (User)

Repositories trong .../repository/:
- UserRepository extends JpaRepository<User, Long>
  + findByUsername(String) Optional
  + findByEmail(String) Optional
  + @Query findActiveUsersByDepartment
  + existsByUsername, existsByEmail
- RoleRepository với findByCode
- DepartmentRepository với findByCode, findRootDepartments

Yêu cầu:
- Dùng @Entity, @Table đúng tên bảng snake_case
- Relationships có @JoinColumn rõ ràng
- Lazy loading mặc định
- @SQLRestriction cho soft delete (Hibernate 6.3+)
- equals/hashCode dùng id
- toString không include relationships (tránh lazy init exception)
```

## Prompt 1.2 — Spring Security + JWT

```
Triển khai authentication với Spring Security 6 + JWT (RS256) trong module
congvan-auth.

Yêu cầu:
1. SecurityConfig:
   - Stateless session
   - CSRF disable cho API, enable cho form (nếu có)
   - CORS configuration (frontend localhost:3000)
   - Public endpoints: /api/v1/auth/**, /swagger-ui/**, /v3/api-docs/**,
     /actuator/health
   - Còn lại authenticated
   - Custom JwtAuthenticationFilter

2. JwtService:
   - generateAccessToken(UserPrincipal, 30m)
   - generateRefreshToken(UserPrincipal, 7d)
   - validateToken(String) -> Claims
   - extractUsername, extractRoles
   - Dùng RSA key pair, load từ keystore hoặc env
   - Tạo script generate-keys.sh để tạo RSA key pair

3. Argon2PasswordEncoder (không dùng BCrypt):
   - Params: memory 65536, iterations 3, parallelism 1
   - Implement PasswordEncoder interface

4. UserPrincipal implements UserDetails:
   - Wrap User entity
   - Override getAuthorities() -> List từ User.roles

5. CustomUserDetailsService với loadUserByUsername

6. JwtAuthenticationFilter:
   - Extract Bearer token từ Authorization header
   - Validate, set SecurityContext
   - Skip public endpoints

7. AuthController (POST /api/v1/auth/):
   - /login: {username, password} -> {accessToken, refreshToken, user}
     + Refresh token trong HttpOnly cookie
     + Reset failed_login_attempts khi thành công
     + Lock account sau 5 lần sai
   - /refresh: rotate refresh token
   - /logout: invalidate refresh token (blacklist Redis)
   - /me: trả về thông tin user hiện tại

8. Rate limiting cho /login: 5 requests/phút/IP (Bucket4j)

Viết đầy đủ code với error handling. Comment tiếng Việt cho nghiệp vụ.
```

## Prompt 1.3 — Frontend: Login page + auth flow

```
Triển khai authentication flow trong frontend Next.js:

1. src/schemas/auth.schema.ts:
   - loginSchema với Zod (username required min 3, password min 8)
   - Export type LoginRequest, LoginResponse

2. src/lib/api-client.ts:
   - Axios instance với baseURL từ env
   - Request interceptor: attach Bearer token
   - Response interceptor: nếu 401 + có refresh token -> gọi refresh,
     retry request. Nếu refresh fail -> redirect /login

3. src/stores/auth-store.ts (Zustand):
   - State: user, accessToken, isAuthenticated
   - Actions: setAuth, clearAuth, updateUser
   - Persist accessToken vào sessionStorage (không localStorage)

4. src/hooks/use-auth.ts:
   - useLogin mutation
   - useLogout mutation
   - useCurrentUser query

5. src/app/(auth)/login/page.tsx:
   - Form với react-hook-form + zod resolver
   - shadcn Input, Button, Card, Label
   - Hiển thị error từ server (account locked, wrong credentials)
   - Loading state
   - Redirect đến /dashboard sau login thành công

6. src/middleware.ts:
   - Check access token cookie
   - Redirect / → /dashboard nếu đã login
   - Redirect protected routes → /login nếu chưa login
   - Matcher exclude static files

7. Components:
   - AuthProvider wrap children (QueryProvider, Toaster)
   - LogoutButton trong Header

UI tiếng Việt hoàn toàn. Error messages user-friendly.
Accessibility đủ (aria-label, focus management).
```

---

# PHẦN 4: PHASE 2 — MASTER DATA

## Prompt 2.1 — Entities & CRUD API

```
Triển khai module congvan-masterdata với các entities và CRUD API:

Entities (SoftDeletableEntity):
1. Organization (cơ quan ngoài): code unique, name, address, phone, email, contactPerson
2. DocumentType: code unique (QĐ, CV, TB, ...), name, description,
   isSystemDefault (29 loại NĐ30 không xóa được), displayOrder
3. ConfidentialityLevel: code enum (THUONG, MAT, TOI_MAT, TUYET_MAT),
   name, displayOrder, requiresSpecialHandling boolean
4. PriorityLevel: code enum (THUONG, KHAN, THUONG_KHAN, HOA_TOC),
   name, displayOrder, hoursToProcess
5. DocumentBook: code, name, level (1 hoặc 2), departmentId (null cho cấp 1),
   year, description, isActive
6. DocumentBookCounter: bookId, year, nextNumber (dùng FOR UPDATE khi cấp số)

Với mỗi entity tạo:
- Repository extends JpaRepository với các method tùy chỉnh
- Service interface + impl với @Transactional
- Controller với GET (list có pagination + filter), GET by id,
  POST (tạo mới), PATCH (update), DELETE (soft delete)
- DTO Request/Response (Java Records)
- Mapper với MapStruct

Yêu cầu:
- @PreAuthorize("hasRole('ADMIN')") cho POST/PATCH/DELETE
- @PreAuthorize("isAuthenticated()") cho GET
- Không cho xóa system default (isSystemDefault = true)
- Validation: @NotBlank, @Size, pattern cho code
- Response chuẩn: BaseResponse<T> với success, data, message
- Error handling qua GlobalExceptionHandler
- Audit log cho mọi thao tác write

Endpoints cụ thể:
- /api/v1/organizations
- /api/v1/document-types
- /api/v1/confidentiality-levels
- /api/v1/priority-levels
- /api/v1/document-books
- /api/v1/departments (đã có module auth nhưng thêm CRUD full ở đây)
```

## Prompt 2.2 — Seed data 29 loại văn bản + mức mật/khẩn

```
Tạo Flyway migration V5__seed_master_data.sql để seed:

1. 29 document_types đúng theo Phụ lục III NĐ 30/2020/NĐ-CP:
| STT | Tên | Viết tắt |
| 1 | Nghị quyết (cá biệt) | NQ |
| 2 | Quyết định (cá biệt) | QĐ |
| 3 | Chỉ thị | CT |
| 4 | Quy chế | QC |
| 5 | Quy định | QyĐ |
| 6 | Thông cáo | TC |
| 7 | Thông báo | TB |
| 8 | Hướng dẫn | HD |
| 9 | Chương trình | CTr |
| 10 | Kế hoạch | KH |
| 11 | Phương án | PA |
| 12 | Đề án | ĐA |
| 13 | Dự án | DA |
| 14 | Báo cáo | BC |
| 15 | Biên bản | BB |
| 16 | Tờ trình | TTr |
| 17 | Hợp đồng | HĐ |
| 18 | Công điện | CĐ |
| 19 | Bản ghi nhớ | BGN |
| 20 | Bản thỏa thuận | BTT |
| 21 | Giấy ủy quyền | GUQ |
| 22 | Giấy mời | GM |
| 23 | Giấy giới thiệu | GGT |
| 24 | Giấy nghỉ phép | GNP |
| 25 | Phiếu gửi | PG |
| 26 | Phiếu chuyển | PC |
| 27 | Phiếu báo | PB |
| 28 | Thư công | TCG |
| 29 | Công văn | CV |
Tất cả is_system_default = true.

2. 4 confidentiality_levels: THUONG, MAT, TOI_MAT, TUYET_MAT (đúng thứ tự)

3. 4 priority_levels: THUONG (0h), KHAN (24h), THUONG_KHAN (8h), HOA_TOC (2h)

4. 6 roles: ADMIN, VAN_THU_CQ, VAN_THU_PB, LANH_DAO, TRUONG_PHONG, CHUYEN_VIEN

5. 1 admin user default:
   - username: admin
   - password: Admin@123 (hashed với Argon2, dùng giá trị đã hash)
   - role: ADMIN
   - Note trong migration: phải đổi password sau lần đăng nhập đầu

6. 3 departments mẫu: BGH (Ban Giám hiệu), VP (Văn phòng), PDT (Phòng Đào tạo)

7. 2 document_books mẫu:
   - VB-DEN-2026 (sổ văn bản đến năm 2026, level 1)
   - VB-DI-2026 (sổ văn bản đi năm 2026, level 1)
   - Tạo kèm counter với year=2026, nextNumber=1
```

---

# PHẦN 5: PHASE 3 — INBOUND DOCUMENTS

## Prompt 3.1 — Entities và cấu trúc module

```
Triển khai module congvan-inbound cho quản lý công văn đến.

Tham chiếu: đọc kỹ file docs/architecture_congvan_full_defense.md §5.3, §7.3
và docs/website_framework_vietnam_legal.md §4 trước khi code.

1. Entity Document (shared với outbound, đặt trong congvan-common/domain):
   - Kế thừa SoftDeletableEntity
   - Fields đầy đủ theo docs §7.3 (documents table):
     * code_number, title_summary, content_text
     * document_direction enum (INBOUND, OUTBOUND)
     * issue_mode enum (PAPER, ELECTRONIC, nullable)
     * sender_org_id, sender_user_id
     * issue_date, due_date
     * document_type_id, confidentiality_level_id, priority_level_id
     * current_status (String, sử dụng state machine)
     * current_department_id, current_assignee_user_id
     * active_workflow_instance_id
     * accepted_ocr_result_id
     * approved_version_id
     * issued_file_id
   - Enums InboundStatus và OutboundStatus riêng biệt

2. DocumentRecipient entity (bảng document_recipients):
   - recipient_type enum (ORGANIZATION, DEPARTMENT, USER)
   - Check constraint: đúng 1 trong 3 recipient_*_id khác NULL

3. DocumentFile entity:
   - Fields: document_id, version_id, file_name, mime_type, file_path,
     file_size, checksum_sha256, file_role enum, is_encrypted,
     uploaded_by, uploaded_at
   - file_role: ORIGINAL, SCAN, DRAFT_PDF, FINAL_PDF, SIGNED, ATTACHMENT

4. DocumentBookEntry entity:
   - document_id, document_book_id, entry_year, entry_number,
     entry_status enum (RESERVED, OFFICIAL, CANCELLED), entry_date,
     summary, note, created_by
   - Unique constraint (document_book_id, entry_year, entry_number) WHERE
     entry_status != CANCELLED

Tạo migrations V6__create_documents.sql cho tất cả bảng trên, với
indexes và constraints đầy đủ.
```

## Prompt 3.2 — Service cấp số an toàn (transaction + FOR UPDATE)

```
Triển khai DocumentNumberingService trong congvan-masterdata (service
được chia sẻ giữa inbound và outbound).

Chức năng:
- Cấp số văn bản đi/đến với transaction và FOR UPDATE
- Phân biệt văn bản thường và văn bản mật (sổ riêng)
- Reset counter theo năm

Interface:
public interface DocumentNumberingService {
    NumberAllocation allocateNumber(
        Long documentBookId,
        Integer year,
        AllocationType type  // RESERVED hoặc OFFICIAL
    );

    void cancelReservedNumber(Long entryId, String reason);

    void confirmReservedAsOfficial(Long entryId);
}

Implementation yêu cầu:
1. @Transactional(isolation = READ_COMMITTED)
2. Query counter với SELECT ... FOR UPDATE:
   @Query(value = "SELECT * FROM document_book_counters
                   WHERE book_id = :bookId AND year = :year
                   FOR UPDATE", nativeQuery = true)
3. Nếu counter chưa có cho year → tạo mới với next_number = 1
4. Validate uniqueness trước khi insert entry
5. Tăng counter bằng UPDATE (không dùng entity setter để tránh
   Hibernate optimize lại)
6. Ghi audit_log trong cùng transaction

Test cases phải cover:
- Cấp số bình thường
- 2 request đồng thời (dùng @Transactional + CompletableFuture trong test)
- Năm mới (counter chưa tồn tại)
- Sổ mật không đụng sổ thường
- Hủy số RESERVED

Viết service + test integration với Testcontainers PostgreSQL.
```

## Prompt 3.3 — API tiếp nhận văn bản đến

```
Triển khai InboundDocumentController + Service trong congvan-inbound.

Endpoints:
1. POST /api/v1/inbound-documents
   Body: CreateInboundRequest {
       senderOrgId, senderName (free-text nếu không có trong DM),
       originalCodeNumber, originalIssueDate,
       documentTypeId, titleSummary,
       confidentialityLevelId, priorityLevelId,
       dueDate, sourceChannel (POSTAL, DIRECT, ELECTRONIC, EMAIL, FAX)
   }
   Response: InboundDocumentResponse với id, số đến đã cấp

   Logic:
   - Validate input
   - Tạo Document với direction=INBOUND, status=NEW
   - Cấp số đến (gọi DocumentNumberingService)
   - Ghi DocumentBookEntry status=OFFICIAL
   - Trả response
   Permission: VAN_THU_CQ hoặc VAN_THU_PB

2. GET /api/v1/inbound-documents
   Query params: page, size, sort, search, fromDate, toDate, status,
   confidentialityLevel, priorityLevel, departmentId
   Response: PageResponse<InboundDocumentSummary>
   Logic: JPA Specification filter, respect quyền xem theo department

3. GET /api/v1/inbound-documents/{id}
   Response: InboundDocumentDetail (bao gồm files, OCR result, workflow history)
   Permission: check quyền xem (department scoping + confidentiality)

4. PATCH /api/v1/inbound-documents/{id}
   Body: UpdateInboundRequest (chỉ fields cho phép update)
   Logic: validate status cho phép edit (chỉ NEW, WAITING_DETAIL)

5. POST /api/v1/inbound-documents/{id}/files
   Multipart upload file scan/PDF
   Logic: validate MIME + magic bytes + max 50MB, lưu MinIO,
   tạo DocumentFile với file_role=SCAN

6. POST /api/v1/inbound-documents/{id}/ocr
   Logic: trigger OCR job (push to RabbitMQ queue "ocr.jobs")

7. POST /api/v1/inbound-documents/{id}/forward
   Body: { toDepartmentId, toUserId, note }
   Logic: tạo workflow step FORWARD, tạo assignment ACTIVE,
   update current_department/current_assignee, gửi notification

8. POST /api/v1/inbound-documents/{id}/reject
   Body: { reason }
   Logic: update status=REJECTED

Yêu cầu:
- Tất cả endpoint có @PreAuthorize chi tiết
- Validation đầy đủ
- Audit log mọi action
- Response time budget: list API < 300ms
- Swagger annotation đầy đủ

Viết đầy đủ controller, service, DTOs, mapper, tests.
```

---

# PHẦN 6: PHASE 4 — OUTBOUND DOCUMENTS

## Prompt 4.1 — Draft và Version Management

```
Triển khai module congvan-outbound với versioning đầy đủ.

Tham chiếu docs/architecture_congvan_full_defense.md §5.4, §7.3 (document_versions)

Entity DocumentVersion:
- document_id (FK)
- version_number (int, auto-increment per document)
- content_snapshot (TEXT)
- checksum_sha256
- version_status enum (WORKING, SUBMITTED, APPROVED, SUPERSEDED)
- change_note
- created_by, created_at
- @Immutable (Hibernate) sau khi status != WORKING

Service logic:

1. createDraft(CreateOutboundRequest req):
   - Tạo Document direction=OUTBOUND, status=DRAFT
   - Tạo DocumentVersion version_number=1, status=WORKING
   - Chưa cấp số
   - Return documentId, versionId

2. updateDraft(Long documentId, UpdateOutboundRequest req):
   - Validate status = DRAFT và version đang WORKING
   - Update version content
   - Tăng checksum

3. submitForApproval(Long documentId):
   - Validate đầy đủ fields (title_summary, content, recipients, issue_mode)
   - Lock version hiện tại (WORKING → SUBMITTED)
   - Update status DRAFT → WAITING_DEPT_APPROVAL
   - Tạo workflow instance
   - Gửi notification cho trưởng phòng

4. returnForRevision(Long documentId, String reason, Long approverId):
   - Validate approver có quyền
   - Version hiện tại: SUBMITTED → SUPERSEDED
   - Tạo version mới WORKING (copy từ SUPERSEDED)
   - Status về DRAFT
   - Ghi approval với decision=RETURNED

5. approveByDepartmentHead(Long documentId, Long approverId, String note):
   - Validate status = WAITING_DEPT_APPROVAL
   - Ghi approval với level=DEPARTMENT_HEAD, decision=APPROVED
   - Chuyển status → WAITING_UNIT_APPROVAL
   - Gửi notification cho lãnh đạo đơn vị

6. approveByUnitLeader(Long documentId, Long approverId, String note):
   - Validate status = WAITING_UNIT_APPROVAL
   - Ghi approval level=UNIT_LEADER
   - CHỐT approved_version_id (quan trọng!)
   - Version: SUBMITTED → APPROVED
   - Chuyển status → WAITING_SIGN (nếu electronic) hoặc WAITING_PAPER_ISSUE

Tất cả method trong @Transactional. Dùng Outbox pattern cho notification.

Viết đầy đủ entity, service, controller, DTOs, tests.
Đặc biệt: test race condition khi 2 người duyệt cùng lúc.
```

## Prompt 4.2 — Cấp số và phát hành

```
Triển khai logic cấp số + phát hành cho outbound document.

Endpoints:

1. POST /api/v1/outbound-documents/{id}/issue
   Permission: VAN_THU_CQ
   Logic:
   - Validate status = APPROVED (đã ký số) hoặc WAITING_PAPER_ISSUE
   - Nếu electronic: phải có digital_signatures với status=SUCCESS
     trên approved_version
   - Cấp số chính thức (DocumentNumberingService với type=OFFICIAL)
   - Update document: code_number, issue_date
   - Status → ISSUED
   - Nếu đã có RESERVED entry, confirm thành OFFICIAL
   - Ghi workflow step ISSUE
   - Audit log

2. POST /api/v1/outbound-documents/{id}/dispatch
   Permission: VAN_THU_CQ
   Body: { channels: [{recipientId, method: PAPER|ELECTRONIC|FAX|EMAIL, note}] }
   Logic:
   - Validate status = ISSUED
   - Ghi dispatch records
   - Gửi qua trục liên thông nếu electronic (mock cho demo)
   - Status → DISPATCHED

3. POST /api/v1/outbound-documents/{id}/recall
   Body: { reason }
   Permission: LANH_DAO hoặc người ký
   Logic:
   - Validate status in (ISSUED, DISPATCHED)
   - Status → RECALLED
   - Gửi notification thu hồi cho tất cả recipients
   - Giữ data, không xóa

4. GET /api/v1/outbound-documents/{id}/versions
   Response: danh sách versions với diff summary

Constraints nghiệp vụ quan trọng:
- Theo Điều 15 NĐ 30/2020: "Số bắt đầu liên tiếp từ 01 vào ngày 01/01 và
  kết thúc vào ngày 31/12 hàng năm" → system phải check year của
  issue_date, dùng counter đúng năm
- Văn bản mật: dùng sổ riêng (tách book_id)
- Trong cùng transaction phải atomic: cấp số + update document +
  confirm entry + audit log

Viết code + test với các case edge (cấp số trùng, cấp số sai năm, etc.).
```

---

# PHẦN 7: PHASE 5 — WORKFLOW & APPROVAL

## Prompt 5.1 — Workflow engine

```
Triển khai module congvan-workflow — engine quản lý luồng xử lý tài liệu.

Tham chiếu docs/architecture_congvan_full_defense.md §5.5, §7.3

Entities:
1. WorkflowInstance: id, document_id, workflow_type, started_by,
   started_at, ended_at, status (ACTIVE, COMPLETED, CANCELLED)

2. WorkflowStep (append-only, immutable):
   - workflow_instance_id, step_order
   - from_user_id, to_user_id, from_department_id, to_department_id
   - action_type enum: FORWARD, APPROVE, REJECT, RETURN, ASSIGN, SIGN,
     ISSUE, ARCHIVE
   - action_note, action_time
   - result_status enum: PENDING, COMPLETED, CANCELLED

3. Approval (tham chiếu workflow_step):
   - document_id, workflow_step_id, document_version_id
   - approver_user_id, approval_level enum (DEPARTMENT_HEAD, UNIT_LEADER)
   - decision enum (APPROVED, REJECTED, RETURNED)
   - note, approved_at

4. Assignment:
   - document_id, workflow_step_id
   - assigned_by_user_id, assigned_to_department_id, assigned_to_user_id
   - assigned_at, due_date
   - assignment_status enum (ACTIVE, COMPLETED, CANCELLED, REASSIGNED)
   - Check: chỉ 1 assignment ACTIVE cho mỗi document tại 1 thời điểm

Service methods:
1. WorkflowService:
   - startWorkflow(documentId, workflowType, startedBy)
   - addStep(WorkflowStep)
   - completeStep(stepId)
   - endWorkflow(instanceId, status)

2. ApprovalService:
   - requestApproval(documentId, versionId, approverId, level)
   - approve/reject/return với decision và note
   - Mỗi decision tạo mới WorkflowStep + Approval trong transaction

3. AssignmentService:
   - assign(documentId, toDepartment/toUser, assignedBy, dueDate)
   - complete(assignmentId)
   - reassign(oldAssignmentId, newAssignee)
   - Đảm bảo invariant: 1 ACTIVE per document

APIs:
- GET /api/v1/approvals/pending - danh sách cần duyệt của current user
- POST /api/v1/approvals/{id}/decide - duyệt/từ chối/trả lại
- GET /api/v1/documents/{id}/workflow-history - lịch sử đầy đủ
- GET /api/v1/documents/{id}/assignments - lịch sử phân công
- POST /api/v1/workflow/assign - phân công người xử lý

Yêu cầu test:
- Test invariant 1 ACTIVE assignment
- Test race condition approve cùng lúc
- Test rollback khi exception
```

## Prompt 5.2 — Outbox Pattern cho Notification

```
Triển khai Outbox Pattern để gửi notification mà không break transaction.

Entities:
1. OutboxMessage:
   - id (UUID)
   - aggregate_type (DOCUMENT, APPROVAL, WORKFLOW)
   - aggregate_id
   - event_type (DOCUMENT_FORWARDED, APPROVAL_REQUESTED, ...)
   - payload (JSONB)
   - status enum (PENDING, PUBLISHED, FAILED)
   - created_at, published_at
   - retry_count, last_error

2. Notification:
   - id, recipient_user_id
   - notification_type enum (ASSIGNMENT, APPROVAL_REQUEST,
     DEADLINE_WARNING, STATUS_CHANGE, SYSTEM_ERROR, INFO)
   - title, message
   - reference_entity_type, reference_entity_id
   - priority enum (HIGH, MEDIUM, LOW)
   - is_read, read_at, created_at

Implementation:

1. OutboxPublisher @Service:
   - publish(String aggregateType, Long aggregateId, String eventType,
     Object payload)
   - LƯU vào outbox table trong transaction hiện tại (@Transactional(REQUIRED))

2. OutboxRelayScheduler @Component:
   @Scheduled(fixedDelay = 2000)
   public void relay() {
     - Đọc 50 messages PENDING (ORDER BY created_at ASC)
     - Publish lên RabbitMQ exchange "congvan.events"
     - Nếu success: update status=PUBLISHED, published_at=NOW()
     - Nếu fail: retry_count++, nếu > 5 → FAILED
   }

3. NotificationEventListener @RabbitListener:
   - Subscribe các event
   - Resolve recipients từ event (ai cần notification)
   - Insert vào notifications table
   - Gửi email (nếu config SMTP)
   - Push qua WebSocket (future)

4. NotificationController:
   - GET /api/v1/notifications - list của current user (pagination)
   - GET /api/v1/notifications/unread-count
   - PATCH /api/v1/notifications/{id}/read
   - POST /api/v1/notifications/mark-all-read

5. Tích hợp vào nghiệp vụ:
   Trong ApprovalService.approve(), sau khi commit DB:
   outboxPublisher.publish("APPROVAL", approval.getId(),
     "APPROVAL_COMPLETED", new ApprovalCompletedPayload(...));

Tại sao Outbox quan trọng:
- Nếu gọi RabbitMQ trực tiếp trong transaction và MQ down, transaction sẽ fail
- Nếu gọi sau transaction, có thể commit DB xong nhưng send MQ fail → mất message
- Outbox: lưu message cùng transaction DB, publish async sau → guaranteed delivery

Viết code đầy đủ, có integration test mô phỏng MQ down.
```

---

# PHẦN 8: PHASE 6 — OCR SERVICE

## Prompt 6.1 — PaddleOCR integration

```
Hoàn thiện OCR service (Python) với PaddleOCR cho tiếng Việt.

Tham chiếu docs/tech_stack_congvan_system.md §5

1. app/services/ocr_engine.py:
from paddleocr import PaddleOCR
import threading

class OcrEngineSingleton:
    _instance = None
    _lock = threading.Lock()

    @classmethod
    def get_instance(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = PaddleOCR(
                        use_angle_cls=True,
                        lang='vi',
                        show_log=False
                    )
        return cls._instance

class OcrService:
    async def process(image_bytes: bytes) -> OcrResult:
        # Load image, run OCR, collect (bbox, text, confidence)
        ...

2. app/services/preprocessor.py:
- deskew(img): detect skew angle, rotate
- denoise(img): bilateral filter
- binarize(img): Otsu threshold
- normalize_size(img, max_dim=2400)

3. app/services/field_extractor.py:
Extract với regex + position heuristics:
- code_number: r'\b\d+/[A-ZĐ]{2,}(-[A-ZĐ]+)?\b'
- issue_date: multiple patterns
- title_summary: text sau "V/v:" hoặc "Về việc:"
- recipient: text trong "Kính gửi:"
- Signer: text trước chữ ký

4. app/services/pdf_handler.py:
- pdf_to_images(pdf_bytes, dpi=200) -> list[bytes]
- Xử lý PDF nhiều trang

5. app/api/ocr.py:
@router.post("/ocr/process")
async def process_ocr(
    file: UploadFile,
    api_key: str = Depends(verify_api_key)
) -> OcrResponse:
    - Validate file (size, MIME)
    - Preprocess
    - Run OCR
    - Extract fields
    - Return response với confidence scores

6. Schemas (Pydantic v2):
class OcrResponse(BaseModel):
    raw_text: str
    overall_confidence: float
    processing_time_ms: int
    fields: list[ExtractedField]
    page_count: int

class ExtractedField(BaseModel):
    name: Literal['code_number', 'issue_date', 'title_summary',
                   'sender_organization', 'recipient', 'signer', 'content']
    value: str
    confidence: float
    bbox: Optional[list[int]] = None

7. Error handling:
- Timeout (> OCR_TIMEOUT_SECONDS): trả 408
- File quá lớn: trả 413
- File corrupt: trả 422
- PaddleOCR error: trả 500 với log chi tiết

8. Dockerfile multi-stage:
- Stage 1: build với requirements
- Stage 2: runtime nhẹ, COPY app + models từ stage 1
- Healthcheck endpoint

9. Tests:
- Test với 10 file PDF mẫu trong tests/fixtures/
- Test preprocessing với ảnh nghiêng, mờ, noise
- Test field extraction với các pattern khác nhau
- Test timeout, file corrupt

Write đầy đủ code.
```

## Prompt 6.2 — Backend OCR integration (async qua RabbitMQ)

```
Triển khai module congvan-ocr trong backend Spring Boot để tích hợp
với OCR service Python qua RabbitMQ.

Entities (tham chiếu docs §7.3):
1. OcrJob: id, document_id, file_id, engine_name (TESSERACT, PADDLE, EASYOCR),
   status enum (PENDING, PROCESSING, COMPLETED, FAILED, TIMEOUT,
   SERVICE_UNAVAILABLE), retry_of_job_id, started_at, ended_at, triggered_by

2. OcrResult: id, ocr_job_id, raw_text, confidence_score, reviewed_by,
   reviewed_at, is_accepted boolean

3. OcrExtractedField: id, ocr_result_id, field_name, field_value,
   confidence_score

Flow:

1. User trigger OCR qua API POST /inbound-documents/{id}/ocr
   → OcrService.createJob(documentId, fileId, engine)
   → Tạo OcrJob status=PENDING
   → Publish message lên RabbitMQ exchange "ocr.requests":
     { jobId, documentId, fileId, fileUrl (presigned MinIO URL), engine }

2. OCR service Python consume, process, publish result ra
   exchange "ocr.results":
   { jobId, status, rawText, confidence, fields: [...], error }

3. Backend OcrResultConsumer @RabbitListener:
   - Validate job còn tồn tại + đang PROCESSING
   - Nếu status=COMPLETED:
     * Create OcrResult (is_accepted=false)
     * Create OcrExtractedFields
     * Update OcrJob status=COMPLETED
     * Send notification cho user
   - Nếu status=FAILED: update job, notify
   - Idempotent: nếu job đã COMPLETED rồi thì skip

4. User xem kết quả, chỉnh sửa, confirm:
   POST /api/v1/ocr/results/{id}/accept
   Body: { editedFields: {...} }
   → Update is_accepted=true
   → Update document.accepted_ocr_result_id
   → Update document.content_text từ raw_text đã confirm

5. Rule quan trọng:
   - Kết quả OCR KHÔNG được tự động apply vào document
   - Chỉ sau khi user accept mới trở thành dữ liệu chính thức
   - Mỗi lần re-OCR → tạo OcrJob mới, không ghi đè

APIs cần có:
- POST /api/v1/ocr/jobs - tạo job manually
- GET /api/v1/ocr/jobs/{id} - trạng thái job
- GET /api/v1/ocr/results/{id} - xem kết quả
- POST /api/v1/ocr/results/{id}/accept - chấp nhận kết quả
- POST /api/v1/ocr/results/{id}/reject - reject, tạo lại job

Config RabbitMQ:
- Exchange: ocr.requests, ocr.results (topic)
- Queue names: ocr.process.queue, ocr.result.queue
- DLX cho message fail 3 lần
- TTL 10 phút cho request

Viết đầy đủ: entities, service, consumer, controller, tests.
```

---

# PHẦN 9: PHASE 7 — DIGITAL SIGNATURE

## Prompt 7.1 — PDF signing với Bouncy Castle + PDFBox

```
Triển khai module congvan-signature cho ký số PDF.

Tham chiếu docs/tech_stack_congvan_system.md §6 (có code example)

Entities:
1. Certificate: id, user_id, certificate_serial, issuer_name, subject_name,
   valid_from, valid_to, is_revoked, certificate_file_path (.pem/.cer),
   uploaded_by, uploaded_at

2. DigitalSignature: id, document_id, document_version_id, source_file_id,
   signed_file_id, signer_user_id, certificate_id,
   signature_algorithm (default "SHA256withRSA"),
   hash_algorithm (default "SHA-256"),
   source_file_hash_sha256, signed_at,
   signature_status enum (PENDING, SUCCESS, FAILED, INVALIDATED)

3. SignatureVerification: id, signature_id, verified_by,
   verification_time, verification_result enum (VALID, INVALID, UNKNOWN),
   verification_note

Services:

1. CertificateService:
   - uploadCertificate(userId, PKCS12 keystore + password)
     → Extract X509Certificate, verify chain, lưu cert info
   - generateSelfSignedCert(userId, validityDays) - cho demo
   - listActiveCertificates(userId)
   - revokeCertificate(certId, reason)
   - isExpiringSoon(certId, daysThreshold) - cho alert

2. PdfSigningService:
   - sign(Long documentId, Long versionId, Long certificateId,
          char[] keystorePassword)
     Return: signedFileId (DocumentFile)

   Implementation theo code ví dụ trong docs §6.4:
   - Load PDF từ MinIO
   - Validate: version = approved_version_id, hash file chưa đổi
   - Load private key từ PKCS12 keystore (password từ user input)
   - Setup PDSignature với Filter=Adobe.PPKLite,
     SubFilter=ETSI.CAdES.detached
   - Set signer name, location, reason, signDate
   - Hash content (exclude signature placeholder)
   - Sign với Bouncy Castle CMSSignedDataGenerator
   - Include signing certificate + chain
   - Add signed attributes (signing-time)
   - Embed signature vào PDF
   - Upload file signed lên MinIO
   - Create DocumentFile với file_role=SIGNED
   - Create DigitalSignature record với status=SUCCESS
   - Update document.issued_file_id = signedFileId

3. SignatureVerificationService:
   - verify(signedFileId or documentId)
   Implementation theo docs §6.5:
   - Load PDF
   - Extract signatures qua PDFBox
   - Với mỗi signature:
     * Verify CMS signature
     * Check cert validity period
     * Check cert revocation (CRL nếu có, OCSP future)
     * Check cert chain về Root CA
   - Return VerificationResult với chi tiết

APIs:
- POST /api/v1/signatures/sign - ký văn bản
  Body: { documentId, versionId, certificateId, keystorePassword }
  Permission: LANH_DAO hoặc TRUONG_PHONG
- POST /api/v1/signatures/verify - verify 1 file
- GET /api/v1/signatures/document/{documentId} - lịch sử ký
- GET /api/v1/certificates/my - danh sách cert của tôi
- POST /api/v1/certificates/upload - upload cert
- POST /api/v1/certificates/generate-self-signed - cho demo

Bảo mật:
- keystorePassword KHÔNG log, KHÔNG lưu
- Private key chỉ dùng in-memory, destroy sau khi ký xong
- Rate limit sign API: 10/phút/user

Viết đầy đủ code + tests. Include 1 test self-signed cert end-to-end
(generate cert → sign → verify).
```

## Prompt 7.2 — Tạo self-signed cert cho demo

```
Tạo utility class SelfSignedCertGenerator để tạo certificate giả lập
cho demo (không cần CA thật).

Yêu cầu:
1. Method generateCertificate(subject, validityDays):
   - Tạo RSA keypair 2048-bit
   - Tạo X509Certificate với subject CN=[name], O=[org],
     OU=[department], C=VN
   - Valid from now, valid to now + validityDays
   - Signature algorithm SHA256withRSA
   - Basic Constraints: CA=false
   - Key Usage: digitalSignature, nonRepudiation
   - Extended Key Usage: emailProtection, timeStamping

2. Method exportToPkcs12(keyPair, cert, alias, password):
   - Tạo PKCS12 keystore chứa key + cert
   - Return byte[]

3. Method exportCertOnly(cert): byte[] PEM format

4. Script demo:
   scripts/generate-demo-certs.sh:
   - Tạo cert cho admin, lanh_dao_1, truong_phong_1
   - Export PKCS12 (password: "demo123!")
   - Export public cert PEM
   - Insert vào database qua SQL

5. REST endpoint POST /api/v1/certificates/generate-self-signed
   Body: { subject, organization, validityDays, password }
   Permission: ADMIN (hoặc cho phép user tự tạo trong demo)
   Response: { certificateId, downloadKeystoreUrl, warning:
     "Chỉ dùng cho demo, không có giá trị pháp lý" }

6. Lưu ý:
   - Thêm banner UI hiển thị "Demo mode - chứng thư self-signed" khi user
     dùng cert này
   - Trong báo cáo bảo vệ, ghi rõ: khi triển khai thật phải dùng cert
     do Ban Cơ yếu Chính phủ cấp (chữ ký số chuyên dùng công vụ)

Viết code Java với Bouncy Castle. Include 1 main method để test nhanh.
```

---

# PHẦN 10: PHASE 8 — SEARCH

## Prompt 8.1 — PostgreSQL FTS cho tiếng Việt

```
Triển khai full-text search cho tiếng Việt trên document.

1. Migration V10__add_search_vector.sql:

-- Tạo text search configuration Vietnamese (đã có ở V2)
-- Thêm column generated tsvector
ALTER TABLE documents ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    setweight(to_tsvector('vietnamese',
        unaccent(coalesce(code_number, ''))), 'A') ||
    setweight(to_tsvector('vietnamese',
        unaccent(coalesce(title_summary, ''))), 'B') ||
    setweight(to_tsvector('vietnamese',
        unaccent(coalesce(content_text, ''))), 'C')
) STORED;

CREATE INDEX idx_documents_search_vector
ON documents USING GIN (search_vector);

-- Tương tự cho ocr_results để search trong OCR content
ALTER TABLE ocr_results ADD COLUMN search_vector tsvector ...;
CREATE INDEX idx_ocr_results_search ON ocr_results USING GIN (search_vector);

2. Module congvan-search:

SearchService:
- searchDocuments(SearchCriteria criteria, Pageable pageable):
  Page<DocumentSearchResult>
  Logic:
  - Nếu có keyword: query với websearch_to_tsquery
  - Filter thêm: date range, confidentiality, priority, document_type,
    department, status
  - Phân quyền: check user có quyền xem mỗi doc không (apply ở SQL
    WHERE hoặc filter Java)
  - Order by: ts_rank DESC nếu có keyword, created_at DESC nếu không

- searchOcrContent(String keyword, Pageable): tìm trong OCR content

- suggestKeywords(String prefix, int limit): gợi ý autocomplete

3. Native query với tsquery:
@Query(value = """
    SELECT d.*, ts_rank(d.search_vector, query) as rank
    FROM documents d,
         websearch_to_tsquery('vietnamese', unaccent(:keyword)) query
    WHERE d.search_vector @@ query
      AND d.is_deleted = false
      AND d.current_department_id IN (:allowedDeptIds)
    ORDER BY rank DESC
    LIMIT :limit OFFSET :offset
""", nativeQuery = true)

4. APIs:
- GET /api/v1/search/documents?q=...&fromDate=...&toDate=...&
       confidentialityLevel=...&type=...&page=0&size=20
- GET /api/v1/search/fulltext?q=... (search cả metadata + OCR content)
- GET /api/v1/search/suggest?prefix=... (autocomplete)

5. Highlight matched terms trong response:
  Dùng ts_headline để highlight:
  SELECT ts_headline('vietnamese', title_summary, query,
    'StartSel=<mark>, StopSel=</mark>') as highlighted

6. Test cases:
- Search với dấu "hội nghị" → match "hoi nghi" (unaccent)
- Search exact phrase "công văn khẩn"
- Filter combo
- Phân quyền: user phòng A không thấy doc phòng B
- Performance với 10,000 documents

Viết SearchService + SearchController + integration tests với Testcontainers.
```

---

# PHẦN 11: PHASE 9 — FRONTEND UI

## Prompt 9.1 — Layout chính và Sidebar navigation

```
Tạo layout chính cho dashboard frontend Next.js.

1. src/app/(dashboard)/layout.tsx:
   - Sidebar trái (có thể collapse)
   - Header trên (user info, notification bell, logout)
   - Main content area
   - Footer đơn giản

2. src/components/layout/Sidebar.tsx:
   Menu theo docs/website_framework_vietnam_legal.md §2 (sitemap):
   - Trang chủ (icon Home)
   - Văn bản đến (icon Inbox) với submenu:
     * Tiếp nhận
     * Danh sách VB đến
     * Chờ xử lý của tôi
   - Văn bản đi (icon Send) với submenu:
     * Soạn thảo mới
     * Danh sách VB đi
     * Dự thảo của tôi
     * Chờ duyệt của tôi
     * Chờ ký số
   - Sổ công văn (icon Book):
     * Sổ VB đến
     * Sổ VB đi
     * Sổ VB mật (nếu có quyền)
   - Ký số (icon Signature)
   - Tìm kiếm (icon Search)
   - Báo cáo (icon BarChart3)
   - Danh mục (icon Database) - chỉ admin
   - Quản trị (icon Settings) - chỉ admin

   Yêu cầu:
   - Filter menu theo role user (hide nếu không có quyền)
   - Active state cho route hiện tại
   - Responsive: auto collapse ở mobile
   - Accessibility

3. src/components/layout/Header.tsx:
   - Breadcrumb theo route hiện tại
   - Search bar quick (Ctrl+K)
   - Notification bell với badge count (dùng useNotifications hook)
   - User dropdown: tên, role, profile, settings, logout

4. src/components/layout/NotificationDropdown.tsx:
   - Popover với list notifications
   - Mark as read
   - "Xem tất cả" dẫn đến /notifications

5. src/stores/ui-store.ts (Zustand):
   - sidebarCollapsed, setSidebarCollapsed
   - theme, setTheme (light/dark)

6. UI toàn bộ tiếng Việt
7. Dùng shadcn/ui components (Button, Sheet, Avatar, DropdownMenu, etc.)
8. Icons từ lucide-react

Viết đầy đủ, có TypeScript types chặt chẽ.
```

## Prompt 9.2 — Trang Dashboard

```
Tạo trang /dashboard theo mockup trong docs/website_framework_vietnam_legal.md §3.1

Page: src/app/(dashboard)/page.tsx

Components cần có:

1. WelcomeBanner: "Xin chào [tên]" + role + ngày hiện tại VN

2. StatsCards (grid 3 cột):
   Card "VB đến chờ xử lý" - số + link
   Card "VB đi chờ ký số" - số + link
   Card "VB cần ghi sổ" - số + link

3. UrgentAlertCards (grid 3 cột):
   Card "VB khẩn" - breakdown Hỏa tốc/Thượng khẩn/Khẩn với màu
     đỏ/cam/vàng
   Card "VB quá hạn" - số + link danh sách
   Card "VB mật" - số + link (chỉ hiện nếu có quyền)

4. Charts (grid 2 cột):
   - LineChart: Số VB theo tháng (12 tháng gần nhất)
   - PieChart: Tỷ lệ đúng hạn / quá hạn

5. RecentDocumentsTable:
   - 10 VB gần nhất (mix đi và đến)
   - Cột: Số, Loại, Trích yếu, Ngày, Trạng thái, Actions
   - Click row → chi tiết

API cần gọi (backend):
- GET /api/v1/dashboard/stats (tổng quan)
- GET /api/v1/dashboard/my-tasks
- GET /api/v1/dashboard/recent-documents?limit=10

Backend cần tạo DashboardController + Service tương ứng.

Yêu cầu:
- Dùng Recharts cho charts
- Loading state (Skeleton) khi fetching
- Error state (hiển thị thân thiện)
- Refresh button
- Auto refresh mỗi 30s với React Query
- Dashboard khác nhau theo role (lãnh đạo vs văn thư vs chuyên viên)

Viết full frontend code + backend endpoint.
```

## Prompt 9.3 — Màn hình Tiếp nhận công văn đến

```
Tạo trang /inbound/new theo docs §4.1 (form tiếp nhận VB đến).

Form cần đầy đủ trường theo Phụ lục VI NĐ 30/2020:

Section 1 - Nguồn tiếp nhận:
- Nguồn: radio (Bưu điện / Trực tiếp / Điện tử / Email / Fax)
- Ngày đến: date picker (mặc định hôm nay)
- Số đến: disabled, "Cấp tự động sau khi lưu"

Section 2 - Thông tin văn bản:
- Cơ quan ban hành: combobox searchable (từ organizations API)
  + Cho phép nhập tự do nếu không có trong DM
- Số, ký hiệu: text với hint "vd: 123/QĐ-BNV"
- Ngày ban hành: date picker
- Loại văn bản: select (29 loại từ document_types API)
- Lĩnh vực: select optional
- Trích yếu nội dung: textarea, max 500 chars, counter

Section 3 - Phân loại:
- Độ mật: radio group (4 mức), hiển thị warning nếu chọn Mật+
- Độ khẩn: radio group (4 mức)

Section 4 - Xử lý:
- Hạn giải quyết: date picker (gợi ý theo priority)
- Ghi chú: textarea optional

Section 5 - File đính kèm:
- Drop zone (react-dropzone) hoặc click chọn
- Accept: PDF, JPG, PNG, TIFF
- Max 50MB / file, max 10 files
- Show preview (PDF thumbnail, image thumbnail)
- Checkbox "Tự động chạy OCR sau khi lưu"

Validation với Zod schema:
- senderName HOẶC senderOrgId required
- originalCodeNumber required
- titleSummary required, max 500
- Nếu độ mật > THUONG: hiển thị cảnh báo + require lý do

Submit flow:
1. POST /api/v1/inbound-documents (JSON data)
   Response: { documentId, codeNumber, ... }
2. POST /api/v1/inbound-documents/{documentId}/files (multipart, files)
3. Nếu user chọn auto-OCR:
   POST /api/v1/inbound-documents/{documentId}/ocr
4. Toast success + redirect /inbound/{id}

UI/UX:
- Form chia tab hoặc section rõ ràng
- Save draft tự động mỗi 30s (lưu vào localStorage tạm)
- Keyboard navigation
- Confirmation dialog nếu rời trang mà chưa save
- Error inline field-level + toast server error

Viết full code.
```

## Prompt 9.4 — Màn hình OCR review

```
Tạo màn hình review kết quả OCR tại /inbound/{id}/ocr/{ocrResultId}
(tham chiếu docs/website_framework_vietnam_legal.md §8.3)

Layout split screen 50/50:

Bên trái - PDF Viewer:
- Dùng react-pdf hiển thị file scan
- Zoom in/out, chuyển trang
- Highlight các bbox được OCR bóc tách (optional, nâng cao)

Bên phải - Form trường bóc tách:
- Mỗi trường có:
  * Label (vd: "Số, ký hiệu")
  * Input editable
  * Confidence badge (màu theo %):
    - >= 90%: xanh ✓
    - 70-89%: vàng ⚠ (cần kiểm tra)
    - < 70%: đỏ (phải sửa)
  * Button "Bỏ qua" nếu field không đúng

Trường hiển thị:
- Số, ký hiệu (code_number)
- Ngày ban hành (issue_date)
- Cơ quan ban hành (sender_organization)
- Loại văn bản (document_type)
- Trích yếu (title_summary)

Dưới cùng:
- Textarea "Văn bản đầy đủ (full text)" - có thể edit
  (được dùng cho search sau này)

Actions:
- [← Chạy lại OCR]: reject kết quả hiện tại, tạo job mới
  POST /api/v1/ocr/results/{id}/reject
- [Lưu nháp]: update fields nhưng chưa accept
- [Xác nhận & Lưu]: accept kết quả
  POST /api/v1/ocr/results/{id}/accept
  Body: { editedFields: {...}, editedFullText: "..." }

Yêu cầu:
- Realtime validation khi user edit
- Highlight field nào đã được edit vs giữ nguyên OCR
- Keyboard shortcuts: Ctrl+S save draft, Ctrl+Enter confirm
- Warning nếu user accept mà còn trường confidence < 70% chưa sửa

Backend endpoints cần có:
- GET /api/v1/ocr/results/{id} trả về đầy đủ + file URL
- POST /api/v1/ocr/results/{id}/accept
- POST /api/v1/ocr/results/{id}/reject

Viết đầy đủ frontend + backend endpoints còn thiếu.
```

---

# PHẦN 12: PHASE 10 — TESTING

## Prompt 10.1 — Unit tests Backend

```
Viết unit tests cho module [TÊN MODULE] (ví dụ: congvan-outbound).

Yêu cầu:
1. Dùng JUnit 5 + Mockito
2. Test coverage > 80% cho service layer
3. Test naming: should_ExpectedBehavior_When_StateUnderTest
4. Mỗi test có Arrange - Act - Assert sections rõ ràng

Cho mỗi service, test:
- Happy path (normal case)
- Edge cases (null, empty, boundary)
- Error cases (invalid state, not found, permission denied)
- Race condition scenarios nếu relevant

Ví dụ cho OutboundDocumentService.approveByUnitLeader():
- should_SetStatusToWaitingSign_When_ElectronicIssueMode
- should_SetStatusToWaitingPaperIssue_When_PaperIssueMode
- should_SetApprovedVersionId_When_Approved
- should_ThrowBusinessException_When_DocumentNotInWaitingUnitApproval
- should_ThrowUnauthorizedException_When_ApproverIsNotUnitLeader
- should_LockVersion_When_Approved (check version_status = APPROVED)
- should_CreateApprovalRecord_When_Approved
- should_PublishOutboxMessage_When_Approved
- should_RollbackAllChanges_When_OutboxWriteFails

Mock:
- Repository methods
- Các service dependency (ApprovalService, WorkflowService, ...)
- Clock (dùng fixed Clock cho test time-dependent logic)

Verify:
- Đúng method của mock được gọi
- Đúng arguments
- Đúng số lần call
- Không có interaction không mong muốn

Viết đầy đủ test file.
```

## Prompt 10.2 — Integration tests với Testcontainers

```
Viết integration tests cho module [TÊN] dùng Testcontainers + PostgreSQL.

Setup:
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class [ModuleName]IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("congvan_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        ...
    }
}

Test scenarios:

1. End-to-end document lifecycle:
- Create draft → Update → Submit → Approve dept → Approve unit
  → Sign → Issue → Dispatch
- Verify database state sau mỗi bước
- Verify audit_log được ghi đúng

2. Concurrent number allocation:
- 10 threads cùng allocate number
- Verify không có số trùng
- Verify counter đúng giá trị cuối

3. Version management:
- Create draft → submit → return for revision
- Verify version 1 status=SUPERSEDED
- Verify version 2 status=WORKING
- Update version 2 → submit → approve
- Verify approved_version_id = version 2
- Verify version 1 không bị modify

4. Permission enforcement:
- User A phòng 1 create doc
- User B phòng 2 tìm kiếm → không thấy doc của A
- User ADMIN tìm kiếm → thấy được

5. File upload + OCR:
- Upload PDF mock
- Trigger OCR
- Wait message arrive (dùng CountDownLatch + @RabbitListener test)
- Verify ocr_job, ocr_result records
- Accept result
- Verify document.accepted_ocr_result_id

Dùng @Sql để seed test data.
Cleanup sau mỗi test với @Transactional rollback hoặc @Sql cleanup.
```

## Prompt 10.3 — E2E tests với Playwright

```
Setup Playwright cho E2E test frontend + backend.

1. cd frontend && pnpm add -D @playwright/test
   pnpm exec playwright install

2. playwright.config.ts:
- testDir: ./e2e
- baseURL từ env
- use { headless: true, screenshot: 'on-failure' }
- projects: chromium, firefox, webkit

3. Scripts:
- e2e/setup.ts: global setup (seed DB qua API, login admin)
- e2e/auth.setup.ts: login và save storageState
- e2e/fixtures/: test data

4. Test scenarios:

e2e/inbound-full-flow.spec.ts:
test('Văn thư tiếp nhận và xử lý VB đến', async ({ page }) => {
    // Login as văn thư
    await loginAs(page, 'vanthu1', 'password');

    // Tạo VB đến
    await page.goto('/inbound/new');
    await page.fill('[name="originalCodeNumber"]', '123/QĐ-BNV');
    // ... fill all fields
    await page.click('button:has-text("Lưu")');

    // Upload file
    await page.setInputFiles('input[type="file"]',
        'e2e/fixtures/cv-mau.pdf');
    await page.click('button:has-text("Upload")');

    // Wait OCR complete
    await page.waitForSelector('text=OCR hoàn thành', { timeout: 30000 });

    // Review OCR
    await page.click('button:has-text("Xem kết quả OCR")');
    await page.click('button:has-text("Xác nhận")');

    // Forward to department
    await page.click('button:has-text("Chuyển xử lý")');
    await page.selectOption('select[name="department"]', 'Phòng HCTH');
    await page.click('button:has-text("Xác nhận chuyển")');

    // Assert success
    await expect(page.locator('.toast')).toContainText('Chuyển thành công');
});

e2e/outbound-full-flow.spec.ts:
- Chuyên viên tạo dự thảo
- Trình Trưởng phòng duyệt
- Trưởng phòng duyệt
- Lãnh đạo duyệt
- Lãnh đạo ký số
- Văn thư cấp số & phát hành
- Verify toàn bộ workflow_steps và trạng thái cuối

e2e/search.spec.ts:
- Tạo nhiều docs
- Search với Vietnamese keyword (có dấu)
- Verify results

5. Chạy CI: thêm Playwright step vào GitHub Actions workflow
- Start docker-compose trước
- Run: pnpm exec playwright test
- Upload report artifact

Viết đầy đủ setup + 3 test specs trên.
```

---

# PHẦN 13: TEMPLATE TÁI SỬ DỤNG

Dùng các template dưới đây khi cần làm công việc tương tự.

## Template A — Thêm Entity mới

```
Thêm entity [TÊN ENTITY] vào module [TÊN MODULE].

Fields:
- field1: type (constraints)
- field2: type (nullable, ...)
- ...

Relationships:
- ManyToOne với [Entity X] qua [field_name]
- OneToMany với [Entity Y]

Yêu cầu chuẩn (theo MASTER CONTEXT):
1. Kế thừa [BaseEntity | SoftDeletableEntity] phù hợp
2. Tạo Flyway migration V[N]__add_[table_name].sql
3. Tạo Repository với các query method cần thiết
4. Tạo Service interface + impl với @Transactional
5. Tạo Controller với CRUD endpoints (REST, @PreAuthorize phù hợp)
6. Tạo DTOs: CreateRequest, UpdateRequest, Response (Java Records)
7. Tạo Mapper với MapStruct
8. Viết unit tests cho Service (coverage > 80%)
9. Cập nhật OpenAPI annotations
```

## Template B — Thêm API endpoint

```
Thêm endpoint [HTTP METHOD] [URL PATH] cho [mục đích].

Request: [body/query params]
Response: [structure]
Permission: [role]

Business logic:
1. [step 1]
2. [step 2]
3. [step 3]

Yêu cầu:
- Validation: @Valid trên request, custom validator nếu cần
- Transaction: @Transactional với isolation level phù hợp
- Error handling: throw đúng exception, có error code
- Audit log: ghi mọi thao tác write
- OpenAPI annotation
- Unit test happy path + edge cases + error cases

Nếu có notification: dùng Outbox pattern.
```

## Template C — Tạo React component

```
Tạo component [TÊN COMPONENT] tại src/components/[path]/[name].tsx

Props:
- prop1: type
- prop2: type (optional)

Behavior:
- [khi user click X → làm Y]
- [khi data fetch xong → hiển thị ...]

Yêu cầu:
- TypeScript strict, không any
- Props interface tách riêng
- Dùng shadcn/ui components làm base
- TailwindCSS cho styling (không CSS inline)
- Accessibility: aria-*, keyboard nav, focus management
- Loading state với Skeleton
- Error state với error message user-friendly
- Empty state nếu relevant
- Responsive (mobile + desktop)
- Storybook story nếu là reusable component (optional)

Data fetching (nếu có): dùng TanStack Query hook.
Form (nếu có): dùng react-hook-form + Zod.
```

## Template D — Code review

```
Review đoạn code sau theo MASTER CONTEXT:

[PASTE CODE HERE]

Kiểm tra:
1. Có tuân thủ tất cả nguyên tắc trong MASTER CONTEXT không?
   - Transaction đúng chỗ?
   - Audit log đầy đủ?
   - Permission check?
   - Validation?
   - Error handling?

2. Security issues:
   - SQL injection risk?
   - XSS risk?
   - Sensitive data leak?
   - Permission bypass?

3. Performance:
   - N+1 queries?
   - Missing index?
   - Unnecessary fetch?

4. Code quality:
   - Naming?
   - Duplication?
   - Single responsibility?
   - Test coverage?

5. Nghiệp vụ:
   - Đúng quy định pháp luật VN (NĐ 30, Luật GDĐT)?
   - Edge cases nghiệp vụ đã cover?

Output format:
- Issue 1 (severity: HIGH/MEDIUM/LOW): [mô tả + fix]
- Issue 2 ...
- Suggested refactor (nếu có)
```

## Template E — Debug lỗi

```
Đang gặp lỗi khi [hành động]:

Error message / stack trace:
```[paste error]```

Code liên quan:
```[paste relevant code]```

Điều tôi đã thử:
1. [đã thử gì]
2. ...

Yêu cầu:
1. Phân tích root cause (không chỉ fix bề mặt)
2. Đưa ra 2-3 giải pháp, phân tích pros/cons
3. Đề xuất giải pháp tốt nhất + code fix cụ thể
4. Đề xuất test để prevent regression
5. Nếu là lỗi kiến trúc: gợi ý refactor lớn hơn

Tuân thủ MASTER CONTEXT.
```

---

# PHẦN 14: TIPS DÙNG PROMPT HIỆU QUẢ

## ✅ Nên làm

1. **Luôn dán MASTER CONTEXT đầu mỗi conversation mới.** Không skip dù bạn nghĩ AI nhớ context cũ.

2. **Chia nhỏ task.** Thay vì "tạo module outbound hoàn chỉnh" → chia thành 5-6 prompts nhỏ (entity, service cấp số, service approval, controller, tests).

3. **Tham chiếu docs rõ ràng.** "Tham chiếu docs/tech_stack §5" thay vì "như đã nói trước".

4. **Đưa ra acceptance criteria.** "Coverage > 80%", "API response < 300ms", "Test 3 case: happy, edge, error".

5. **Request code đầy đủ.** Thêm câu "Viết đầy đủ code, không bỏ qua bất kỳ file nào, không dùng `// ... rest`".

6. **Review code AI sinh ra kỹ.** AI có thể:
   - Tạo code nhưng bỏ qua edge case
   - Dùng thư viện version cũ
   - Bỏ audit log / transaction
   - Fake implementation (return mock data)

7. **Test ngay khi AI vừa sinh code.** Đừng tích lũy rồi mới test.

## ❌ Tránh

1. **Đừng hỏi "tạo cả dự án cho tôi".** AI sẽ sinh code cẩu thả, thiếu, không integrate được.

2. **Đừng bỏ qua validation AI có hiểu đúng context không.** Cuối MASTER CONTEXT có câu "Xác nhận bạn đã hiểu context" - đợi AI xác nhận rồi hãy gửi task.

3. **Đừng trust 100% code AI sinh.** Đặc biệt với:
   - Security (auth, permission, crypto)
   - Transaction + concurrency
   - Nghiệp vụ phức tạp (cấp số, workflow)
   - Câu lệnh SQL phức tạp

4. **Đừng đổi stack giữa chừng.** Nếu đã chọn Spring Boot thì đừng hỏi "làm lại bằng Node".

5. **Đừng copy-paste code AI mà không đọc.** Mỗi dòng đều phải hiểu.

## 🎯 Mẫu conversation hiệu quả

```
User: [Dán MASTER CONTEXT]
AI: [Xác nhận đã hiểu]

User: [Dán task prompt 0.1 - Tạo cấu trúc monorepo]
AI: [Sinh code tree + pom.xml + các file]

User: Có. Tiếp tục với prompt 0.2.
AI: ...

User: Wait. pom.xml ở prompt 0.1 đang dùng Spring Boot 3.1,
     MASTER CONTEXT yêu cầu 3.2+. Sửa lại.
AI: [Sửa]

User: Tốt. Bây giờ [task tiếp theo]
AI: ...
```

---

# 🧪 CHECKPOINT QUAN TRỌNG

Sau khi hoàn thành mỗi phase, làm checkpoint trước khi đi tiếp:

- [ ] **Phase 0:** docker compose up chạy được, backend + frontend start lên được, migrations chạy thành công
- [ ] **Phase 1:** Login admin/Admin@123 thành công, JWT được generate, /api/v1/users/me trả về đúng thông tin
- [ ] **Phase 2:** Admin CRUD được các master data, danh sách 29 document types hiển thị đầy đủ
- [ ] **Phase 3:** Tiếp nhận được VB đến, cấp số thành công, số liên tiếp theo năm
- [ ] **Phase 4:** Full flow soạn → duyệt → chốt version hoạt động đúng
- [ ] **Phase 5:** Workflow log đầy đủ, notification nhận được qua Outbox
- [ ] **Phase 6:** OCR service trả kết quả bóc tách 4/5 field chính xác với file mẫu
- [ ] **Phase 7:** Ký số self-signed thành công, verify ra VALID
- [ ] **Phase 8:** Search tiếng Việt có dấu hoạt động
- [ ] **Phase 9:** UI đầy đủ các màn hình quan trọng
- [ ] **Phase 10:** Tests pass > 80% coverage

**Đạt được 7/11 checkpoint là đã đủ để bảo vệ đạt 7-7.5 điểm.**
**Đạt 9+/11 checkpoint là có thể bảo vệ đạt 8-9 điểm.**

---

**Chúc nhóm code thuận lợi và bảo vệ thành công! 🚀**
