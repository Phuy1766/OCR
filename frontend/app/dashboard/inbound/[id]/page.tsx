'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { ArrowLeft, Download, FileText, Loader2, UserPlus, Undo2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Textarea } from '@/components/ui/textarea';
import { DocumentStatusBadge } from '@/components/document-status-badge';
import {
  inboundFileDownloadUrl,
  useInboundDocument,
  useRecallInboundDocument,
} from '@/hooks/use-inbound';
import { useAssignDocument } from '@/hooks/use-workflow';
import { useAuthStore } from '@/stores/auth-store';
import { ApiCallError } from '@/lib/api-client';

export default function InboundDetailPage() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const id = params?.id ?? '';
  const { data, isLoading } = useInboundDocument(id);
  const recall = useRecallInboundDocument(id);
  const assignMut = useAssignDocument(id);
  const canRecall = useAuthStore((s) => s.user?.permissions.includes('INBOUND:RECALL') ?? false);
  const canAssign = useAuthStore((s) => s.user?.permissions.includes('WORKFLOW:ASSIGN') ?? false);
  const [reason, setReason] = useState('');
  const [confirmRecall, setConfirmRecall] = useState(false);
  const [showAssign, setShowAssign] = useState(false);
  const [assignUserId, setAssignUserId] = useState('');
  const [assignDueDate, setAssignDueDate] = useState('');
  const [assignNote, setAssignNote] = useState('');

  if (isLoading || !data) return <div className="text-sm text-muted-foreground">Đang tải…</div>;

  const handleRecall = async () => {
    if (reason.length < 5) {
      toast.error('Lý do thu hồi tối thiểu 5 ký tự.');
      return;
    }
    try {
      await recall.mutateAsync(reason);
      toast.success('Đã thu hồi công văn');
      setConfirmRecall(false);
      router.refresh();
    } catch (err) {
      toast.error(err instanceof ApiCallError ? err.message : 'Không thu hồi được.');
    }
  };

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" asChild>
        <Link href="/dashboard/inbound">
          <ArrowLeft className="mr-1.5 h-3.5 w-3.5" /> Danh sách
        </Link>
      </Button>

      <Card>
        <CardHeader className="flex flex-row items-start justify-between gap-4">
          <div className="flex-1 space-y-1">
            <div className="flex items-center gap-2">
              {data.bookNumber && (
                <Badge variant="outline">
                  Số {data.bookNumber}/{data.bookYear}
                </Badge>
              )}
              <DocumentStatusBadge status={data.status} />
              {data.recalled && data.recalledReason && (
                <span className="text-xs text-destructive">— {data.recalledReason}</span>
              )}
            </div>
            <CardTitle>{data.subject}</CardTitle>
            {data.externalIssuer && (
              <CardDescription>
                Từ: <strong>{data.externalIssuer}</strong>
                {data.externalReferenceNumber && ` (${data.externalReferenceNumber})`}
                {data.externalIssuedDate && ` — ${data.externalIssuedDate}`}
              </CardDescription>
            )}
          </div>
          <div className="flex flex-col gap-2">
            {canAssign && data.status === 'REGISTERED' && !data.recalled && (
              <Button size="sm" onClick={() => setShowAssign(true)}>
                <UserPlus className="mr-1.5 h-3.5 w-3.5" /> Phân công
              </Button>
            )}
            {canRecall && !data.recalled && (
              <Button
                variant="destructive"
                size="sm"
                onClick={() => setConfirmRecall(true)}
              >
                <Undo2 className="mr-1.5 h-3.5 w-3.5" /> Thu hồi
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <dl className="grid gap-3 text-sm md:grid-cols-2">
            <div>
              <dt className="text-xs text-muted-foreground">Ngày đến</dt>
              <dd>{data.receivedDate ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Kênh tiếp nhận</dt>
              <dd>{data.receivedFromChannel ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Hạn xử lý</dt>
              <dd>{data.dueDate ?? '—'}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Người đăng ký</dt>
              <dd className="font-mono text-xs">{data.createdBy ?? '—'}</dd>
            </div>
          </dl>

          {data.summary && (
            <div>
              <div className="text-xs text-muted-foreground">Tóm tắt</div>
              <p className="mt-1 whitespace-pre-line text-sm">{data.summary}</p>
            </div>
          )}

          <div>
            <div className="mb-2 text-sm font-semibold">File đính kèm ({data.files.length})</div>
            <ul className="space-y-1.5">
              {data.files.map((f) => (
                <li
                  key={f.id}
                  className="flex items-center gap-2 rounded-md border bg-background p-2 text-sm"
                >
                  <FileText className="h-4 w-4 text-muted-foreground" />
                  <span className="flex-1 truncate">{f.fileName}</span>
                  <span className="text-xs text-muted-foreground">
                    {(f.sizeBytes / 1024).toFixed(0)} KB
                  </span>
                  <Button asChild variant="outline" size="sm">
                    <a
                      href={inboundFileDownloadUrl(data.id, f.id)}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      <Download className="mr-1.5 h-3.5 w-3.5" /> Tải
                    </a>
                  </Button>
                </li>
              ))}
            </ul>
          </div>
        </CardContent>
      </Card>

      {showAssign && (
        <Card>
          <CardHeader>
            <CardTitle>Phân công xử lý</CardTitle>
            <CardDescription>
              Chỉ định chuyên viên xử lý + hạn xử lý. Người được giao sẽ nhận thông báo.
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid gap-3 md:grid-cols-2">
              <div>
                <Label htmlFor="assignee">User ID người được giao *</Label>
                <Input
                  id="assignee"
                  placeholder="UUID của chuyên viên"
                  value={assignUserId}
                  onChange={(e) => setAssignUserId(e.target.value)}
                />
                <p className="mt-1 text-xs text-muted-foreground">
                  Phase 5 cần nhập UUID. Phase 6+ sẽ có user picker.
                </p>
              </div>
              <div>
                <Label htmlFor="duedate">Hạn xử lý</Label>
                <Input
                  id="duedate"
                  type="date"
                  value={assignDueDate}
                  onChange={(e) => setAssignDueDate(e.target.value)}
                />
              </div>
            </div>
            <div>
              <Label htmlFor="note">Ghi chú</Label>
              <Textarea
                id="note"
                rows={2}
                value={assignNote}
                onChange={(e) => setAssignNote(e.target.value)}
              />
            </div>
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setShowAssign(false)}>
                Hủy
              </Button>
              <Button
                disabled={!assignUserId || assignMut.isPending}
                onClick={async () => {
                  try {
                    await assignMut.mutateAsync({
                      assignedToUserId: assignUserId,
                      dueDate: assignDueDate || undefined,
                      note: assignNote || undefined,
                    });
                    toast.success('Đã phân công');
                    setShowAssign(false);
                    setAssignUserId('');
                    setAssignDueDate('');
                    setAssignNote('');
                    router.refresh();
                  } catch (err) {
                    toast.error(
                      err instanceof ApiCallError ? err.message : 'Phân công thất bại.',
                    );
                  }
                }}
              >
                {assignMut.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Xác nhận phân công
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {confirmRecall && (
        <Card>
          <CardHeader>
            <CardTitle className="text-destructive">Xác nhận thu hồi (BR-11)</CardTitle>
            <CardDescription>
              Công văn đã thu hồi sẽ chuyển trạng thái RECALLED và không thể thao tác tiếp.
              Dữ liệu vẫn được lưu (BR-09).
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Textarea
              rows={3}
              placeholder="Lý do thu hồi (≥ 5 ký tự)..."
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
            <div className="flex justify-end gap-2">
              <Button variant="outline" onClick={() => setConfirmRecall(false)}>
                Hủy
              </Button>
              <Button variant="destructive" onClick={handleRecall} disabled={recall.isPending}>
                Xác nhận thu hồi
              </Button>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
