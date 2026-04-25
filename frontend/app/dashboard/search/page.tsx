'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Search, Sparkles } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { DocumentStatusBadge } from '@/components/document-status-badge';
import { useSearchDocuments } from '@/hooks/use-search';
import type { DocumentStatus } from '@/types/document';

export default function SearchPage() {
  const [q, setQ] = useState('');
  const [direction, setDirection] = useState<'INBOUND' | 'OUTBOUND' | ''>('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [submitted, setSubmitted] = useState({ q: '', direction: '', fromDate: '', toDate: '' });

  const { data, isLoading } = useSearchDocuments(
    {
      q: submitted.q || undefined,
      direction: (submitted.direction || undefined) as 'INBOUND' | 'OUTBOUND' | undefined,
      fromDate: submitted.fromDate || undefined,
      toDate: submitted.toDate || undefined,
      size: 50,
    },
    Boolean(submitted.q || submitted.direction || submitted.fromDate || submitted.toDate),
  );

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>Tìm kiếm văn bản</CardTitle>
          <CardDescription>
            Tìm full-text trong tiếng Việt (có dấu hoặc không dấu) trên trích yếu, số/ký hiệu,
            cơ quan ban hành và nội dung OCR đã xác nhận.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              setSubmitted({ q, direction, fromDate, toDate });
            }}
            className="space-y-3"
          >
            <div className="flex gap-2">
              <Input
                value={q}
                onChange={(e) => setQ(e.target.value)}
                placeholder='VD: "bổ nhiệm cán bộ" hoặc "15/QĐ-UBND"'
                autoFocus
              />
              <Button type="submit">
                <Search className="mr-1.5 h-3.5 w-3.5" /> Tìm
              </Button>
            </div>

            <div className="grid gap-3 md:grid-cols-3">
              <div>
                <Label>Hướng</Label>
                <select
                  className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                  value={direction}
                  onChange={(e) => setDirection(e.target.value as 'INBOUND' | 'OUTBOUND' | '')}
                >
                  <option value="">Tất cả</option>
                  <option value="INBOUND">Công văn đến</option>
                  <option value="OUTBOUND">Công văn đi</option>
                </select>
              </div>
              <div>
                <Label htmlFor="fromDate">Từ ngày</Label>
                <Input
                  id="fromDate"
                  type="date"
                  value={fromDate}
                  onChange={(e) => setFromDate(e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="toDate">Đến ngày</Label>
                <Input
                  id="toDate"
                  type="date"
                  value={toDate}
                  onChange={(e) => setToDate(e.target.value)}
                />
              </div>
            </div>
          </form>
        </CardContent>
      </Card>

      {isLoading && <div className="text-sm text-muted-foreground">Đang tìm…</div>}

      {data && (
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-base">
                Tìm thấy {data.totalElements} kết quả
              </CardTitle>
              {data.fuzzyFallback && (
                <Badge variant="outline">
                  <Sparkles className="mr-1 h-3 w-3" /> Gợi ý gần đúng (typo tolerance)
                </Badge>
              )}
            </div>
          </CardHeader>
          <CardContent>
            {data.hits.length === 0 ? (
              <p className="text-sm text-muted-foreground">Không có kết quả phù hợp.</p>
            ) : (
              <ul className="space-y-3">
                {data.hits.map((h) => {
                  const detailHref =
                    h.direction === 'INBOUND'
                      ? `/dashboard/inbound/${h.documentId}`
                      : `/dashboard/outbound/${h.documentId}`;
                  return (
                    <li key={h.documentId} className="rounded-md border p-3">
                      <div className="flex flex-wrap items-center gap-2 text-xs text-muted-foreground">
                        <Badge variant="outline">
                          {h.direction === 'INBOUND' ? 'Đến' : 'Đi'}
                        </Badge>
                        <DocumentStatusBadge status={h.status as DocumentStatus} />
                        {h.bookNumber && (
                          <Badge variant="secondary">
                            Số {h.bookNumber}/{h.bookYear}
                          </Badge>
                        )}
                        {h.matchSource === 'OCR' && (
                          <Badge variant="outline">Match nội dung OCR</Badge>
                        )}
                        {h.score > 0 && (
                          <span>Score: {h.score.toFixed(3)}</span>
                        )}
                      </div>
                      <Link
                        href={detailHref}
                        className="mt-1 block font-medium hover:underline"
                      >
                        {h.subject}
                      </Link>
                      {h.headline && h.headline !== h.subject && (
                        <p
                          className="mt-1 text-sm text-muted-foreground"
                          // ts_headline trả HTML đã escape entities + bọc <mark>
                          dangerouslySetInnerHTML={{ __html: h.headline }}
                        />
                      )}
                      {(h.externalReferenceNumber || h.externalIssuer) && (
                        <div className="mt-1 text-xs text-muted-foreground">
                          {h.externalReferenceNumber && (
                            <span>Số/ký hiệu: {h.externalReferenceNumber}</span>
                          )}
                          {h.externalIssuer && (
                            <span> · Từ: {h.externalIssuer}</span>
                          )}
                        </div>
                      )}
                    </li>
                  );
                })}
              </ul>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
