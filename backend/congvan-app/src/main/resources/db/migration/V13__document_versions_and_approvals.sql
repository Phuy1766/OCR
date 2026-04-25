-- =========================================================================
-- V13 — Versioning + Approval cho công văn đi.
-- Căn cứ: architecture §5.4, BR-07 (khóa version sau duyệt cuối).
-- document_versions là IMMUTABLE — append-only sau khi tạo.
-- =========================================================================

-- ================= document_versions =================
CREATE TABLE document_versions (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id         UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number      INT          NOT NULL,
    parent_version_id   UUID         NULL REFERENCES document_versions(id),

    -- Snapshot toàn bộ metadata + tham chiếu file tại thời điểm version
    content_snapshot    JSONB        NOT NULL,

    -- Trạng thái version: DRAFT (đang sửa), SUBMITTED (đã gửi duyệt),
    -- APPROVED (đã duyệt cuối, immutable), SUPERSEDED (đã có bản mới hơn),
    -- REJECTED (bị từ chối, đã có version mới sau đó)
    version_status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',

    -- Hash SHA-256 của content_snapshot — chốt khi APPROVED, dùng verify
    -- toàn vẹn khi ký số (BR-07).
    hash_sha256         VARCHAR(64)  NULL,

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by          UUID         NULL REFERENCES users(id),

    CONSTRAINT uq_document_version UNIQUE (document_id, version_number),
    CONSTRAINT chk_version_status CHECK (
        version_status IN ('DRAFT','SUBMITTED','APPROVED','SUPERSEDED','REJECTED')
    ),
    CONSTRAINT chk_version_number CHECK (version_number >= 1)
);
CREATE INDEX idx_versions_doc ON document_versions(document_id, version_number DESC);
CREATE INDEX idx_versions_status ON document_versions(version_status);

COMMENT ON TABLE document_versions IS
    'IMMUTABLE snapshot — append-only. APPROVED version chốt approved_version_id (BR-07).';

-- Chặn UPDATE/DELETE từ ứng dụng (tương tự audit_logs).
CREATE OR REPLACE FUNCTION document_versions_no_modification()
RETURNS TRIGGER AS $$
BEGIN
    -- Cho phép UPDATE để chuyển trạng thái (DRAFT→SUBMITTED→APPROVED, hoặc → SUPERSEDED).
    -- Nhưng KHÔNG cho phép thay đổi content_snapshot, hash_sha256, version_number.
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'document_versions là append-only — không cho phép DELETE'
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.content_snapshot IS DISTINCT FROM OLD.content_snapshot THEN
        RAISE EXCEPTION 'content_snapshot là immutable — không được sửa sau khi tạo version'
            USING ERRCODE = 'check_violation';
    END IF;
    IF NEW.version_number <> OLD.version_number THEN
        RAISE EXCEPTION 'version_number là immutable'
            USING ERRCODE = 'check_violation';
    END IF;
    -- hash chỉ được set 1 lần (khi APPROVED)
    IF OLD.hash_sha256 IS NOT NULL AND NEW.hash_sha256 IS DISTINCT FROM OLD.hash_sha256 THEN
        RAISE EXCEPTION 'hash_sha256 đã chốt — không được thay đổi (BR-07)'
            USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_document_versions_immutable
    BEFORE UPDATE OR DELETE ON document_versions
    FOR EACH ROW EXECUTE FUNCTION document_versions_no_modification();

-- ================= ALTER documents — chốt approved_version_id =================
ALTER TABLE documents
    ADD COLUMN approved_version_id UUID NULL REFERENCES document_versions(id);

CREATE INDEX idx_documents_approved_version
    ON documents(approved_version_id) WHERE approved_version_id IS NOT NULL;

COMMENT ON COLUMN documents.approved_version_id IS
    'Khóa version đã duyệt cuối (BR-07). Sau khi set, không cho sửa nội dung.';

-- ================= ALTER document_files — gắn version =================
-- Phase 3 để version_id NULL; Phase 4 các file của VB đi gắn với version cụ thể.
ALTER TABLE document_files
    ADD CONSTRAINT fk_doc_files_version
    FOREIGN KEY (version_id) REFERENCES document_versions(id);

-- ================= approvals =================
CREATE TABLE approvals (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_id      UUID         NOT NULL REFERENCES document_versions(id),
    approval_level  VARCHAR(30)  NOT NULL,
        -- DEPARTMENT_HEAD (cấp phòng — TRUONG_PHONG)
        -- UNIT_LEADER (cấp đơn vị — LANH_DAO/CAP_PHO)
    decision        VARCHAR(20)  NOT NULL,
        -- APPROVED | REJECTED
    comment         VARCHAR(2000) NULL,
    decided_by      UUID         NOT NULL REFERENCES users(id),
    decided_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_approval_level CHECK (
        approval_level IN ('DEPARTMENT_HEAD','UNIT_LEADER')
    ),
    CONSTRAINT chk_approval_decision CHECK (decision IN ('APPROVED','REJECTED'))
);
CREATE INDEX idx_approvals_doc ON approvals(document_id, decided_at DESC);
COMMENT ON TABLE approvals IS
    'Mỗi quyết định duyệt là 1 row. Cùng 1 (document, version, level) chỉ nên có 1 record APPROVED.';
