-- =========================================================================
-- V12 — Permissions cho công văn đến (Phase 3) + bổ sung gán role.
-- Thay thế các permission DOC:* tạm ở V5 bằng INBOUND:* cụ thể hơn.
-- =========================================================================

-- Thêm permission INBOUND:*
INSERT INTO permissions (code, name, resource, action, description) VALUES
    ('INBOUND:CREATE',    'Tiếp nhận công văn đến',
     'INBOUND', 'CREATE',  'Đăng ký mới VB đến vào sổ.'),
    ('INBOUND:UPDATE',    'Sửa thông tin VB đến',
     'INBOUND', 'UPDATE',  'Cập nhật metadata VB đến chưa hoàn tất.'),
    ('INBOUND:READ_OWN',  'Xem VB đến được giao',
     'INBOUND', 'READ',    'Chỉ xem VB mình tạo/được giao.'),
    ('INBOUND:READ_DEPT', 'Xem VB đến trong phòng/ban',
     'INBOUND', 'READ',    'Xem toàn bộ VB đến của phòng/ban.'),
    ('INBOUND:READ_ALL',  'Xem toàn bộ VB đến đơn vị',
     'INBOUND', 'READ',    'Xem mọi VB đến trong cơ quan.'),
    ('INBOUND:RECALL',    'Thu hồi VB đến',
     'INBOUND', 'RECALL',  'Đánh dấu VB đến là RECALLED (BR-11).');

-- Map vào role:
-- ADMIN: tất cả
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'INBOUND:%'
ON CONFLICT DO NOTHING;

-- VAN_THU_CQ: tạo + sửa + recall + xem all
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VAN_THU_CQ'
  AND p.code IN ('INBOUND:CREATE','INBOUND:UPDATE','INBOUND:RECALL','INBOUND:READ_ALL')
ON CONFLICT DO NOTHING;

-- VAN_THU_PB: tạo + sửa + xem trong phòng ban
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VAN_THU_PB'
  AND p.code IN ('INBOUND:CREATE','INBOUND:UPDATE','INBOUND:READ_DEPT')
ON CONFLICT DO NOTHING;

-- LANH_DAO + CAP_PHO: xem all + recall
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('LANH_DAO','CAP_PHO')
  AND p.code IN ('INBOUND:READ_ALL','INBOUND:RECALL')
ON CONFLICT DO NOTHING;

-- TRUONG_PHONG: xem phòng ban + recall (trong phạm vi)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'TRUONG_PHONG'
  AND p.code IN ('INBOUND:READ_DEPT','INBOUND:RECALL')
ON CONFLICT DO NOTHING;

-- CHUYEN_VIEN: chỉ xem của mình
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CHUYEN_VIEN' AND p.code = 'INBOUND:READ_OWN'
ON CONFLICT DO NOTHING;

-- LUU_TRU: xem all (lưu trữ cần xem mọi VB)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'LUU_TRU' AND p.code = 'INBOUND:READ_ALL'
ON CONFLICT DO NOTHING;
