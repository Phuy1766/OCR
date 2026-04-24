# Tài liệu dự án — Hệ thống Quản lý Công văn đi/đến

> **Dự án NCKH sinh viên cấp trường:** Nghiên cứu các kỹ thuật xử lý chữ ký số và nhận diện ký tự quang học OCR hiện đại ứng dụng trong phát triển thử nghiệm hệ thống thông tin hỗ trợ quản lý công văn đi và đến của một đơn vị.

---

## 📂 Cấu trúc thư mục tài liệu khuyến nghị

Khi đưa vào repo code, nên tổ chức như sau:

```
congvan-system/                          # Repo gốc
├── README.md                            # README chính của repo (hướng dẫn build, run)
├── docs/                                # ← Thư mục chứa các file markdown này
│   ├── README.md                        # File này — mục lục
│   ├── 01-architecture-and-business.md  # Kiến trúc & nghiệp vụ
│   ├── 02-tech-stack.md                 # Công nghệ & triển khai
│   └── 03-legal-framework.md            # Khung web theo luật VN
├── backend/                             # Code Spring Boot
├── frontend/                            # Code Next.js
├── ocr-service/                         # Code FastAPI + PaddleOCR
├── db/                                  # Migration scripts (Flyway)
├── docker/                              # Dockerfile, docker-compose.yml
├── scripts/                             # Build scripts, seed data
└── .github/workflows/                   # CI/CD GitHub Actions
```

---

## 📄 Danh sách tài liệu trong thư mục `docs/`

### 1. `01-architecture-and-business.md`
**Nội dung:** Kiến trúc hệ thống + nghiệp vụ chi tiết.

Bao gồm:
- Bài toán, mục tiêu, phạm vi
- Use case diagram (3 nhóm: VB đến, VB đi, quản trị)
- Tác nhân, vai trò, ma trận phân quyền
- Kiến trúc tổng thể (Component Diagram)
- 9 module nghiệp vụ (Auth, Master Data, Inbound, Outbound, Workflow, OCR, Signature, Search, Audit)
- Sequence diagram luồng công văn đến/đi
- Schema CSDL chi tiết (25+ bảng)
- ERD mức logic
- State diagram vòng đời công văn
- Thiết kế API (REST endpoints)
- Kiến trúc backend phân tầng
- Validation rules
- Bảo mật
- Deployment diagram
- Kịch bản demo

**Ai dùng:** Toàn nhóm, đặc biệt là **Nhóm 1 (BA/Docs)** và **Nhóm 2 (Backend core)**.

---

### 2. `02-tech-stack.md`
**Nội dung:** Chốt công nghệ, framework, thư viện, hướng triển khai.

Bao gồm:
- Triết lý chọn công nghệ (4 tiêu chí)
- Bảng stack tổng quan (tất cả các tầng)
- Frontend: Next.js 14 + TypeScript + shadcn/ui (có cấu trúc thư mục chi tiết)
- Backend: Spring Boot 3 + Java 21 (cấu trúc Maven module)
- OCR: PaddleOCR + FastAPI (pipeline xử lý)
- Chữ ký số: Apache PDFBox + Bouncy Castle (code ví dụ)
- Database: PostgreSQL 16 (FTS tiếng Việt, RLS, Flyway)
- Bảo mật: 7 lớp defense-in-depth
- Docker + Nginx + CI/CD
- Monitoring
- Roadmap 5 tháng chi tiết
- Phân công 8 thành viên
- Checklist trước bảo vệ
- Câu hỏi thường gặp khi bảo vệ

**Ai dùng:** **Team Lead** để chốt stack, **Backend/Frontend dev** để tham chiếu khi code.

---

### 3. `03-legal-framework.md`
**Nội dung:** Khung website theo đúng pháp luật Việt Nam.

Bao gồm:
- Căn cứ pháp lý (NĐ 30/2020, Luật GDĐT 2023, Luật BVBMNN 2018, Luật Lưu trữ 2024)
- Sitemap 11 nhóm chức năng
- Chi tiết từng nhóm: trường dữ liệu, workflow, quy tắc pháp lý
- 29 loại văn bản hành chính (Phụ lục III)
- Mẫu Sổ đăng ký (Phụ lục IV)
- Mẫu Phiếu giải quyết văn bản đến
- Trường thông tin đầu vào (Phụ lục VI)
- Quy định ký số (2 chữ ký: cá nhân + cơ quan)
- Phân loại chữ ký điện tử theo Luật GDĐT 2023
- Lập hồ sơ & lưu trữ (Chương IV NĐ 30)
- Ma trận phân quyền theo vai trò công tác văn thư
- **14 business rules với căn cứ pháp lý**
- Checklist compliance pháp lý

**Ai dùng:** **Team Lead** để chốt nghiệp vụ, **Frontend dev** để build form đúng trường, **BA** để viết báo cáo.

---

