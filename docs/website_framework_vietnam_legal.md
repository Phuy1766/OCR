# Khung Website Hệ thống Quản lý Công văn đi/đến
## Theo chuẩn nghiệp vụ văn thư và pháp luật Việt Nam

> **Tài liệu này** chuẩn hóa toàn bộ cấu trúc website (sitemap, màn hình, trường dữ liệu, quy tắc nghiệp vụ) theo **Nghị định số 30/2020/NĐ-CP** ngày 05/3/2020 của Chính phủ về công tác văn thư, **Luật Giao dịch điện tử 2023** (Luật số 20/2023/QH15, hiệu lực từ 01/7/2024), và các văn bản pháp luật liên quan. Đây là **bộ khung đúng luật** để đề tài đủ tư cách triển khai trong cơ quan nhà nước Việt Nam.

---

# 1. Căn cứ pháp lý

## 1.1. Văn bản pháp luật chính áp dụng

| STT | Văn bản | Nội dung áp dụng trong hệ thống |
|---|---|---|
| 1 | **Nghị định 30/2020/NĐ-CP** (05/3/2020) | Công tác văn thư — **cốt lõi**, áp dụng toàn bộ quy trình văn bản đi/đến |
| 2 | **Luật Giao dịch điện tử 2023** (Luật 20/2023/QH15, hiệu lực 01/7/2024) | Chữ ký điện tử, chữ ký số, giá trị pháp lý văn bản điện tử |
| 3 | **Luật Bảo vệ bí mật nhà nước 2018** (Luật 29/2018/QH14) | Quản lý văn bản mật (3 mức: Mật, Tối mật, Tuyệt mật) |
| 4 | **Luật Lưu trữ 2011** (sửa đổi 2024 — Luật 33/2024/QH15) | Lập hồ sơ, nộp lưu, bảo quản, khai thác tài liệu lưu trữ |
| 5 | **Nghị định 130/2018/NĐ-CP** | Chữ ký số công cộng và dịch vụ chứng thực (đang được thay thế bằng NĐ mới theo Luật GDĐT 2023) |
| 6 | **Thông tư 01/2019/TT-BNV** | Quản lý văn bản điện tử trong cơ quan nhà nước |
| 7 | **TCVN 11816** (tiêu chuẩn quốc gia) | Chữ ký số nâng cao PAdES cho văn bản điện tử |

## 1.2. Các Phụ lục quan trọng của NĐ 30/2020 cần tuân thủ

| Phụ lục | Nội dung | Áp dụng vào màn hình |
|---|---|---|
| **Phụ lục I** | Thể thức, kỹ thuật trình bày văn bản hành chính, vị trí chữ ký số | Màn hình soạn thảo, màn hình ký số |
| **Phụ lục II** | Quy tắc viết hoa | Validate nội dung văn bản |
| **Phụ lục III** | Bảng chữ viết tắt tên loại văn bản (29 loại) + mẫu trình bày | Danh mục loại văn bản |
| **Phụ lục IV** | Mẫu Sổ đăng ký văn bản đi, Sổ đăng ký văn bản đến, Phiếu giải quyết văn bản đến | Màn hình sổ công văn, phiếu xử lý |
| **Phụ lục V** | Mẫu Danh mục hồ sơ, Mục lục hồ sơ nộp lưu, Biên bản giao nhận | Màn hình lập hồ sơ, nộp lưu |
| **Phụ lục VI** | **Trường thông tin đầu vào của dữ liệu quản lý văn bản** | **Schema cơ sở dữ liệu — cực kỳ quan trọng** |

---

# 2. Sitemap tổng quan

Cấu trúc website được chia thành **11 nhóm chức năng lớn**, ánh xạ đúng với nhiệm vụ của Văn thư cơ quan theo Điều 6 NĐ 30/2020:

```
HỆ THỐNG QUẢN LÝ CÔNG VĂN ĐI/ĐẾN
│
├── [1] TRANG CHỦ / DASHBOARD
│   ├── Tổng quan công việc
│   ├── Công việc cần xử lý của tôi
│   ├── Thống kê văn bản theo trạng thái
│   ├── Cảnh báo văn bản khẩn, sắp hạn
│   └── Thông báo mới
│
├── [2] QUẢN LÝ VĂN BẢN ĐẾN (theo Điều 20–24 NĐ 30)
│   ├── Tiếp nhận văn bản đến
│   │   ├── Tiếp nhận văn bản giấy (bóc bì, đóng dấu ĐẾN)
│   │   ├── Tiếp nhận văn bản điện tử (nhận qua trục liên thông)
│   │   └── Tiếp nhận văn bản mật (đăng ký riêng)
│   ├── Đăng ký văn bản đến
│   │   ├── Cấp số đến (liên tiếp theo năm, thống nhất giấy + điện tử)
│   │   └── Nhập trường thông tin đầu vào (Phụ lục VI)
│   ├── Số hóa văn bản đến (OCR — Phụ lục I)
│   ├── Trình, chuyển giao văn bản đến
│   │   ├── Trình lãnh đạo cho ý kiến chỉ đạo
│   │   ├── Chuyển đơn vị/cá nhân chủ trì, phối hợp
│   │   └── Phiếu giải quyết văn bản đến (Phụ lục IV)
│   ├── Theo dõi, giải quyết văn bản đến
│   │   ├── Cập nhật trạng thái xử lý
│   │   ├── Ghi ý kiến giải quyết
│   │   └── Báo cáo hoàn thành
│   └── Thu hồi văn bản đến (nếu có thông báo thu hồi từ nơi gửi)
│
├── [3] QUẢN LÝ VĂN BẢN ĐI (theo Điều 14–19 NĐ 30)
│   ├── Soạn thảo văn bản
│   │   ├── Chọn loại văn bản (29 loại — Phụ lục III)
│   │   ├── Chọn mẫu trình bày (theo Phụ lục III)
│   │   ├── Nhập nội dung theo thể thức (Phụ lục I)
│   │   └── Gắn phụ lục kèm theo
│   ├── Duyệt nội dung (cấp phòng → cấp đơn vị)
│   │   ├── Duyệt bởi Trưởng phòng
│   │   ├── Duyệt bởi Lãnh đạo đơn vị
│   │   ├── Trả lại (yêu cầu chỉnh sửa)
│   │   └── Quản lý phiên bản dự thảo
│   ├── Ký văn bản
│   │   ├── Ký số cá nhân (người có thẩm quyền)
│   │   ├── Ký số cơ quan, tổ chức (Văn thư)
│   │   └── Ký văn bản giấy (bút mực xanh)
│   ├── Cấp số, thời gian ban hành (Điều 15 NĐ 30)
│   │   ├── Cấp số tự động theo sổ + năm
│   │   └── Văn bản mật cấp số riêng
│   ├── Đăng ký văn bản đi (Điều 16)
│   ├── Nhân bản, đóng dấu / Ký số cơ quan (Điều 17)
│   ├── Phát hành, chuyển phát văn bản đi (Điều 18)
│   │   ├── Phát hành điện tử (qua trục liên thông)
│   │   ├── Phát hành giấy (in, đóng dấu, gửi bưu điện)
│   │   └── Theo dõi trạng thái chuyển phát
│   ├── Lưu văn bản đi (Điều 19)
│   │   ├── Bản gốc lưu tại Văn thư
│   │   └── Bản chính lưu tại hồ sơ công việc
│   └── Thu hồi văn bản đi (khi có sai sót)
│
├── [4] SỔ ĐĂNG KÝ VĂN BẢN (Phụ lục IV NĐ 30)
│   ├── Sổ đăng ký văn bản đến
│   │   ├── Sổ đăng ký văn bản đến (chung)
│   │   ├── Sổ đăng ký văn bản mật đến (riêng)
│   │   └── In sổ ra giấy để lưu
│   ├── Sổ đăng ký văn bản đi
│   │   ├── Sổ đăng ký văn bản đi (chung)
│   │   ├── Sổ đăng ký văn bản mật đi (riêng)
│   │   └── In sổ ra giấy để lưu
│   ├── Sổ chuyển giao văn bản (cấp 2)
│   └── Quản lý bộ đếm cấp số theo năm
│
├── [5] KÝ SỐ (theo Luật GDĐT 2023 + Điều 13 NĐ 30)
│   ├── Ký số văn bản đi
│   │   ├── Ký số cá nhân (chữ ký số chuyên dùng công vụ)
│   │   └── Ký số cơ quan, tổ chức
│   ├── Xác minh chữ ký số
│   │   ├── Kiểm tra tính hợp lệ
│   │   ├── Kiểm tra tính toàn vẹn (hash file)
│   │   └── Kiểm tra chứng thư số (CRL/OCSP)
│   ├── Quản lý chứng thư số (Certificate)
│   │   ├── Danh sách chứng thư của tôi
│   │   ├── Upload/cài đặt chứng thư
│   │   ├── Thông báo sắp hết hạn
│   │   └── Thu hồi chứng thư
│   └── Ký hàng loạt (batch sign)
│
├── [6] SỐ HÓA & OCR (Phụ lục I NĐ 30)
│   ├── Upload file scan
│   ├── Tiền xử lý ảnh (deskew, denoise)
│   ├── Chạy OCR bóc tách văn bản
│   ├── Xác nhận kết quả OCR
│   │   ├── Chỉnh sửa trường thông tin
│   │   └── Chấp nhận làm dữ liệu chính thức
│   └── OCR hàng loạt (batch OCR)
│
├── [7] LẬP HỒ SƠ & LƯU TRỮ (Chương IV NĐ 30 + Luật Lưu trữ 2024)
│   ├── Danh mục hồ sơ năm (Phụ lục V)
│   │   ├── Lập danh mục đầu năm
│   │   ├── Phê duyệt danh mục
│   │   └── Ban hành danh mục
│   ├── Lập hồ sơ công việc
│   │   ├── Mở hồ sơ
│   │   ├── Thu thập, cập nhật văn bản vào hồ sơ
│   │   ├── Kết thúc hồ sơ
│   │   └── Biên mục hồ sơ
│   ├── Nộp lưu hồ sơ vào Lưu trữ cơ quan
│   │   ├── Mục lục hồ sơ nộp lưu
│   │   └── Biên bản giao nhận
│   ├── Quản lý kho lưu trữ điện tử
│   └── Khai thác, mượn đọc hồ sơ
│
├── [8] TÌM KIẾM & BÁO CÁO
│   ├── Tra cứu văn bản
│   │   ├── Tra cứu theo số, ký hiệu
│   │   ├── Tra cứu theo trích yếu/nội dung
│   │   ├── Tra cứu nâng cao (đa tiêu chí)
│   │   └── Tra cứu full-text (OCR content)
│   ├── Báo cáo thống kê
│   │   ├── Báo cáo văn bản đi/đến theo kỳ
│   │   ├── Báo cáo theo loại văn bản
│   │   ├── Báo cáo theo đơn vị
│   │   ├── Báo cáo tiến độ xử lý
│   │   └── Báo cáo văn bản quá hạn
│   └── Xuất báo cáo (PDF, Excel)
│
├── [9] DANH MỤC HỆ THỐNG (Master Data)
│   ├── Danh mục loại văn bản (29 loại — Phụ lục III)
│   ├── Danh mục lĩnh vực/chuyên ngành
│   ├── Danh mục độ mật (Mật, Tối mật, Tuyệt mật)
│   ├── Danh mục độ khẩn (Khẩn, Thượng khẩn, Hỏa tốc)
│   ├── Danh mục phòng/ban, đơn vị
│   ├── Danh mục cơ quan, tổ chức bên ngoài
│   ├── Danh mục sổ văn bản
│   ├── Danh mục mẫu văn bản
│   ├── Danh mục chức danh
│   └── Danh mục thời hạn bảo quản
│
├── [10] TÀI KHOẢN & PHÂN QUYỀN
│   ├── Quản lý tài khoản người dùng
│   ├── Quản lý vai trò (Role)
│   ├── Ma trận phân quyền
│   ├── Quản lý phiên đăng nhập
│   └── Lịch sử đăng nhập
│
└── [11] QUẢN TRỊ HỆ THỐNG
    ├── Nhật ký hệ thống (audit log — append-only)
    ├── Cấu hình chung
    ├── Cấu hình trục liên thông văn bản
    ├── Cấu hình SMTP (gửi thông báo email)
    ├── Sao lưu & khôi phục dữ liệu
    ├── Quản lý phiên bản phần mềm
    └── Thông tin hệ thống
```

