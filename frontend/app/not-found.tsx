import Link from 'next/link';
import { FileQuestion } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function NotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      <FileQuestion className="h-16 w-16 text-muted-foreground/50" />
      <div>
        <h1 className="text-3xl font-semibold">404 — Không tìm thấy trang</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Đường dẫn không tồn tại hoặc bạn không có quyền truy cập.
        </p>
      </div>
      <div className="flex gap-2">
        <Button asChild>
          <Link href="/dashboard">Về trang chủ</Link>
        </Button>
        <Button variant="outline" asChild>
          <Link href="/login">Đăng nhập</Link>
        </Button>
      </div>
    </div>
  );
}
