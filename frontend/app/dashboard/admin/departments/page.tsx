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
import {
  useCreateDepartment,
  useDepartments,
  useOrganizations,
} from '@/hooks/use-organizations';
import { ApiCallError } from '@/lib/api-client';

type FormValues = { code: string; name: string };

export default function DepartmentsPage() {
  const orgs = useOrganizations();
  const [orgId, setOrgId] = useState<string | null>(null);

  // Khi chưa chọn, auto pick org đầu tiên
  if (!orgId && orgs.data && orgs.data.length > 0) {
    setOrgId(orgs.data[0]!.id);
  }

  const depts = useDepartments(orgId);
  const create = useCreateDepartment();
  const [showForm, setShowForm] = useState(false);
  const { register, handleSubmit, reset, formState } = useForm<FormValues>({
    defaultValues: { code: '', name: '' },
  });

  const onSubmit = handleSubmit(async (values) => {
    if (!orgId) return;
    try {
      await create.mutateAsync({
        organizationId: orgId,
        code: values.code.trim(),
        name: values.name.trim(),
      });
      toast.success('Tạo phòng/ban thành công');
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
          <CardTitle>Phòng / Ban</CardTitle>
          <CardDescription>Chọn tổ chức để xem cấu trúc phòng ban trực thuộc.</CardDescription>
        </div>
        <Button size="sm" onClick={() => setShowForm((v) => !v)} disabled={!orgId}>
          <Plus className="mr-1.5 h-3.5 w-3.5" /> Thêm
        </Button>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-3">
          <Label className="whitespace-nowrap">Tổ chức:</Label>
          <select
            className="h-9 min-w-[240px] rounded-md border border-input bg-background px-3 text-sm"
            value={orgId ?? ''}
            onChange={(e) => setOrgId(e.target.value || null)}
          >
            {orgs.data?.map((o) => (
              <option key={o.id} value={o.id}>
                {o.code} — {o.name}
              </option>
            ))}
          </select>
        </div>

        {showForm && orgId && (
          <form
            onSubmit={onSubmit}
            className="grid gap-3 rounded-md border bg-muted/20 p-4 md:grid-cols-3"
          >
            <div>
              <Label htmlFor="code">Mã</Label>
              <Input id="code" {...register('code', { required: true, minLength: 2 })} />
            </div>
            <div className="md:col-span-2">
              <Label htmlFor="name">Tên phòng/ban</Label>
              <Input id="name" {...register('name', { required: true, minLength: 2 })} />
            </div>
            <Button
              type="submit"
              className="md:col-span-3 md:justify-self-start"
              disabled={formState.isSubmitting}
            >
              {formState.isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Tạo phòng/ban
            </Button>
          </form>
        )}

        {depts.isLoading ? (
          <div className="text-sm text-muted-foreground">Đang tải…</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Mã</TableHead>
                <TableHead>Tên</TableHead>
                <TableHead>Cấp trên</TableHead>
                <TableHead>Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {depts.data?.map((d) => (
                <TableRow key={d.id}>
                  <TableCell>
                    <Badge variant="outline">{d.code}</Badge>
                  </TableCell>
                  <TableCell className="font-medium">{d.name}</TableCell>
                  <TableCell className="text-muted-foreground">
                    {d.parentId ? d.parentId.slice(0, 8) : '—'}
                  </TableCell>
                  <TableCell>
                    {d.active ? (
                      <Badge variant="secondary">Hoạt động</Badge>
                    ) : (
                      <Badge variant="destructive">Ngưng</Badge>
                    )}
                  </TableCell>
                </TableRow>
              ))}
              {depts.data?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} className="text-center text-muted-foreground">
                    Chưa có phòng/ban.
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
