import { Badge } from '@/components/ui/badge';
import type { DocumentStatus } from '@/types/document';

const STATUS_LABEL: Record<DocumentStatus, string> = {
  RECEIVED: 'Vừa nhận',
  REGISTERED: 'Đã đăng ký',
  OCR_PENDING: 'Chờ OCR',
  OCR_COMPLETED: 'Đã OCR',
  ASSIGNED: 'Đã phân công',
  IN_PROGRESS: 'Đang xử lý',
  COMPLETED: 'Hoàn tất',
  ARCHIVED: 'Đã lưu trữ',
  RECALLED: 'Đã thu hồi',
  DRAFT: 'Dự thảo',
  PENDING_DEPT_APPROVAL: 'Chờ duyệt phòng',
  PENDING_LEADER_APPROVAL: 'Chờ duyệt lãnh đạo',
  APPROVED: 'Đã duyệt',
  PENDING_SIGN: 'Chờ ký số',
  SIGNED: 'Đã ký số',
  ISSUED: 'Đã phát hành',
  SENT: 'Đã gửi',
  REJECTED: 'Đã từ chối',
};

const STATUS_VARIANT: Record<DocumentStatus, 'default' | 'secondary' | 'destructive' | 'outline'> =
  {
    RECEIVED: 'outline',
    REGISTERED: 'secondary',
    OCR_PENDING: 'outline',
    OCR_COMPLETED: 'secondary',
    ASSIGNED: 'default',
    IN_PROGRESS: 'default',
    COMPLETED: 'secondary',
    ARCHIVED: 'outline',
    RECALLED: 'destructive',
    DRAFT: 'outline',
    PENDING_DEPT_APPROVAL: 'outline',
    PENDING_LEADER_APPROVAL: 'outline',
    APPROVED: 'secondary',
    PENDING_SIGN: 'outline',
    SIGNED: 'default',
    ISSUED: 'default',
    SENT: 'secondary',
    REJECTED: 'destructive',
  };

export function DocumentStatusBadge({ status }: { status: DocumentStatus }) {
  return <Badge variant={STATUS_VARIANT[status]}>{STATUS_LABEL[status]}</Badge>;
}
