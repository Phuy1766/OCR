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
import { useCreateOrganization, useOrganizations } from '@/hooks/use-organizations';
import { ApiCallError } from '@/lib/api-client';

type FormValues = { code: string; name: string; fullName?: string };

export default function OrganizationsPage() {
  const { data, isLoading } = useOrganizations();
  const create = useCreateOrganization();
  const [showForm, setShowForm] = useState(false);
  const { register, handleSubmit, reset, formState } = useForm<FormValues>({
    defaultValues: { code: '', name: '', fullName: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    try {
      await create.mutateAsync({
        code: values.code.trim(),
        name: values.name.trim(),
        fullName: values.fullName?.trim() || undefined,
      });
      toast.success('Tạo tổ chức thành công');
      reset();
      setShowForm(false);
    } catch (err) {
      toast.error(err instanceof ApiCallError ? err.message : 'Không thể tạo.');
    }
  });

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-start justify-between">
          <div>
            <CardTitle>Tổ chức / Cơ quan</CardTitle>
            <CardDescription>
              Cây tổ chức (cấp trên → cấp dưới). Văn thư cơ quan quản lý sổ theo từng tổ chức.
            </CardDescription>
          </div>
          <Button size="sm" onClick={() => setShowForm((v) => !v)}>
            <Plus className="mr-1.5 h-3.5 w-3.5" /> Thêm
          </Button>
        </CardHeader>
        <CardContent>
          {showForm && (
            <form
              onSubmit={onSubmit}
              className="mb-6 grid gap-3 rounded-md border bg-muted/20 p-4 md:grid-cols-3"
            >
              <div>
                <Label htmlFor="code">Mã</Label>
                <Input id="code" {...register('code', { required: true, minLength: 2 })} />
              </div>
              <div>
                <Label htmlFor="name">Tên</Label>
                <Input id="name" {...register('name', { required: true, minLength: 2 })} />
              </div>
              <div>
                <Label htmlFor="fullName">Tên đầy đủ</Label>
                <Input id="fullName" {...register('fullName')} />
              </div>
              <Button
                type="submit"
                className="md:col-span-3 md:justify-self-start"
                disabled={formState.isSubmitting}
              >
                {formState.isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                Tạo tổ chức
              </Button>
            </form>
          )}

          {isLoading ? (
            <div className="text-sm text-muted-foreground">Đang tải…</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Mã</TableHead>
                  <TableHead>Tên</TableHead>
                  <TableHead>Tên đầy đủ</TableHead>
                  <TableHead>Trạng thái</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data?.map((o) => (
                  <TableRow key={o.id}>
                    <TableCell>
                      <Badge variant="outline">{o.code}</Badge>
                    </TableCell>
                    <TableCell className="font-medium">{o.name}</TableCell>
                    <TableCell className="text-muted-foreground">{o.fullName}</TableCell>
                    <TableCell>
                      {o.active ? (
                        <Badge variant="secondary">Hoạt động</Badge>
                      ) : (
                        <Badge variant="destructive">Ngưng</Badge>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
