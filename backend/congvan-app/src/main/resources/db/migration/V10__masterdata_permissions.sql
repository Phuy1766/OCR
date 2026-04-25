-- =========================================================================
-- V10 — Thêm permission MASTERDATA:* và gán cho các role phù hợp.
--   - MASTERDATA:READ   → tất cả role đã xác thực (qua USER:VIEW_SELF không đủ)
--   - MASTERDATA:MANAGE → ADMIN + VAN_THU_CQ (văn thư cơ quan quản lý sổ, danh mục)
-- =========================================================================

INSERT INTO permissions (code, name, resource, action, description) VALUES
    ('MASTERDATA:READ',    'Xem danh mục master data',
     'MASTERDATA', 'READ',    'Xem 29 loại VB, độ mật/khẩn, sổ CV.'),
    ('MASTERDATA:MANAGE',  'Quản lý danh mục master data',
     'MASTERDATA', 'MANAGE',  'Tạo/sửa/khóa document_books; không được sửa seed.');

-- ADMIN đã có tất cả permissions qua mapping CROSS JOIN ở V5 → tự động.
-- Nhưng V5 INSERT xong rồi không tự cập nhật. Bổ sung thủ công:
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
  AND p.code IN ('MASTERDATA:READ', 'MASTERDATA:MANAGE')
ON CONFLICT DO NOTHING;

-- VAN_THU_CQ cần cả MANAGE (tạo sổ, khóa sổ) và READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'VAN_THU_CQ'
  AND p.code IN ('MASTERDATA:READ', 'MASTERDATA:MANAGE')
ON CONFLICT DO NOTHING;

-- Các role còn lại chỉ READ (để nhìn được dropdown khi soạn VB)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code IN ('VAN_THU_PB','LANH_DAO','CAP_PHO','TRUONG_PHONG','CHUYEN_VIEN','LUU_TRU')
  AND p.code = 'MASTERDATA:READ'
ON CONFLICT DO NOTHING;
