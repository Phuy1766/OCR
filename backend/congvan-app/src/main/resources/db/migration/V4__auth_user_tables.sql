-- =========================================================================
-- V4 — Auth & User schema.
-- Căn cứ: architecture §5.1 + §7, legal §12 (ma trận phân quyền 8 role),
-- tech_stack §8 (JWT RS256, Argon2id, lockout).
-- BR-10 (audit append-only) + BR-12 (khóa tài khoản sau 5 lần sai).
-- =========================================================================

-- ================= organizations =================
CREATE TABLE organizations (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code                VARCHAR(50)  NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    full_name           VARCHAR(500) NULL,
    tax_code            VARCHAR(20)  NULL,
    address             VARCHAR(500) NULL,
    phone               VARCHAR(30)  NULL,
    email               VARCHAR(255) NULL,
    parent_id           UUID         NULL REFERENCES organizations(id),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID         NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          UUID         NULL,
    deleted_at          TIMESTAMPTZ  NULL,
    deleted_by          UUID         NULL
);
CREATE INDEX idx_organizations_parent ON organizations(parent_id) WHERE is_deleted = FALSE;
COMMENT ON TABLE organizations IS 'Đơn vị, cơ quan (cấp cao nhất). Hỗ trợ cây đơn vị trực thuộc.';

-- ================= departments =================
CREATE TABLE departments (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID         NOT NULL REFERENCES organizations(id),
    code                VARCHAR(50)  NOT NULL,
    name                VARCHAR(255) NOT NULL,
    parent_id           UUID         NULL REFERENCES departments(id),
    head_user_id        UUID         NULL,             -- trưởng phòng (FK đặt sau khi có users)
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID         NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by          UUID         NULL,
    deleted_at          TIMESTAMPTZ  NULL,
    deleted_by          UUID         NULL,
    CONSTRAINT uq_departments_org_code UNIQUE (organization_id, code)
);
CREATE INDEX idx_departments_org    ON departments(organization_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_departments_parent ON departments(parent_id)       WHERE is_deleted = FALSE;
COMMENT ON TABLE departments IS 'Phòng/ban trực thuộc tổ chức, có thể lồng nhau (tree).';

-- ================= users =================
CREATE TABLE users (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username                 VARCHAR(100) NOT NULL UNIQUE,
    email                    VARCHAR(255) NOT NULL UNIQUE,
    password_hash            VARCHAR(255) NOT NULL,             -- Argon2id encoded
    full_name                VARCHAR(255) NOT NULL,
    phone                    VARCHAR(30)  NULL,
    organization_id          UUID         NULL REFERENCES organizations(id),
    department_id            UUID         NULL REFERENCES departments(id),
    position_title           VARCHAR(255) NULL,                 -- vd: "Trưởng phòng Tổ chức cán bộ"
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    is_locked                BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_count       INT          NOT NULL DEFAULT 0,
    locked_until             TIMESTAMPTZ  NULL,                 -- soft lock (auto unlock sau khoảng thời gian)
    last_login_at            TIMESTAMPTZ  NULL,
    last_login_ip            VARCHAR(45)  NULL,
    password_changed_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    must_change_password     BOOLEAN      NOT NULL DEFAULT FALSE,
    is_deleted               BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by               UUID         NULL,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by               UUID         NULL,
    deleted_at               TIMESTAMPTZ  NULL,
    deleted_by               UUID         NULL
);
CREATE INDEX idx_users_department ON users(department_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_org        ON users(organization_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_active     ON users(is_active, is_locked) WHERE is_deleted = FALSE;
COMMENT ON COLUMN users.password_hash IS 'Argon2id encoded string — KHÔNG dùng BCrypt (BR security).';
COMMENT ON COLUMN users.failed_login_count IS 'Đếm số lần login sai liên tiếp; reset về 0 khi login thành công.';

-- Gắn FK head_user_id sau khi users có.
ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head_user FOREIGN KEY (head_user_id) REFERENCES users(id);

-- ================= roles =================
CREATE TABLE roles (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(50)  NOT NULL UNIQUE,            -- ADMIN, VAN_THU_CQ, LANH_DAO, ...
    name         VARCHAR(100) NOT NULL,                   -- "Quản trị hệ thống"
    description  VARCHAR(500) NULL,
    is_system    BOOLEAN      NOT NULL DEFAULT FALSE,     -- role mặc định không cho xóa
    display_order INT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ================= permissions =================
CREATE TABLE permissions (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code         VARCHAR(100) NOT NULL UNIQUE,            -- "DOC:READ", "DOC:APPROVE", "USER:MANAGE"
    name         VARCHAR(255) NOT NULL,
    resource     VARCHAR(50)  NOT NULL,                   -- DOC, USER, REPORT, ...
    action       VARCHAR(50)  NOT NULL,                   -- READ, CREATE, UPDATE, DELETE, APPROVE, ...
    description  VARCHAR(500) NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ================= role_permissions =================
CREATE TABLE role_permissions (
    role_id       UUID        NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID        NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_id)
);

-- ================= user_roles =================
-- Scope theo department: 1 user có thể có nhiều role, mỗi role có thể gắn
-- trong phạm vi department nào (null = toàn cơ quan).
CREATE TABLE user_roles (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id       UUID         NOT NULL REFERENCES roles(id),
    department_id UUID         NULL REFERENCES departments(id),     -- scope
    assigned_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    assigned_by   UUID         NULL,
    expires_at    TIMESTAMPTZ  NULL,
    CONSTRAINT uq_user_roles UNIQUE (user_id, role_id, department_id)
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- ================= refresh_tokens =================
-- Lưu refresh token đang active để hỗ trợ rotation và revoke.
-- Chỉ lưu HASH (không lưu plaintext).
CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash   VARCHAR(128) NOT NULL UNIQUE,          -- SHA-256 hex
    parent_id    UUID         NULL REFERENCES refresh_tokens(id),  -- rotation chain
    user_agent   VARCHAR(500) NULL,
    ip_address   VARCHAR(45)  NULL,
    issued_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ  NULL,
    revoked_reason VARCHAR(100) NULL                     -- LOGOUT, ROTATED, COMPROMISED
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id) WHERE revoked_at IS NULL;
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens(expires_at) WHERE revoked_at IS NULL;

-- ================= login_attempts =================
-- Lưu mọi lần thử login (cả thành công lẫn thất bại) cho audit và phát hiện
-- brute-force. Áp dụng BR-12: khóa tài khoản sau 5 lần sai liên tiếp.
CREATE TABLE login_attempts (
    id           BIGSERIAL    PRIMARY KEY,
    username     VARCHAR(100) NOT NULL,
    user_id      UUID         NULL REFERENCES users(id),  -- NULL nếu username không tồn tại
    ip_address   VARCHAR(45)  NULL,
    user_agent   VARCHAR(500) NULL,
    success      BOOLEAN      NOT NULL,
    failure_reason VARCHAR(100) NULL,                     -- INVALID_PASSWORD, ACCOUNT_LOCKED, ...
    attempt_time TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_login_attempts_username_time ON login_attempts(username, attempt_time DESC);
CREATE INDEX idx_login_attempts_ip_time       ON login_attempts(ip_address, attempt_time DESC) WHERE ip_address IS NOT NULL;
