'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Bell, CheckCheck } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import {
  useMarkAllRead,
  useMarkNotificationRead,
  useNotifications,
  useUnreadCount,
} from '@/hooks/use-workflow';

export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const unread = useUnreadCount();
  const list = useNotifications();
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllRead();
  const count = unread.data?.count ?? 0;

  return (
    <div className="relative">
      <Button
        variant="ghost"
        size="icon"
        className="relative h-9 w-9"
        onClick={() => setOpen((v) => !v)}
        aria-label="Thông báo"
      >
        <Bell className="h-4 w-4" />
        {count > 0 && (
          <span className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-destructive px-1 text-[10px] font-semibold text-destructive-foreground">
            {count > 99 ? '99+' : count}
          </span>
        )}
      </Button>

      {open && (
        <>
          <button
            type="button"
            aria-label="Đóng"
            className="fixed inset-0 z-30"
            onClick={() => setOpen(false)}
          />
          <div className="absolute right-0 top-11 z-40 w-80 rounded-md border bg-popover shadow-md">
            <div className="flex items-center justify-between border-b p-3">
              <div className="text-sm font-semibold">Thông báo</div>
              {count > 0 && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => markAllRead.mutate()}
                  className="h-7 text-xs"
                >
                  <CheckCheck className="mr-1 h-3 w-3" /> Đánh dấu đã đọc
                </Button>
              )}
            </div>
            <div className="max-h-96 overflow-y-auto">
              {list.data?.content?.length === 0 ? (
                <div className="p-6 text-center text-sm text-muted-foreground">
                  Không có thông báo.
                </div>
              ) : (
                list.data?.content?.slice(0, 10).map((n) => (
                  <Link
                    key={n.id}
                    href={
                      n.entityType === 'documents' && n.entityId
                        ? `/dashboard/inbound/${n.entityId}`
                        : '#'
                    }
                    onClick={() => {
                      if (!n.readAt) markRead.mutate(n.id);
                      setOpen(false);
                    }}
                    className={cn(
                      'block border-b px-3 py-2 text-sm transition hover:bg-accent',
                      !n.readAt && 'bg-accent/30',
                    )}
                  >
                    <div className="flex items-start gap-2">
                      <Badge variant="outline" className="text-[10px]">
                        {n.type}
                      </Badge>
                      {!n.readAt && (
                        <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" />
                      )}
                    </div>
                    <div className="mt-1 line-clamp-2 font-medium">{n.title}</div>
                    {n.body && (
                      <div className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">
                        {n.body}
                      </div>
                    )}
                    <div className="mt-1 text-[10px] text-muted-foreground">
                      {new Date(n.createdAt).toLocaleString('vi-VN')}
                    </div>
                  </Link>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
