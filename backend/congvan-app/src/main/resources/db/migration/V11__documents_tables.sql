-- =========================================================================
-- V11 — Core schema cho công văn: documents, document_files,
-- document_recipients, document_book_entries.
-- Căn cứ: architecture §5.3-§5.4 + §7. Phụ lục VI NĐ 30/2020 (trường bắt buộc).
-- BR-09: soft delete; BR-10: audit (đã có ở V3); BR-11: thu hồi không xóa.
-- =========================================================================

-- ================= documents =================
-- Bảng core lưu state hiện tại của VB (cả đến và đi). Phase 3 chỉ dùng cho INBOUND;
-- Phase 4 mở rộng cho OUTBOUND. Soft delete (BR-09) + audit fields.
CREATE TABLE documents (
    id                          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    direction                   VARCHAR(10)  NOT NULL,
        -- INBOUND (công văn đến) | OUTBOUND (công văn đi)
    document_type_id            UUID         NOT NULL REFERENCES document_types(id),
    confidentiality_level_id    UUID         NOT NULL REFERENCES confidentiality_levels(id),
    priority_level_id           UUID         NOT NULL REFERENCES priority_levels(id),

    -- Trích yếu + nội dung tóm tắt (Phụ lục VI)
    subject                     VARCHAR(1000) NOT NULL,
    summary                     TEXT         NULL,

    -- Trạng thái máy (state machine)
    status                      VARCHAR(30)  NOT NULL,

    -- Sổ đăng ký + số được cấp
    book_id                     UUID         NULL REFERENCES document_books(id),
    book_year                   INT          NULL,
    book_number                 BIGINT       NULL,

    -- VB đến: ngày đến (BR-05) + nguồn nhận
    received_date               DATE         NULL,
    received_from_channel       VARCHAR(30)  NULL,
        -- POST | EMAIL | SCAN | HAND_DELIVERED | OTHER

    -- VB đến: thông tin VB gốc do bên gửi cấp
    external_reference_number   VARCHAR(100) NULL,
        -- vd "15/QĐ-UBND" — số/ký hiệu của bên gửi
    external_issuer             VARCHAR(500) NULL,
    external_issued_date        DATE         NULL,

    -- VB đi: ngày phát hành
    issued_date                 DATE         NULL,

    -- Người xử lý hiện tại (Phase 5 sẽ thay đổi qua workflow)
    current_handler_user_id     UUID         NULL REFERENCES users(id),
    current_handler_dept_id     UUID         NULL REFERENCES departments(id),
    due_date                    DATE         NULL,

    -- Phòng ban chủ thể (để filter READ_DEPT)
    organization_id             UUID         NOT NULL REFERENCES organizations(id),
    department_id               UUID         NULL REFERENCES departments(id),

    -- BR-11: thu hồi
    is_recalled                 BOOLEAN      NOT NULL DEFAULT FALSE,
    recalled_at                 TIMESTAMPTZ  NULL,
    recalled_by                 UUID         NULL REFERENCES users(id),
    recalled_reason             VARCHAR(1000) NULL,

    -- BR-09: soft delete + audit
    is_deleted                  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                  UUID         NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by                  UUID         NULL,
    deleted_at                  TIMESTAMPTZ  NULL,
    deleted_by                  UUID         NULL,

    CONSTRAINT chk_documents_direction CHECK (direction IN ('INBOUND','OUTBOUND')),
    CONSTRAINT chk_documents_received_channel
        CHECK (received_from_channel IS NULL
            OR received_from_channel IN ('POST','EMAIL','SCAN','HAND_DELIVERED','OTHER'))
);

