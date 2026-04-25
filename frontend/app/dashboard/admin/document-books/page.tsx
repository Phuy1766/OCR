'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { toast } from 'sonner';
import { Loader2, Plus } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
import { useDocumentBooks } from '@/hooks/use-master-data';
import { useCreateDocumentBook, useOrganizations } from '@/hooks/use-organizations';
import { ApiCallError } from '@/lib/api-client';
import type { BookType, ConfidentialityScope } from '@/types/master-data';

type FormValues = {
  code: string;
  name: string;
  bookType: BookType;
  confidentialityScope: ConfidentialityScope;
  prefix?: string;
};

export default function DocumentBooksPage() {
  const orgs = useOrganizations();
  const [orgId, setOrgId] = useState<string | null>(null);
  const [filterType, setFilterType] = useState<BookType | ''>('');
  if (!orgId && orgs.data && orgs.data.length > 0) setOrgId(orgs.data[0]!.id);

  const books = useDocumentBooks({
    organizationId: orgId ?? undefined,
    bookType: filterType || undefined,
  });
  const create = useCreateDocumentBook();
  const [showForm, setShowForm] = useState(false);
  const { register, handleSubmit, reset, formState } = useForm<FormValues>({
    defaultValues: {
      code: '',
      name: '',
      bookType: 'OUTBOUND',
      confidentialityScope: 'NORMAL',
    },
  });

  const onSubmit = handleSubmit(async (values) => {
    if (!orgId) return;
    try {
      await create.mutateAsync({
        organizationId: orgId,
        code: values.code.trim(),
        name: values.name.trim(),
        bookType: values.bookType,
        confidentialityScope: values.confidentialityScope,
        prefix: values.prefix?.trim() || undefined,
      });
      toast.success('Tạo sổ đăng ký thành công');
      reset();
      setShowForm(false);
    } catch (err) {
      toast.error(err instanceof ApiCallError ? err.message : 'Không thể tạo.');
    }
  });

  return (
    <Card>
      <CardHeader className="flex flex-row items-start justify-between gap-4">
        <div className="flex-1">
          <CardTitle>Sổ đăng ký công văn</CardTitle>
          <CardDescription>
            Mỗi tổ chức có thể có nhiều sổ — sổ thường và sổ mật (BR-03). Số reset đầu năm.
          </CardDescription>
        </div>
        <Button size="sm" onClick={() => setShowForm((v) => !v)} disabled={!orgId}>
          <Plus className="mr-1.5 h-3.5 w-3.5" /> Thêm sổ
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <Label className="whitespace-nowrap">Tổ chức:</Label>
          <select
            className="h-9 rounded-md border border-input bg-background px-3 text-sm"
            value={orgId ?? ''}
            onChange={(e) => setOrgId(e.target.value || null)}
          >
            {orgs.data?.map((o) => (
              <option key={o.id} value={o.id}>
                {o.code} — {o.name}
              </option>
            ))}
          </select>
          <Label className="whitespace-nowrap">Loại:</Label>
          <select
            className="h-9 rounded-md border border-input bg-background px-3 text-sm"
            value={filterType}
            onChange={(e) => setFilterType(e.target.value as BookType | '')}
          >
            <option value="">Tất cả</option>
            <option value="INBOUND">Công văn đến</option>
            <option value="OUTBOUND">Công văn đi</option>
          </select>
        </div>

        {showForm && orgId && (
          <form
            onSubmit={onSubmit}
            className="grid gap-3 rounded-md border bg-muted/20 p-4 md:grid-cols-3"
          >
            <div>
              <Label htmlFor="code">Mã sổ</Label>
              <Input id="code" {...register('code', { required: true, minLength: 2 })} />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="name">Tên sổ</Label>
              <Input id="name" {...register('name', { required: true, minLength: 2 })} />
            </div>
            <div>
              <Label>Loại</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('bookType')}
              >
                <option value="INBOUND">Công văn đến</option>
                <option value="OUTBOUND">Công văn đi</option>
              </select>
            </div>
            <div>
              <Label>Phạm vi mật</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('confidentialityScope')}
              >
                <option value="NORMAL">Thường</option>
                <option value="SECRET">Sổ mật (BR-03)</option>
              </select>
            </div>
            <div>
              <Label htmlFor="prefix">Prefix</Label>
              <Input id="prefix" {...register('prefix')} placeholder="VD: QĐ-UBND" />
            </div>
            <Button
              type="submit"
              className="md:col-span-3 md:justify-self-start"
              disabled={formState.isSubmitting}
            >
              {formState.isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Tạo sổ
            </Button>
          </form>
        )}

        {books.isLoading ? (
          <div className="text-sm text-muted-foreground">Đang tải…</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Mã</TableHead>
                <TableHead>Tên</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead>Phạm vi</TableHead>
                <TableHead>Prefix</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {books.data?.map((b) => (
                <TableRow key={b.id}>
                  <TableCell>
                    <Badge variant="outline">{b.code}</Badge>
                  </TableCell>
                  <TableCell className="font-medium">{b.name}</TableCell>
                  <TableCell>
                    <Badge variant="secondary">
                      {b.bookType === 'INBOUND' ? 'Đến' : 'Đi'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {b.confidentialityScope === 'SECRET' ? (
                      <Badge variant="destructive">Mật</Badge>
                    ) : (
                      <span className="text-muted-foreground">Thường</span>
                    )}
                  </TableCell>
                  <TableCell className="text-muted-foreground">{b.prefix ?? '—'}</TableCell>
                  <TableCell>
                    {b.active ? (
                      <Badge variant="secondary">Hoạt động</Badge>
                    ) : (
                      <Badge variant="destructive">Khóa</Badge>
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {books.data?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground">
                    Chưa có sổ đăng ký.
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
