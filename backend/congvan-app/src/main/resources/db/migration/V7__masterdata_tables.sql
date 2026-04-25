-- =========================================================================
-- V7 — Master data schema: document_types, confidentiality_levels,
-- priority_levels, document_books, document_book_counters.
-- Căn cứ: architecture §7, legal §3 + Phụ lục III.
-- BR-01: số CV đi reset 01/01 hàng năm; BR-02: cấp số dùng SELECT FOR UPDATE
-- trên document_book_counters; BR-03: VB mật có sổ riêng.
-- =========================================================================

-- ================= document_types =================
-- 29 loại VB hành chính theo Phụ lục III NĐ 30/2020. Seed ở V8.
CREATE TABLE document_types (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(50)  NOT NULL UNIQUE,           -- NGHI_QUYET, QUYET_DINH, ...
    abbreviation  VARCHAR(10)  NOT NULL,                  -- NQ, QĐ, CT, ... (dùng trong số VB)
    name          VARCHAR(100) NOT NULL,                  -- "Nghị quyết", "Quyết định", ...
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    is_system     BOOLEAN      NOT NULL DEFAULT TRUE,     -- seed system, không cho xóa
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE document_types IS '29 loại văn bản hành chính (Phụ lục III NĐ 30/2020).';
COMMENT ON COLUMN document_types.abbreviation IS 'Viết tắt dùng trong số hiệu VB (vd: 15/QĐ-UBND).';

-- ================= confidentiality_levels =================
-- 4 mức độ mật: Bình thường, Mật, Tối mật, Tuyệt mật (Luật BVBMNN 2018).
CREATE TABLE confidentiality_levels (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(50)  NOT NULL UNIQUE,           -- BINH_THUONG, MAT, TOI_MAT, TUYET_MAT
    name          VARCHAR(100) NOT NULL,
    level         INT          NOT NULL,                  -- 0=BT, 1=MAT, 2=TOI_MAT, 3=TUYET_MAT
    color         VARCHAR(20)  NULL,                      -- hex color cho UI
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_confidentiality_level UNIQUE (level)
);
COMMENT ON TABLE confidentiality_levels IS
    'Độ mật theo Luật BVBMNN 2018 — level cao hơn thì chặt chẽ hơn.';

-- ================= priority_levels =================
-- 4 mức khẩn: Bình thường, Khẩn, Thượng khẩn, Hỏa tốc (NĐ 30/2020).
CREATE TABLE priority_levels (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    level         INT          NOT NULL,                  -- 0=BT, 1=KHAN, 2=THUONG_KHAN, 3=HOA_TOC
    color         VARCHAR(20)  NULL,
    sla_hours     INT          NULL,                      -- số giờ xử lý khuyến nghị (BR-05)
    description   VARCHAR(500) NULL,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_priority_level UNIQUE (level)
);
COMMENT ON COLUMN priority_levels.sla_hours IS
    'SLA giờ xử lý đề xuất (null = không giới hạn). BR-04: khẩn/hỏa tốc ưu tiên.';

-- ================= document_books =================
-- Sổ đăng ký công văn đến/đi, có thể có nhiều sổ cho cùng 1 tổ chức
-- (sổ thường, sổ mật — BR-03). Số CV reset theo năm (BR-01).
CREATE TABLE document_books (
    id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id        UUID         NOT NULL REFERENCES organizations(id),
    code                   VARCHAR(50)  NOT NULL,                -- SỔ CV ĐẾN 2026
    name                   VARCHAR(255) NOT NULL,
    book_type              VARCHAR(20)  NOT NULL,                -- INBOUND / OUTBOUND
    confidentiality_scope  VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
        -- NORMAL = sổ thường (VB không mật)
        -- SECRET = sổ mật riêng (VB Mật/Tối mật/Tuyệt mật — BR-03)
    prefix                 VARCHAR(50)  NULL,                    -- prefix trong số hiệu, optional
    description            VARCHAR(500) NULL,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by             UUID         NULL,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_by             UUID         NULL,
    deleted_at             TIMESTAMPTZ  NULL,
    deleted_by             UUID         NULL,
    CONSTRAINT uq_document_books_org_code UNIQUE (organization_id, code),
    CONSTRAINT chk_book_type CHECK (book_type IN ('INBOUND','OUTBOUND')),
    CONSTRAINT chk_book_scope CHECK (confidentiality_scope IN ('NORMAL','SECRET'))
);
CREATE INDEX idx_document_books_org_active
    ON document_books(organization_id, book_type) WHERE is_deleted = FALSE AND is_active = TRUE;

COMMENT ON TABLE document_books IS
    'Sổ đăng ký VB. VB mật (Mật/Tối mật/Tuyệt mật) phải dùng sổ có scope=SECRET riêng.';

-- ================= document_book_counters =================
-- Counter cấp số cho từng (book, year). BR-01: reset 01/01; BR-02: dùng
-- SELECT ... FOR UPDATE trong transaction để đảm bảo không trùng số.
CREATE TABLE document_book_counters (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    book_id      UUID         NOT NULL REFERENCES document_books(id),
    year         INT          NOT NULL,
    next_number  BIGINT       NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_book_year UNIQUE (book_id, year),
    CONSTRAINT chk_counter_next_number_positive CHECK (next_number >= 1),
    CONSTRAINT chk_counter_year CHECK (year BETWEEN 2020 AND 2100)
);
CREATE INDEX idx_counter_book ON document_book_counters(book_id);

COMMENT ON TABLE document_book_counters IS
    'Counter cấp số. Mỗi (book_id, year) có 1 row. Truy cập trong @Transactional với SELECT FOR UPDATE (BR-02) để tránh race condition.';
