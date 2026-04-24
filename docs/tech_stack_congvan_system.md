# Tech Stack & Triển khai — Hệ thống quản lý công văn đi/đến tích hợp OCR và chữ ký số

> **Tài liệu bổ sung cho:** `architecture_congvan_full_defense.md`
> **Mục đích:** Chốt ngôn ngữ, framework, thư viện, mô hình bảo mật và hướng triển khai cụ thể để nhóm có thể bắt tay vào code ngay và bảo vệ đề tài thuyết phục.
> **Phạm vi:** Đề tài NCKH sinh viên cấp trường — ưu tiên stack mature, bảo mật tốt, có hệ sinh thái mạnh, khả thi trong thời hạn đề tài (3–6 tháng, nhóm 8 người).

---

# 1. Triết lý lựa chọn công nghệ

Trước khi đi vào chi tiết, cần thống nhất **4 tiêu chí** để đánh giá mọi lựa chọn công nghệ trong đề tài này:

1. **Mature & Production-proven** — ưu tiên công nghệ đã được dùng trong các hệ thống công văn/ECM (Enterprise Content Management) thực tế ở Việt Nam (VNPT iOffice, MISA QLVB, eDoc…). Không chạy theo công nghệ mới chưa có cộng đồng lớn.

2. **Strong security posture by default** — vì đây là hệ thống quản lý văn bản nhà nước (có công văn mật, chữ ký số), không thể dùng framework "tự build security từ đầu".

3. **Rich ecosystem cho PDF / ký số / OCR** — đây là 3 yêu cầu lõi. Stack nào có thư viện mature cho cả 3 thì được cộng điểm lớn.

4. **Khả thi với nhóm sinh viên** — không chọn stack mà team phải học lại từ đầu trong khi deadline ngắn. Stack phải có đủ tài liệu tiếng Việt/English, Stack Overflow, và người hướng dẫn đã quen.

---

# 2. Tổng quan Stack đề xuất (bảng chốt)