---

# 3. Chi tiết nhóm chức năng [1] — Dashboard

## 3.1. Nội dung màn hình chính (theo vai trò)

### Dashboard Văn thư đơn vị
```
┌─────────────────────────────────────────────────────────────────┐
│  XIN CHÀO [Tên văn thư] — Văn thư đơn vị [Tên đơn vị]           │
│  Hôm nay: Thứ Hai, 25/04/2026                                   │
└─────────────────────────────────────────────────────────────────┘

┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ VB ĐẾN chờ xử lý │ │ VB ĐI chờ ký số  │ │ VB cần ghi sổ    │
│      12          │ │      3           │ │      5           │
│ [Vào xử lý →]    │ │ [Trình ký →]     │ │ [Ghi sổ →]       │
└──────────────────┘ └──────────────────┘ └──────────────────┘

┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ VB khẩn          │ │ VB quá hạn       │ │ VB mật           │
│ 🔴 Hỏa tốc: 1   │ │ ⚠ 3 văn bản      │ │ 🔒 2             │
│ 🟠 Thượng khẩn:2│ │ [Xem danh sách]  │ │ [Xem danh sách]  │
│ 🟡 Khẩn: 4      │ │                  │ │                  │
└──────────────────┘ └──────────────────┘ └──────────────────┘

📊 BIỂU ĐỒ: Số văn bản theo tháng (12 tháng gần nhất)
📊 BIỂU ĐỒ: Tỷ lệ văn bản đúng hạn / quá hạn
📋 BẢNG: 10 văn bản gần đây nhất
```

### Dashboard Lãnh đạo đơn vị
- Số văn bản chờ cho ý kiến chỉ đạo
- Số công văn đi chờ duyệt, chờ ký số
- Thống kê xử lý toàn đơn vị
- Biểu đồ văn bản theo phòng ban
- Danh sách văn bản khẩn cần ý kiến

### Dashboard Chuyên viên
- Văn bản được giao xử lý
- Văn bản tôi đang soạn (dự thảo)
- Deadline sắp đến (3 ngày tới)
- Thông báo từ hệ thống

---

# 4. Chi tiết nhóm chức năng [2] — Quản lý văn bản đến

Theo **Điều 20–24 NĐ 30/2020**, quy trình văn bản đến gồm: **Tiếp nhận → Đăng ký → Trình, chuyển giao → Giải quyết, theo dõi → Lưu**.

## 4.1. Màn hình "Tiếp nhận văn bản đến"

### Trường dữ liệu cần nhập (theo Phụ lục VI NĐ 30/2020)

| Nhóm | Trường | Bắt buộc | Ghi chú |
|---|---|---|---|
| **Nguồn nhập** | Số đến | ✅ Tự động | Liên tiếp theo năm (bắt đầu 01 từ 01/01), **thống nhất giấy + điện tử** |
| | Ngày đến | ✅ | Mặc định: hôm nay |
| | Nguồn tiếp nhận | ✅ | `Bưu điện` / `Trực tiếp` / `Điện tử qua trục liên thông` / `Fax` / `Email` |
| **Từ văn bản gốc** | Tên cơ quan/tổ chức ban hành | ✅ | Từ danh mục `organizations` |
| | Số, ký hiệu văn bản | ✅ | Ví dụ: `123/QĐ-BNV` |
| | Ngày ban hành | ✅ | |
| | Loại văn bản | ✅ | 29 loại theo Phụ lục III (Quyết định, Công văn, Thông báo...) |
| | Lĩnh vực | ⚪ | |
| | Trích yếu nội dung | ✅ | Tối đa 500 ký tự |
| | Ngôn ngữ | ⚪ | Mặc định: tiếng Việt |
| | Số trang | ⚪ | |
| | Số lượng bản | ⚪ | |
| **Phân loại** | Độ mật | ✅ | `Thường` / `Mật` / `Tối mật` / `Tuyệt mật` |
| | Độ khẩn | ✅ | `Thường` / `Khẩn` / `Thượng khẩn` / `Hỏa tốc` |
| **Xử lý** | Hạn giải quyết | ⚪ | |
| | Đơn vị/người nhận | ⚪ | Điền sau khi lãnh đạo có ý kiến |
| | Ý kiến chỉ đạo | ⚪ | Điền sau khi lãnh đạo xử lý |
| **File đính kèm** | File scan/PDF | ⚪ | Bắt buộc nếu là văn bản giấy đã số hóa |
| | File gốc điện tử | ⚪ | Nếu là văn bản điện tử |
| | File chữ ký số đi kèm | ⚪ | Nếu là văn bản điện tử đã ký số |

### Luồng thao tác (Workflow)

```
[1] Tiếp nhận vật lý (giấy) hoặc điện tử
       ↓
[2] Kiểm tra bì (giấy) / xác minh chữ ký số (điện tử)
       ↓
[3] Bóc bì (nếu giấy + không mật gửi đích danh)
       → Đóng dấu "ĐẾN" vật lý
       → Số hóa (scan)
       ↓
[4] Gọi OCR bóc tách tự động
       ↓
[5] Văn thư nhập/xác nhận trường thông tin đầu vào
       ↓
[6] Hệ thống cấp số đến tự động (transaction + FOR UPDATE)
       ↓
[7] Lưu vào Sổ đăng ký văn bản đến (Phụ lục IV)
       ↓
[8] In Phiếu giải quyết văn bản đến (nếu cần bản giấy)
       ↓
[9] Trình lãnh đạo cho ý kiến chỉ đạo
       ↓
[10] Chuyển giao đơn vị/cá nhân xử lý
       ↓
[11] Theo dõi giải quyết, cập nhật trạng thái
       ↓
[12] Kết thúc → Lưu hồ sơ công việc
```

## 4.2. Quy tắc pháp lý bắt buộc

> **Điều 20 NĐ 30/2020:** Tất cả văn bản giấy đến (bao gồm cả văn bản có dấu chỉ độ mật) gửi cơ quan, tổ chức thuộc diện đăng ký tại Văn thư cơ quan phải được bóc bì, đóng dấu "ĐẾN". Đối với văn bản gửi đích danh cá nhân hoặc tổ chức đoàn thể trong cơ quan, tổ chức thì Văn thư cơ quan chuyển cho nơi nhận (không bóc bì).

> **Điều 22 NĐ 30/2020:** Số đến của văn bản được lấy liên tiếp theo thứ tự và trình tự thời gian tiếp nhận văn bản trong năm, thống nhất giữa văn bản giấy và văn bản điện tử.

> **Điều 23 NĐ 30/2020:** Văn bản phải được Văn thư cơ quan trình trong ngày, chậm nhất là trong ngày làm việc tiếp theo đến người có thẩm quyền chỉ đạo giải quyết.

> **Văn bản khẩn:** "Hỏa tốc", "Thượng khẩn" và "Khẩn" phải được đăng ký, trình và chuyển giao **NGAY** sau khi nhận được.

> **Văn bản mật:** Đăng ký theo quy định của pháp luật về bảo vệ bí mật nhà nước (Luật 29/2018/QH14). Cấp hệ thống số riêng.

