'use client';

import { useState } from 'react';
import Link from 'next/link';
import { toast } from 'sonner';
import { CheckCircle2, ClipboardList } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Skeleton } from '@/components/ui/skeleton';
import { EmptyState } from '@/components/empty-state';
import { useCompleteAssignment, useMyAssignments } from '@/hooks/use-workflow';
import { ApiCallError } from '@/lib/api-client';
import type { AssignmentStatus } from '@/types/workflow';

const STATUS_LABEL: Record<AssignmentStatus, string> = {
  ACTIVE: 'Đang xử lý',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
  REASSIGNED: 'Chuyển người khác',
};

const STATUS_VARIANT: Record<AssignmentStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  ACTIVE: 'default',
  COMPLETED: 'secondary',
  CANCELLED: 'outline',
  REASSIGNED: 'outline',
};

function CompleteRow({ id, onDone }: { id: string; onDone: () => void }) {
  const [open, setOpen] = useState(false);
  const [summary, setSummary] = useState('');
  const complete = useCompleteAssignment(id);

  const submit = async () => {
    if (summary.trim().length < 5) {
      toast.error('Tóm tắt kết quả tối thiểu 5 ký tự.');
      return;
    }
    try {
      await complete.mutateAsync({ resultSummary: summary });
      toast.success('Đã hoàn tất');
      setOpen(false);
      setSummary('');
      onDone();
    } catch (err) {
      toast.error(err instanceof ApiCallError ? err.message : 'Không thể hoàn tất.');
    }
  };

  if (!open) {
    return (
      <Button size="sm" variant="outline" onClick={() => setOpen(true)}>
        <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" /> Hoàn tất
      </Button>
    );
  }
  return (
    <div className="space-y-2">
      <Textarea
        rows={2}
        placeholder="Tóm tắt kết quả xử lý (≥ 5 ký tự)..."
        value={summary}
        onChange={(e) => setSummary(e.target.value)}
      />
      <div className="flex gap-2">
        <Button size="sm" onClick={submit} disabled={complete.isPending}>
          Xác nhận
        </Button>
        <Button size="sm" variant="ghost" onClick={() => setOpen(false)}>
          Hủy
        </Button>
      </div>
    </div>
  );
}

export default function TasksPage() {
  const [filter, setFilter] = useState<AssignmentStatus | undefined>('ACTIVE');
  const { data, isLoading, refetch } = useMyAssignments(filter);

  return (
    <Card>
      <CardHeader>
        <CardTitle>Công việc của tôi</CardTitle>
        <CardDescription>
          Các công văn được phân công xử lý. Bấm &quot;Hoàn tất&quot; sau khi xử lý xong.
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex gap-2">
          {(['ACTIVE', 'COMPLETED', undefined] as const).map((s) => (
            <Button
              key={s ?? 'all'}
              variant={filter === s ? 'default' : 'outline'}
              size="sm"
              onClick={() => setFilter(s)}
            >
              {s ? STATUS_LABEL[s] : 'Tất cả'}
            </Button>
          ))}
        </div>

        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : data?.content?.length === 0 ? (
          <EmptyState
            icon={ClipboardList}
            title={filter === 'ACTIVE' ? 'Không có việc đang chờ' : 'Không có công việc nào'}
            description={
              filter === 'ACTIVE'
                ? 'Bạn đã hoàn tất tất cả các công việc được phân công.'
                : undefined
            }
          />
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Công văn</TableHead>
                <TableHead>Hạn xử lý</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead className="w-48">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data?.content?.map((a) => (
                <TableRow key={a.id}>
                  <TableCell>
                    <Link
                      href={`/dashboard/inbound/${a.documentId}`}
                      className="font-mono text-xs hover:underline"
                    >
                      {a.documentId.slice(0, 8)}…
                    </Link>
                    {a.note && (
                      <div className="mt-1 max-w-md truncate text-xs text-muted-foreground">
                        {a.note}
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{a.dueDate ?? '—'}</TableCell>
                  <TableCell>
                    <Badge variant={STATUS_VARIANT[a.status]}>{STATUS_LABEL[a.status]}</Badge>
                  </TableCell>
                  <TableCell>
                    {a.status === 'ACTIVE' ? (
                      <CompleteRow id={a.id} onDone={() => refetch()} />
                    ) : (
                      a.resultSummary && (
                        <div className="text-xs text-muted-foreground line-clamp-2">
                          {a.resultSummary}
                        </div>
                      )
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
