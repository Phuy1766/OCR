-- =========================================================================
-- V17 — Digital Signature schema. PAdES (PDFBox + Bouncy Castle).
-- Căn cứ: BR-06/12 (2 chữ ký số bắt buộc cho VB điện tử), BR-07 (ký đúng
-- approved_version), TCVN 11816.
-- =========================================================================

-- ================= certificates =================
CREATE TABLE certificates (
    id                      UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    type                    VARCHAR(20)  NOT NULL,
        -- PERSONAL (gắn user) | ORGANIZATION (cấp cơ quan, dùng VAN_THU_CQ ký)
    owner_user_id           UUID         NULL REFERENCES users(id),
    owner_organization_id   UUID         NULL REFERENCES organizations(id),
    alias                   VARCHAR(255) NOT NULL,
        -- Tên hiển thị (vd "Lê Văn A — Vụ trưởng")
    subject_dn              VARCHAR(1000) NOT NULL,
        -- "CN=Lê Văn A, OU=Vụ X, O=Bộ Y, C=VN"
    issuer_dn               VARCHAR(1000) NULL,
    serial_number           VARCHAR(100) NOT NULL,
    valid_from              TIMESTAMPTZ  NOT NULL,
    valid_to                TIMESTAMPTZ  NOT NULL,
    storage_key             VARCHAR(500) NOT NULL,
        -- Object key trong MinIO bucket congvan-signed
    is_revoked              BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at              TIMESTAMPTZ  NULL,
    revoked_reason          VARCHAR(500) NULL,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              UUID         NULL,

    CONSTRAINT chk_cert_type CHECK (type IN ('PERSONAL','ORGANIZATION')),
    CONSTRAINT chk_cert_owner_consistent CHECK (
        (type = 'PERSONAL' AND owner_user_id IS NOT NULL)
        OR (type = 'ORGANIZATION' AND owner_organization_id IS NOT NULL)
    )
);
CREATE INDEX idx_certs_owner_user ON certificates(owner_user_id) WHERE is_revoked = FALSE;
CREATE INDEX idx_certs_owner_org  ON certificates(owner_organization_id) WHERE is_revoked = FALSE;
CREATE INDEX idx_certs_validity   ON certificates(valid_to) WHERE is_revoked = FALSE;

COMMENT ON TABLE certificates IS
    'Cert PKCS#12 lưu trong MinIO. Password do user nhập mỗi lần ký, KHÔNG lưu DB.';

-- ================= digital_signatures =================
CREATE TABLE digital_signatures (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id         UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_id          UUID         NOT NULL REFERENCES document_versions(id),
    source_file_id      UUID         NOT NULL REFERENCES document_files(id),
        -- File trước khi ký (FINAL_PDF của approved_version)
    signed_file_id      UUID         NOT NULL REFERENCES document_files(id),
        -- File sau khi ký (file_role = SIGNED)
    certificate_id      UUID         NOT NULL REFERENCES certificates(id),
    signature_type      VARCHAR(20)  NOT NULL,
        -- PERSONAL (lãnh đạo) | ORGANIZATION (cơ quan)
    signer_user_id      UUID         NOT NULL REFERENCES users(id),
    signed_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    reason              VARCHAR(500) NULL,
    location            VARCHAR(500) NULL,
    contact_info        VARCHAR(500) NULL,

    CONSTRAINT chk_sig_type CHECK (signature_type IN ('PERSONAL','ORGANIZATION'))
);
CREATE INDEX idx_signatures_doc ON digital_signatures(document_id);
CREATE INDEX idx_signatures_version ON digital_signatures(version_id);

-- BR-06/12: 1 VB chỉ có tối đa 1 chữ ký PERSONAL + 1 ORGANIZATION cho 1 version
CREATE UNIQUE INDEX uq_signature_per_type
    ON digital_signatures(document_id, version_id, signature_type);

COMMENT ON TABLE digital_signatures IS
    'BR-06/12: VB điện tử bắt buộc 2 chữ ký (PERSONAL của lãnh đạo + ORGANIZATION '
    'của cơ quan). Mỗi version chỉ có 1 cặp.';

-- ================= signature_verifications =================
CREATE TABLE signature_verifications (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    signature_id    UUID         NOT NULL REFERENCES digital_signatures(id) ON DELETE CASCADE,
    is_valid        BOOLEAN      NOT NULL,
    failure_reason  VARCHAR(500) NULL,
    details         JSONB        NULL,
        -- {certSerial, signerSubject, hashAlgo, signatureTimestamp}
    verified_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    verified_by     UUID         NULL REFERENCES users(id)
);
CREATE INDEX idx_sig_verifications_sig ON signature_verifications(signature_id, verified_at DESC);

-- ================= Permissions =================
INSERT INTO permissions (code, name, resource, action, description) VALUES
    ('SIGN:PERSONAL',     'Ký số cá nhân',
     'SIGN', 'PERSONAL',  'Lãnh đạo/cấp phó ký số cá nhân lên VB.'),
    ('SIGN:ORGANIZATION', 'Ký số cơ quan',
     'SIGN', 'ORGANIZATION', 'Văn thư cơ quan ký số cơ quan (con dấu điện tử).'),
    ('SIGN:VERIFY',       'Xác minh chữ ký',
     'SIGN', 'VERIFY',    'Xác minh tính toàn vẹn chữ ký số trên VB.'),
    ('CERT:MANAGE',       'Quản lý certificate',
     'CERT', 'MANAGE',    'Upload, gán, thu hồi cert PKCS#12.');

-- ADMIN: tất cả
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code IN ('SIGN:PERSONAL','SIGN:ORGANIZATION','SIGN:VERIFY','CERT:MANAGE')
ON CONFLICT DO NOTHING;

-- LANH_DAO + CAP_PHO: SIGN:PERSONAL + SIGN:VERIFY (chuyên dùng công vụ
-- là chữ ký người có thẩm quyền đứng đầu cơ quan)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('LANH_DAO','CAP_PHO')
  AND p.code IN ('SIGN:PERSONAL','SIGN:VERIFY')
ON CONFLICT DO NOTHING;

-- VAN_THU_CQ: SIGN:ORGANIZATION + SIGN:VERIFY + CERT:MANAGE (quản lý cert cơ quan)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'VAN_THU_CQ'
  AND p.code IN ('SIGN:ORGANIZATION','SIGN:VERIFY','CERT:MANAGE')
ON CONFLICT DO NOTHING;

-- Mọi role có thể VERIFY (xem chữ ký có hợp lệ)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('VAN_THU_PB','TRUONG_PHONG','CHUYEN_VIEN','LUU_TRU')
  AND p.code = 'SIGN:VERIFY'
ON CONFLICT DO NOTHING;