## 4.3. Màn hình "Phiếu giải quyết văn bản đến"

Theo **mẫu Phụ lục IV NĐ 30/2020**:

```
┌─────────────────────────────────────────────────────────────┐
│  [TÊN CQ CHỦ QUẢN]                                          │
│  [TÊN ĐƠN VỊ]                                               │
│                                                             │
│           PHIẾU GIẢI QUYẾT VĂN BẢN ĐẾN                      │
│                                                             │
│  Số đến:  ______  Ngày đến: __/__/____                      │
│  Văn bản số, ký hiệu: _______________________               │
│  Ngày ban hành: __/__/____                                  │
│  Của: _______________________________________               │
│  Trích yếu: _________________________________               │
│  _____________________________________________              │
│                                                             │
│  Ý kiến chỉ đạo của lãnh đạo:                               │
│  _____________________________________________              │
│  _____________________________________________              │
│                                                             │
│  Chuyển:     □ Đơn vị chủ trì: ______________               │
│              □ Đơn vị phối hợp: _____________               │
│              □ Cá nhân: _____________________               │
│  Thời hạn giải quyết: __/__/____                            │
│                                                             │
│  Người chỉ đạo                    Ngày ký: __/__/____       │
│  (Ký tên)                                                   │
└─────────────────────────────────────────────────────────────┘
```

Trên website: hiển thị dưới dạng form, có nút "In phiếu giấy" và "Ký số điện tử".

---

# 5. Chi tiết nhóm chức năng [3] — Quản lý văn bản đi

Theo **Điều 14–19 NĐ 30/2020**, trình tự quản lý văn bản đi gồm 5 bước:
1. Cấp số, thời gian ban hành
2. Đăng ký văn bản đi
3. Nhân bản, đóng dấu (giấy) / Ký số cơ quan (điện tử)
4. Phát hành và theo dõi việc chuyển phát
5. Lưu văn bản đi

## 5.1. Màn hình "Soạn thảo văn bản đi"

### Phần 1 — Thông tin chung

| Trường | Bắt buộc | Ghi chú |
|---|---|---|
| Loại văn bản | ✅ | Chọn trong 29 loại (Phụ lục III): Quyết định, Thông báo, Công văn, Công điện, Báo cáo, Tờ trình, Giấy mời, Kế hoạch, Biên bản, Hợp đồng, Công văn, … |
| Số văn bản | ⚪ Auto | Cấp khi trình ký thành công |
| Ngày ban hành | ⚪ Auto | Cấp cùng với số |
| Cơ quan ban hành | ✅ Auto | Chính là đơn vị của người tạo |
| Độ mật | ✅ | `Thường` / `Mật` / `Tối mật` / `Tuyệt mật` |
| Độ khẩn | ✅ | `Thường` / `Khẩn` / `Thượng khẩn` / `Hỏa tốc` |
| Lĩnh vực | ⚪ | Từ danh mục lĩnh vực |
| Phương thức phát hành | ✅ | `Điện tử (ký số)` / `Giấy (đóng dấu)` / `Song song` |

### Phần 2 — Nội dung văn bản (theo thể thức Phụ lục I)

Thể thức văn bản hành chính bắt buộc có 9 thành phần chính theo Phụ lục I NĐ 30/2020:

1. **Quốc hiệu và Tiêu ngữ**
   ```
   CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
   Độc lập - Tự do - Hạnh phúc
   ```

2. **Tên cơ quan, tổ chức ban hành văn bản**
   - Cơ quan chủ quản trực tiếp: chữ in hoa, cỡ 12–13, đứng
   - Tên cơ quan ban hành: chữ in hoa, cỡ 12–13, đứng, **đậm**, canh giữa

3. **Số, ký hiệu văn bản**
   - Ví dụ: `123/QĐ-BNV`, `45/CV-UBND`
   - Format: `[Số]/[Viết tắt loại VB]-[Viết tắt cơ quan]`

4. **Địa danh và thời gian ban hành**
   - Ví dụ: `Hà Nội, ngày 25 tháng 4 năm 2026`

5. **Tên loại và trích yếu nội dung**
   - Tên loại: chữ in hoa, cỡ 13–14, đậm
   - Trích yếu: chữ thường, cỡ 13–14, đậm

6. **Nội dung văn bản**
   - Chữ thường, cỡ 13–14, font Times New Roman

7. **Chức vụ, họ tên và chữ ký của người có thẩm quyền**

8. **Dấu, chữ ký số của cơ quan, tổ chức**

9. **Nơi nhận**
   - Danh sách đơn vị/cá nhân nhận văn bản
   - Có đánh số thứ tự: `- Như Điều…; (1)`, `- Lưu: VT, [đơn vị]; (2)`

**Website phải cung cấp:**
- Trình soạn thảo (rich text editor) với **template có sẵn các thành phần trên**
- Validator tự động check đủ 9 thành phần
- Preview dưới dạng PDF trước khi trình ký

### Phần 3 — Nơi nhận (Recipients)

Mỗi văn bản có thể có **nhiều nơi nhận**, chia thành 2 loại:
- **Để thực hiện**: nơi nhận chính để triển khai
- **Để biết** / **Để báo cáo**: nơi nhận tham khảo

| Trường | Bắt buộc | Ghi chú |
|---|---|---|
| Loại nơi nhận | ✅ | `Tổ chức bên ngoài` / `Phòng/Ban nội bộ` / `Cá nhân` / `Lưu Văn thư` |
| Tên nơi nhận | ✅ | Từ danh mục (hoặc nhập tự do nếu không có trong DM) |
| Mục đích | ✅ | `Để thực hiện` / `Để báo cáo` / `Để biết` / `Lưu` |
| Số lượng bản | ✅ | Với văn bản giấy |
| Phương thức gửi | ✅ | `Điện tử` / `Giấy` / `Fax` / `Email` |
| Ghi chú | ⚪ | |

## 5.2. Quản lý phiên bản dự thảo (Versioning)

Theo Điều 9 NĐ 30, văn bản đi phải được soạn thảo và **người đứng đầu cơ quan, tổ chức quyết định việc ký**. Trong quá trình soạn thảo có thể bị trả lại để chỉnh sửa nhiều lần. Hệ thống cần:

- Mỗi lần lưu = tạo `document_version` mới
- Trạng thái version: `WORKING` (đang soạn) → `SUBMITTED` (trình duyệt) → `APPROVED` (đã duyệt) → `SUPERSEDED` (bị thay bằng version sau)
- Hiển thị **lịch sử thay đổi** với "diff" giữa các version
- Khi lãnh đạo duyệt version nào thì **khóa version đó**, chỉ đúng version đã duyệt mới được ký số/phát hành

## 5.3. Màn hình "Trình ký & Phê duyệt"

### Luồng phê duyệt mặc định (Điều 13 NĐ 30/2020)

```
Chuyên viên soạn thảo
        ↓
   [Trình duyệt]
        ↓
Trưởng phòng/ban
   ↓             ↓
[Duyệt]    [Trả lại]
   ↓             ↓
Lãnh đạo    Về dự thảo
 đơn vị     (version mới)
   ↓             ↓
[Ký số]    [Trả lại]
   ↓
Đã ký → Cấp số → Phát hành
```

**Lưu ý pháp lý quan trọng (Điều 13):**
- Người đứng đầu cơ quan, tổ chức có thẩm quyền ký tất cả văn bản.
- Có thể **giao cấp phó ký thay** các văn bản thuộc lĩnh vực được phân công.
- Có thể **giao đơn vị thuộc cơ quan ký thừa lệnh** một số loại văn bản (phải quy định trong quy chế làm việc).
- **Người ký chịu trách nhiệm trước pháp luật** về văn bản do mình ký.

## 5.4. Màn hình "Cấp số và phát hành"

### Quy tắc cấp số (Điều 15 NĐ 30/2020) — BẮT BUỘC

1. **Số bắt đầu từ 01 ngày 01 tháng 01** và kết thúc vào **31 tháng 12** hàng năm.
2. Số được lấy **theo thứ tự và trình tự thời gian ban hành** của cơ quan trong năm.
3. **Văn bản mật được cấp hệ thống số RIÊNG** (không chung với văn bản thường).
4. Cấp số sau khi văn bản có chữ ký của người có thẩm quyền, chậm nhất là trong ngày làm việc tiếp theo.
5. Với văn bản điện tử, cấp số thực hiện bằng chức năng của Hệ thống (tự động).

### Implementation: cấp số trong transaction

```
Khi người dùng bấm "Cấp số và Phát hành":
  BEGIN TRANSACTION;
    -- 1. Lock dòng counter để tránh race condition
    SELECT * FROM document_book_counters
    WHERE book_id = ? AND year = ?
    FOR UPDATE;

    -- 2. Lấy next_number
    next_num = counter.next_number;

    -- 3. Kiểm tra unique (để chắc chắn)
    -- Kiểm tra không tồn tại entry_number này trong cùng book + year

    -- 4. Insert entry mới
    INSERT INTO document_book_entries
      (document_book_id, entry_year, entry_number, document_id,
       entry_status, entry_date, ...)
    VALUES (?, ?, next_num, ?, 'OFFICIAL', NOW(), ...);

    -- 5. Tăng counter
    UPDATE document_book_counters
    SET next_number = next_num + 1
    WHERE book_id = ? AND year = ?;

    -- 6. Cập nhật documents
    UPDATE documents
    SET code_number = ?, issue_date = NOW(),
        current_status = 'DA_PHAT_HANH'
    WHERE id = ?;

    -- 7. Ghi audit log
    INSERT INTO audit_logs ...;
  COMMIT;
```

---

# 6. Chi tiết nhóm chức năng [4] — Sổ đăng ký văn bản