| Tầng | Công nghệ chốt | Alternative | Lý do chốt |
|---|---|---|---|
| **Frontend Framework** | **Next.js 14+ (App Router) + TypeScript** | Vue 3 + Nuxt 3 | Next.js có SSR/SSG, middleware bảo mật built-in, ecosystem lớn nhất cho React |
| **UI Library** | **shadcn/ui + TailwindCSS** | Ant Design, Material-UI | shadcn/ui copy-paste code, dễ custom, không vendor lock-in |
| **State/Data fetching** | **TanStack Query + Zustand** | Redux Toolkit | Đơn giản, ít boilerplate, đủ cho dự án quy mô này |
| **Form & Validation** | **React Hook Form + Zod** | Formik + Yup | Performance tốt hơn, type-safe với TypeScript |
| **Backend Framework** | **Spring Boot 3.x + Java 21 (LTS)** | ASP.NET Core 8 (C#) | Ecosystem PDF/ký số/Bouncy Castle mạnh nhất, Spring Security mature |
| **API Style** | **REST + OpenAPI 3.0 (Swagger)** | GraphQL | REST đơn giản, dễ bảo vệ, đủ cho nghiệp vụ này |
| **Database** | **PostgreSQL 16** | MySQL 8, MariaDB | Full-text search, JSONB, tsvector tốt cho tiếng Việt, partial indexes mạnh |
| **ORM** | **Spring Data JPA + Hibernate** | MyBatis, jOOQ | Mature nhất với Spring Boot, hỗ trợ transaction tốt |
| **Cache / Queue** | **Redis 7** | Memcached | Vừa cache, vừa làm session store, vừa làm message queue nhẹ |
| **Message Queue** | **RabbitMQ** (cho OCR batch) | Kafka, Redis Streams | Nhẹ, đủ cho dự án thử nghiệm, Spring AMQP tích hợp tốt |
| **OCR Engine** | **PaddleOCR** (Python service) | VietOCR, Tesseract 5 | Accuracy cao nhất cho tiếng Việt (nhiều benchmark 2023–2024 xác nhận) |
| **OCR Service Framework** | **FastAPI (Python 3.11+)** | Flask, Django | Async native, OpenAPI auto-gen, nhẹ, phù hợp inference service |
| **PDF & Chữ ký số** | **Apache PDFBox 3.x + Bouncy Castle** | iText 7 (có AGPL, cẩn thận license) | Open-source thuần Apache 2.0, không vướng license thương mại |
| **File Storage** | **MinIO (S3-compatible)** | Local filesystem, AWS S3 | Self-hosted, API S3, dễ backup, chuẩn industry |
| **Search** | **PostgreSQL FTS (giai đoạn đầu)** → **Elasticsearch 8** (giai đoạn sau) | OpenSearch, Meilisearch | PG FTS đủ cho demo; ES cho scaling thật |
| **Authentication** | **Spring Security 6 + JWT (access + refresh)** | Keycloak, Auth0 | Built-in, không phụ thuộc bên ngoài |
| **Password Hashing** | **Argon2id** | BCrypt | Argon2 là winner của Password Hashing Competition 2015, khuyến nghị OWASP |
| **Reverse Proxy** | **Nginx** | Caddy, Traefik | Industry standard, cấu hình TLS/rate limiting dễ |
| **Container** | **Docker + Docker Compose** | Podman | Chuẩn công nghiệp, dev/prod parity |
| **CI/CD** | **GitHub Actions** | GitLab CI, Jenkins | Free cho public repo, tích hợp sẵn với GitHub |
| **Monitoring** | **Prometheus + Grafana + Loki** | ELK Stack | Nhẹ hơn ELK, đủ cho demo |
| **Testing** | **JUnit 5 + Mockito (backend), Vitest + Playwright (frontend)** | TestNG, Jest, Cypress | Chuẩn hiện đại 2024–2026 |

---

# 3. Frontend — Next.js 14 + TypeScript

## 3.1. Tại sao chọn Next.js thay vì React thuần / CRA

| Tiêu chí | Next.js 14 | React (Vite) | Vue 3 + Nuxt |
|---|---|---|---|
| SSR / SEO | ✅ Built-in | ❌ Cần setup thêm | ✅ |
| Middleware bảo mật | ✅ Built-in | ❌ | ✅ |
| File-based routing | ✅ | ❌ | ✅ |
| API Routes (BFF pattern) | ✅ | ❌ | ✅ |
| Ecosystem React | ✅ Lớn nhất | ✅ | ❌ |
| Học đường cong | Trung bình | Dễ | Trung bình |
| Phù hợp hệ thống nội bộ | ✅ | ✅ | ✅ |

**Kết luận:** Next.js App Router + Server Components giúp:
- Giảm bundle size (code không cần ở client thì giữ lại server)
- Middleware chặn request chưa auth ngay tại edge
- Dễ làm protected route theo role

## 3.2. Cấu trúc thư mục frontend

```
frontend/
├── src/
│   ├── app/                          # App Router
│   │   ├── (auth)/
│   │   │   ├── login/
│   │   │   └── layout.tsx
│   │   ├── (dashboard)/
│   │   │   ├── inbound/              # Công văn đến
│   │   │   ├── outbound/             # Công văn đi
│   │   │   ├── approvals/            # Phê duyệt
│   │   │   ├── search/
│   │   │   ├── admin/
│   │   │   └── layout.tsx
│   │   ├── api/                      # BFF routes (nếu cần)
│   │   └── layout.tsx
│   ├── components/
│   │   ├── ui/                       # shadcn/ui primitives
│   │   ├── documents/                # Domain components
│   │   ├── ocr/
│   │   ├── signature/
│   │   └── shared/
│   ├── lib/
│   │   ├── api-client.ts             # Axios/fetch wrapper
│   │   ├── auth.ts                   # Auth utilities
│   │   └── utils.ts
│   ├── hooks/
│   │   ├── use-documents.ts          # TanStack Query hooks
│   │   ├── use-ocr.ts
│   │   └── use-auth.ts
│   ├── schemas/                      # Zod schemas
│   │   ├── document.schema.ts
│   │   └── user.schema.ts
│   ├── stores/                       # Zustand stores
│   │   └── auth-store.ts
│   └── types/
│       └── api.types.ts
├── public/
├── next.config.js
├── tailwind.config.ts
└── tsconfig.json
```

## 3.3. Thư viện chính cho frontend

| Mục đích | Thư viện | Version | Ghi chú |
|---|---|---|---|
| UI primitives | `shadcn/ui` + `@radix-ui/*` | latest | Accessibility tốt |
| Styling | `tailwindcss` | 3.4+ | JIT compiler |
| Data fetching | `@tanstack/react-query` | 5.x | Cache, optimistic update |
| State | `zustand` | 4.x | Đơn giản, đủ dùng |
| Form | `react-hook-form` + `@hookform/resolvers` | latest | Performance tốt |
| Validation | `zod` | 3.x | Type-safe, share schema với backend |
| HTTP client | `axios` hoặc `ky` | latest | Interceptor cho auth |
| PDF viewer | `react-pdf` (pdf.js) | 7.x | Hiển thị PDF trong trang |
| Rich text editor (soạn công văn) | `Tiptap` | 2.x | Extensible, headless |
| File upload | `react-dropzone` | 14.x | Drag-drop nhiều file |
| Date picker | `date-fns` + `react-day-picker` | latest | i18n tiếng Việt |
| Icon | `lucide-react` | latest | Tree-shakeable |
| Chart (dashboard) | `recharts` hoặc `@tanstack/react-charts` | latest | Cho dashboard thống kê |
| Notification | `sonner` | latest | Toast đẹp, nhẹ |
| Table | `@tanstack/react-table` | 8.x | Headless, flexible |

## 3.4. Pattern quan trọng cho frontend

### Auth Flow với HttpOnly Cookie + Middleware

```typescript
// middleware.ts
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const token = request.cookies.get('access_token');
  const isAuthPage = request.nextUrl.pathname.startsWith('/login');

  if (!token && !isAuthPage) {
    return NextResponse.redirect(new URL('/login', request.url));
  }
  if (token && isAuthPage) {
    return NextResponse.redirect(new URL('/inbound', request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico|api/public).*)'],
};
```

### Chia sẻ Zod schema giữa FE và BE

Dùng `zod` để định nghĩa schema **một lần**, generate TypeScript types cho FE, đồng thời dùng thư viện như `zod-to-openapi` để sinh OpenAPI spec đồng bộ với backend (nếu backend là Node). Nếu backend Java, giữ schema đồng bộ bằng OpenAPI Generator.

---

# 4. Backend — Spring Boot 3 + Java 21 (LTS)

## 4.1. Tại sao chọn Spring Boot thay vì .NET / Node.js / Laravel

| Tiêu chí | Spring Boot 3 | ASP.NET Core 8 | Node.js (NestJS) | Laravel |
|---|---|---|---|---|
| Transaction management | ✅ Xuất sắc (@Transactional) | ✅ Tốt | ⚠️ Manual hơn | ✅ Tốt |
| Thư viện ký số PDF | ✅ **Bouncy Castle + PDFBox (tốt nhất)** | ✅ iTextSharp | ⚠️ Hạn chế | ⚠️ Hạn chế |
| Bảo mật mặc định | ✅ Spring Security rất mature | ✅ Identity | ⚠️ Cần tự cấu hình | ✅ |
| Tài liệu tiếng Việt | ✅ Nhiều | ✅ | ✅ | ✅ |
| Phổ biến trong hệ thống chính phủ VN | ✅ **Rất phổ biến** | ✅ | ⚠️ | ⚠️ |
| Performance (I/O bound) | ✅ Virtual Threads (Java 21) | ✅ | ✅ Async-first | ⚠️ |
| Tooling | ✅ IntelliJ IDEA | ✅ Visual Studio | ✅ VSCode | ✅ |

**Điểm quyết định:** Bouncy Castle là thư viện mã hóa **tham chiếu của ngành** (reference implementation). Nó hỗ trợ PKCS#7, PKCS#11, RSA, ECDSA, PAdES… đầy đủ. Kết hợp với Apache PDFBox để nhúng chữ ký số vào PDF đúng chuẩn PAdES (EU) và TCVN 11816 (Việt Nam).

.NET với iTextSharp cũng tốt tương đương, nhưng iText có license AGPL (thương mại cần mua). Bouncy Castle là MIT/Apache — an toàn về license.

## 4.2. Cấu trúc Maven/Gradle cho Modular Monolith

```
backend/
├── pom.xml                           # Parent POM
├── congvan-app/                      # Module chạy (Spring Boot Application)
│   ├── src/main/java/vn/edu/.../CongvanApplication.java
│   └── src/main/resources/application.yml
├── congvan-common/                   # DTO, utils, exceptions dùng chung
├── congvan-auth/                     # Module Auth & User
│   ├── domain/                       # Entity, Value Object
│   ├── repository/
│   ├── service/
│   ├── controller/
│   └── security/
├── congvan-masterdata/               # Module danh mục
├── congvan-inbound/                  # Module công văn đến
├── congvan-outbound/                 # Module công văn đi
├── congvan-workflow/                 # Module workflow & approval
├── congvan-ocr/                      # Module OCR (client gọi OCR service)
├── congvan-signature/                # Module chữ ký số
├── congvan-search/                   # Module tìm kiếm
├── congvan-audit/                    # Module audit & notification
└── congvan-integration/              # Adapter gọi OCR service, email, queue
```

**Quy tắc phụ thuộc giữa module:**
- Module nghiệp vụ **chỉ phụ thuộc** `congvan-common` và `congvan-auth`.
- Không có dependency vòng tròn (circular dependency).
- Giao tiếp giữa module qua **interface (port) + implementation (adapter)** — pattern Hexagonal/Clean Architecture nhẹ.

## 4.3. Thư viện chính cho backend

```xml
<!-- Spring Boot Starters -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- JWT -->
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>

<!-- Argon2 Password Hashing -->
<dependency>
  <groupId>de.mkammerer</groupId>
  <artifactId>argon2-jvm</artifactId>
  <version>2.11</version>
</dependency>

<!-- PDF & Chữ ký số -->
<dependency>
  <groupId>org.apache.pdfbox</groupId>
  <artifactId>pdfbox</artifactId>
  <version>3.0.3</version>
</dependency>
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcprov-jdk18on</artifactId>
  <version>1.78.1</version>
</dependency>
<dependency>
  <groupId>org.bouncycastle</groupId>
  <artifactId>bcpkix-jdk18on</artifactId>
  <version>1.78.1</version>
</dependency>

<!-- MinIO Client (File storage) -->
<dependency>
  <groupId>io.minio</groupId>
  <artifactId>minio</artifactId>
  <version>8.5.12</version>
</dependency>

<!-- MapStruct (DTO mapping) -->
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>1.5.5.Final</version>
</dependency>

<!-- Database migration -->
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>

<!-- OpenAPI / Swagger -->
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>

<!-- Rate limiting -->
<dependency>
  <groupId>com.bucket4j</groupId>
  <artifactId>bucket4j-core</artifactId>
  <version>8.10.1</version>
</dependency>

<!-- Monitoring -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Testing -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.testcontainers</groupId>
  <artifactId>postgresql</artifactId>
  <scope>test</scope>
</dependency>
```

## 4.4. Cấu trúc một module điển hình (ví dụ module Inbound)

```
congvan-inbound/
└── src/main/java/vn/edu/uni/congvan/inbound/
    ├── domain/
    │   ├── InboundDocument.java             # @Entity
    │   ├── InboundDocumentStatus.java       # enum
    │   └── events/
    │       └── DocumentReceivedEvent.java   # Domain event
    ├── repository/
    │   ├── InboundDocumentRepository.java   # Spring Data JPA
    │   └── specifications/
    │       └── InboundSearchSpec.java       # JPA Criteria cho tìm kiếm
    ├── service/
    │   ├── InboundDocumentService.java      # Interface
    │   ├── impl/
    │   │   └── InboundDocumentServiceImpl.java
    │   └── validator/
    │       └── InboundDocumentValidator.java
    ├── controller/
    │   ├── InboundDocumentController.java
    │   └── dto/
    │       ├── CreateInboundRequest.java
    │       ├── UpdateInboundRequest.java
    │       └── InboundDocumentResponse.java
    ├── mapper/
    │   └── InboundDocumentMapper.java       # MapStruct
    └── exception/
        └── InboundDocumentException.java
```

## 4.5. Key pattern: Transactional + Outbox cho consistency

Khi duyệt công văn đi, cần đồng thời:
1. Ghi `approval`
2. Tạo `workflow_step`
3. Update `documents.current_status`
4. Update `approved_version_id`
5. Ghi `audit_log`
6. **Gửi notification** (bất đồng bộ)

**Pattern Outbox:** không gọi RabbitMQ trực tiếp trong transaction (nếu MQ down thì rollback cũng không rollback được message đã send). Thay vào đó:

```java
@Transactional
public void approve(Long documentId, ApprovalRequest req, User approver) {
    // 1–5: tất cả DB operations trong cùng transaction
    var doc = documentRepo.findByIdForUpdate(documentId);
    validateState(doc, approver);

    approvalRepo.save(new Approval(...));
    workflowStepRepo.save(new WorkflowStep(...));
    doc.setCurrentStatus(APPROVED);
    doc.setApprovedVersionId(req.versionId());
    auditLogRepo.save(new AuditLog(...));

    // 6: Lưu message vào outbox table (cùng transaction)
    outboxRepo.save(new OutboxMessage(
        "NOTIFICATION",
        new ApprovalNotificationPayload(...)
    ));
}

// Scheduled job đọc outbox và publish lên RabbitMQ
@Scheduled(fixedDelay = 2000)
public void publishOutboxMessages() {
    var messages = outboxRepo.findUnpublishedBatch(50);
    for (var msg : messages) {
        try {
            rabbitTemplate.convertAndSend(msg.topic(), msg.payload());
            msg.markPublished();
        } catch (Exception e) {
            log.error("Failed to publish", e);
        }
    }
}
```

Pattern này đảm bảo **"exactly-once delivery"** theo góc nhìn nghiệp vụ, là best practice trong hệ thống có transaction + messaging.

---

# 5. OCR — PaddleOCR + FastAPI Service

## 5.1. Tại sao tách OCR thành service Python riêng

- OCR engine chất lượng cao (PaddleOCR, VietOCR) đều là **Python-native** — viết bằng Java sẽ mất thời gian integrate, chất lượng kém hơn.
- OCR là **CPU/GPU-heavy**, nếu chạy chung với backend Spring sẽ làm nghẽn thread pool xử lý business logic.
- Cô lập giúp **scale OCR service độc lập** (chạy nhiều replica cho batch lớn) mà không cần scale toàn bộ backend.
- Dễ thay thế engine (Paddle → VietOCR → Azure Form Recognizer) mà không ảnh hưởng backend.

## 5.2. So sánh OCR engine cho tiếng Việt

| Engine | Độ chính xác tiếng Việt | Tốc độ | License | Deploy |
|---|---|---|---|---|
| **PaddleOCR** | ⭐⭐⭐⭐⭐ | Trung bình–Nhanh | Apache 2.0 | Python, Docker |
| **VietOCR** | ⭐⭐⭐⭐⭐ (tối ưu cho VN) | Trung bình | Apache 2.0 | Python, PyTorch |
| **Tesseract 5** | ⭐⭐⭐ (cần train lại cho VN) | Nhanh | Apache 2.0 | C++, dễ deploy |
| **EasyOCR** | ⭐⭐⭐⭐ | Chậm | Apache 2.0 | Python |
| **Azure Form Recognizer** | ⭐⭐⭐⭐⭐ | Nhanh | Thương mại | Cloud API |

**Khuyến nghị:** Dùng **PaddleOCR** cho bài toán tổng quát (vừa detect text, vừa recognize), có khả năng nhận cả bảng, và hỗ trợ sẵn tiếng Việt. Làm fallback/so sánh bằng **VietOCR** khi confidence thấp.

## 5.3. Cấu trúc OCR Service (Python)

```
ocr-service/
├── app/
│   ├── main.py                       # FastAPI app
│   ├── api/
│   │   ├── ocr.py                    # POST /ocr/process
│   │   └── health.py
│   ├── core/
│   │   ├── config.py                 # Pydantic Settings
│   │   └── security.py               # Internal API key check
│   ├── services/
│   │   ├── preprocessor.py           # Deskew, denoise, binarize (OpenCV)
│   │   ├── ocr_engine.py             # PaddleOCR wrapper
│   │   ├── field_extractor.py        # Regex + NER bóc tách trường
│   │   └── postprocessor.py
│   ├── schemas/
│   │   └── ocr.py                    # Pydantic models
│   └── workers/
│       └── celery_app.py             # Nếu dùng Celery cho async
├── models/                           # Pre-trained weights
├── tests/
├── Dockerfile
├── requirements.txt
└── docker-compose.yml
```

## 5.4. Thư viện Python chính

```
# requirements.txt
fastapi==0.115.0
uvicorn[standard]==0.30.6
pydantic==2.9.0
pydantic-settings==2.5.2

# OCR
paddleocr==2.8.1
paddlepaddle==2.6.1
# hoặc
vietocr==0.3.12

# Image preprocessing
opencv-python-headless==4.10.0
pillow==10.4.0
numpy==1.26.4

# PDF handling
pypdf==5.0.1
pdf2image==1.17.0

# Field extraction (regex + NER)
vncorenlp==1.0.3                      # Tokenizer tiếng Việt
underthesea==6.8.4                    # NLP tiếng Việt

# Async queue
celery==5.4.0
redis==5.0.8

# Monitoring
prometheus-client==0.20.0

# Testing
pytest==8.3.3
httpx==0.27.2
```

## 5.5. Pipeline xử lý OCR

```
┌──────────────────────────────────────────────────────────────────┐
│  Input: PDF / Ảnh scan                                           │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│  1. PRE-PROCESSING (OpenCV)                                      │
│     • Convert PDF → image (nếu cần)                              │
│     • Grayscale                                                  │
│     • Deskew (xoay ảnh bị lệch)                                  │
│     • Denoise (giảm nhiễu)                                       │
│     • Adaptive thresholding (binarize)                           │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│  2. OCR (PaddleOCR)                                              │
│     • Text detection (DB, EAST)                                  │
│     • Text recognition (CRNN, SVTR)                              │
│     • Output: list[(bbox, text, confidence)]                     │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│  3. FIELD EXTRACTION                                             │
│     • Regex cho các trường có pattern rõ:                        │
│       - Số ký hiệu: /\d+\/\w+-\w+/                               │
│       - Ngày: /ngày\s+\d+\s+tháng\s+\d+\s+năm\s+\d+/             │
│     • Position-based: trích yếu thường nằm sau "V/v:"            │
│     • NER (VNCoreNLP) cho tên tổ chức, tên người                 │
└──────────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────────┐
│  4. RESPONSE TO BACKEND                                          │
│     {                                                            │
│       "raw_text": "...",                                         │
│       "confidence": 0.89,                                        │
│       "fields": [                                                │
│         {"name":"code_number","value":"123/QĐ-ĐHCN","conf":0.95},│
│         {"name":"issue_date","value":"2026-04-10","conf":0.90},  │
│         {"name":"title_summary","value":"...","conf":0.78}       │
│       ]                                                          │
│     }                                                            │
└──────────────────────────────────────────────────────────────────┘
```

## 5.6. Giao tiếp Backend ↔ OCR Service

- **Authentication giữa services:** Internal API key (không để OCR service public ra internet).
- **Transport:** HTTP REST (đơn giản), hoặc gRPC nếu cần tối ưu latency.
- **Pattern:** Async — backend tạo `ocr_job`, gửi job vào RabbitMQ, OCR service consume, xử lý xong callback webhook về backend hoặc publish result ra queue khác.

---

# 6. Chữ ký số — Apache PDFBox + Bouncy Castle

## 6.1. Quy chuẩn ký số tại Việt Nam

Đề tài cần tuân thủ:
- **TCVN 11816-1:2017** — Định dạng chữ ký số nâng cao PAdES (Việt Nam adopt từ ETSI EN 319 142)
- **Nghị định 130/2018/NĐ-CP** — về chữ ký số và dịch vụ chứng thực
- **Thông tư 41/2017/TT-BTTTT** — quy chuẩn kỹ thuật

Trong phạm vi demo, **không cần** ký với chứng thư do RootCA Việt Nam cấp thật (rất phức tạp), chỉ cần **mô phỏng đúng quy trình PKI** với self-signed CA hoặc test certificate.

## 6.2. Kiến trúc 3 mức ký số

| Mức | Mô tả | Dùng khi nào |
|---|---|---|
| **Mức 1 — Demo/Test** | Self-signed cert, private key lưu trong file `.p12` (protected bằng password), ký server-side | Dùng cho báo cáo/bảo vệ đề tài |
| **Mức 2 — USB Token** | Private key trong USB Token (HSM nhỏ), ký client-side qua plugin (VNPT Plugin, ViettelSign Plugin), backend chỉ verify | Dùng khi triển khai pilot |
| **Mức 3 — Remote Signing** | Private key trên HSM của CA (ví dụ MySign của VNPT), ký qua API, backend gọi API của CA | Dùng khi triển khai thật, lớn |

**Khuyến nghị cho đề tài:** Implement **Mức 1** đầy đủ + mô tả thiết kế cho Mức 2, 3 trong báo cáo → thể hiện tầm nhìn mở rộng.

## 6.3. Flow ký số PAdES-B-LT (long-term validation)

```
┌─────────────────────────────────────────────────────────────┐
│  1. User click "Ký số" trên văn bản version đã duyệt        │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  2. Backend validate:                                       │
│     • user có certificate active?                           │
│     • certificate còn hạn, chưa bị revoke?                  │
│     • version đang ký = approved_version_id?                │
│     • hash file chưa đổi?                                   │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  3. Prepare PDF signature dictionary (PDFBox)               │
│     • Tạo SignatureDictionary                               │
│     • Set Filter=Adobe.PPKLite, SubFilter=ETSI.CAdES.detached│
│     • Reserve ByteRange cho chữ ký                          │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Compute hash (SHA-256) of PDF content                   │
│     • Loại trừ phần signature placeholder                   │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  5. Sign hash với private key (Bouncy Castle CMS)           │
│     • CMSSignedDataGenerator                                │
│     • addSignerInfoGenerator(RSA/SHA-256)                   │
│     • Include signing certificate + chain                   │
│     • Add signed attributes (signing-time, content-type)    │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  6. (Optional) Request timestamp từ TSA                     │
│     • Freetsa.org hoặc TSA nội bộ                           │
│     • Gắn RFC 3161 timestamp vào signature                  │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  7. Embed signature vào PDF (thay signature placeholder)    │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  8. Save signed file to MinIO                               │
│     • Ghi digital_signatures row                            │
│     • Update documents.issued_file_id                       │
│     • Gửi notification                                      │
└─────────────────────────────────────────────────────────────┘
```

## 6.4. Code ví dụ ký số (Java + PDFBox + Bouncy Castle)

```java
@Service
public class PdfSigningService {

    public byte[] signPdf(byte[] pdfBytes, PrivateKey privateKey,
                          X509Certificate cert) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            signature.setName("Nguyễn Văn A"); // signer name
            signature.setLocation("Hà Nội, Việt Nam");
            signature.setReason("Phê duyệt công văn");
            signature.setSignDate(Calendar.getInstance());

            doc.addSignature(signature);

            ExternalSigningSupport signingSupport =
                doc.saveIncrementalForExternalSigning(
                    new ByteArrayOutputStream()
                );

            byte[] contentToSign = IOUtils.toByteArray(
                signingSupport.getContent()
            );

            // Tạo CMS signature với Bouncy Castle
            CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
            ContentSigner signer = new JcaContentSignerBuilder(
                "SHA256withRSA"
            ).build(privateKey);

            gen.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                    new JcaDigestCalculatorProviderBuilder().build()
                ).build(signer, cert)
            );
            gen.addCertificates(
                new JcaCertStore(List.of(cert))
            );

            CMSSignedData signedData = gen.generate(
                new CMSProcessableByteArray(contentToSign),
                false
            );

            signingSupport.setSignature(signedData.getEncoded());

            // Return signed PDF bytes
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            doc.save(output);
            return output.toByteArray();
        }
    }
}
```

## 6.5. Flow xác minh chữ ký

```java
public SignatureVerificationResult verify(byte[] signedPdf) throws Exception {
    try (PDDocument doc = Loader.loadPDF(signedPdf)) {
        List<PDSignature> signatures = doc.getSignatureDictionaries();
        for (PDSignature sig : signatures) {
            byte[] signedContent = sig.getSignedContent(signedPdf);
            byte[] signatureBytes = sig.getContents(signedPdf);

            CMSSignedData cms = new CMSSignedData(
                new CMSProcessableByteArray(signedContent),
                signatureBytes
            );

            Store<X509CertificateHolder> certStore = cms.getCertificates();
            SignerInformationStore signers = cms.getSignerInfos();

            for (SignerInformation signer : signers.getSigners()) {
                Collection<X509CertificateHolder> matches =
                    certStore.getMatches(signer.getSID());
                X509CertificateHolder certHolder = matches.iterator().next();
                X509Certificate cert = new JcaX509CertificateConverter()
                    .getCertificate(certHolder);

                boolean valid = signer.verify(
                    new JcaSimpleSignerInfoVerifierBuilder().build(cert)
                );

                if (!valid) return SignatureVerificationResult.INVALID;

                // Check cert validity, revocation (CRL/OCSP)
                cert.checkValidity();
                // TODO: OCSP check ở production
            }
        }
        return SignatureVerificationResult.VALID;
    }
}
```

---

# 7. Database — PostgreSQL 16

## 7.1. Tại sao PostgreSQL thay vì MySQL

| Tính năng | PostgreSQL 16 | MySQL 8 |
|---|---|---|
| Full-text search (tiếng Việt) | ✅ Rất mạnh (tsvector + unaccent + pg_trgm) | ⚠️ Kém |
| JSONB indexing | ✅ GIN index trên JSONB | ⚠️ JSON generated column |
| Partial index | ✅ | ❌ |
| CHECK constraint với sub-query | ✅ | ⚠️ |
| Row-level security | ✅ | ❌ |
| Array type | ✅ | ❌ |
| Extensions | ✅ Nhiều (pg_trgm, unaccent, pgcrypto) | ⚠️ |
| ACID rigor | ✅ Nghiêm ngặt | ✅ |
| Concurrency (MVCC) | ✅ Tốt hơn | ✅ |

Với hệ thống cần full-text search nội dung OCR **tiếng Việt có dấu**, partial index theo trạng thái, JSONB cho metadata linh hoạt → PostgreSQL là lựa chọn rõ ràng.

## 7.2. Extensions cần bật

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";     -- UUID
CREATE EXTENSION IF NOT EXISTS "pgcrypto";      -- Hashing, encryption
CREATE EXTENSION IF NOT EXISTS "unaccent";      -- Bỏ dấu tiếng Việt
CREATE EXTENSION IF NOT EXISTS "pg_trgm";       -- Fuzzy search
CREATE EXTENSION IF NOT EXISTS "btree_gin";     -- GIN index cho composite
```

## 7.3. Full-text search tiếng Việt

PostgreSQL mặc định không có text search config cho tiếng Việt. Cách xử lý:

```sql
-- 1. Tạo text search config Vietnamese (dựa trên simple + unaccent)
CREATE TEXT SEARCH CONFIGURATION vietnamese (COPY = simple);
ALTER TEXT SEARCH CONFIGURATION vietnamese
  ALTER MAPPING FOR hword, hword_part, word
  WITH unaccent, simple;

-- 2. Thêm cột tsvector vào bảng documents
ALTER TABLE documents
  ADD COLUMN search_vector tsvector
  GENERATED ALWAYS AS (
    setweight(to_tsvector('vietnamese', coalesce(code_number, '')), 'A') ||
    setweight(to_tsvector('vietnamese', coalesce(title_summary, '')), 'B') ||
    setweight(to_tsvector('vietnamese', coalesce(content_text, '')), 'C')
  ) STORED;

-- 3. Tạo GIN index
CREATE INDEX idx_documents_search ON documents USING GIN (search_vector);

-- 4. Query tìm kiếm
SELECT id, code_number, title_summary,
       ts_rank(search_vector, query) AS rank
FROM documents,
     to_tsquery('vietnamese', unaccent('công văn & khẩn')) AS query
WHERE search_vector @@ query
ORDER BY rank DESC
LIMIT 20;
```

## 7.4. Indexing strategy quan trọng

```sql
-- B-tree cho filter theo trạng thái
CREATE INDEX idx_docs_status ON documents(current_status)
  WHERE is_deleted = false;

-- Partial index cho công văn đang xử lý (chỉ một phần data được query nhiều)
CREATE INDEX idx_docs_active_assignee
  ON documents(current_assignee_user_id)
  WHERE current_status IN ('DANG_XU_LY', 'CHO_DUYET');

-- Composite index cho sổ công văn
CREATE UNIQUE INDEX idx_book_entries_unique
  ON document_book_entries(document_book_id, entry_year, entry_number)
  WHERE entry_status != 'CANCELLED';

-- Index cho JSONB metadata
CREATE INDEX idx_docs_metadata ON documents USING GIN (metadata_json);
```

## 7.5. Row-Level Security (RLS) cho phân quyền

PostgreSQL RLS có thể enforce phân quyền **ở tầng DB**, giảm rủi ro bug phân quyền ở application layer:

```sql
-- Enable RLS
ALTER TABLE documents ENABLE ROW LEVEL SECURITY;

-- Policy: văn thư phòng/ban chỉ xem công văn của phòng mình
CREATE POLICY dept_visibility ON documents
  FOR SELECT
  USING (
    current_setting('app.user_role') = 'ADMIN'
    OR current_department_id = current_setting('app.user_dept_id')::bigint
    OR created_by = current_setting('app.user_id')::bigint
  );
```

Tuy nhiên trong đề tài, **chỉ nên mô tả RLS trong báo cáo như một "future enhancement"**, chưa cần implement để tránh phức tạp hóa demo.

## 7.6. Migration — dùng Flyway

Tất cả thay đổi schema đi qua Flyway migration, không sửa tay DB production:

```
src/main/resources/db/migration/
├── V1__init_schema.sql
├── V2__create_users_tables.sql
├── V3__create_documents_tables.sql
├── V4__create_workflow_tables.sql
├── V5__create_ocr_tables.sql
├── V6__create_signature_tables.sql
├── V7__add_search_vector.sql
└── V8__seed_master_data.sql
```

---

# 8. Bảo mật — Mô hình phòng thủ nhiều lớp (Defense in Depth)

## 8.1. Sơ đồ mô hình bảo mật

```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: NETWORK                                                │
│   • HTTPS/TLS 1.3 bắt buộc                                      │
│   • Firewall — chỉ mở port 443 ra internet                      │
│   • Nginx reverse proxy: rate limit, DDoS basic                 │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│ Layer 2: EDGE / WAF                                             │
│   • Nginx: security headers (CSP, X-Frame-Options, HSTS)        │
│   • Block common attacks (SQL injection, XSS patterns)          │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│ Layer 3: AUTHENTICATION                                         │
│   • Spring Security 6 + JWT                                     │
│   • Access token: 30 phút                                       │
│   • Refresh token: 7 ngày, HttpOnly cookie, rotation            │
│   • Password: Argon2id (memory=64MB, iterations=3)              │
│   • Optional: 2FA TOTP cho admin/lãnh đạo                       │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│ Layer 4: AUTHORIZATION                                          │
│   • RBAC: vai trò (admin, văn thư, lãnh đạo, ...)               │
│   • Resource-based: @PreAuthorize trên từng endpoint            │
│   • Scope-based: kiểm tra department, confidentiality_level     │
│   • Tier-down: công văn mật cần quyền thêm                      │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│ Layer 5: INPUT VALIDATION                                       │
│   • Bean Validation (JSR-380) ở controller                      │
│   • Custom validator cho business rules                         │
│   • File upload: check magic bytes, whitelist MIME, antivirus   │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│ Layer 6: DATA PROTECTION                                        │
│   • PostgreSQL prepared statements (chống SQL injection)        │
│   • Argon2 password hash                                        │
│   • AES-256-GCM cho file mật (encryption at rest)               │
│   • TLS 1.3 cho DB connection (encryption in transit)           │
│   • Secrets trong HashiCorp Vault / env vars (không commit Git) │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────┐
│ Layer 7: AUDIT & MONITORING                                     │
│   • audit_log append-only cho mọi thao tác quan trọng           │
│   • Login attempt logging + account lockout (5 lần sai)         │
│   • Anomaly detection cơ bản (login lạ giờ, download lạ)        │
│   • Prometheus alert: rate 4xx/5xx tăng đột biến                │
└─────────────────────────────────────────────────────────────────┘
```

## 8.2. Cấu hình Spring Security 6 (cấu hình chính)

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh")
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(forbiddenHandler)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'self'; script-src 'self'; object-src 'none';"
                ))
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable()) // deprecated, CSP thay thế
                .referrerPolicy(ref -> ref.policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts
                    .maxAgeInSeconds(31536000)
                    .includeSubDomains(true)
                )
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
}
```

## 8.3. Bảo vệ endpoint với `@PreAuthorize`

```java
@RestController
@RequestMapping("/api/v1/outbound-documents")
public class OutboundDocumentController {

    @PostMapping("/{id}/sign")
    @PreAuthorize("hasRole('LANH_DAO') and @docSecurity.canSign(#id, authentication)")
    public ResponseEntity<?> signDocument(
        @PathVariable Long id,
        @RequestBody @Valid SignRequest req,
        Authentication auth
    ) {
        // ...
    }
}

// Custom security evaluator
@Component("docSecurity")
public class DocumentSecurityEvaluator {
    public boolean canSign(Long docId, Authentication auth) {
        var doc = repo.findById(docId).orElseThrow();
        var user = (UserPrincipal) auth.getPrincipal();
        return doc.getCurrentStatus() == CHO_KY_SO
            && user.hasCertificateActive()
            && doc.getApprovedVersionId() != null;
    }
}
```

## 8.4. File upload security

File upload là vector tấn công quan trọng. Kiểm tra:

```java
public void validateUploadedFile(MultipartFile file) {
    // 1. Size limit
    if (file.getSize() > 50 * 1024 * 1024) {
        throw new FileTooLargeException();
    }

    // 2. Content-type whitelist
    var allowed = Set.of(
        "application/pdf",
        "image/jpeg", "image/png", "image/tiff",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    if (!allowed.contains(file.getContentType())) {
        throw new UnsupportedFileTypeException();
    }

    // 3. Magic byte check (chống fake extension)
    try (var is = file.getInputStream()) {
        byte[] header = is.readNBytes(8);
        if (!isValidPdfMagicBytes(header) && !isValidImageMagicBytes(header)) {
            throw new FileContentMismatchException();
        }
    }

    // 4. Filename sanitization
    String safeFilename = UUID.randomUUID() + getExtension(file.getOriginalFilename());
    // KHÔNG dùng originalFilename làm path (path traversal)

    // 5. Antivirus scan (production)
    // clamAvService.scan(file.getBytes());
}
```

## 8.5. Secrets management

**KHÔNG BAO GIỜ** commit các giá trị sau vào Git:
- JWT secret key
- DB password
- Private key ký số
- SMTP password
- API keys

**Thay thế bằng:**
- Dev: `.env` file trong `.gitignore` + Spring `@ConfigurationProperties`
- Staging/Prod: HashiCorp Vault hoặc cloud secret manager (AWS Secrets Manager, GCP Secret Manager)

```yaml
# application.yml — dùng placeholder
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-ttl: 30m
    refresh-token-ttl: 7d
  signing:
    keystore-path: ${SIGNING_KEYSTORE_PATH}
    keystore-password: ${SIGNING_KEYSTORE_PASSWORD}
```

## 8.6. OWASP Top 10 — đối chiếu

| OWASP 2021 | Cách hệ thống phòng vệ |
|---|---|
| A01: Broken Access Control | RBAC + resource-based check, `@PreAuthorize`, RLS (future) |
| A02: Cryptographic Failures | TLS 1.3, Argon2, AES-256-GCM, không tự implement crypto |
| A03: Injection | JPA prepared statements, Bean Validation, escape output |
| A04: Insecure Design | Threat modeling, review kiến trúc, separation of duties |
| A05: Security Misconfiguration | Security headers, default deny, không expose Actuator |
| A06: Vulnerable Components | OWASP Dependency-Check trong CI, Dependabot |
| A07: Authentication Failures | Argon2, account lockout, MFA (optional), secure cookie |
| A08: Software Integrity Failures | Verify dependencies checksum, signed commits, SCA |
| A09: Logging Failures | Audit log append-only, centralized log (Loki), alert |
| A10: SSRF | Whitelist URL cho internal call (OCR service), validate URL scheme |

---

# 9. Kiến trúc triển khai — Docker + Nginx

## 9.1. Sơ đồ deployment

```
┌──────────────────────────────────────────────────────────────────┐
│                      INTERNET / INTRANET                         │
└──────────────────────────────────────────────────────────────────┘
                              │ HTTPS :443
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│  NGINX (Reverse Proxy + TLS termination + Rate Limit)            │
│  • Static files caching                                          │
│  • WebSocket proxy (future SSE notification)                     │
└──────────────────────────────────────────────────────────────────┘
         │                              │
         ▼ :3000                        ▼ :8080
┌─────────────────────┐        ┌──────────────────────────────────┐
│  FRONTEND           │        │  BACKEND API                     │
│  Next.js container  │◄──────►│  Spring Boot container           │
│                     │        │  • Auth, Inbound, Outbound       │
│                     │        │  • Workflow, Search, Audit       │
└─────────────────────┘        └──────────────────────────────────┘
                                        │              │
                                        │              │
                        ┌───────────────┤              ├──────────────────┐
                        ▼               ▼              ▼                  ▼
                ┌──────────────┐  ┌──────────┐  ┌────────────┐  ┌──────────────┐
                │ OCR SERVICE  │  │ PostgreSQL│  │   Redis    │  │   MinIO      │
                │ FastAPI      │  │ :5432     │  │  :6379     │  │   :9000      │
                │ PaddleOCR    │  │ Port      │  │ Cache +    │  │ S3-compatible│
                │ :5000        │  │ internal  │  │ Session    │  │ File Storage │
                └──────────────┘  └──────────┘  └────────────┘  └──────────────┘
                                                     │
                                                     │
                                         ┌──────────────────┐
                                         │   RabbitMQ       │
                                         │   :5672          │
                                         │   OCR job queue  │
                                         └──────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│  MONITORING STACK (tùy chọn)                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                        │
│  │Prometheus│  │ Grafana  │  │   Loki   │                        │
│  └──────────┘  └──────────┘  └──────────┘                        │
└──────────────────────────────────────────────────────────────────┘
```

## 9.2. `docker-compose.yml` cho development

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: congvan
      POSTGRES_USER: congvan_user
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pg_data:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U congvan_user"]
      interval: 10s

  redis:
    image: redis:7-alpine
    command: redis-server --appendonly yes --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data
    ports:
      - "6379:6379"

  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    environment:
      RABBITMQ_DEFAULT_USER: congvan
      RABBITMQ_DEFAULT_PASS: ${RABBITMQ_PASSWORD}
    ports:
      - "5672:5672"
      - "15672:15672"

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_USER}
      MINIO_ROOT_PASSWORD: ${MINIO_PASSWORD}
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"

  ocr-service:
    build: ./ocr-service
    environment:
      INTERNAL_API_KEY: ${OCR_API_KEY}
      REDIS_URL: redis://:${REDIS_PASSWORD}@redis:6379/0
    depends_on:
      - redis
    ports:
      - "5000:5000"
    deploy:
      resources:
        limits:
          memory: 4G

  backend:
    build: ./backend
    environment:
      SPRING_PROFILES_ACTIVE: docker
      DB_URL: jdbc:postgresql://postgres:5432/congvan
      DB_USER: congvan_user
      DB_PASSWORD: ${DB_PASSWORD}
      REDIS_HOST: redis
      REDIS_PASSWORD: ${REDIS_PASSWORD}
      RABBITMQ_HOST: rabbitmq
      MINIO_ENDPOINT: http://minio:9000
      OCR_SERVICE_URL: http://ocr-service:5000
      OCR_API_KEY: ${OCR_API_KEY}
      JWT_SECRET: ${JWT_SECRET}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_started
      rabbitmq:
        condition: service_started
      minio:
        condition: service_started
    ports:
      - "8080:8080"

  frontend:
    build: ./frontend
    environment:
      NEXT_PUBLIC_API_URL: http://localhost:8080/api/v1
    depends_on:
      - backend
    ports:
      - "3000:3000"

  nginx:
    image: nginx:1.27-alpine
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - frontend
      - backend

