-- =========================================================================
-- V9 — Seed 4 mức mật (Luật BVBMNN 2018) + 4 mức khẩn (NĐ 30/2020).
-- SLA hours cho priority: đề xuất (không phải quy định pháp lý cứng).
-- =========================================================================

-- ================= confidentiality_levels =================
INSERT INTO confidentiality_levels (code, name, level, color, description, display_order) VALUES
    ('BINH_THUONG', 'Bình thường', 0, '#6b7280',
     'Văn bản không mật — phạm vi sử dụng thông thường.', 10),
    ('MAT',         'Mật',          1, '#2563eb',
     'Văn bản mật cấp độ 1 — phạm vi người được phép sử dụng.', 20),
    ('TOI_MAT',     'Tối mật',      2, '#dc2626',
     'Văn bản tối mật cấp độ 2 — chỉ người có thẩm quyền cao được phép.', 30),
    ('TUYET_MAT',   'Tuyệt mật',    3, '#7c2d12',
     'Văn bản tuyệt mật cấp độ 3 — chỉ lãnh đạo đứng đầu được phép.', 40);

-- ================= priority_levels =================
-- sla_hours: đề xuất nội bộ — bình thường không giới hạn, khẩn 48h, thượng khẩn 24h, hỏa tốc 6h.
INSERT INTO priority_levels (code, name, level, color, sla_hours, description, display_order) VALUES
    ('BINH_THUONG',  'Bình thường',   0, '#6b7280',  NULL,
     'Xử lý theo quy trình thông thường.', 10),
    ('KHAN',         'Khẩn',          1, '#eab308',  48,
     'Ưu tiên xử lý trong 48 giờ.', 20),
    ('THUONG_KHAN',  'Thượng khẩn',   2, '#f97316',  24,
     'Ưu tiên cao — xử lý trong 24 giờ.', 30),
    ('HOA_TOC',      'Hỏa tốc',       3, '#dc2626',  6,
     'Xử lý ngay — tối đa 6 giờ. BR-04 ưu tiên notification + OCR.', 40);
