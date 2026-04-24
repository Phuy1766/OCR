-- =========================================================================
-- V6 — Bootstrap organization + admin mặc định.
-- Admin được tạo với password_hash placeholder; ApplicationBootstrap sẽ
-- cập nhật hash thật từ env APP_BOOTSTRAP_ADMIN_PASSWORD khi khởi động
-- lần đầu (must_change_password = TRUE).
-- =========================================================================

-- Tổ chức mẫu
INSERT INTO organizations (id, code, name, full_name, is_active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'ROOT',
    'Cơ quan (cấu hình sau)',
    'Cơ quan chủ quản — cập nhật thông tin trong màn hình quản trị.',
    TRUE
);

-- Admin user với password_hash placeholder (Argon2id của chuỗi rỗng sẽ
-- không bao giờ khớp); ApplicationBootstrap sẽ cập nhật hash thật.
INSERT INTO users (
    id, username, email, password_hash, full_name, organization_id,
    is_active, is_locked, must_change_password, position_title
)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'admin',
    'admin@congvan.local',
    '$argon2id$v=19$m=65536,t=3,p=1$PLACEHOLDER_SALT_REPLACEME_$PLACEHOLDER_HASH_REPLACEME__________',
    'Quản trị viên hệ thống',
    '00000000-0000-0000-0000-000000000001',
    TRUE,
    FALSE,
    TRUE,
    'System Administrator'
);

-- Gán role ADMIN toàn cơ quan
INSERT INTO user_roles (user_id, role_id, department_id, assigned_at)
SELECT
    '00000000-0000-0000-0000-000000000002'::uuid,
    r.id,
    NULL,
    NOW()
FROM roles r WHERE r.code = 'ADMIN';
