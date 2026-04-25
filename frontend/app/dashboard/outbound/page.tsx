'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Plus, Send } from 'lucide-react';
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
import { Skeleton } from '@/components/ui/skeleton';
import { DocumentStatusBadge } from '@/components/document-status-badge';
import { EmptyState } from '@/components/empty-state';
import { Pagination } from '@/components/pagination';
import { useOutboundDocuments } from '@/hooks/use-outbound';

const PAGE_SIZE = 20;

export default function OutboundListPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useOutboundDocuments({ page, size: PAGE_SIZE });

  const total = data?.totalElements ?? 0;

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
      <CardContent className="space-y-4">
        {isLoading ? (
          <div className="space-y-2">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        ) : data?.content?.length === 0 ? (
          <EmptyState
            icon={Send}
            title="Chưa có công văn đi"
            description="Bắt đầu bằng cách soạn dự thảo công văn đi mới."
            action={
              <Button asChild size="sm">
                <Link href="/dashboard/outbound/new">
                  <Plus className="mr-1.5 h-3.5 w-3.5" /> Soạn mới
                </Link>
              </Button>
            }
          />
        ) : (
          <>
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
              </TableBody>
            </Table>
            <Pagination
              page={page}
              size={PAGE_SIZE}
              totalElements={total}
              onPageChange={setPage}
            />
          </>
        )}
      </CardContent>
    </Card>
  );
}
