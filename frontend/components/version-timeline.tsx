import { GitCommit } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import type { DocumentVersion, VersionStatus } from '@/types/outbound';

const STATUS_LABEL: Record<VersionStatus, string> = {
  DRAFT: 'Đang sửa',
  SUBMITTED: 'Đã gửi duyệt',
  APPROVED: 'Đã duyệt (chốt)',
  SUPERSEDED: 'Bị thay thế',
  REJECTED: 'Bị từ chối',
};

const STATUS_VARIANT: Record<VersionStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  DRAFT: 'outline',
  SUBMITTED: 'secondary',
  APPROVED: 'default',
  SUPERSEDED: 'outline',
  REJECTED: 'destructive',
};

export function VersionTimeline({ versions }: { versions: DocumentVersion[] }) {
  if (versions.length === 0)
    return <p className="text-sm text-muted-foreground">Chưa có version.</p>;
  return (
    <ol className="space-y-3">
      {versions.map((v) => (
        <li key={v.id} className="flex gap-3">
          <div className="mt-1">
            <GitCommit className="h-4 w-4 text-muted-foreground" />
          </div>
          <div className="flex-1 space-y-1">
            <div className="flex items-center gap-2">
              <span className="text-sm font-medium">v{v.versionNumber}</span>
              <Badge variant={STATUS_VARIANT[v.versionStatus]}>
                {STATUS_LABEL[v.versionStatus]}
              </Badge>
              {v.hashSha256 && (
                <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">
                  sha256: {v.hashSha256.slice(0, 12)}…
                </code>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {new Date(v.createdAt).toLocaleString('vi-VN')}
            </p>
          </div>
        </li>
      ))}
    </ol>
  );
}
