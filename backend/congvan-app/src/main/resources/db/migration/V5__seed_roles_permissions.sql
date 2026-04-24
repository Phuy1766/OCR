-- =========================================================================
-- V5 — Seed 8 role theo legal §12 + permission baseline.
-- Sẽ mở rộng permissions ở các phase sau khi có module cụ thể.
-- =========================================================================

-- 8 role (is_system = TRUE → không cho xóa)
INSERT INTO roles (code, name, description, is_system, display_order) VALUES
    ('ADMIN',          'Quản trị hệ thống',             'Toàn quyền cấu hình hệ thống, danh mục, người dùng.', TRUE, 10),
    ('VAN_THU_CQ',     'Văn thư cơ quan',               'Tiếp nhận, đăng ký, cấp số, phát hành, ký cơ quan.',  TRUE, 20),
    ('VAN_THU_PB',     'Văn thư phòng/ban',             'Tiếp nhận & đăng ký trong phạm vi phòng/ban.',        TRUE, 30),
    ('LANH_DAO',       'Lãnh đạo đơn vị',               'Duyệt, ký số cá nhân với thẩm quyền cao nhất.',       TRUE, 40),
    ('CAP_PHO',        'Cấp phó',                       'Duyệt, ký số cá nhân theo ủy quyền.',                 TRUE, 50),
    ('TRUONG_PHONG',   'Trưởng phòng',                  'Duyệt cấp phòng, phân công chuyên viên.',             TRUE, 60),
    ('CHUYEN_VIEN',    'Chuyên viên',                   'Soạn thảo, xử lý công văn được giao.',                TRUE, 70),
    ('LUU_TRU',        'Lưu trữ',                       'Xem, lập hồ sơ, quản lý thời hạn bảo quản.',          TRUE, 80);

-- Permission baseline (phase 1 đủ dùng; các phase sau bổ sung).
INSERT INTO permissions (code, name, resource, action, description) VALUES
    -- USER
    ('USER:READ',       'Xem người dùng',         'USER', 'READ',    'Xem danh sách, chi tiết user trong phạm vi.'),
    ('USER:MANAGE',     'Quản lý người dùng',     'USER', 'MANAGE',  'Tạo, sửa, khóa, gán role — chỉ ADMIN.'),
    ('USER:VIEW_SELF',  'Xem hồ sơ bản thân',     'USER', 'VIEW_SELF','Xem /users/me.'),
    -- DEPARTMENT / ORGANIZATION
    ('ORG:READ',        'Xem cơ cấu tổ chức',     'ORG',  'READ',    'Xem organizations, departments.'),
    ('ORG:MANAGE',      'Quản lý cơ cấu',         'ORG',  'MANAGE',  'CRUD organizations, departments — ADMIN.'),
    -- DOCUMENT (placeholder, phase 3-4 mở rộng)
    ('DOC:READ_OWN',    'Xem VB của mình',        'DOC',  'READ',    'Xem VB mình tạo/được giao.'),
    ('DOC:READ_DEPT',   'Xem VB phòng ban',       'DOC',  'READ',    'Xem toàn bộ VB trong phòng ban.'),
    ('DOC:READ_ALL',    'Xem toàn bộ VB đơn vị',  'DOC',  'READ',    'Phạm vi toàn cơ quan.'),
    ('DOC:CREATE',      'Soạn thảo VB',           'DOC',  'CREATE',  'Tạo dự thảo VB đi.'),
    ('DOC:APPROVE',     'Duyệt VB',               'DOC',  'APPROVE', 'Phê duyệt dự thảo.'),
    ('DOC:SIGN',        'Ký số cá nhân',          'DOC',  'SIGN',    'Ký số với certificate cá nhân.'),
    ('DOC:ISSUE',       'Phát hành / cấp số',     'DOC',  'ISSUE',   'Cấp số, ký số cơ quan, phát hành.'),
    -- AUDIT
    ('AUDIT:READ',      'Xem nhật ký hệ thống',   'AUDIT','READ',    'Xem audit_logs — ADMIN.');

-- Role → Permission mapping (legal §12 ma trận phân quyền)
-- ADMIN: tất cả
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- VAN_THU_CQ: nhận/đăng ký VB, cấp số, ký cơ quan, phát hành, xem toàn đơn vị
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VAN_THU_CQ'
  AND p.code IN ('USER:VIEW_SELF','ORG:READ',
                 'DOC:READ_ALL','DOC:CREATE','DOC:ISSUE');

-- VAN_THU_PB: nhận/đăng ký VB trong phòng ban, xem phòng ban
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VAN_THU_PB'
  AND p.code IN ('USER:VIEW_SELF','ORG:READ',
                 'DOC:READ_DEPT','DOC:CREATE');

-- LANH_DAO: duyệt, ký số cá nhân, xem toàn đơn vị
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'LANH_DAO'
  AND p.code IN ('USER:VIEW_SELF','USER:READ','ORG:READ',
                 'DOC:READ_ALL','DOC:APPROVE','DOC:SIGN');

-- CAP_PHO: như LANH_DAO
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CAP_PHO'
  AND p.code IN ('USER:VIEW_SELF','USER:READ','ORG:READ',
                 'DOC:READ_ALL','DOC:APPROVE','DOC:SIGN');

-- TRUONG_PHONG: duyệt cấp phòng, xem phòng ban
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'TRUONG_PHONG'
  AND p.code IN ('USER:VIEW_SELF','USER:READ','ORG:READ',
                 'DOC:READ_DEPT','DOC:CREATE','DOC:APPROVE');

-- CHUYEN_VIEN: soạn thảo, xem công văn của mình
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CHUYEN_VIEN'
  AND p.code IN ('USER:VIEW_SELF','ORG:READ',
                 'DOC:READ_OWN','DOC:CREATE');

-- LUU_TRU: xem toàn đơn vị
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'LUU_TRU'
  AND p.code IN ('USER:VIEW_SELF','ORG:READ',
                 'DOC:READ_ALL');
