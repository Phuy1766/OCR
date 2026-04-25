'use client';

import Link from 'next/link';
import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { DocumentStatusBadge } from '@/components/document-status-badge';
import { useOutboundDocuments } from '@/hooks/use-outbound';

export default function OutboundListPage() {
  const { data, isLoading } = useOutboundDocuments({ page: 0, size: 50 });

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div>
          <CardTitle>Công văn đi</CardTitle>
          <CardDescription>
            Soạn dự thảo, duyệt cấp phòng → cấp đơn vị → ký số → phát hành.
          </CardDescription>
        </div>
        <Button asChild size="sm">
          <Link href="/dashboard/outbound/new">
            <Plus className="mr-1.5 h-3.5 w-3.5" /> Soạn mới
          </Link>
        </Button>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="text-sm text-muted-foreground">Đang tải…</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-24">Số CV</TableHead>
                <TableHead>Trích yếu</TableHead>
                <TableHead>Phiên bản</TableHead>
                <TableHead>Ngày phát hành</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data?.content?.map((d) => (
                <TableRow key={d.id}>
                  <TableCell>
                    <Badge variant="outline">
                      {d.bookNumber ? `${d.bookNumber}/${d.bookYear}` : '—'}
                    </Badge>
                  </TableCell>
                  <TableCell className="max-w-md">
                    <Link
                      href={`/dashboard/outbound/${d.id}`}
                      className="font-medium hover:underline"
                    >
                      {d.subject}
                    </Link>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    v{d.versions[d.versions.length - 1]?.versionNumber ?? 1}
                    {d.approvedVersionId && ' · đã chốt'}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{d.issuedDate ?? '—'}</TableCell>
                  <TableCell>
                    <DocumentStatusBadge status={d.status} />
                  </TableCell>
                </TableRow>
              ))}
              {data?.content?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    Chưa có công văn đi.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
