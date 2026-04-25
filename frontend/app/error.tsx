'use client';

import { useEffect } from 'react';
import { AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[GlobalError]', error);
  }, [error]);

  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      <AlertTriangle className="h-16 w-16 text-destructive/70" />
      <div>
        <h1 className="text-2xl font-semibold">Đã xảy ra lỗi</h1>
        <p className="mt-2 max-w-md text-sm text-muted-foreground">
          Hệ thống gặp sự cố không mong muốn. Vui lòng thử lại hoặc liên hệ quản trị viên.
        </p>
        {error.digest && (
          <p className="mt-1 text-xs text-muted-foreground">Mã lỗi: {error.digest}</p>
        )}
      </div>
      <div className="flex gap-2">
        <Button onClick={() => reset()}>Thử lại</Button>
        <Button variant="outline" onClick={() => (window.location.href = '/dashboard')}>
          Về trang chủ
        </Button>
      </div>
    </div>
  );
}
