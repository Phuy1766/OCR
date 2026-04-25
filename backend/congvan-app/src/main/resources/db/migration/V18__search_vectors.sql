-- =========================================================================
-- V18 — Full-text search tiếng Việt cho documents + ocr_results.
-- Dùng config "vietnamese" (đã tạo ở V2: unaccent + simple).
-- Hibernate UPDATE entity sẽ KHÔNG tự refresh GENERATED column → dùng trigger.
-- =========================================================================

-- ================= documents.search_vector =================
ALTER TABLE documents
    ADD COLUMN search_vector tsvector;

-- Trigger function: tính lại tsvector khi insert/update các trường text liên quan
CREATE OR REPLACE FUNCTION documents_update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('vietnamese',
            coalesce(NEW.subject, '')), 'A')
        || setweight(to_tsvector('vietnamese',
            coalesce(NEW.external_reference_number, '')), 'A')
        || setweight(to_tsvector('vietnamese',
            coalesce(NEW.external_issuer, '')), 'B')
        || setweight(to_tsvector('vietnamese',
            coalesce(NEW.summary, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_documents_search_vector
    BEFORE INSERT OR UPDATE OF subject, summary, external_reference_number, external_issuer
    ON documents
    FOR EACH ROW EXECUTE FUNCTION documents_update_search_vector();

-- Backfill cho rows hiện tại (nếu có)
UPDATE documents SET subject = subject;

CREATE INDEX idx_documents_search_vector
    ON documents USING gin(search_vector)
    WHERE is_deleted = false;

-- pg_trgm cho fuzzy fallback trên subject
CREATE INDEX idx_documents_subject_trgm
    ON documents USING gin(subject gin_trgm_ops)
    WHERE is_deleted = false;

CREATE INDEX idx_documents_external_ref_trgm
    ON documents USING gin(external_reference_number gin_trgm_ops)
    WHERE is_deleted = false AND external_reference_number IS NOT NULL;

COMMENT ON COLUMN documents.search_vector IS
    'tsvector ghép subject(A) + external_reference_number(A) + issuer(B) + summary(C). '
    'Cập nhật qua trigger trg_documents_search_vector.';

-- ================= ocr_results.search_vector =================
ALTER TABLE ocr_results
    ADD COLUMN search_vector tsvector;

CREATE OR REPLACE FUNCTION ocr_results_update_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('vietnamese', coalesce(NEW.raw_text, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ocr_results_search_vector
    BEFORE INSERT OR UPDATE OF raw_text ON ocr_results
    FOR EACH ROW EXECUTE FUNCTION ocr_results_update_search_vector();

CREATE INDEX idx_ocr_results_search_vector
    ON ocr_results USING gin(search_vector)
    WHERE is_accepted = true;

COMMENT ON INDEX idx_ocr_results_search_vector IS
    'GIN trên raw_text — chỉ index OCR đã được accept (BR-09 source of truth).';

-- ================= SEARCH:READ permission =================
INSERT INTO permissions (code, name, resource, action, description) VALUES
    ('SEARCH:READ', 'Tìm kiếm văn bản',
     'SEARCH', 'READ', 'Tìm kiếm full-text + filter trên VB đến/đi.');

-- Mọi role đã xác thực đều có thể search (kết quả đã filter theo INBOUND/OUTBOUND scope)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE p.code = 'SEARCH:READ'
ON CONFLICT DO NOTHING;
