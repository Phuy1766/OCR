'use client';

import Link from 'next/link';
import { Inbox, Send, ClipboardCheck, Bell, ArrowRight } from 'lucide-react';
import { useMe } from '@/hooks/use-auth';
import { useDashboardStats } from '@/hooks/use-dashboard';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/empty-state';
import { Badge } from '@/components/ui/badge';

function StatCard({
  icon: Icon,
  label,
  value,
  href,
  loading,
}: {
  icon: typeof Inbox;
  label: string;
  value: number;
  href?: string;
  loading?: boolean;
}) {
  const inner = (
    <Card className="transition-colors hover:border-primary/50">
      <CardContent className="flex items-center gap-4 p-4">
        <div className="rounded-md bg-primary/10 p-2 text-primary">
          <Icon className="h-5 w-5" />
        </div>
        <div className="flex-1">
          <div className="text-xs text-muted-foreground">{label}</div>
          {loading ? (
            <Skeleton className="mt-1 h-7 w-12" />
          ) : (
            <div className="text-2xl font-semibold">{value}</div>
          )}
        </div>
      </CardContent>
    </Card>
  );
  return href ? <Link href={href}>{inner}</Link> : inner;
}

function fmtDate(s: string | null) {
  if (!s) return '—';
  try {
    return new Date(s).toLocaleDateString('vi-VN');
  } catch {
    return s;
  }
}

export default function DashboardHome() {
  const { data: user } = useMe();
  const { data: stats, isLoading } = useDashboardStats();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">
          Xin chào{user ? `, ${user.fullName}` : ''}
        </h1>
        <p className="text-sm text-muted-foreground">
          Tổng quan công việc và công văn của bạn.
          {user?.mustChangePassword && (
            <span className="ml-2 rounded bg-destructive/10 px-2 py-0.5 text-xs text-destructive">
              Bạn cần đổi mật khẩu
            </span>
          )}
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard
          icon={Inbox}
          label="Công văn đến (7 ngày)"
          value={stats?.counts.inboundThisWeek ?? 0}
          href="/dashboard/inbound"
          loading={isLoading}
        />
        <StatCard
          icon={Send}
          label="Công văn đi đã ban hành"
          value={stats?.counts.outboundIssued ?? 0}
          href="/dashboard/outbound"
          loading={isLoading}
        />
        <StatCard
          icon={ClipboardCheck}
          label="Việc đang chờ tôi"
          value={stats?.counts.myActiveAssignments ?? 0}
          href="/dashboard/tasks"
          loading={isLoading}
        />
        <StatCard
          icon={Bell}
          label="Thông báo chưa đọc"
          value={stats?.counts.unreadNotifications ?? 0}
          loading={isLoading}
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle className="text-base">Công văn đến gần đây</CardTitle>
              <CardDescription>5 công văn đến mới nhất</CardDescription>
            </div>
            <Link
              href="/dashboard/inbound"
              className="flex items-center gap-1 text-xs text-primary hover:underline"
            >
              Xem tất cả <ArrowRight className="h-3 w-3" />
            </Link>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : stats?.recentInbound.length ? (
              <ul className="space-y-1">
                {stats.recentInbound.map((d) => (
                  <li key={d.id} className="rounded-md border p-2 text-sm">
                    <Link href={`/dashboard/inbound/${d.id}`} className="block hover:underline">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-[10px]">
                          {d.bookNumber ? `${d.bookNumber}/${d.bookYear}` : 'Chưa số'}
                        </Badge>
                        <span className="line-clamp-1 flex-1">{d.subject}</span>
                      </div>
                      <div className="mt-0.5 text-xs text-muted-foreground">
                        {fmtDate(d.createdAt)} · {d.status}
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState title="Chưa có công văn đến" />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <div>
              <CardTitle className="text-base">Công văn đi gần đây</CardTitle>
              <CardDescription>5 công văn đi mới nhất</CardDescription>
            </div>
            <Link
              href="/dashboard/outbound"
              className="flex items-center gap-1 text-xs text-primary hover:underline"
            >
              Xem tất cả <ArrowRight className="h-3 w-3" />
            </Link>
          </CardHeader>
          <CardContent>
            {isLoading ? (
              <div className="space-y-2">
                {Array.from({ length: 4 }).map((_, i) => (
                  <Skeleton key={i} className="h-10 w-full" />
                ))}
              </div>
            ) : stats?.recentOutbound.length ? (
              <ul className="space-y-1">
                {stats.recentOutbound.map((d) => (
                  <li key={d.id} className="rounded-md border p-2 text-sm">
                    <Link href={`/dashboard/outbound/${d.id}`} className="block hover:underline">
                      <div className="flex items-center gap-2">
                        <Badge variant="outline" className="text-[10px]">
                          {d.bookNumber ? `${d.bookNumber}/${d.bookYear}` : 'Chưa số'}
                        </Badge>
                        <span className="line-clamp-1 flex-1">{d.subject}</span>
                      </div>
                      <div className="mt-0.5 text-xs text-muted-foreground">
                        {fmtDate(d.createdAt)} · {d.status}
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            ) : (
              <EmptyState title="Chưa có công văn đi" />
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex-row items-center justify-between space-y-0">
          <div>
            <CardTitle className="text-base">Việc đang chờ xử lý</CardTitle>
            <CardDescription>Các phân công đang giao cho bạn</CardDescription>
          </div>
          <Link
            href="/dashboard/tasks"
            className="flex items-center gap-1 text-xs text-primary hover:underline"
          >
            Xem tất cả <ArrowRight className="h-3 w-3" />
          </Link>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-2">
              {Array.from({ length: 3 }).map((_, i) => (
                <Skeleton key={i} className="h-10 w-full" />
              ))}
            </div>
          ) : stats?.myPendingTasks.length ? (
            <ul className="space-y-1">
              {stats.myPendingTasks.map((t) => (
                <li key={t.assignmentId} className="rounded-md border p-2 text-sm">
                  <Link
                    href={`/dashboard/tasks/${t.assignmentId}`}
                    className="block hover:underline"
                  >
                    <div className="line-clamp-1">{t.subject}</div>
                    <div className="mt-0.5 text-xs text-muted-foreground">
                      Hạn: {fmtDate(t.dueDate)}
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState title="Bạn không có việc đang chờ" />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