## 6.1. Mẫu Sổ đăng ký văn bản đến (Phụ lục IV NĐ 30/2020)

```
┌────┬──────┬────────┬────────────┬────────────┬──────────┬─────────┬───────┐
│ Số │ Ngày │ Số, KH │ Ngày tháng │ Tên loại   │ Nơi ban  │ Người   │ Ghi   │
│ đến│ đến  │văn bản │   VB       │ và trích   │ hành VB  │ nhận    │ chú   │
│    │      │        │            │ yếu nd     │          │         │       │
├────┼──────┼────────┼────────────┼────────────┼──────────┼─────────┼───────┤
│ 1  │25/4/ │123/QĐ- │20/4/2026   │ QUYẾT ĐỊNH │ Bộ Nội   │ Phòng   │       │
│    │2026  │BNV     │            │ V/v ban    │ vụ       │ HCTH    │       │
│    │      │        │            │ hành quy   │          │         │       │
│    │      │        │            │ chế làm    │          │         │       │
│    │      │        │            │ việc       │          │         │       │
├────┼──────┼────────┼────────────┼────────────┼──────────┼─────────┼───────┤
│ 2  │ ...  │  ...   │    ...     │    ...     │   ...    │  ...    │       │
└────┴──────┴────────┴────────────┴────────────┴──────────┴─────────┴───────┘
```

**Trên website:** hiển thị dạng bảng tương tự, có các tính năng:
- Lọc theo năm, theo sổ, theo độ mật
- Tìm kiếm inline
- **Nút "In sổ"** → xuất PDF đúng mẫu Phụ lục IV (bắt buộc theo NĐ 30: "Văn bản đến được đăng ký vào Hệ thống phải được in ra giấy đầy đủ các trường thông tin theo mẫu Sổ đăng ký văn bản đến, ký nhận và đóng sổ để quản lý")
- Nút "Xuất Excel"
- Bộ đếm cuối sổ (Tổng số văn bản trong năm: ___)

## 6.2. Mẫu Sổ đăng ký văn bản đi (Phụ lục IV NĐ 30/2020)

```
┌──────┬────────┬────────────┬──────────┬─────────────┬──────────┬──────────┬────────┬──────┐
│ Số,  │ Ngày   │ Tên loại   │ Người ký │ Nơi nhận VB │ Đơn vị,  │ Số       │ Phương │ Ghi  │
│ KH   │ VB     │ và trích   │          │             │ người    │ lượng    │ thức   │ chú  │
│ VB   │        │ yếu nd VB  │          │             │ nhận bản │ bản      │ gửi    │      │
│      │        │            │          │             │ lưu      │          │        │      │
├──────┼────────┼────────────┼──────────┼─────────────┼──────────┼──────────┼────────┼──────┤
│45/CV │25/4/   │CÔNG VĂN    │Nguyễn    │UBND các     │Phòng     │10        │Giấy +  │      │
│-UBND │2026    │V/v triển   │Văn A     │huyện        │HCTH      │          │điện tử │      │
│      │        │khai KH     │Chủ tịch  │             │          │          │        │      │
│      │        │chuyển đổi  │          │             │          │          │        │      │
│      │        │số          │          │             │          │          │        │      │
└──────┴────────┴────────────┴──────────┴─────────────┴──────────┴──────────┴────────┴──────┘
```

## 6.3. Sổ riêng cho văn bản mật

Theo Luật 29/2018 và Điều 22 NĐ 30:
- Văn bản mật/tối mật/tuyệt mật có **sổ đăng ký riêng**
- Chỉ người có thẩm quyền mới xem/thao tác được
- Giao diện riêng, URL riêng, yêu cầu xác thực 2 yếu tố
- Log chi tiết mỗi lần truy cập

---

# 7. Chi tiết nhóm chức năng [5] — Ký số

## 7.1. Phân loại chữ ký số theo Luật GDĐT 2023

Điều 22 Luật Giao dịch điện tử 2023 (hiệu lực 01/7/2024) phân loại **3 loại chữ ký điện tử**:

| Loại | Mô tả | Áp dụng trong hệ thống |
|---|---|---|
| **Chữ ký điện tử chuyên dùng** | Do cơ quan, tổ chức tự tạo lập để sử dụng nội bộ | Dùng nội bộ đơn vị (ví dụ: ký xác nhận đọc, ký nhận văn bản) |
| **Chữ ký số công cộng** | Do tổ chức cung cấp dịch vụ chứng thực công cộng cấp (VNPT-CA, Viettel-CA, FPT-CA, NewCA…) | Dùng cho doanh nghiệp, cá nhân giao dịch với cơ quan nhà nước |
| **Chữ ký số chuyên dùng công vụ** | Do **Ban Cơ yếu Chính phủ** cấp cho cơ quan nhà nước, cán bộ công chức | **Bắt buộc dùng cho văn bản công vụ của cơ quan nhà nước** |

> **Quan trọng:** OTP, SMS, chữ ký scan, sinh trắc học **KHÔNG phải chữ ký điện tử** theo Luật GDĐT 2023 (Điều 22). Không được dùng các hình thức này để "ký" văn bản công vụ.

## 7.2. Ký số theo Điều 13 và Phụ lục I NĐ 30/2020

### Trên văn bản điện tử phải có 2 chữ ký số:

**1. Chữ ký số của người có thẩm quyền ký văn bản**
- Vị trí: cuối văn bản, bên phải, đúng chỗ chữ ký thường
- Hình ảnh chữ ký số: hiển thị hình đại diện (không bắt buộc là chữ ký tay)
- Thông tin: Họ tên, chức vụ, thời gian ký, cơ quan cấp chứng thư

**2. Chữ ký số của cơ quan, tổ chức**
- Vị trí: bên trái chữ ký người có thẩm quyền (vị trí dấu)
- Hình ảnh: dấu tròn đỏ của cơ quan
- Thông tin: Tên cơ quan, thời gian ký, số seri chứng thư

### Với Phụ lục kèm theo văn bản:
- **Phụ lục cùng tệp tin** với văn bản chính → chỉ ký số văn bản chính, **không ký số trên phụ lục**
- **Phụ lục khác tệp tin** → Văn thư cơ quan thực hiện ký số trên **từng tệp tin** phụ lục
  - Vị trí: **góc trên, bên phải, trang đầu** của văn bản kèm theo
  - Hình ảnh chữ ký số của cơ quan: không hiển thị
  - Thông tin: số và ký hiệu văn bản; thời gian ký (ngày tháng năm; giờ phút giây; múi giờ +07:00)

## 7.3. Màn hình "Ký số văn bản đi"

```
┌─────────────────────────────────────────────────────────────────┐
│  KÝ SỐ VĂN BẢN ĐI                                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Văn bản: Công văn V/v triển khai kế hoạch chuyển đổi số        │
│  Số hiệu: (chưa cấp — cấp sau khi ký)                           │
│  Version đã duyệt: v3 — phê duyệt bởi [Lãnh đạo X] lúc 10:30    │
│                                                                 │
│  [PDF Preview của văn bản]                                      │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │                                                         │    │
│  │  [Nội dung văn bản]                                     │    │
│  │                                                         │    │
│  │                     TM. UBND HUYỆN X                    │    │
│  │      [Vị trí            CHỦ TỊCH                        │    │
│  │       chữ ký số  ]                                      │    │
│  │      [cơ quan    ]     [Vị trí chữ ký số người ký]      │    │
│  │                                                         │    │
│  │                        Nguyễn Văn A                     │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Chứng thư số đang sử dụng:                                     │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  Chủ thể: CN=Nguyễn Văn A, O=UBND Huyện X               │    │
│  │  Nhà cấp: Ban Cơ yếu Chính phủ - Root CA                │    │
│  │  Hiệu lực: 01/01/2025 — 31/12/2027 (Còn 610 ngày)       │    │
│  │  Loại: Chữ ký số chuyên dùng công vụ ✅                 │    │
│  │  Trạng thái: Còn hiệu lực ✅                            │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Phương thức ký:                                                │
│    ◉ USB Token (khuyến nghị)                                    │
│    ◯ Remote Signing (VNPT MySign / Viettel SmartCA)             │
│    ◯ HSM cơ quan                                                │
│                                                                 │
│  [Nhập mã PIN USB Token: ______]                                │
│                                                                 │
│  [❌ Hủy]                         [✅ Ký số]                    │
└─────────────────────────────────────────────────────────────────┘
```

## 7.4. Quản lý chứng thư số

