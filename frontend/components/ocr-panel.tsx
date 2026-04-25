'use client';

import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { CheckCircle2, Clock, Loader2, ScanLine, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ApiCallError } from '@/lib/api-client';
import { useAcceptOcr, useOcrJob } from '@/hooks/use-ocr';
import { useAuthStore } from '@/stores/auth-store';
import type { OcrJobStatus } from '@/types/ocr';

const STATUS_LABEL: Record<OcrJobStatus, string> = {
  PENDING: 'Đang xếp hàng',
  PROCESSING: 'Đang xử lý OCR…',
  COMPLETED: 'Đã có kết quả',
  FAILED: 'OCR thất bại',
  TIMEOUT: 'OCR quá thời gian',
  SERVICE_UNAVAILABLE: 'Dịch vụ OCR tạm dừng',
};

const STATUS_VARIANT: Record<OcrJobStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  PENDING: 'outline',
  PROCESSING: 'outline',
  COMPLETED: 'secondary',
  FAILED: 'destructive',
  TIMEOUT: 'destructive',
  SERVICE_UNAVAILABLE: 'destructive',
};

export function OcrPanel({ documentId }: { documentId: string }) {
  const { data: job, isLoading } = useOcrJob(documentId);
  const accept = useAcceptOcr(documentId);
  const canAccept = useAuthStore((s) => s.user?.permissions.includes('OCR:ACCEPT') ?? false);

  // Form state — tự fill từ OCR fields khi nhận được kết quả
  const [refNumber, setRefNumber] = useState('');
  const [issuer, setIssuer] = useState('');
  const [issuedDate, setIssuedDate] = useState('');
  const [subject, setSubject] = useState('');

  useEffect(() => {
    if (job?.result?.fields) {
      const byName = (n: string) =>
        job.result?.fields.find((f) => f.fieldName === n)?.fieldValue ?? '';
      setRefNumber((cur) => cur || byName('external_reference_number'));
      setIssuer((cur) => cur || byName('external_issuer'));
      setIssuedDate((cur) => cur || byName('external_issued_date'));
      setSubject((cur) => cur || byName('subject'));
    }
  }, [job?.result?.fields]);

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>OCR</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">Đang tải…</div>
        </CardContent>
      </Card>
    );
  }
  if (!job) return null;

  const Icon = job.status === 'PROCESSING' || job.status === 'PENDING' ? Loader2 : ScanLine;

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <Icon
            className={`h-4 w-4 ${
              job.status === 'PROCESSING' || job.status === 'PENDING' ? 'animate-spin' : ''
            }`}
          />
          <CardTitle>Kết quả OCR</CardTitle>
          <Badge variant={STATUS_VARIANT[job.status]}>{STATUS_LABEL[job.status]}</Badge>
          {job.result?.confidenceAvg && (
            <span className="text-xs text-muted-foreground">
              Độ tin cậy: {(Number(job.result.confidenceAvg) * 100).toFixed(0)}%
            </span>
          )}
          {job.result?.accepted && (
            <Badge variant="default">
              <CheckCircle2 className="mr-1 h-3 w-3" /> Đã chấp nhận
            </Badge>
          )}
        </div>
        {job.errorMessage && (
          <CardDescription className="text-destructive">
            <XCircle className="mr-1 inline h-3.5 w-3.5" /> {job.errorMessage}
          </CardDescription>
        )}
        {(job.status === 'PENDING' || job.status === 'PROCESSING') && (
          <CardDescription>
            <Clock className="mr-1 inline h-3.5 w-3.5" /> Đang chờ kết quả từ OCR service…
          </CardDescription>
        )}
      </CardHeader>

      {job.status === 'COMPLETED' && job.result && !job.result.accepted && (
        <CardContent className="space-y-3">
          <p className="text-xs text-muted-foreground">
            Văn thư xem xét kết quả, có thể chỉnh trước khi chấp nhận. Sau khi chấp nhận, các
            trường sẽ được áp vào hồ sơ công văn.
          </p>
          <div className="grid gap-3 md:grid-cols-2">
            <div>
              <Label htmlFor="ocr-ref">Số/ký hiệu gốc</Label>
              <Input
                id="ocr-ref"
                value={refNumber}
                onChange={(e) => setRefNumber(e.target.value)}
                disabled={!canAccept}
              />
              <FieldConfidence
                conf={
                  job.result.fields.find((f) => f.fieldName === 'external_reference_number')
                    ?.confidence
                }
              />
            </div>
            <div>
              <Label htmlFor="ocr-date">Ngày ban hành</Label>
              <Input
                id="ocr-date"
                type="date"
                value={issuedDate}
                onChange={(e) => setIssuedDate(e.target.value)}
                disabled={!canAccept}
              />
              <FieldConfidence
                conf={
                  job.result.fields.find((f) => f.fieldName === 'external_issued_date')
                    ?.confidence
                }
              />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="ocr-issuer">Cơ quan ban hành</Label>
              <Input
                id="ocr-issuer"
                value={issuer}
                onChange={(e) => setIssuer(e.target.value)}
                disabled={!canAccept}
              />
              <FieldConfidence
                conf={
                  job.result.fields.find((f) => f.fieldName === 'external_issuer')?.confidence
                }
              />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="ocr-subject">Trích yếu</Label>
              <Textarea
                id="ocr-subject"
                rows={2}
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                disabled={!canAccept}
              />
              <FieldConfidence
                conf={job.result.fields.find((f) => f.fieldName === 'subject')?.confidence}
              />
            </div>
          </div>

          {canAccept && (
            <div className="flex justify-end">
              <Button
                onClick={async () => {
                  try {
                    await accept.mutateAsync({
                      jobId: job.jobId,
                      externalReferenceNumber: refNumber || undefined,
                      externalIssuer: issuer || undefined,
                      externalIssuedDate: issuedDate || undefined,
                      subject: subject || undefined,
                    });
                    toast.success('Đã chấp nhận kết quả OCR + áp metadata vào VB');
                  } catch (err) {
                    toast.error(
                      err instanceof ApiCallError ? err.message : 'Không chấp nhận được',
                    );
                  }
                }}
                disabled={accept.isPending}
              >
                {accept.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" /> Chấp nhận kết quả
              </Button>
            </div>
          )}
        </CardContent>
      )}

      {job.result?.rawText && (
        <CardContent>
          <details className="text-xs">
            <summary className="cursor-pointer text-muted-foreground">Xem text gốc OCR</summary>
            <pre className="mt-2 max-h-64 overflow-auto rounded bg-muted p-3 font-mono text-[11px] leading-tight">
              {job.result.rawText}
            </pre>
          </details>
        </CardContent>
      )}
    </Card>
  );
}

function FieldConfidence({ conf }: { conf?: number | null }) {
  if (conf == null) return null;
  const pct = Number(conf) * 100;
  const color = pct >= 80 ? 'text-emerald-600' : pct >= 60 ? 'text-amber-600' : 'text-destructive';
  return (
    <p className={`mt-1 text-[10px] ${color}`}>OCR confidence: {pct.toFixed(0)}%</p>
  );
}
