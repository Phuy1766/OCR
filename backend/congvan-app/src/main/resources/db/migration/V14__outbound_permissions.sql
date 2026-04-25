-- =========================================================================
-- V14 — Permissions cho công văn đi.
-- Phân quyền theo legal §12: chuyên viên soạn, trưởng phòng duyệt cấp phòng,
-- lãnh đạo duyệt cấp đơn vị, văn thư cơ quan cấp số + phát hành.
-- =========================================================================

INSERT INTO permissions (code, name, resource, action, description) VALUES
    ('OUTBOUND:CREATE_DRAFT',  'Soạn dự thảo VB đi',
     'OUTBOUND', 'CREATE',  'Tạo dự thảo công văn đi.'),
    ('OUTBOUND:UPDATE',        'Sửa dự thảo VB đi',
     'OUTBOUND', 'UPDATE',  'Sửa dự thảo (tạo version mới).'),
    ('OUTBOUND:SUBMIT',        'Gửi dự thảo lên duyệt',
     'OUTBOUND', 'SUBMIT',  'Đẩy DRAFT sang PENDING_DEPT_APPROVAL.'),
    ('OUTBOUND:APPROVE_DEPT',  'Duyệt cấp phòng',
     'OUTBOUND', 'APPROVE', 'Trưởng phòng duyệt VB của phòng mình.'),
    ('OUTBOUND:APPROVE_LEADER','Duyệt cấp đơn vị',
     'OUTBOUND', 'APPROVE', 'Lãnh đạo/cấp phó duyệt cuối — chốt version (BR-07).'),
    ('OUTBOUND:ISSUE',         'Cấp số + phát hành VB đi',
     'OUTBOUND', 'ISSUE',   'Văn thư cơ quan cấp số, phát hành sau khi đã duyệt.'),
    ('OUTBOUND:READ_OWN',      'Xem VB đi của mình',
     'OUTBOUND', 'READ',    'Chỉ xem VB do mình tạo.'),
    ('OUTBOUND:READ_DEPT',     'Xem VB đi trong phòng/ban',
     'OUTBOUND', 'READ',    'Xem VB của phòng/ban.'),
    ('OUTBOUND:READ_ALL',      'Xem toàn bộ VB đi đơn vị',
     'OUTBOUND', 'READ',    'Xem mọi VB đi.'),
    ('OUTBOUND:RECALL',        'Thu hồi VB đi',
     'OUTBOUND', 'RECALL',  'Thu hồi VB đã phát hành (BR-11).');

-- ADMIN: tất cả
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'OUTBOUND:%'
ON CONFLICT DO NOTHING;

-- VAN_THU_CQ: chỉ ISSUE + READ_ALL + RECALL (không soạn, không duyệt)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VAN_THU_CQ'
  AND p.code IN ('OUTBOUND:ISSUE','OUTBOUND:READ_ALL','OUTBOUND:RECALL')
ON CONFLICT DO NOTHING;

-- VAN_THU_PB: soạn + sửa + submit, xem trong phòng
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VAN_THU_PB'
  AND p.code IN ('OUTBOUND:CREATE_DRAFT','OUTBOUND:UPDATE','OUTBOUND:SUBMIT',
                 'OUTBOUND:READ_DEPT')
ON CONFLICT DO NOTHING;

-- TRUONG_PHONG: APPROVE_DEPT + soạn + xem phòng
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'TRUONG_PHONG'
  AND p.code IN ('OUTBOUND:CREATE_DRAFT','OUTBOUND:UPDATE','OUTBOUND:SUBMIT',
                 'OUTBOUND:APPROVE_DEPT','OUTBOUND:READ_DEPT')
ON CONFLICT DO NOTHING;

-- LANH_DAO + CAP_PHO: APPROVE_LEADER + RECALL + xem all
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('LANH_DAO','CAP_PHO')
  AND p.code IN ('OUTBOUND:APPROVE_LEADER','OUTBOUND:READ_ALL','OUTBOUND:RECALL')
ON CONFLICT DO NOTHING;

-- CHUYEN_VIEN: soạn + sửa + submit + xem của mình
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CHUYEN_VIEN'
  AND p.code IN ('OUTBOUND:CREATE_DRAFT','OUTBOUND:UPDATE','OUTBOUND:SUBMIT',
                 'OUTBOUND:READ_OWN')
ON CONFLICT DO NOTHING;

-- LUU_TRU: chỉ xem all
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'LUU_TRU' AND p.code = 'OUTBOUND:READ_ALL'
ON CONFLICT DO NOTHING;
