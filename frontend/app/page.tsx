import Link from 'next/link';

export default function HomePage() {
  return (
    <main className="container flex min-h-screen flex-col items-center justify-center gap-6 py-16">
      <div className="text-center">
        <h1 className="text-3xl font-bold tracking-tight md:text-4xl">
          Hệ thống quản lý công văn
        </h1>
        <p className="mt-2 text-muted-foreground">
          Công văn đi/đến tích hợp OCR và chữ ký số — NĐ 30/2020/NĐ-CP, Luật GDĐT 2023.
        </p>
      </div>
      <div className="flex flex-wrap gap-3">
        <Link
          href="/login"
          className="inline-flex h-10 items-center rounded-md bg-primary px-6 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          Đăng nhập
        </Link>
        <a
          href="/api/health"
          className="inline-flex h-10 items-center rounded-md border border-input px-6 text-sm font-medium hover:bg-accent"
        >
          Kiểm tra kết nối
        </a>
      </div>
      <p className="text-xs text-muted-foreground">Phiên bản MVP · Phase 0</p>
    </main>
  );
}
