-- =========================================================================
-- V1 — Baseline: bật extension cần thiết cho toàn bộ dự án.
-- Căn cứ: docs/tech_stack §7 (PostgreSQL 16, FTS tiếng Việt, UUID, crypto).
-- Idempotent: CREATE EXTENSION IF NOT EXISTS.
-- =========================================================================

-- UUID v4 cho khóa chính (gen_random_uuid trong pgcrypto là đủ, nhưng
-- uuid-ossp giữ lại để tương thích driver cũ).
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Mã hóa (dùng gen_random_uuid, digest, crypt trong pgcrypto).
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Bỏ dấu tiếng Việt cho FTS và fuzzy search.
CREATE EXTENSION IF NOT EXISTS "unaccent";

-- Trigram cho fuzzy / ILIKE có index.
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- GIN index cho kiểu scalar (timestamp, uuid) — hỗ trợ composite GIN.
CREATE EXTENSION IF NOT EXISTS "btree_gin";