volumes:
  pg_data:
  redis_data:
  minio_data:
```

## 9.3. Cấu hình Nginx production

```nginx
# /etc/nginx/nginx.conf
events { worker_connections 1024; }

http {
    include /etc/nginx/mime.types;

    # Rate limiting
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;
    limit_req_zone $binary_remote_addr zone=login:10m rate=3r/m;

    # Gzip
    gzip on;
    gzip_types text/plain text/css application/json application/javascript;

    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header Content-Security-Policy "default-src 'self'; script-src 'self'; object-src 'none';" always;

    # HTTP → HTTPS
    server {
        listen 80;
        return 301 https://$host$request_uri;
    }

    # HTTPS
    server {
        listen 443 ssl http2;
        server_name congvan.example.com;

        ssl_certificate     /etc/nginx/ssl/fullchain.pem;
        ssl_certificate_key /etc/nginx/ssl/privkey.pem;
        ssl_protocols TLSv1.2 TLSv1.3;
        ssl_ciphers HIGH:!aNULL:!MD5;
        ssl_prefer_server_ciphers on;

        client_max_body_size 50M;

        # Frontend (Next.js)
        location / {
            proxy_pass http://frontend:3000;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # Backend API
        location /api/ {
            limit_req zone=api burst=20 nodelay;
            proxy_pass http://backend:8080;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        }

        # Login endpoint stricter rate limit
        location /api/v1/auth/login {
            limit_req zone=login burst=5 nodelay;
            proxy_pass http://backend:8080;
        }
    }
}
```

---

# 10. DevOps & CI/CD — GitHub Actions

## 10.1. Pipeline CI/CD

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:

jobs:
  backend-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_PASSWORD: test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - name: Run tests
        run: mvn -B test
      - name: Run OWASP Dependency Check
        run: mvn org.owasp:dependency-check-maven:check
      - name: Upload coverage
        uses: codecov/codecov-action@v4

  frontend-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
      - run: npm ci
      - run: npm run lint
      - run: npm run type-check
      - run: npm run test
      - run: npm run build

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Trivy scan
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          severity: 'CRITICAL,HIGH'
```

## 10.2. Branching strategy

- `main` — production-ready
- `develop` — integration branch
- `feature/*` — feature branches
- `hotfix/*` — urgent fixes
- Tag version: `v1.0.0`, `v1.1.0`, … (Semantic Versioning)

---

# 11. Monitoring & Observability

## 11.1. 3 trụ cột Observability

| Trụ cột | Công cụ | Mục đích |
|---|---|---|
| **Metrics** | Prometheus + Grafana | CPU, RAM, request rate, error rate, DB pool, custom business metrics |
| **Logs** | Loki + Promtail | Centralized logs, search, correlation ID |
| **Traces** | OpenTelemetry + Jaeger (optional) | Distributed tracing, bottleneck detection |

## 11.2. Custom business metrics

Ngoài metrics mặc định của Spring Boot Actuator, expose thêm các metrics đặc thù nghiệp vụ:

```java
@Component
public class BusinessMetrics {
    private final Counter documentsCreated;
    private final Timer ocrProcessingTime;
    private final Gauge pendingApprovals;

    public BusinessMetrics(MeterRegistry registry,
                          ApprovalRepository approvalRepo) {
        this.documentsCreated = Counter.builder("documents.created.total")
            .description("Total documents created")
            .tag("direction", "any")
            .register(registry);

        this.ocrProcessingTime = Timer.builder("ocr.processing.duration")
            .description("OCR processing time")
            .register(registry);

        this.pendingApprovals = Gauge.builder("approvals.pending.count",
                approvalRepo, r -> r.countPending())
            .register(registry);
    }
}
```

## 11.3. Alerting rules (Prometheus)

```yaml
groups:
  - name: congvan_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        annotations:
          summary: "Error rate > 5% trong 5 phút"

      - alert: OcrServiceDown
        expr: up{job="ocr-service"} == 0
        for: 2m
        annotations:
          summary: "OCR service không phản hồi"

      - alert: DatabaseConnectionsExhausted
        expr: hikaricp_connections_active / hikaricp_connections_max > 0.9
        for: 5m
        annotations:
          summary: "DB connection pool gần cạn"
```

---

# 12. Kế hoạch triển khai (Roadmap 5 tháng — nhóm 8 người)

## Tháng 1 — Nền móng

| Tuần | Việc | Nhóm thực hiện |
|---|---|---|
| 1 | Setup repo, CI/CD, Docker Compose, Flyway migration khởi tạo | 1 dev backend + 1 dev devops |
| 2 | Module Auth & User: đăng nhập, JWT, RBAC, seed users | Nhóm 2 (Backend core) |
| 3 | Frontend skeleton: Next.js + shadcn/ui + layout, trang login | Nhóm 4 (Frontend) |
| 4 | Module Master Data: danh mục phòng ban, loại công văn, sổ | Nhóm 2 |
| | Activity diagram + use case chi tiết | Nhóm 1 (Nghiệp vụ) |

## Tháng 2 — Nghiệp vụ cốt lõi

| Tuần | Việc |
|---|---|
| 5 | Module Inbound Document: CRUD, upload file, state machine |
| 6 | Module Outbound Document: CRUD, versioning, draft |
| 7 | Module Workflow & Approval: forward, approve, reject, assignments |
| 8 | Sổ công văn: document_book_counters, transaction cấp số |

## Tháng 3 — OCR & Chữ ký số

| Tuần | Việc |
|---|---|
| 9 | Setup OCR service (FastAPI + PaddleOCR), test với công văn mẫu |
| 10 | Field extractor: regex + NER, mapping vào ocr_extracted_fields |
| 11 | Tích hợp OCR backend ↔ OCR service (RabbitMQ async) |
| 12 | Ký số: PDFBox + Bouncy Castle, flow sign + verify, certificate management |

## Tháng 4 — Tìm kiếm, UI, Polish

| Tuần | Việc |
|---|---|
| 13 | Full-text search PostgreSQL, tsvector tiếng Việt |
| 14 | Dashboard, notification, audit log UI |
| 15 | Integration testing end-to-end với Playwright |
| 16 | UI polish, responsive, accessibility, i18n |

## Tháng 5 — Hoàn thiện & Bảo vệ

| Tuần | Việc |
|---|---|
| 17 | Security hardening: pentest tự review, OWASP checklist |
| 18 | Viết báo cáo, slide thuyết trình |
| 19 | Dry-run bảo vệ, chỉnh sửa demo |
| 20 | Bảo vệ đề tài |

---

# 13. Phân công theo thành viên (gợi ý nhóm 8 người)

| Thành viên | Vai trò | Công việc chính |
|---|---|---|
| SV1 | **Team Lead / Backend Senior** | Kiến trúc, code review, module core (Auth, Workflow) |
| SV2 | **Backend — Document Module** | Inbound, Outbound, Document Book |
| SV3 | **Backend — Integration** | OCR client, Signature service, RabbitMQ, MinIO |
| SV4 | **OCR Engineer (Python)** | OCR service, preprocess, field extraction |
| SV5 | **Frontend Lead** | Next.js setup, layout, routing, auth flow |
| SV6 | **Frontend — Forms & Tables** | Form nhập công văn, danh sách, search UI |
| SV7 | **Frontend — OCR & Signature UX** | Màn hình OCR review, signature UI, PDF viewer |
| SV8 | **Business Analyst + Docs + Test** | Use case, activity, test case, viết báo cáo |

---

# 14. Các quyết định kỹ thuật cần chốt sớm (và lý do)

1. **Monorepo hay Multi-repo?**
   → **Monorepo** (backend + frontend + OCR service trong 1 repo), dùng workspace/submodule. Dễ quản lý phiên bản đồng bộ cho đề tài.

2. **JWT signing algorithm?**
   → **RS256** (RSA asymmetric) thay vì HS256. Dễ rotate key mà không invalidate toàn bộ token, và public key có thể share cho các service khác verify.

3. **Cache strategy?**
   → Cache **master data** (danh mục phòng ban, loại CV) với TTL 10 phút.
   → **KHÔNG cache** documents (cần consistency cao).
   → Cache JWT blacklist trong Redis với TTL = thời gian còn lại của token.

4. **Pagination — offset hay cursor?**
   → Dùng **offset-based** cho đề tài (đơn giản, phù hợp dataset nhỏ).
   → Ghi chú "sẽ chuyển cursor khi data lớn" trong future work.

5. **Database connection pool size?**
   → HikariCP, `maximumPoolSize = 20` cho backend (đủ cho demo).
   → Formula chuẩn: `connections = ((core_count * 2) + effective_spindle_count)` — tham khảo [HikariCP pool-sizing](https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing).

6. **Thời điểm chốt hash file để ký số?**
   → Hash tại thời điểm **lãnh đạo duyệt cấp cuối**, lưu vào `documents.approved_file_hash`. Khi ký, so sánh hash để đảm bảo file chưa bị sửa.

7. **UUID hay Long ID cho primary key?**
   → **Long ID tự tăng** cho entity nghiệp vụ (dễ debug, index nhỏ hơn).
   → **UUID** cho file path, public ID exposed ra URL (chống enumeration attack).

---

# 15. Checklist kỹ thuật trước khi bảo vệ

## 15.1. Bảo mật
- [ ] HTTPS enforce, HTTP redirect 301 → HTTPS
- [ ] CSP, HSTS, X-Frame-Options headers
- [ ] Password hash với Argon2id
- [ ] JWT access + refresh token, refresh rotation
- [ ] Account lockout sau 5 lần sai
- [ ] Rate limiting cho login (3/phút) và API chung (10/giây)
- [ ] File upload: whitelist MIME, magic bytes, size limit
- [ ] Không có secret trong Git
- [ ] OWASP Dependency-Check pass (không có CVE Critical)
- [ ] SQL injection test pass (parameterized queries)
- [ ] XSS test pass (escape output, CSP)

## 15.2. Dữ liệu
- [ ] Flyway migration version-controlled
- [ ] Index cho foreign key, status, search
- [ ] Unique constraint cho số công văn (theo sổ + năm)
- [ ] Transaction cho mọi thao tác đổi trạng thái
- [ ] Soft delete cho documents, files, versions
- [ ] Audit log append-only
- [ ] Backup script (pg_dump) + test restore

## 15.3. Hiệu năng
- [ ] N+1 query đã được giải quyết (dùng `@EntityGraph` hoặc fetch join)
- [ ] Pagination cho tất cả endpoint list
- [ ] Cache master data
- [ ] OCR chạy async, không block request
- [ ] File lớn stream (không load toàn bộ vào memory)

## 15.4. Nghiệp vụ
- [ ] Cấp số trong transaction với `SELECT ... FOR UPDATE`
- [ ] `approved_version_id` được khóa sau khi duyệt
- [ ] File đã ký gắn đúng `document_version_id`
- [ ] OCR result chỉ được dùng sau khi user confirm
- [ ] Công văn mật có luồng riêng, hạn chế truy cập
- [ ] Notification được tạo cho mọi sự kiện quan trọng

## 15.5. Demo
- [ ] Seed data: 3 phòng ban, 10 users đủ role, 20 công văn mẫu
- [ ] Kịch bản demo end-to-end chạy mượt
- [ ] File PDF mẫu để OCR + test signature
- [ ] Certificate test (self-signed) để ký số demo
- [ ] Dashboard hiển thị thống kê hấp dẫn
- [ ] Backup plan khi demo lỗi (video record)

---

# 16. Điểm nhấn học thuật để bảo vệ

Khi bảo vệ, nhóm nên nhấn mạnh **những điểm mà nhiều đề tài khác bỏ qua**, thể hiện chiều sâu nghiên cứu:

1. **So sánh OCR engine**: đã benchmark PaddleOCR vs VietOCR vs Tesseract trên bộ dữ liệu công văn thật, có bảng số liệu chính xác/tốc độ → điểm nghiên cứu thực.

2. **Full-text search tiếng Việt**: đã giải bài toán tsvector + unaccent cho tiếng Việt có dấu — không phải chỉ dùng LIKE '%...%'.

3. **Transaction + Outbox Pattern**: đảm bảo consistency giữa DB và Message Queue — thể hiện hiểu biết về distributed system, không chỉ "code CRUD".

4. **Version lock + Hash chain**: khi lãnh đạo duyệt, hash được khóa; nếu file bị sửa sau đó, ký số sẽ fail — đây là thiết kế nghiêm túc về tính toàn vẹn.

5. **Layered security (Defense in Depth)**: 7 lớp bảo mật rõ ràng thay vì chỉ "login + JWT".

6. **RBAC + Resource-based + Confidentiality enforcement**: mô hình phân quyền 3 chiều.

7. **Observability**: metrics nghiệp vụ custom thay vì chỉ monitoring hệ thống cơ bản.

8. **Mở rộng**: đã thiết kế để dễ chuyển sang microservices nếu cần (modular monolith), dễ thay OCR engine (Hexagonal), dễ scale (stateless backend).

---

# 17. Những điểm dễ bị hỏi khi bảo vệ — và cách trả lời

| Câu hỏi có thể gặp | Gợi ý trả lời |
|---|---|
| "Tại sao không dùng microservices?" | Phạm vi đề tài thử nghiệm, team 8 người, complexity microservices vượt giá trị mang lại. Kiến trúc Modular Monolith đã tách module rõ ràng, **tối thiểu hóa cost nhưng giữ khả năng mở rộng**. |
| "Làm sao biết OCR chính xác?" | Không claim 100%. OCR là **hỗ trợ bán tự động**, có bước user confirm bắt buộc. Kết quả OCR chỉ thành dữ liệu chính thức sau khi `is_accepted = true`. |
| "Bảo mật khác gì hệ thống thông thường?" | 7 lớp defense-in-depth, không chỉ auth. Ký số có version lock, hash chain. Phân quyền 3 chiều (RBAC + resource + confidentiality). File mật encrypt AES-256. |
| "Scale như thế nào?" | Backend stateless → horizontal scale. Database có read replica. OCR service là stateless container, scale N replica theo queue depth. File storage MinIO cluster. Search chuyển sang Elasticsearch. |
| "Nếu ký số bị lỗi giữa chừng?" | Transaction rollback, status về `CHO_KY_SO`. User có thể retry. Outbox pattern đảm bảo notification không bị mất. |
| "Chữ ký số có chuẩn không?" | Dùng PAdES (TCVN 11816) + PKCS#7/CMS, hash SHA-256, RSA 2048-bit. Tuân thủ Nghị định 130/2018/NĐ-CP. Có thể nâng cấp lên remote signing với CA thật. |

---

# 18. Tổng kết

Stack đề xuất cho đề tài:

```
Frontend:   Next.js 14 + TypeScript + shadcn/ui + TailwindCSS
            + TanStack Query + Zustand + React Hook Form + Zod

Backend:    Spring Boot 3 + Java 21 LTS
            + Spring Security 6 + Spring Data JPA + Flyway
            + Apache PDFBox + Bouncy Castle (ký số)
            + MapStruct + OpenAPI + Actuator

OCR:        FastAPI + Python 3.11 + PaddleOCR + OpenCV
            + VietOCR fallback + Celery (nếu async phức tạp)

Database:   PostgreSQL 16 (FTS tiếng Việt) + Redis 7

Infrastructure:
            Docker + Docker Compose + Nginx
            + RabbitMQ + MinIO
            + Prometheus + Grafana + Loki (optional)

CI/CD:      GitHub Actions + OWASP Dependency-Check + Trivy
```

**3 nguyên tắc xuyên suốt khi code:**

1. **Mọi thay đổi trạng thái → transaction + audit_log.** Không có ngoại lệ.
2. **Mọi file được ký/phát hành → gắn với version cụ thể + hash.** Nếu hash mismatch, từ chối thao tác.
3. **Mọi dữ liệu từ bên ngoài (user input, OCR output, file upload) → validate trước khi xử lý.** Never trust, always verify.

---

# 19. Tài liệu tham khảo

- Spring Boot 3 Reference — https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/
- Spring Security 6 — https://docs.spring.io/spring-security/reference/index.html
- Next.js App Router — https://nextjs.org/docs/app
- PaddleOCR — https://github.com/PaddlePaddle/PaddleOCR
- VietOCR — https://github.com/pbcquoc/vietocr
- Apache PDFBox — https://pdfbox.apache.org/
- Bouncy Castle — https://www.bouncycastle.org/
- PostgreSQL Full-text search — https://www.postgresql.org/docs/16/textsearch.html
- OWASP Top 10 2021 — https://owasp.org/Top10/
- TCVN 11816 — Tiêu chuẩn chữ ký số nâng cao PAdES Việt Nam
- Nghị định 130/2018/NĐ-CP — Chữ ký số và dịch vụ chứng thực chữ ký số
- ETSI EN 319 142 — PAdES Baseline Profiles

---

> **Ghi chú cuối:** Tài liệu này là **đề xuất kỹ thuật chi tiết**, có thể điều chỉnh tùy điều kiện nhóm (máy yếu, không chạy được PaddleOCR thì fallback Tesseract; không dùng được RabbitMQ thì dùng Spring `@Async` thread pool, etc.). Điều quan trọng là **giữ nguyên triết lý kiến trúc và các nguyên tắc bảo mật** đã nêu — đó mới là điểm giúp đề tài nổi bật khi bảo vệ.
