'use client';

import { useMe } from '@/hooks/use-auth';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';

export default function DashboardHome() {
  const { data: user, isLoading } = useMe();

  if (isLoading || !user) return <div className="text-sm text-muted-foreground">Đang tải…</div>;

  return (
    <div className="grid gap-4 md:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle>Xin chào, {user.fullName}</CardTitle>
          <CardDescription>
            {user.positionTitle ?? 'Thành viên hệ thống'}
            {user.mustChangePassword && (
              <span className="ml-2 rounded bg-destructive/10 px-2 py-0.5 text-xs text-destructive">
                Bạn cần đổi mật khẩu
              </span>
            )}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-2 text-sm">
          <div>
            <span className="text-muted-foreground">Email: </span>
            {user.email}
          </div>
          <div>
            <span className="text-muted-foreground">Vai trò: </span>
            {user.roles.join(', ') || '—'}
          </div>
          <div>
            <span className="text-muted-foreground">Quyền: </span>
            {user.permissions.length} quyền
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Phase 1 hoàn thành</CardTitle>
          <CardDescription>Các tính năng nghiệp vụ sẽ được bổ sung từ Phase 2.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-1 text-sm text-muted-foreground">
          <div>✓ Đăng nhập / đăng xuất an toàn (JWT RS256)</div>
          <div>✓ Refresh token xoay vòng, chống replay</div>
          <div>✓ Khóa tài khoản sau 5 lần sai (BR-12)</div>
          <div>✓ Phân quyền theo 8 role (NĐ 30/2020 §12)</div>
        </CardContent>
      </Card>
    </div>
  );
}
