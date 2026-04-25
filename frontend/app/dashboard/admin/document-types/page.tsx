'use client';

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
import { useDocumentTypes } from '@/hooks/use-master-data';

export default function DocumentTypesPage() {
  const { data, isLoading } = useDocumentTypes();

  return (
    <Card>
      <CardHeader>
        <CardTitle>29 loại văn bản hành chính</CardTitle>
        <CardDescription>
          Phụ lục III NĐ 30/2020/NĐ-CP — danh mục seed hệ thống (không chỉnh sửa qua giao diện).
        </CardDescription>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="text-sm text-muted-foreground">Đang tải…</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-16">#</TableHead>
                <TableHead className="w-20">Viết tắt</TableHead>
                <TableHead>Tên loại VB</TableHead>
                <TableHead>Mô tả</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {data?.map((t, idx) => (
                <TableRow key={t.id}>
                  <TableCell className="text-muted-foreground">{idx + 1}</TableCell>
                  <TableCell>
                    <Badge variant="secondary">{t.abbreviation}</Badge>
                  </TableCell>
                  <TableCell className="font-medium">{t.name}</TableCell>
                  <TableCell className="text-muted-foreground">{t.description}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
