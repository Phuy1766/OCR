-- =========================================================================
-- V2 — Text search configuration "vietnamese": bỏ dấu + simple tokenizer.
-- Dùng cho Phase 8 (search) trên các cột trích yếu, nội dung công văn.
-- PostgreSQL mặc định chưa có stemmer tiếng Việt, dùng unaccent + simple
-- đã đủ cho MVP (có thể bổ sung custom dictionary sau).
-- =========================================================================

-- Tạo config mới kế thừa từ "simple" (không stem, không stopword).
DROP TEXT SEARCH CONFIGURATION IF EXISTS vietnamese;
CREATE TEXT SEARCH CONFIGURATION vietnamese (COPY = simple);

-- Gắn unaccent filter cho các token chữ cái & hyphenated word.
ALTER TEXT SEARCH CONFIGURATION vietnamese
    ALTER MAPPING FOR hword, hword_part, word
    WITH unaccent, simple;

-- Sanity check: to_tsvector('vietnamese', 'Công văn số 15') phải trả về lexeme không dấu.
-- SELECT to_tsvector('vietnamese', 'Công văn số 15/QĐ-UBND');
