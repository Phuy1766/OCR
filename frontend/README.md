# congvan-frontend

Next.js 14 App Router / TypeScript strict / TailwindCSS + shadcn/ui / TanStack Query v5.

## Cấu trúc

```
app/
  (auth)/login/          # Đăng nhập (Phase 1)
  (dashboard)/           # Khu vực sau đăng nhập (Phase 9)
  api/health/            # Healthcheck cho Docker
  layout.tsx             # Root layout, QueryProvider, Toaster
  page.tsx               # Trang chủ public
components/
  providers/             # QueryProvider
  ui/                    # shadcn primitives (Phase 1+)
  documents/             # Domain components (Phase 3+)
lib/
  utils.ts               # cn(), Tailwind class merge
  api-client.ts          # Axios, withCredentials cho HttpOnly cookie
hooks/                   # TanStack Query hooks (Phase 1+)
schemas/                 # Zod schemas (Phase 1+)
stores/                  # Zustand stores (Phase 1+)
types/                   # TypeScript types
```

## Chạy local

```bash
pnpm install
pnpm dev                 # http://localhost:3000
pnpm type-check          # Verify TS strict
pnpm build               # Production build
```
