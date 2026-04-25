'use client';

import { useState } from 'react';
import Link from 'next/link';
import { Plus, Search } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Badge } from '@/components/ui/badge';
import { DocumentStatusBadge } from '@/components/document-status-badge';
import { useInboundDocuments } from '@/hooks/use-inbound';

export default function InboundListPage() {
  const [q, setQ] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const { data, isLoading } = useInboundDocuments({ q: q || undefined, page: 0, size: 50 });

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div>
          <CardTitle>Công văn đến</CardTitle>
          <CardDescription>
            Tiếp nhận, đăng ký và theo dõi công văn đến theo NĐ 30/2020/NĐ-CP.
          </CardDescription>
        </div>
        <Button asChild size="sm">
          <Link href="/dashboard/inbound/new">
            <Plus className="mr-1.5 h-3.5 w-3.5" /> Tiếp nhận mới
          </Link>
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            setQ(searchInput);
          }}
          className="flex gap-2"
        >
          <Input
            placeholder="Tìm theo trích yếu hoặc số/ký hiệu..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
          />
          <Button type="submit" variant="outline">
            <Search className="mr-1.5 h-3.5 w-3.5" /> Tìm
          </Button>
        </form>

        {isLoading ? (
          <div className="text-sm text-muted-foreground">Đang tải…</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-24">Số CV</TableHead>
                <TableHead>Trích yếu</TableHead>
                <TableHead>Số/Ký hiệu gốc</TableHead>
                <TableHead>Ngày đến</TableHead>
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
                      href={`/dashboard/inbound/${d.id}`}
                      className="font-medium hover:underline"
                    >
                      {d.subject}
                    </Link>
                    {d.externalIssuer && (
                      <div className="text-xs text-muted-foreground">
                        Từ: {d.externalIssuer}
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {d.externalReferenceNumber ?? '—'}
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {d.receivedDate ?? '—'}
                  </TableCell>
                  <TableCell>
                    <DocumentStatusBadge status={d.status} />
                  </TableCell>
                </TableRow>
              ))}
              {data?.content?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} className="text-center text-muted-foreground">
                    Chưa có công văn đến.
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