## 🔗 Cách 3 tài liệu bổ trợ nhau

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   01-architecture       02-tech-stack         03-legal          │
│   (Nghiệp vụ là gì)     (Làm bằng gì)         (Đúng luật ra sao)│
│                                                                 │
│       ↓                      ↓                      ↓           │
│                                                                 │
│   Sequence, ERD,        Java/Python,          NĐ 30/2020,       │
│   state machine         Bouncy Castle,        Luật GDĐT 2023,   │
│                         PaddleOCR             Phụ lục IV, VI    │
│                                                                 │
│                          ↓↓↓                                    │
│                                                                 │
│                      CODE THỰC TẾ                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

**Khi code một chức năng**, đọc theo thứ tự:
1. Đọc `03-legal-framework.md` để biết **phải có trường gì, ràng buộc gì theo luật**.
2. Đọc `01-architecture-and-business.md` để biết **logic nghiệp vụ, luồng xử lý, quan hệ CSDL**.
3. Đọc `02-tech-stack.md` để biết **dùng thư viện nào, code pattern nào**.

---

## 🗂️ Bản đồ liên kết giữa tài liệu và module code

| Module code | Đọc tài liệu nào | Mục nào |
|---|---|---|
| `congvan-auth` | 01, 02, 03 | 01 §5.1, 02 §8, 03 §12 |
| `congvan-masterdata` | 01, 03 | 01 §5.2, 03 §11 |
| `congvan-inbound` | 01, 03 | 01 §5.3, 03 §4 |
| `congvan-outbound` | 01, 03 | 01 §5.4, 03 §5 |
| `congvan-workflow` | 01 | 01 §5.5 |
| `congvan-ocr` (backend) | 01, 02 | 01 §5.6, 02 §5 |
| `ocr-service` (Python) | 02 | 02 §5 |
| `congvan-signature` | 01, 02, 03 | 01 §5.7, 02 §6, 03 §7 |
| `congvan-search` | 01, 02 | 01 §5.8, 02 §7 |
| `congvan-audit` | 01, 03 | 01 §5.9, 03 §13 (BR-10) |
| `frontend/` | 02, 03 | 02 §3, 03 §2-10 (UI từng màn hình) |
| `db/migration/` | 01, 02 | 01 §7 (bảng CSDL), 02 §7 |
| `docker/` | 02 | 02 §9 |

---

## ✅ Checklist trước khi code

Trước khi một thành viên bắt đầu code module được giao, tick đủ các mục:

- [ ] Đã đọc toàn bộ mục liên quan đến module của mình trong 01, 02, 03
- [ ] Hiểu các **business rule** có căn cứ pháp lý trong tài liệu 03 §13
- [ ] Biết **pattern code** (transaction, outbox, version lock, audit log) trong tài liệu 02
- [ ] Biết **các trường dữ liệu bắt buộc** theo Phụ lục VI NĐ 30/2020
- [ ] Đã setup local dev environment (docker-compose.yml theo 02 §9.2)
- [ ] Đã clone repo, tạo branch `feature/<tên>`

---

## 📝 Quy ước cập nhật tài liệu

Khi có thay đổi thiết kế trong quá trình code:

1. **KHÔNG sửa trực tiếp 3 file tài liệu gốc** — giữ là bản gốc để tham chiếu khi bảo vệ.
2. Tạo file `docs/CHANGELOG.md` ghi lại mọi thay đổi so với thiết kế gốc, kèm lý do.
3. Nếu thay đổi lớn (ví dụ đổi công nghệ), thảo luận với team trước và ghi rõ trong CHANGELOG.
4. Khi viết báo cáo cuối cùng, tổng hợp cả tài liệu gốc + CHANGELOG để thể hiện quá trình nghiên cứu.

---

## 🎓 Khi bảo vệ đề tài

Chuẩn bị:
- **Bản in 3 tài liệu này** (đóng thành 1 quyển phụ lục báo cáo) — thể hiện chiều sâu nghiên cứu.
- **Slide tóm tắt** rút từ các mục quan trọng (đặc biệt là §16 tài liệu 02 và §17 tài liệu 03).
- **Bảng đối chiếu** giữa tài liệu và code thực tế — chứng minh code đúng thiết kế.

Nếu hội đồng hỏi câu gì, tra cứu nhanh:
- "Tại sao chọn stack này?" → Tài liệu 02 §2
- "Có đúng luật VN không?" → Tài liệu 03 §17.1
- "Cấu trúc CSDL như nào?" → Tài liệu 01 §7
- "Bảo mật ra sao?" → Tài liệu 02 §8 + Tài liệu 03 §13 BR-12
- "Kiến trúc có mở rộng được không?" → Tài liệu 01 §4.4

---

## 📞 Phân công phụ trách tài liệu

| Tài liệu | Owner (gợi ý) | Trách nhiệm |
|---|---|---|
| 01-architecture-and-business.md | SV8 (BA + Docs) | Rà soát, cập nhật CHANGELOG khi thay đổi |
| 02-tech-stack.md | SV1 (Team Lead) | Chốt stack cuối cùng, rà soát khi có đổi thư viện |
| 03-legal-framework.md | SV8 (BA + Docs) | Cập nhật nếu có văn bản pháp luật mới |
| README.md (file này) | SV1 (Team Lead) | Index, đảm bảo link hoạt động |

---

**Chúc nhóm code và bảo vệ thành công! 🎯**