CREATE INDEX idx_documents_org           ON documents(organization_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_documents_dept          ON documents(department_id)   WHERE is_deleted = FALSE;
CREATE INDEX idx_documents_handler       ON documents(current_handler_user_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_documents_book          ON documents(book_id, book_year, book_number);
CREATE INDEX idx_documents_status        ON documents(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_documents_direction_at  ON documents(direction, created_at DESC) WHERE is_deleted = FALSE;
CREATE INDEX idx_documents_due_date      ON documents(due_date) WHERE is_deleted = FALSE AND due_date IS NOT NULL;

COMMENT ON TABLE documents IS
    'Công văn (đến/đi). Soft delete BR-09. State trong cột status (string enum).';
COMMENT ON COLUMN documents.book_number IS
    'Số được cấp khi đăng ký. Cùng với book_id+book_year là duy nhất (BR-01/02).';

-- ================= document_files =================
CREATE TABLE document_files (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_id      UUID         NULL,                 -- Phase 4: gắn với document_versions
    file_role       VARCHAR(30)  NOT NULL,
        -- ORIGINAL_SCAN | ATTACHMENT | DRAFT_PDF | FINAL_PDF | SIGNED
    file_name       VARCHAR(500) NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    size_bytes      BIGINT       NOT NULL,
    sha256          VARCHAR(64)  NOT NULL,             -- hash hex 64 ký tự
    storage_key     VARCHAR(500) NOT NULL UNIQUE,      -- key trong MinIO
    uploaded_by     UUID         NULL REFERENCES users(id),
    uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- Soft delete
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at      TIMESTAMPTZ  NULL,
    deleted_by      UUID         NULL,

    CONSTRAINT chk_doc_files_role CHECK (file_role IN
        ('ORIGINAL_SCAN','ATTACHMENT','DRAFT_PDF','FINAL_PDF','SIGNED')),
    CONSTRAINT chk_doc_files_size CHECK (size_bytes > 0 AND size_bytes <= 52428800) -- 50MB
);
CREATE INDEX idx_doc_files_doc ON document_files(document_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_doc_files_version ON document_files(version_id) WHERE version_id IS NOT NULL;

COMMENT ON COLUMN document_files.sha256 IS
    'SHA-256 hex của nội dung file — phục vụ kiểm tra toàn vẹn (BR-07).';
COMMENT ON COLUMN document_files.storage_key IS
    'Pattern: {direction}/{yyyy}/{mm}/{document_id}/{uuid}-{filename}';

-- ================= document_recipients (Phase 4 dùng) =================
CREATE TABLE document_recipients (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    recipient_type  VARCHAR(30)  NOT NULL,             -- INTERNAL_DEPT | EXTERNAL_ORG | PERSON
    organization_id UUID         NULL REFERENCES organizations(id),
    department_id   UUID         NULL REFERENCES departments(id),
    user_id         UUID         NULL REFERENCES users(id),
    external_name   VARCHAR(500) NULL,
    external_email  VARCHAR(255) NULL,
    delivery_method VARCHAR(30)  NULL,
        -- EMAIL | POST | HAND_DELIVERED | SYSTEM
    delivered_at    TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_recipients_type CHECK (
        recipient_type IN ('INTERNAL_DEPT','EXTERNAL_ORG','PERSON')
    )
);
CREATE INDEX idx_doc_recipients_doc ON document_recipients(document_id);

-- ================= document_book_entries =================
-- Mỗi VB được đăng ký vào 1 sổ với (book_id, year, number) — UNIQUE.
-- entry_status: RESERVED (đã giữ số nhưng chưa chính thức), OFFICIAL (chính thức),
-- CANCELLED (hủy — không tái sử dụng số).
CREATE TABLE document_book_entries (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id      UUID         NOT NULL REFERENCES document_books(id),
    year         INT          NOT NULL,
    number       BIGINT       NOT NULL,
    document_id  UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    entry_status VARCHAR(20)  NOT NULL DEFAULT 'OFFICIAL',
    entered_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    entered_by   UUID         NULL REFERENCES users(id),
    cancelled_at TIMESTAMPTZ  NULL,
    cancelled_by UUID         NULL REFERENCES users(id),
    cancelled_reason VARCHAR(500) NULL,

    CONSTRAINT uq_book_entry UNIQUE (book_id, year, number),
    CONSTRAINT chk_book_entry_status CHECK (entry_status IN ('RESERVED','OFFICIAL','CANCELLED'))
);
CREATE INDEX idx_book_entries_doc ON document_book_entries(document_id);
COMMENT ON TABLE document_book_entries IS
    'Bản ghi trong sổ đăng ký. Số CANCELLED không được tái sử dụng (in sổ vẫn xuất hiện).';
