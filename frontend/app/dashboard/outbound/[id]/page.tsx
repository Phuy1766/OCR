'use client';

import { useState } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { ArrowLeft, CheckCircle2, FileText, Send, Stamp, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { DocumentStatusBadge } from '@/components/document-status-badge';
import { VersionTimeline } from '@/components/version-timeline';
import {
  useApproveDept,
  useApproveLeader,
  useIssueOutbound,
  useOutboundDocument,
  useSubmitOutbound,
} from '@/hooks/use-outbound';
import { useDocumentBooks } from '@/hooks/use-master-data';
import { useAuthStore } from '@/stores/auth-store';
import { ApiCallError } from '@/lib/api-client';

export default function OutboundDetailPage() {
  const router = useRouter();
  const params = useParams<{ id: string }>();
  const id = params?.id ?? '';
  const { data, isLoading } = useOutboundDocument(id);

  const submit = useSubmitOutbound(id);
  const approveDept = useApproveDept(id);
  const approveLeader = useApproveLeader(id);
  const issue = useIssueOutbound(id);

  const perms = useAuthStore((s) => s.user?.permissions ?? []);
  const canSubmit = perms.includes('OUTBOUND:SUBMIT');
  const canApproveDept = perms.includes('OUTBOUND:APPROVE_DEPT');
  const canApproveLeader = perms.includes('OUTBOUND:APPROVE_LEADER');
  const canIssue = perms.includes('OUTBOUND:ISSUE');

  const books = useDocumentBooks({
    organizationId: data?.organizationId,
    bookType: 'OUTBOUND',
  });
  const [comment, setComment] = useState('');
  const [bookId, setBookId] = useState<string>('');

  if (isLoading || !data) return <div className="text-sm text-muted-foreground">Đang tải…</div>;

  const wrap = async (action: () => Promise<unknown>) => {
    try {
      await action();
      router.refresh();
    } catch (err) {
      toast.error(err instanceof ApiCallError ? err.message : 'Thao tác thất bại.');
    }
  };

  return (
    <div className="space-y-4">
      <Button variant="ghost" size="sm" asChild>
        <Link href="/dashboard/outbound">
          <ArrowLeft className="mr-1.5 h-3.5 w-3.5" /> Danh sách
        </Link>
      </Button>

      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            {data.bookNumber && (
              <Badge variant="outline">
                Số {data.bookNumber}/{data.bookYear}
              </Badge>
            )}
            <DocumentStatusBadge status={data.status} />
          </div>
          <CardTitle className="mt-2">{data.subject}</CardTitle>
          <CardDescription>
            Tạo ngày {new Date(data.createdAt).toLocaleDateString('vi-VN')}
            {data.issuedDate && ` · Phát hành ${data.issuedDate}`}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {data.summary && (
            <div>
              <div className="text-xs text-muted-foreground">Tóm tắt</div>
              <p className="mt-1 whitespace-pre-line text-sm">{data.summary}</p>
            </div>
          )}

          <div>
            <div className="mb-2 text-sm font-semibold">
              File phiên bản hiện tại ({data.latestFiles.length})
            </div>
            <ul className="space-y-1.5">
              {data.latestFiles.map((f) => (
                <li
                  key={f.id}
                  className="flex items-center gap-2 rounded-md border bg-background p-2 text-sm"
                >
                  <FileText className="h-4 w-4 text-muted-foreground" />
                  <span className="flex-1 truncate">{f.fileName}</span>
                  <Badge variant="secondary">{f.fileRole}</Badge>
                  <span className="text-xs text-muted-foreground">
                    {(f.sizeBytes / 1024).toFixed(0)} KB
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Lịch sử phiên bản</CardTitle>
          <CardDescription>
            Mỗi lần sửa hoặc duyệt sinh ra một bản ghi mới (immutable).
          </CardDescription>
        </CardHeader>
        <CardContent>
          <VersionTimeline versions={data.versions} />
        </CardContent>
      </Card>

      {data.approvals.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Quyết định duyệt</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {data.approvals.map((a) => (
              <div key={a.id} className="rounded-md border p-3 text-sm">
                <div className="flex items-center gap-2">
                  <Badge
                    variant={a.decision === 'APPROVED' ? 'secondary' : 'destructive'}
                  >
                    {a.decision === 'APPROVED' ? 'Đồng ý' : 'Từ chối'}
                  </Badge>
                  <span className="text-xs text-muted-foreground">
                    {a.approvalLevel === 'DEPARTMENT_HEAD' ? 'Cấp phòng' : 'Cấp đơn vị'} ·{' '}
                    {new Date(a.decidedAt).toLocaleString('vi-VN')}
                  </span>
                </div>
                {a.comment && <p className="mt-1 whitespace-pre-line">{a.comment}</p>}
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* ---------- Action panel ---------- */}
      {(canSubmit || canApproveDept || canApproveLeader || canIssue) && !data.recalled && (
        <Card>
          <CardHeader>
            <CardTitle>Thao tác</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {data.status === 'DRAFT' && canSubmit && (
              <Button onClick={() => wrap(submit.mutateAsync.bind(submit))}>
                <Send className="mr-1.5 h-3.5 w-3.5" /> Gửi lên cấp duyệt
              </Button>
            )}

            {(data.status === 'PENDING_DEPT_APPROVAL' && canApproveDept) ||
            (data.status === 'PENDING_LEADER_APPROVAL' && canApproveLeader) ? (
              <div className="space-y-2">
                <Textarea
                  rows={3}
                  placeholder="Ý kiến duyệt (tùy chọn)..."
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                />
                <div className="flex gap-2">
                  <Button
                    onClick={() => {
                      const action =
                        data.status === 'PENDING_DEPT_APPROVAL' ? approveDept : approveLeader;
                      wrap(() => action.mutateAsync({ decision: 'APPROVED', comment }));
                      setComment('');
                    }}
                  >
                    <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" /> Đồng ý
                  </Button>
                  <Button
                    variant="destructive"
                    onClick={() => {
                      const action =
                        data.status === 'PENDING_DEPT_APPROVAL' ? approveDept : approveLeader;
                      wrap(() => action.mutateAsync({ decision: 'REJECTED', comment }));
                      setComment('');
                    }}
                  >
                    <XCircle className="mr-1.5 h-3.5 w-3.5" /> Từ chối, trả về dự thảo
                  </Button>
                </div>
              </div>
            ) : null}

            {data.status === 'APPROVED' && canIssue && (
              <div className="space-y-2">
                <label className="text-sm font-medium">Sổ phát hành</label>
                <select
                  className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                  value={bookId}
                  onChange={(e) => setBookId(e.target.value)}
                >
                  <option value="">— Chọn sổ —</option>
                  {books.data?.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.code} — {b.name}
                    </option>
                  ))}
                </select>
                <Button
                  disabled={!bookId}
                  onClick={() => wrap(() => issue.mutateAsync({ bookId }))}
                >
                  <Stamp className="mr-1.5 h-3.5 w-3.5" /> Cấp số &amp; phát hành
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
