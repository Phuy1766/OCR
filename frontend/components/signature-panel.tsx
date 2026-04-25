'use client';

import { useState } from 'react';
import { toast } from 'sonner';
import {
  CheckCircle2,
  FileSignature,
  Loader2,
  ShieldCheck,
  Stamp,
  XCircle,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { ApiCallError } from '@/lib/api-client';
import { useAuthStore } from '@/stores/auth-store';
import {
  useCertificates,
  useDocumentSignatures,
  useSignDocument,
  useVerifySignatures,
} from '@/hooks/use-signature';
import type { SignatureType, VerificationResult } from '@/types/signature';

export function SignaturePanel({ documentId }: { documentId: string }) {
  const { data: signatures } = useDocumentSignatures(documentId);
  const verify = useVerifySignatures(documentId);
  const [verResults, setVerResults] = useState<VerificationResult[] | null>(null);

  const perms = useAuthStore((s) => s.user?.permissions ?? []);
  const canSignPersonal = perms.includes('SIGN:PERSONAL');
  const canSignOrg = perms.includes('SIGN:ORGANIZATION');

  const hasPersonal = signatures?.some((s) => s.signatureType === 'PERSONAL');
  const hasOrg = signatures?.some((s) => s.signatureType === 'ORGANIZATION');

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <FileSignature className="h-4 w-4" /> Chữ ký số
            </CardTitle>
            <CardDescription>
              BR-06/12: VB điện tử cần đủ 2 chữ ký (cá nhân + cơ quan) trước khi phát hành.
            </CardDescription>
          </div>
          {(signatures?.length ?? 0) > 0 && (
            <Button
              variant="outline"
              size="sm"
              onClick={async () => {
                try {
                  const r = await verify.mutateAsync();
                  setVerResults(r);
                  toast.success('Đã xác minh chữ ký');
                } catch (err) {
                  toast.error(
                    err instanceof ApiCallError ? err.message : 'Verify thất bại',
                  );
                }
              }}
              disabled={verify.isPending}
            >
              <ShieldCheck className="mr-1.5 h-3.5 w-3.5" /> Xác minh
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2 text-sm">
          <Badge variant={hasPersonal ? 'default' : 'outline'}>
            {hasPersonal ? (
              <CheckCircle2 className="mr-1 h-3 w-3" />
            ) : (
              <XCircle className="mr-1 h-3 w-3" />
            )}
            Chữ ký cá nhân
          </Badge>
          <Badge variant={hasOrg ? 'default' : 'outline'}>
            {hasOrg ? (
              <CheckCircle2 className="mr-1 h-3 w-3" />
            ) : (
              <XCircle className="mr-1 h-3 w-3" />
            )}
            Chữ ký cơ quan
          </Badge>
        </div>

        {signatures && signatures.length > 0 && (
          <ul className="space-y-1 text-sm">
            {signatures.map((s) => {
              const v = verResults?.find((r) => r.signatureId === s.id);
              return (
                <li key={s.id} className="flex items-center gap-2 rounded border p-2">
                  {s.signatureType === 'PERSONAL' ? (
                    <FileSignature className="h-4 w-4 text-muted-foreground" />
                  ) : (
                    <Stamp className="h-4 w-4 text-muted-foreground" />
                  )}
                  <div className="flex-1">
                    <div className="font-medium">
                      {s.signatureType === 'PERSONAL' ? 'Chữ ký cá nhân' : 'Chữ ký cơ quan'}
                      {s.reason && (
                        <span className="text-muted-foreground"> · {s.reason}</span>
                      )}
                    </div>
                    <div className="text-xs text-muted-foreground">
                      {new Date(s.signedAt).toLocaleString('vi-VN')}
                      {s.location && ` · ${s.location}`}
                    </div>
                    {v && (
                      <div
                        className={`mt-1 text-xs ${
                          v.valid ? 'text-emerald-600' : 'text-destructive'
                        }`}
                      >
                        {v.valid ? '✓ Hợp lệ' : `✗ ${v.failureReason}`}
                        {v.subjectDn && ` · ${v.subjectDn}`}
                      </div>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}

        {!hasPersonal && canSignPersonal && (
          <SignForm documentId={documentId} type="PERSONAL" />
        )}
        {hasPersonal && !hasOrg && canSignOrg && (
          <SignForm documentId={documentId} type="ORGANIZATION" />
        )}
      </CardContent>
    </Card>
  );
}

function SignForm({ documentId, type }: { documentId: string; type: SignatureType }) {
  const user = useAuthStore((s) => s.user);
  const ownerFilter =
    type === 'PERSONAL' && user ? user.id : undefined;
  const certs = useCertificates(type, ownerFilter);
  const sign = useSignDocument(documentId, type);
  const [certId, setCertId] = useState('');
  const [password, setPassword] = useState('');
  const [reason, setReason] = useState(
    type === 'PERSONAL' ? 'Phê duyệt văn bản' : 'Đóng dấu cơ quan',
  );

  const submit = async () => {
    if (!certId || !password) {
      toast.error('Chọn cert và nhập mật khẩu PKCS#12');
      return;
    }
    try {
      await sign.mutateAsync({
        certificateId: certId,
        pkcs12Password: password,
        reason,
      });
      toast.success(`Ký ${type === 'PERSONAL' ? 'cá nhân' : 'cơ quan'} thành công`);
      setPassword('');
    } catch (err) {
      toast.error(err instanceof ApiCallError ? err.message : 'Ký thất bại');
    }
  };

  return (
    <div className="space-y-2 rounded-md border bg-muted/20 p-3">
      <div className="text-sm font-medium">
        {type === 'PERSONAL' ? 'Ký cá nhân' : 'Ký cơ quan'}
      </div>
      <div className="grid gap-2 md:grid-cols-2">
        <div>
          <Label className="text-xs">Certificate</Label>
          <select
            className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm"
            value={certId}
            onChange={(e) => setCertId(e.target.value)}
          >
            <option value="">— Chọn cert —</option>
            {certs.data?.map((c) => (
              <option key={c.id} value={c.id} disabled={!c.currentlyValid}>
                {c.alias} ({c.serialNumber.slice(0, 12)}…)
                {!c.currentlyValid && ' · Hết hạn'}
              </option>
            ))}
          </select>
        </div>
        <div>
          <Label className="text-xs">Mật khẩu PKCS#12</Label>
          <Input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Mật khẩu file .p12"
          />
        </div>
        <div className="md:col-span-2">
          <Label className="text-xs">Lý do</Label>
          <Input value={reason} onChange={(e) => setReason(e.target.value)} />
        </div>
      </div>
      <Button onClick={submit} disabled={sign.isPending}>
        {sign.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
        {type === 'PERSONAL' ? (
          <FileSignature className="mr-1.5 h-3.5 w-3.5" />
        ) : (
          <Stamp className="mr-1.5 h-3.5 w-3.5" />
        )}
        {type === 'PERSONAL' ? 'Ký cá nhân' : 'Ký cơ quan'}
      </Button>
    </div>
  );
}
