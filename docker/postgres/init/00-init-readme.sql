-- Script này chạy một lần duy nhất khi volume postgres khởi tạo lần đầu
-- (Postgres Docker image tự động run file trong /docker-entrypoint-initdb.d/).
--
-- Các script setup schema sẽ do Flyway trong backend chạy khi app khởi động,
-- không cần bật extensions ở đây nữa.
--
-- Giữ file này để thư mục docker/postgres/init tồn tại và có thể bổ sung
-- DB/role/extensions phụ sau này (vd: DB riêng cho Keycloak).

SELECT 1;
