'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Loader2, LogOut } from 'lucide-react';
import { useAuthStore } from '@/stores/auth-store';
import { useLogout, useMe } from '@/hooks/use-auth';
import { Button } from '@/components/ui/button';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const accessToken = useAuthStore((s) => s.accessToken);
  const user = useAuthStore((s) => s.user);

  // Nếu chưa có token trong store (F5 hoặc lần đầu vào), thử refresh qua cookie.
  const me = useMe(!accessToken || !user);
  const logout = useLogout();

  useEffect(() => {
    // Sau khi useMe chạy xong: nếu vẫn chưa có user → chuyển /login.
    if (!accessToken && !me.isLoading && me.isError) {
      router.replace('/login');
    }
  }, [accessToken, me.isError, me.isLoading, router]);

  const currentUser = user ?? me.data ?? null;

  if (!currentUser) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-muted/30">
      <header className="border-b bg-background">
        <div className="container flex h-14 items-center justify-between">
          <h1 className="text-sm font-semibold">Hệ thống quản lý công văn</h1>
          <div className="flex items-center gap-3">
            <div className="text-right text-xs leading-tight">
              <div className="font-medium">{currentUser.fullName}</div>
              <div className="text-muted-foreground">{currentUser.roles.join(', ')}</div>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={async () => {
                await logout.mutateAsync();
                router.replace('/login');
              }}
            >
              <LogOut className="mr-1.5 h-3.5 w-3.5" /> Đăng xuất
            </Button>
          </div>
        </div>
      </header>
      <main className="container py-8">{children}</main>
    </div>
  );
}
