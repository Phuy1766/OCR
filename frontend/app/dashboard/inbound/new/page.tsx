'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { FileDropzone } from '@/components/file-dropzone';
import { inboundCreateSchema, type InboundCreateValues } from '@/schemas/inbound';
import {
  useConfidentialityLevels,
  useDocumentBooks,
  useDocumentTypes,
  usePriorityLevels,
} from '@/hooks/use-master-data';
import { useOrganizations } from '@/hooks/use-organizations';
import { useCreateInboundDocument } from '@/hooks/use-inbound';
import { ApiCallError } from '@/lib/api-client';

export default function InboundNewPage() {
  const router = useRouter();
  const orgs = useOrganizations();
  const types = useDocumentTypes();
  const confs = useConfidentialityLevels();
  const priors = usePriorityLevels();

  const [files, setFiles] = useState<File[]>([]);
  const create = useCreateInboundDocument();

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<InboundCreateValues>({
    resolver: zodResolver(inboundCreateSchema),
    defaultValues: {
      receivedFromChannel: 'POST',
    },
  });

  const orgId = watch('organizationId');
  const books = useDocumentBooks({
    organizationId: orgId,
    bookType: 'INBOUND',
  });

  // Auto-pick org đầu tiên
  if (!orgId && orgs.data && orgs.data.length > 0) {
    setValue('organizationId', orgs.data[0]!.id);
  }

  const onSubmit = handleSubmit(async (values) => {
    if (files.length === 0) {
      toast.error('Phải đính kèm ít nhất 1 file scan/PDF (BR-09).');
      return;
    }
    try {
      // Loại bỏ field rỗng
      const data: Record<string, unknown> = {};
      for (const [k, v] of Object.entries(values)) {
        if (v !== undefined && v !== '') data[k] = v;
      }
      const created = await create.mutateAsync({ data, files });
      toast.success(
        `Đã đăng ký số ${created.bookNumber}/${created.bookYear}`,
      );
      router.push(`/dashboard/inbound/${created.id}`);
    } catch (err) {
      toast.error(err instanceof ApiCallError ? err.message : 'Không tạo được.');
    }
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle>Tiếp nhận công văn đến</CardTitle>
        <CardDescription>
          Điền đầy đủ Phụ lục VI NĐ 30/2020 và đính kèm bản scan/PDF gốc.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={onSubmit} className="space-y-4" noValidate>
          <div className="grid gap-4 md:grid-cols-3">
            <div className="md:col-span-3">
              <Label htmlFor="subject">Trích yếu *</Label>
              <Input id="subject" {...register('subject')} />
              {errors.subject && (
                <p className="mt-1 text-xs text-destructive">{errors.subject.message}</p>
              )}
            </div>

            <div>
              <Label>Tổ chức *</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('organizationId')}
              >
                {orgs.data?.map((o) => (
                  <option key={o.id} value={o.id}>
                    {o.code} — {o.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <Label>Loại VB *</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('documentTypeId')}
              >
                <option value="">— Chọn loại —</option>
                {types.data?.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.abbreviation} — {t.name}
                  </option>
                ))}
              </select>
              {errors.documentTypeId && (
                <p className="mt-1 text-xs text-destructive">{errors.documentTypeId.message}</p>
              )}
            </div>

            <div>
              <Label>Sổ đăng ký *</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('bookId')}
              >
                <option value="">— Chọn sổ —</option>
                {books.data?.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.code} — {b.name}{' '}
                    {b.confidentialityScope === 'SECRET' ? '(Mật)' : ''}
                  </option>
                ))}
              </select>
              {errors.bookId && (
                <p className="mt-1 text-xs text-destructive">{errors.bookId.message}</p>
              )}
            </div>

            <div>
              <Label>Mức mật *</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('confidentialityLevelId')}
              >
                <option value="">— Chọn —</option>
                {confs.data?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
              {errors.confidentialityLevelId && (
                <p className="mt-1 text-xs text-destructive">
                  {errors.confidentialityLevelId.message}
                </p>
              )}
            </div>

            <div>
              <Label>Mức khẩn *</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('priorityLevelId')}
              >
                <option value="">— Chọn —</option>
                {priors.data?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
              {errors.priorityLevelId && (
                <p className="mt-1 text-xs text-destructive">
                  {errors.priorityLevelId.message}
                </p>
              )}
            </div>

            <div>
              <Label>Kênh tiếp nhận</Label>
              <select
                className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm"
                {...register('receivedFromChannel')}
              >
                <option value="POST">Bưu điện</option>
                <option value="EMAIL">Email</option>
                <option value="SCAN">Scan trực tiếp</option>
                <option value="HAND_DELIVERED">Trao tay</option>
                <option value="OTHER">Khác</option>
              </select>
            </div>

            <div>
              <Label htmlFor="receivedDate">Ngày đến</Label>
              <Input id="receivedDate" type="date" {...register('receivedDate')} />
            </div>

            <div>
              <Label htmlFor="dueDate">Hạn xử lý</Label>
              <Input id="dueDate" type="date" {...register('dueDate')} />
            </div>

            <div>
              <Label htmlFor="externalReferenceNumber">Số/ký hiệu của bên gửi</Label>
              <Input
                id="externalReferenceNumber"
                {...register('externalReferenceNumber')}
                placeholder="VD: 15/QĐ-UBND"
              />
            </div>

            <div className="md:col-span-2">
              <Label htmlFor="externalIssuer">Cơ quan ban hành</Label>
              <Input id="externalIssuer" {...register('externalIssuer')} />
            </div>

            <div>
              <Label htmlFor="externalIssuedDate">Ngày ban hành</Label>
              <Input
                id="externalIssuedDate"
                type="date"
                {...register('externalIssuedDate')}
              />
            </div>

            <div className="md:col-span-3">
              <Label htmlFor="summary">Tóm tắt nội dung</Label>
              <Textarea id="summary" rows={3} {...register('summary')} />
            </div>

            <div className="md:col-span-3">
              <Label>File đính kèm (bắt buộc — BR-09)</Label>
              <FileDropzone files={files} onChange={setFiles} />
            </div>
          </div>

          <div className="flex justify-end gap-2 border-t pt-4">
            <Button type="button" variant="outline" onClick={() => router.back()}>
              Hủy
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Đăng ký vào sổ
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
}