```
┌─────────────────────────────────────────────────────────────────┐
│  QUẢN LÝ CHỨNG THƯ SỐ                                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Chứng thư số của tôi:                              [+ Thêm]    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 🟢 CT-2025-001234 (Đang sử dụng)                         │    │
│  │    Loại: Chữ ký số chuyên dùng công vụ                   │    │
│  │    Nhà cung cấp: Ban Cơ yếu Chính phủ                    │    │
│  │    Hiệu lực: 01/01/2025 → 31/12/2027                     │    │
│  │    Serial: 3F:2A:8B:...                                  │    │
│  │    [Xem chi tiết] [Tạm dừng] [Thu hồi]                   │    │
│  └─────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ 🟡 CT-2024-009876 (Sắp hết hạn — còn 15 ngày)            │    │
│  │    [Gia hạn] [Xem chi tiết]                              │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

## 7.5. Xác minh chữ ký số (văn bản đến điện tử)

Khi tiếp nhận văn bản đến điện tử, hệ thống phải tự động:
1. **Kiểm tra tính hợp lệ** của chữ ký số (signature valid)
2. **Kiểm tra tính toàn vẹn** của file (hash khớp)
3. **Kiểm tra chứng thư số** (còn hạn, chưa bị thu hồi — CRL/OCSP)
4. **Kiểm tra chuỗi chứng thư** về đến Root CA (Ban Cơ yếu Chính phủ / Root CA quốc gia)
5. Hiển thị kết quả chi tiết (người ký, thời gian ký, trạng thái)

```
┌─────────────────────────────────────────────────────────────────┐
│  KẾT QUẢ XÁC MINH CHỮ KÝ SỐ                                     │
├─────────────────────────────────────────────────────────────────┤
│  File: CV_123_BNV.pdf                                           │
│                                                                 │
│  ✅ Chữ ký số hợp lệ                                            │
│  ✅ Tính toàn vẹn: File chưa bị thay đổi sau khi ký             │
│  ✅ Chứng thư số còn hiệu lực                                   │
│  ✅ Chuỗi tin cậy: Valid → Root CA Ban Cơ yếu Chính phủ         │
│                                                                 │
│  Thông tin chữ ký:                                              │
│  • Người ký: Nguyễn Văn X (CN=Nguyen Van X, O=Bo Noi vu)        │
│  • Chức vụ: Thứ trưởng Bộ Nội vụ                                │
│  • Thời gian ký: 20/04/2026 14:30:45 (GMT+07:00)                │
│  • Thuật toán: SHA-256 with RSA                                 │
│  • Serial chứng thư: 3F:2A:8B:...                               │
└─────────────────────────────────────────────────────────────────┘
```

---

# 8. Chi tiết nhóm chức năng [6] — Số hóa & OCR

## 8.1. Quy định số hóa theo Phụ lục I NĐ 30/2020

Phụ lục I quy định về **thể thức, kỹ thuật trình bày văn bản hành chính và bản sao văn bản**. Với văn bản giấy được số hóa, bản scan phải đáp ứng:

| Yêu cầu | Chuẩn |
|---|---|
| **Định dạng** | PDF/A (chuẩn lưu trữ dài hạn, ISO 19005) |
| **Độ phân giải** | Tối thiểu **200 DPI** (khuyến nghị 300 DPI) |
| **Màu sắc** | Có màu (nếu văn bản gốc có màu), không được đen trắng nếu bản gốc có đóng dấu đỏ |
| **Nén** | Không nén mất dữ liệu |
| **Chữ ký số văn thư** | Ký số xác nhận đây là bản sao từ bản gốc giấy (Điều 25 NĐ 30) |

## 8.2. Màn hình "Số hóa văn bản"

```
┌─────────────────────────────────────────────────────────────────┐
│  SỐ HÓA VĂN BẢN ĐẾN (Theo Phụ lục I NĐ 30/2020/NĐ-CP)           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Drop zone — kéo thả file PDF/ảnh scan]                        │
│                                                                 │
│  Hoặc [Chọn file từ máy tính]                                   │
│                                                                 │
│  Yêu cầu:                                                       │
│  • Định dạng: PDF, JPG, PNG, TIFF                               │
│  • Độ phân giải: ≥ 200 DPI                                      │
│  • Kích thước tối đa: 50 MB                                     │
│                                                                 │
│  Sau khi upload:                                                │
│  ☐ Tự động chạy OCR bóc tách nội dung                           │
│  ☐ Tự động bóc tách trường: số ký hiệu, ngày, trích yếu         │
│  ☐ Ký số của Văn thư xác nhận bản sao                           │
└─────────────────────────────────────────────────────────────────┘
```

## 8.3. Màn hình "Xác nhận kết quả OCR"

```
┌─────────────────────────────────────────────────────────────────────┐
│  XÁC NHẬN KẾT QUẢ OCR                                               │
├─────────────────────────────────────────────────────────────────────┤
│  [ Bản scan PDF ]              [ Trường bóc tách (chỉnh sửa được) ] │
│  ┌─────────────────┐           ┌──────────────────────────────────┐ │
│  │                 │           │ Số, ký hiệu: [123/QĐ-BNV       ] │ │
│  │  [PDF viewer]   │           │ Confidence: 95% ✅               │ │
│  │                 │           │                                  │ │
│  │                 │           │ Ngày ban hành: [20/04/2026     ] │ │
│  │                 │           │ Confidence: 92% ✅               │ │
│  │                 │           │                                  │ │
│  │                 │           │ Cơ quan ban hành:                │ │
│  │                 │           │ [Bộ Nội vụ                     ] │ │
│  │                 │           │ Confidence: 88% ✅               │ │
│  │                 │           │                                  │ │
│  │                 │           │ Loại văn bản: [Quyết định      ] │ │
│  │                 │           │ Confidence: 90% ✅               │ │
│  │                 │           │                                  │ │
│  │                 │           │ Trích yếu:                       │ │
│  │                 │           │ [V/v ban hành quy chế làm việc]  │ │
│  │                 │           │ Confidence: 72% ⚠  (kiểm tra)    │ │
│  │                 │           │                                  │ │
│  │                 │           │ Độ mật: [Thường            ▼]    │ │
│  │                 │           │ Độ khẩn: [Khẩn             ▼]    │ │
│  └─────────────────┘           └──────────────────────────────────┘ │
│                                                                     │
│  Văn bản text bóc tách đầy đủ (full text):                          │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │ [Toàn bộ text bóc tách được, cho phép sửa]                    │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                     │
│  [ ← Chạy lại OCR] [❌ Hủy]  [💾 Lưu nháp]  [✅ Xác nhận & Lưu]     │
└─────────────────────────────────────────────────────────────────────┘
```

**Quy tắc:** Kết quả OCR chỉ trở thành **dữ liệu chính thức** sau khi Văn thư nhấn "Xác nhận & Lưu" (`is_accepted = true`). Trước đó, chỉ hiển thị gợi ý.

---

# 9. Chi tiết nhóm chức năng [7] — Lập hồ sơ & Lưu trữ

Theo **Chương IV NĐ 30/2020** (Điều 28–31) và **Luật Lưu trữ 2024**.

## 9.1. Danh mục hồ sơ năm

Theo Điều 28 NĐ 30, Danh mục hồ sơ do **người đứng đầu cơ quan, tổ chức phê duyệt**, ban hành vào **đầu năm** và gửi các đơn vị, cá nhân liên quan.

### Màn hình "Danh mục hồ sơ"

| Cột | Nội dung |
|---|---|
| Số, ký hiệu hồ sơ | Ví dụ: `01-HS/VP/2026` |
| Tiêu đề hồ sơ | Ví dụ: "Hồ sơ công văn đi, đến năm 2026 của Văn phòng" |
| Thời hạn bảo quản | `Vĩnh viễn` / `70 năm` / `50 năm` / `20 năm` / `10 năm` / `5 năm` |
| Đơn vị/người lập hồ sơ | |
| Ghi chú | |

## 9.2. Quy trình lập hồ sơ công việc (Điều 29)

```
[1] Mở hồ sơ
    → Cá nhân được giao nhiệm vụ có trách nhiệm mở hồ sơ
    → Cập nhật thông tin ban đầu theo Danh mục hồ sơ đã ban hành
        ↓
[2] Thu thập, cập nhật văn bản, tài liệu vào hồ sơ
    → Các văn bản trong 1 hồ sơ phải có liên quan chặt chẽ
    → Phản ánh đúng trình tự diễn biến của sự việc
        ↓
[3] Kết thúc và biên mục hồ sơ
    → Khi công việc kết thúc
    → Biên mục: số, ký hiệu, tiêu đề, ngày tháng, số trang
        ↓
[4] Nộp lưu vào Lưu trữ cơ quan (Điều 30)
    → Trong thời hạn 01 năm kể từ ngày công việc kết thúc
    → Lập 02 bản "Mục lục hồ sơ nộp lưu" và 02 bản "Biên bản giao nhận"
