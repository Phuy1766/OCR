# Flyway migrations

Migration thực sự nằm ở [`backend/congvan-app/src/main/resources/db/migration/`](../../backend/congvan-app/src/main/resources/db/migration/).

Flyway được cấu hình trong [`backend/congvan-app/src/main/resources/application.yml`](../../backend/congvan-app/src/main/resources/application.yml) để đọc theo classpath khi backend khởi động.

## Quy ước đặt tên

```
V{n}__{snake_case_description}.sql
```

- `V1`, `V2`, ... là số thứ tự migration (không được bỏ số, không tái sử dụng).
- Mỗi migration **immutable** sau khi merge vào main — bug/sửa schema phải tạo migration mới.

## Các migration hiện có

| Version | Mô tả |
| --- | --- |
| `V1__baseline_extensions.sql` | Bật uuid-ossp, pgcrypto, unaccent, pg_trgm, btree_gin |
| `V2__vietnamese_fts_config.sql` | Text search config `vietnamese` (unaccent + simple) |
| `V3__audit_and_outbox_tables.sql` | `audit_logs` (append-only) + `outbox_messages` |
