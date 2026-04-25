-- =========================================================================
-- V16 — OCR tables. Pipeline: register VB đến → tạo ocr_job (PENDING) →
-- worker async gọi OCR service → ocr_result + extracted_fields →
-- người dùng accept → áp metadata vào document.
-- =========================================================================

-- ================= ocr_jobs =================
CREATE TABLE ocr_jobs (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID         NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    file_id         UUID         NOT NULL REFERENCES document_files(id) ON DELETE CASCADE,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
        -- PENDING | PROCESSING | COMPLETED | FAILED | TIMEOUT | SERVICE_UNAVAILABLE
    retry_count     INT          NOT NULL DEFAULT 0,
    error_message   TEXT         NULL,
    enqueued_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    started_at      TIMESTAMPTZ  NULL,
    completed_at    TIMESTAMPTZ  NULL,
    created_by      UUID         NULL,

    CONSTRAINT chk_ocr_job_status CHECK (status IN
        ('PENDING','PROCESSING','COMPLETED','FAILED','TIMEOUT','SERVICE_UNAVAILABLE'))
);
CREATE INDEX idx_ocr_jobs_doc    ON ocr_jobs(document_id);
CREATE INDEX idx_ocr_jobs_status ON ocr_jobs(status, enqueued_at);

COMMENT ON TABLE ocr_jobs IS
    'OCR job queue. Worker async pull PENDING jobs, gọi FastAPI OCR service.';

-- ================= ocr_results =================
CREATE TABLE ocr_results (
    id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id             UUID         NOT NULL UNIQUE REFERENCES ocr_jobs(id) ON DELETE CASCADE,
    raw_text           TEXT         NULL,            -- nội dung text full
    confidence_avg     NUMERIC(4,3) NULL,             -- avg confidence (0..1)
    processing_ms      INT          NULL,             -- thời gian xử lý ms
    engine_version     VARCHAR(50)  NULL,             -- "PaddleOCR 2.8.1"
    page_count         INT          NULL,
    is_accepted        BOOLEAN      NOT NULL DEFAULT FALSE,
    accepted_at        TIMESTAMPTZ  NULL,
    accepted_by        UUID         NULL REFERENCES users(id),
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ocr_results_job ON ocr_results(job_id);

COMMENT ON COLUMN ocr_results.is_accepted IS
    'Chỉ kết quả accepted mới được dùng làm metadata chính thức của VB.';

-- ================= ocr_extracted_fields =================
CREATE TABLE ocr_extracted_fields (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    result_id       UUID         NOT NULL REFERENCES ocr_results(id) ON DELETE CASCADE,
    field_name      VARCHAR(100) NOT NULL,
        -- external_reference_number, external_issuer, external_issued_date,
        -- subject, summary
    field_value     TEXT         NULL,
    confidence      NUMERIC(4,3) NULL,
    bbox            JSONB        NULL,                -- {x,y,w,h} coords in PDF/image
    page_number     INT          NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_ocr_field UNIQUE (result_id, field_name)
);
CREATE INDEX idx_ocr_fields_result ON ocr_extracted_fields(result_id);

-- ================= OCR permissions =================
INSERT INTO permissions (code, name, resource, action, description) VALUES
    ('OCR:READ',    'Xem kết quả OCR',
     'OCR', 'READ',    'Xem kết quả OCR đã sinh ra cho VB.'),
    ('OCR:ACCEPT',  'Chấp nhận / sửa kết quả OCR',
     'OCR', 'ACCEPT',  'Chấp nhận hoặc chỉnh sửa metadata từ OCR thành chính thức.'),
    ('OCR:PROCESS', 'Kích hoạt OCR thủ công',
     'OCR', 'PROCESS', 'Trigger OCR job thủ công cho VB.');

-- ADMIN: tất cả
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code LIKE 'OCR:%'
ON CONFLICT DO NOTHING;

-- VAN_THU_CQ + VAN_THU_PB: PROCESS + ACCEPT + READ (văn thư xác nhận metadata)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('VAN_THU_CQ','VAN_THU_PB')
  AND p.code IN ('OCR:READ','OCR:ACCEPT','OCR:PROCESS')
ON CONFLICT DO NOTHING;

-- Các role còn lại chỉ READ
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code IN ('LANH_DAO','CAP_PHO','TRUONG_PHONG','CHUYEN_VIEN','LUU_TRU')
  AND p.code = 'OCR:READ'
ON CONFLICT DO NOTHING;
