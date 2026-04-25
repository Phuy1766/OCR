export type DocumentStatus =
  | 'RECEIVED'
  | 'REGISTERED'
  | 'OCR_PENDING'
  | 'OCR_COMPLETED'
  | 'ASSIGNED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'ARCHIVED'
  | 'RECALLED'
  | 'DRAFT'
  | 'PENDING_DEPT_APPROVAL'
  | 'PENDING_LEADER_APPROVAL'
  | 'APPROVED'
  | 'PENDING_SIGN'
  | 'SIGNED'
  | 'ISSUED'
  | 'SENT'
  | 'REJECTED';

export type ReceivedFromChannel =
  | 'POST'
  | 'EMAIL'
  | 'SCAN'
  | 'HAND_DELIVERED'
  | 'OTHER';

export type DocumentFileRole =
  | 'ORIGINAL_SCAN'
  | 'ATTACHMENT'
  | 'DRAFT_PDF'
  | 'FINAL_PDF'
  | 'SIGNED';

export interface DocumentFileMeta {
  id: string;
  documentId: string;
  fileRole: DocumentFileRole;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  sha256: string;
  uploadedAt: string;
}

export interface InboundDocument {
  id: string;
  documentTypeId: string;
  confidentialityLevelId: string;
  priorityLevelId: string;
  subject: string;
  summary: string | null;
  status: DocumentStatus;
  bookId: string | null;
  bookYear: number | null;
  bookNumber: number | null;
  receivedDate: string | null;
  receivedFromChannel: ReceivedFromChannel | null;
  externalReferenceNumber: string | null;
  externalIssuer: string | null;
  externalIssuedDate: string | null;
  currentHandlerUserId: string | null;
  currentHandlerDeptId: string | null;
  dueDate: string | null;
  organizationId: string;
  departmentId: string | null;
  recalled: boolean;
  recalledAt: string | null;
  recalledReason: string | null;
  createdAt: string;
  createdBy: string | null;
  files: DocumentFileMeta[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