```

## 9.3. Màn hình "Nộp lưu hồ sơ điện tử"

Theo Điều 30 NĐ 30: "Cá nhân được giao nhiệm vụ giải quyết công việc và lập hồ sơ thực hiện **nộp lưu hồ sơ điện tử vào Lưu trữ cơ quan trên Hệ thống**".

```
┌─────────────────────────────────────────────────────────────────┐
│  NỘP LƯU HỒ SƠ VÀO LƯU TRỮ CƠ QUAN                              │
├─────────────────────────────────────────────────────────────────┤
│  Người nộp: [Nguyễn Văn A — Phòng HCTH]                         │
│  Kỳ nộp lưu: [Năm 2025]                                         │
│                                                                 │
│  Danh sách hồ sơ nộp lưu:                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ ☑ 01-HS/VP/2025: Hồ sơ công văn đi, đến năm 2025        │    │
│  │   • Số VB trong hồ sơ: 245                              │    │
│  │   • Thời hạn bảo quản: Vĩnh viễn                        │    │
│  │   • Số trang: 1,280                                     │    │
│  │   • Kích thước: 185 MB                                  │    │
│  │                                                         │    │
│  │ ☑ 02-HS/VP/2025: Hồ sơ các cuộc họp giao ban năm 2025   │    │
│  │   • ...                                                 │    │
│  └─────────────────────────────────────────────────────────┘    │
│                                                                 │
│  Kiểm tra trước khi nộp:                                        │
│  ✅ Đã kết thúc hồ sơ                                           │
│  ✅ Đã biên mục đầy đủ                                          │
│  ✅ Chữ ký số hợp lệ trên tất cả văn bản                        │
│  ✅ Metadata đầy đủ theo Phụ lục VI                             │
│                                                                 │
│  [❌ Hủy]   [In Mục lục & Biên bản]   [✅ Nộp lưu điện tử]      │
└─────────────────────────────────────────────────────────────────┘
```

## 9.4. Thời hạn bảo quản (theo Luật Lưu trữ)

Hệ thống phải cho phép gán thời hạn bảo quản cho mỗi hồ sơ theo 5 mức:

| Mức | Thời hạn | Ví dụ văn bản |
|---|---|---|
| Vĩnh viễn | Bảo quản vô thời hạn | Quyết định thành lập, quy hoạch dài hạn |
| 70 năm | 70 năm | Hồ sơ cán bộ công chức |
| 50 năm | 50 năm | Hồ sơ đất đai, công trình trọng điểm |
| 20 năm | 20 năm | Hồ sơ dự án thường |
| 10 năm | 10 năm | Báo cáo định kỳ |
| 5 năm | 5 năm | Công văn hành chính thông thường |

---

# 10. Chi tiết nhóm chức năng [8] — Tìm kiếm & Báo cáo

## 10.1. Tra cứu văn bản — các bộ lọc bắt buộc

```
┌─────────────────────────────────────────────────────────────────┐
│  TRA CỨU VĂN BẢN                                                │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Tìm kiếm nhanh: [🔍 Nhập từ khóa...                        ]   │
│                                                                 │
│  ─ Bộ lọc nâng cao ─                                            │
│                                                                 │
│  Loại văn bản:                                                  │
│    ◯ Tất cả  ◉ Văn bản đi  ◯ Văn bản đến                        │
│                                                                 │
│  Số, ký hiệu:          [________________]                       │
│  Trích yếu:            [________________]                       │
│  Nội dung (full-text): [________________] (tìm trong OCR)       │
│                                                                 │
│  Loại văn bản: [Tất cả             ▼]                           │
│  Lĩnh vực:    [Tất cả             ▼]                            │
│                                                                 │
│  Cơ quan ban hành: [________________]                           │
│  Nơi nhận:         [________________]                           │
│  Người ký:         [________________]                           │
│                                                                 │
│  Từ ngày: [__/__/____]  Đến ngày: [__/__/____]                  │
│                                                                 │
│  Độ mật:  ☐ Thường ☐ Mật ☐ Tối mật ☐ Tuyệt mật                  │
│  Độ khẩn: ☐ Thường ☐ Khẩn ☐ Thượng khẩn ☐ Hỏa tốc               │
│                                                                 │
│  Trạng thái xử lý: [Tất cả                 ▼]                   │
│                                                                 │
│  [🔄 Xóa lọc]                              [🔍 Tìm kiếm]        │
└─────────────────────────────────────────────────────────────────┘
```

## 10.2. Báo cáo định kỳ bắt buộc

Theo NĐ 30/2020 và các quy định của Cục Văn thư và Lưu trữ Nhà nước:

| Báo cáo | Tần suất | Nội dung |
|---|---|---|
| **Báo cáo văn bản đi/đến** | Tháng/Quý/Năm | Số lượng, phân loại theo độ mật, khẩn |
| **Báo cáo tình hình xử lý** | Tuần/Tháng | Đúng hạn, quá hạn, chưa xử lý |
| **Báo cáo văn bản mật** | Riêng, có phân quyền | Chỉ người có thẩm quyền xem |
| **Báo cáo ký số** | Tháng | Số văn bản ký số, tỷ lệ thành công |
| **Báo cáo lập hồ sơ** | Năm | Danh mục hồ sơ, tình hình nộp lưu |

## 10.3. Mẫu báo cáo xuất ra

Các báo cáo phải xuất ra được **PDF** theo đúng thể thức hành chính (Phụ lục I) với:
- Quốc hiệu, tiêu ngữ
- Tên cơ quan, tổ chức
- Số, ký hiệu báo cáo
- Ngày tháng
- Tiêu đề báo cáo
- Nội dung với bảng biểu, biểu đồ
- Người lập, người duyệt, chữ ký số

---

# 11. Chi tiết nhóm chức năng [9] — Danh mục hệ thống

## 11.1. Danh mục loại văn bản — 29 loại theo Phụ lục III NĐ 30/2020

| STT | Tên loại | Viết tắt |
|---|---|---|
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
| 28 | Thư công | — |
| 29 | Công văn | — |

**Hệ thống phải:** cho phép thêm/sửa/xóa loại văn bản, nhưng 29 loại mặc định không được xóa (chỉ ẩn).

## 11.2. Danh mục độ mật & độ khẩn

### Độ mật (theo Luật 29/2018/QH14)
| Mức | Mô tả | Dấu |
|---|---|---|
| Thường | Không mật | — |
| Mật | Độ mật cấp 3 | MẬT (chữ đỏ) |
| Tối mật | Độ mật cấp 2 | TỐI MẬT (chữ đỏ, khung kín) |
| Tuyệt mật | Độ mật cấp 1 (cao nhất) | TUYỆT MẬT (chữ đỏ, khung kín đậm) |

### Độ khẩn (theo NĐ 30/2020)
| Mức | Mô tả | Dấu |
|---|---|---|
| Thường | Không khẩn | — |
| Khẩn | Cần xử lý nhanh | KHẨN (chữ đỏ) |
| Thượng khẩn | Xử lý rất nhanh | THƯỢNG KHẨN (chữ đỏ) |
| Hỏa tốc | Xử lý ngay | HỎA TỐC (chữ đỏ, khung) |

Quy tắc: Văn bản độ khẩn "Hỏa tốc", "Thượng khẩn", "Khẩn" **phải được đăng ký, trình và chuyển giao NGAY sau khi nhận được** (Điều 6 NĐ 30).

---

# 12. Chi tiết nhóm chức năng [10] — Tài khoản & Phân quyền

## 12.1. Vai trò người dùng (Role)

| Vai trò | Mô tả theo NĐ 30 | Quyền chính |
|---|---|---|
| **Quản trị hệ thống** | Không được định nghĩa trong NĐ 30, là vai trò kỹ thuật | Quản lý toàn hệ thống |
| **Văn thư cơ quan (Văn thư đơn vị)** | Điều 6 khoản 3 NĐ 30 | Tiếp nhận, đăng ký văn bản đi/đến, quản lý sổ |
| **Văn thư phòng/ban** | Theo quy chế riêng của đơn vị | Tiếp nhận nội bộ, ghi sổ cấp 2 |
| **Lãnh đạo cơ quan (Người đứng đầu)** | Điều 6 khoản 1 NĐ 30 | Ký ban hành văn bản, phê duyệt |
| **Cấp phó lãnh đạo** | Điều 13 khoản 3 NĐ 30 | Ký thay cấp trưởng trong lĩnh vực phụ trách |
| **Trưởng phòng/ban (cấp đơn vị thuộc)** | Có thể ký thừa lệnh (Điều 13 khoản 4) | Duyệt cấp phòng, ký thừa lệnh |
| **Chuyên viên** | Cá nhân được giao công việc (Điều 6 khoản 4) | Soạn thảo dự thảo, xử lý công văn được giao |
| **Cán bộ Lưu trữ** | Theo Luật Lưu trữ | Tiếp nhận nộp lưu, quản lý kho |

## 12.2. Ma trận phân quyền chi tiết

| Chức năng | Quản trị | Văn thư cq | Văn thư p/b | Lãnh đạo | Cấp phó | Trưởng phòng | Chuyên viên | Lưu trữ |
|---|:-:|:-:|:-:|:-:|:-:|:-:|:-:|:-:|
| **Xem tất cả VB của đơn vị** | ✅ | ✅ | ❌ | ✅ | ✅ | ⚪ | ❌ | ✅ |
| **Xem VB của phòng/ban** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ⚪ | ✅ |
| **Xem VB được phân công** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Tiếp nhận VB đến | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Đăng ký VB đến/đi (cấp số) | ❌ | ✅ | ⚪ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Soạn thảo VB đi | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| Duyệt VB đi cấp phòng | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ |
| Duyệt VB đi cấp cơ quan | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| Ký số cá nhân | ❌ | ❌ | ❌ | ✅ | ✅ | ⚪ | ❌ | ❌ |
| Ký số cơ quan | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Phát hành VB đi | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Thu hồi VB | ❌ | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **Xem VB MẬT** | ⚪ (hạn chế) | ⚪ | ❌ | ✅ | ⚪ | ❌ | ❌ | ⚪ |
| **Xem VB TỐI MẬT** | ❌ | ⚪ | ❌ | ✅ | ⚪ | ❌ | ❌ | ⚪ |
| **Xem VB TUYỆT MẬT** | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Lập hồ sơ công việc | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| Nộp lưu hồ sơ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| Tiếp nhận nộp lưu | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Quản lý danh mục | ✅ | ⚪ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Quản lý tài khoản | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Xem audit log | ✅ | ❌ | ❌ | ⚪ | ❌ | ❌ | ❌ | ❌ |

> ✅ = Có quyền, ❌ = Không có quyền, ⚪ = Có điều kiện (theo cấu hình, phạm vi)

---

# 13. Quy tắc nghiệp vụ bắt buộc (Business Rules)

Các quy tắc dưới đây **bắt buộc** được implement để hệ thống đúng luật Việt Nam. Mỗi quy tắc kèm **căn cứ pháp lý** để khi bảo vệ có thể trích dẫn.

## BR-01: Cấp số thống nhất theo năm
> **Căn cứ:** Điều 15 & Điều 22 NĐ 30/2020
- Số VB đi bắt đầu từ 01 vào ngày 01/01, kết thúc 31/12 hàng năm, reset đầu năm mới.
- Số VB đến lấy liên tiếp trong năm, **thống nhất giữa VB giấy và VB điện tử**.
- Phải unique trong phạm vi (sổ, năm).

## BR-02: Cấp số phải dùng transaction
> **Căn cứ:** Tính toàn vẹn dữ liệu, tránh race condition
- `SELECT ... FOR UPDATE` trên bảng counter.
- Nếu 2 người cùng cấp số 1 lúc, chỉ 1 người thành công, người kia được số kế tiếp.

## BR-03: Văn bản mật có sổ riêng
> **Căn cứ:** Điều 22 NĐ 30/2020 + Luật 29/2018/QH14
- Sổ đăng ký văn bản mật tách biệt với sổ thường.
- Số cấp riêng, không chung.
- Chỉ người có thẩm quyền mới truy cập được (quyền `VIEW_SECRET_DOC` trở lên).

## BR-04: Văn bản khẩn xử lý ngay
> **Căn cứ:** Điều 6 khoản 2 NĐ 30/2020
- Khi tiếp nhận văn bản có độ khẩn `Khẩn`/`Thượng khẩn`/`Hỏa tốc`:
  - Hiển thị nổi bật (màu đỏ, biểu tượng chuông)
  - Gửi notification lập tức cho Văn thư + Lãnh đạo
  - Ưu tiên trong hàng đợi OCR

## BR-05: Trong ngày phải đăng ký
> **Căn cứ:** Điều 5 điểm c & Điều 6 NĐ 30/2020
- Văn bản đến nhận ngày nào phải đăng ký trong ngày đó, chậm nhất ngày làm việc tiếp theo.
- Hệ thống cảnh báo nếu văn bản đã nhận quá 24h mà chưa đăng ký.
- Báo cáo "Văn bản tồn đọng" phải có.

## BR-06: Văn bản điện tử phải có đủ 2 chữ ký số
> **Căn cứ:** Điều 5 NĐ 30/2020 + Luật GDĐT 2023
- Văn bản điện tử để có giá trị pháp lý bằng bản gốc giấy phải có:
  1. Chữ ký số của **người có thẩm quyền**
  2. Chữ ký số của **cơ quan, tổ chức**
- Hệ thống reject nếu thiếu 1 trong 2.

## BR-07: Version lock sau phê duyệt
> **Căn cứ:** Nguyên tắc toàn vẹn văn bản
- Khi lãnh đạo phê duyệt version nào → khóa version đó (`APPROVED`).
- File ký số phải là file thuộc version đã duyệt.
- Hash file được chốt tại thời điểm phê duyệt.
- Nếu sau đó sửa nội dung → phải tạo version mới và quay lại quy trình duyệt.

## BR-08: In sổ ra giấy
> **Căn cứ:** Điều 16 & Điều 22 NĐ 30/2020
- Sổ đăng ký trên Hệ thống phải in ra giấy được, đủ các trường theo mẫu Phụ lục IV.
- Văn thư ký nhận và đóng sổ bản giấy để quản lý song song.

## BR-09: Lưu bản gốc điện tử
> **Căn cứ:** Điều 19 NĐ 30/2020
- Bản gốc văn bản điện tử phải được lưu trên Hệ thống của cơ quan ban hành.
- Không được xóa, chỉ soft delete với audit log.

## BR-10: Audit log không được sửa, không được xóa
> **Căn cứ:** Nguyên tắc lưu trữ, minh bạch
- Mọi thao tác quan trọng (tạo, sửa, ký, duyệt, phát hành, thu hồi) đều ghi audit log.
- Bảng `audit_logs` là **append-only**.
- Có trường `ip_address`, `user_id`, `timestamp`, `old_value`, `new_value`.

## BR-11: Thu hồi văn bản
> **Căn cứ:** Điều 18 NĐ 30/2020
- Văn bản giấy: thông báo thu hồi, bên nhận phải gửi lại.
- Văn bản điện tử: thông báo thu hồi, **bên nhận hủy bỏ trên Hệ thống**, thông báo lại qua hệ thống.
- Không xóa dữ liệu — chỉ đánh dấu `status = RECALLED`.

## BR-12: Chữ ký số chuyên dùng công vụ cho văn bản công vụ
> **Căn cứ:** Luật GDĐT 2023 (Điều 22, Điều 24, Điều 50)
- Văn bản công vụ của cơ quan nhà nước phải ký bằng **chữ ký số chuyên dùng công vụ** (do Ban Cơ yếu Chính phủ cấp).
- Không được dùng chữ ký số công cộng (VNPT-CA, Viettel-CA…) cho văn bản công vụ.
- Không được dùng OTP, SMS, chữ ký scan (không phải chữ ký điện tử theo luật).

## BR-13: Thời hạn bảo quản không được giảm
> **Căn cứ:** Luật Lưu trữ
- Hồ sơ đã gán thời hạn bảo quản không được giảm thời hạn (chỉ được tăng).
- Chỉ có thể tiêu hủy sau khi hết thời hạn và được Hội đồng xác định giá trị tài liệu phê duyệt.

## BR-14: Hồ sơ điện tử phải đầy đủ metadata
> **Căn cứ:** Điều 30 NĐ 30/2020 + Phụ lục VI
- Hồ sơ nộp lưu phải có đầy đủ metadata theo Phụ lục VI.
- Lưu trữ cơ quan kiểm tra, nhận hồ sơ theo Danh mục, liên kết chính xác dữ liệu đặc tả với hồ sơ.

---

# 14. Wireframe tổng thể các trang chính

## 14.1. Layout chung

```
┌───────────────────────────────────────────────────────────────────┐
│  [Logo] HỆ THỐNG QLCV   🔔(5)  👤 Nguyễn Văn A ▼  [Đăng xuất]    │
├──────────────┬────────────────────────────────────────────────────┤
│              │                                                    │
│  MENU        │   BREADCRUMB: Trang chủ > Văn bản đến > Tiếp nhận  │
│              │                                                    │
│  🏠 Trang chủ│   ┌──────────────────────────────────────────────┐ │
│  📥 VB đến  │   │                                              │ │
│     ├─Tiếp  │   │                                              │ │
│     │ nhận  │   │       NỘI DUNG CHÍNH                         │ │
│     ├─Đăng  │   │                                              │ │
│     │ ký    │   │                                              │ │
│     ├─Xử lý │   │                                              │ │
│     └─...   │   │                                              │ │
│  📤 VB đi   │   │                                              │ │
│  📒 Sổ CV   │   │                                              │ │
│  ✍ Ký số   │   │                                              │ │
│  📁 Hồ sơ   │   │                                              │ │
│  🔍 Tìm kiếm│   │                                              │ │
│  📊 Báo cáo │   │                                              │ │
│  ⚙ Danh mục│   │                                              │ │
│  👥 Người dg │   │                                              │ │
│              │   └──────────────────────────────────────────────┘ │
├──────────────┴────────────────────────────────────────────────────┤
│  © 2026 Đơn vị X — v1.0.0 — [Hỗ trợ] [Hướng dẫn sử dụng]          │
└───────────────────────────────────────────────────────────────────┘
```

## 14.2. Màn hình danh sách (Data table)

```
┌───────────────────────────────────────────────────────────────────┐
│  DANH SÁCH VĂN BẢN ĐẾN                              [+ Thêm mới]  │
├───────────────────────────────────────────────────────────────────┤
│  🔍 [Tìm kiếm nhanh...]  [🔽 Bộ lọc]  [📊 Xuất Excel] [🖨 In sổ]  │
├───────────────────────────────────────────────────────────────────┤
│  ☐ Số đến│ Ngày│ Số, KH │ Cơ quan │ Trích yếu │Mật│Khẩn│Trạng thái│
├───────────────────────────────────────────────────────────────────┤
│  ☐  125  │25/4 │123/QĐ- │Bộ Nội vụ│QĐ ban hành│   │Khẩn│Chờ xử lý │
│          │     │BNV     │         │quy chế... │   │    │          │
│  ☐  124  │24/4 │45/CV-  │UBND HN  │CV triển   │   │    │Đang xử lý│
│          │     │UBND    │         │khai...    │   │    │          │
│  ☐  123* │24/4 │**HIDDEN│(Mật)    │[Chỉ xem   │🔒 │    │Chờ trình │
│          │     │**      │         │metadata]  │Mật│    │          │
├───────────────────────────────────────────────────────────────────┤
│  Hiển thị 1-20 / 450   [« Đầu] [‹ Trước] 1 2 3 4 [Sau ›] [Cuối »] │
└───────────────────────────────────────────────────────────────────┘
```

---

# 15. Checklist tuân thủ pháp lý (Compliance Checklist)

Trước khi bảo vệ hoặc nghiệm thu, kiểm tra:

## Tuân thủ NĐ 30/2020/NĐ-CP

- [ ] Cấp số văn bản đi từ 01/01 hàng năm, reset đầu năm
- [ ] Cấp số liên tiếp, thống nhất giữa giấy và điện tử (với VB đến)
- [ ] Văn bản mật có sổ riêng, số riêng
- [ ] Có đầy đủ trường thông tin đầu vào theo **Phụ lục VI**
- [ ] Sổ đăng ký đúng mẫu **Phụ lục IV** và in ra giấy được
- [ ] Phiếu giải quyết văn bản đến đúng mẫu **Phụ lục IV**
- [ ] Danh mục hồ sơ đúng mẫu **Phụ lục V**
- [ ] Có đủ 29 loại văn bản hành chính trong danh mục (**Phụ lục III**)
- [ ] Thể thức văn bản đi tuân thủ **Phụ lục I** (9 thành phần)
- [ ] Văn bản điện tử có đủ 2 chữ ký số (cá nhân + cơ quan)
- [ ] Vị trí chữ ký số trên văn bản đúng Phụ lục I
- [ ] Với phụ lục khác tệp tin: có ký số riêng đúng quy định
- [ ] Thông tin chỉ dẫn kèm theo trên phụ lục đúng (số, ký hiệu, ngày tháng)
- [ ] Văn bản khẩn xử lý ngay, có cảnh báo nổi bật
- [ ] Lịch sử xử lý đầy đủ (workflow_steps append-only)
- [ ] Có chức năng thu hồi văn bản điện tử
- [ ] Lập hồ sơ công việc, nộp lưu điện tử theo Chương IV

## Tuân thủ Luật Giao dịch điện tử 2023

- [ ] Hỗ trợ chữ ký số chuyên dùng công vụ (Ban Cơ yếu Chính phủ)
- [ ] Không dùng OTP/SMS/chữ ký scan làm "chữ ký điện tử"
- [ ] Xác minh chữ ký số đủ 4 bước: valid signature, file integrity, cert validity, cert chain
- [ ] Hỗ trợ kiểm tra CRL/OCSP cho thu hồi chứng thư
- [ ] Có dấu thời gian (timestamp) trên chữ ký để chống chối bỏ
- [ ] Giá trị pháp lý văn bản điện tử đã ký = văn bản giấy gốc

## Tuân thủ Luật Bảo vệ bí mật nhà nước 2018

- [ ] Phân loại đúng 3 mức: Mật, Tối mật, Tuyệt mật
- [ ] Phân quyền chặt chẽ theo độ mật
- [ ] Audit log chi tiết mỗi lần truy cập VB mật
- [ ] Mã hóa file mật (AES-256)
- [ ] Sổ riêng cho VB mật
- [ ] Không mở bì VB mật gửi đích danh

## Tuân thủ Luật Lưu trữ

- [ ] Gán thời hạn bảo quản cho hồ sơ (6 mức)
- [ ] Mục lục hồ sơ nộp lưu đúng mẫu
- [ ] Biên bản giao nhận đúng mẫu
- [ ] Kiểm soát khai thác tài liệu lưu trữ

## Tuân thủ chung về bảo mật

- [ ] HTTPS toàn hệ thống
- [ ] Mã hóa file mật tại rest
- [ ] Audit log append-only
- [ ] Backup định kỳ
- [ ] Phân quyền RBAC + Resource-based
- [ ] Xác thực 2 yếu tố cho lãnh đạo và xem VB tối mật/tuyệt mật

---

# 16. Lộ trình triển khai các màn hình theo thứ tự ưu tiên

## Phase 1 — Nền móng (Tuần 1–4)
1. Đăng nhập / Đăng xuất
2. Dashboard
3. Danh mục hệ thống (đơn vị, loại văn bản, độ mật, độ khẩn, …)
4. Quản lý tài khoản & phân quyền

## Phase 2 — Văn bản đến (Tuần 5–8)
5. Tiếp nhận văn bản đến
6. Đăng ký văn bản đến + cấp số
7. Số hóa & OCR
8. Sổ đăng ký văn bản đến + in sổ

## Phase 3 — Văn bản đi (Tuần 9–12)
9. Soạn thảo văn bản đi (với template)
10. Quản lý phiên bản dự thảo
11. Trình ký & phê duyệt
12. Cấp số + Phát hành
13. Sổ đăng ký văn bản đi + in sổ

## Phase 4 — Ký số (Tuần 13–14)
14. Quản lý chứng thư số
15. Ký số văn bản đi (USB Token)
16. Xác minh chữ ký số văn bản đến

## Phase 5 — Xử lý luồng (Tuần 15–16)
17. Trình chuyển văn bản đến
18. Phiếu giải quyết
19. Theo dõi xử lý
20. Thu hồi văn bản

## Phase 6 — Tìm kiếm & Báo cáo (Tuần 17–18)
21. Tìm kiếm nâng cao (metadata + full-text OCR)
22. Báo cáo thống kê
23. Xuất Excel/PDF

## Phase 7 — Hồ sơ & Lưu trữ (Tuần 19–20)
24. Danh mục hồ sơ năm
25. Lập hồ sơ công việc
26. Nộp lưu hồ sơ điện tử

---

# 17. Đặc biệt quan trọng khi bảo vệ

## 17.1. Câu hỏi "Hệ thống có đúng luật Việt Nam không?"

**Trả lời mẫu:**
> "Hệ thống được thiết kế tuân thủ đầy đủ các văn bản pháp luật hiện hành của Việt Nam:
> - **Nghị định 30/2020/NĐ-CP** về công tác văn thư — áp dụng cho toàn bộ quy trình văn bản đi/đến, thể thức trình bày, sổ đăng ký, lập hồ sơ.
> - **Luật Giao dịch điện tử 2023** — quy định về chữ ký số chuyên dùng công vụ, giá trị pháp lý văn bản điện tử.
> - **Luật Bảo vệ bí mật nhà nước 2018** — phân loại và bảo vệ văn bản mật.
> - **Luật Lưu trữ (sửa đổi 2024)** — lập hồ sơ, nộp lưu, thời hạn bảo quản.
>
> Các Phụ lục I, III, IV, V, VI của NĐ 30/2020 được chúng em implement trực tiếp thành:
> - Phụ lục I → module soạn thảo với template đúng thể thức 9 thành phần
> - Phụ lục III → danh mục 29 loại văn bản hành chính
> - Phụ lục IV → mẫu Sổ đăng ký văn bản đi/đến và Phiếu giải quyết
> - Phụ lục V → mẫu Danh mục hồ sơ và Mục lục nộp lưu
> - Phụ lục VI → schema CSDL cho trường thông tin đầu vào"

## 17.2. Điểm mạnh để nhấn mạnh

1. **Hệ thống KHÔNG chỉ là demo công nghệ** — là một ứng dụng có thể đưa vào dùng thực trong cơ quan nhà nước vì tuân thủ đầy đủ các quy định pháp luật.

2. **OCR không chỉ để bóc chữ** — mà là để hỗ trợ Văn thư điền nhanh **các trường thông tin đầu vào theo Phụ lục VI**, giảm thời gian nhập liệu.

3. **Ký số đúng chuẩn PAdES + TCVN** — tương thích với hạ tầng chữ ký số của Ban Cơ yếu Chính phủ, có thể verify được bởi các hệ thống khác.

4. **Có đủ chức năng in sổ ra giấy** — tôn trọng nghiệp vụ hiện hành, không ép người dùng chuyển 100% sang điện tử.

5. **Phân quyền 3 tầng** (RBAC + Resource + Độ mật) — đáp ứng yêu cầu bảo vệ bí mật nhà nước.

## 17.3. Rủi ro và giới hạn (phải chủ động nói ra)

- Hệ thống **thử nghiệm**, chưa tích hợp thật với **Trục liên thông văn bản quốc gia** (trục VDXP của Bộ TT&TT) — cần có API kết nối khi triển khai thực tế.
- Chữ ký số trong demo dùng **self-signed certificate** — khi triển khai thật phải dùng chứng thư do Ban Cơ yếu Chính phủ cấp.
- OCR chính xác phụ thuộc chất lượng bản scan — **không claim 100%**, luôn có bước xác nhận của Văn thư.
- Chưa tích hợp với **hệ thống định danh và xác thực điện tử quốc gia (VNeID)** — có thể là future work.

---

# 18. Kết luận

Khung website này **100% tuân thủ pháp luật Việt Nam hiện hành** về công tác văn thư và giao dịch điện tử. Nhóm có thể yên tâm triển khai và bảo vệ trước hội đồng:

- ✅ **11 nhóm chức năng** phủ đủ nhiệm vụ Văn thư cơ quan theo Điều 6 NĐ 30/2020
- ✅ **29 loại văn bản hành chính** theo Phụ lục III
- ✅ **Trường thông tin đầu vào** theo Phụ lục VI
- ✅ **Mẫu sổ và phiếu** theo Phụ lục IV, V
- ✅ **Chữ ký số** đúng Luật GDĐT 2023
- ✅ **Phân loại độ mật, độ khẩn** đúng chuẩn
- ✅ **Lập hồ sơ và lưu trữ** theo Chương IV NĐ 30 + Luật Lưu trữ
- ✅ **Ma trận phân quyền** theo vai trò công tác văn thư
- ✅ **14 business rules** có căn cứ pháp lý cụ thể

Tài liệu này kết hợp với:
- `architecture_congvan_full_defense.md` (kiến trúc & nghiệp vụ)
- `tech_stack_congvan_system.md` (công nghệ & triển khai)

→ Tạo thành **bộ 3 tài liệu hoàn chỉnh** cho dự án NCKH sinh viên cấp trường, đủ để bảo vệ thuyết phục và có khả năng triển khai thực tế.

---

# 19. Tài liệu tham chiếu pháp lý

1. **Nghị định số 30/2020/NĐ-CP** ngày 05/3/2020 của Chính phủ về công tác văn thư
2. **Luật số 20/2023/QH15** — Luật Giao dịch điện tử (hiệu lực 01/7/2024)
3. **Luật số 29/2018/QH14** — Luật Bảo vệ bí mật nhà nước
4. **Luật số 01/2011/QH13** (sửa đổi 2024 — Luật 33/2024/QH15) — Luật Lưu trữ
5. **Nghị định số 130/2018/NĐ-CP** — về chữ ký số và dịch vụ chứng thực chữ ký số
6. **Thông tư số 01/2019/TT-BNV** — về quản lý văn bản điện tử
7. **Quyết định số 28/2018/QĐ-TTg** — về gửi, nhận văn bản điện tử giữa các cơ quan trong hệ thống hành chính nhà nước
8. **TCVN 11816** — Tiêu chuẩn quốc gia về chữ ký số nâng cao PAdES
9. **Cổng Công báo Chính phủ:** https://vanban.chinhphu.vn
10. **Cục Văn thư và Lưu trữ Nhà nước:** https://luutru.gov.vn
11. **Trung tâm Chứng thực điện tử quốc gia (NEAC):** https://neac.gov.vn

---

> **Lưu ý cuối:** Văn bản pháp luật có thể thay đổi. Trước khi triển khai thật, cần rà soát lại các văn bản hướng dẫn mới nhất (đặc biệt là các Nghị định hướng dẫn Luật GDĐT 2023 đang được Chính phủ ban hành). Tài liệu này phản ánh trạng thái pháp luật tại thời điểm viết (04/2026).
