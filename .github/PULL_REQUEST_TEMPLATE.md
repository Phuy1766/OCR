## Tóm tắt thay đổi

<!-- Mô tả ngắn: feature/bug fix/refactor gì, thuộc Phase nào. -->

## Loại thay đổi

- [ ] feat (tính năng mới)
- [ ] fix (sửa lỗi)
- [ ] refactor (không thay đổi behavior)
- [ ] docs (tài liệu)
- [ ] test (thêm/sửa test)
- [ ] chore (hạ tầng, CI, build)

## Tài liệu tham chiếu

<!-- File/section trong docs/ liên quan. Vd: docs/architecture §5.3, docs/legal §13 BR-06. -->

## Business rule pháp lý đã tuân thủ

<!-- Liệt kê các BR-XX đã áp dụng hoặc xác nhận không ảnh hưởng. -->

## Test plan

- [ ] Unit test mới cho logic chính
- [ ] Integration test với Testcontainers (nếu chạm DB)
- [ ] Smoke test manual qua Swagger / UI

## Checklist bảo mật

- [ ] `@PreAuthorize` cho mọi endpoint không public (nếu là backend)
- [ ] Validation input @Valid + business rule trong Service
- [ ] Ghi audit log cho thao tác write (cùng transaction)
- [ ] Không commit secrets (`.env`, key, cert)
