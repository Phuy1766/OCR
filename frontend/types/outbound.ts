import type { DocumentFileMeta, DocumentStatus } from './document';

export type VersionStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'APPROVED'
  | 'SUPERSEDED'
  | 'REJECTED';

export type ApprovalLevel = 'DEPARTMENT_HEAD' | 'UNIT_LEADER';
export type ApprovalDecision = 'APPROVED' | 'REJECTED';

export interface DocumentVersion {
  id: string;
  documentId: string;
  versionNumber: number;
  parentVersionId: string | null;
  versionStatus: VersionStatus;
  hashSha256: string | null;
  contentSnapshot: string;
  createdAt: string;
  createdBy: string | null;
}

export interface ApprovalRecord {
  id: string;
  documentId: string;
  versionId: string;
  approvalLevel: ApprovalLevel;
  decision: ApprovalDecision;
  comment: string | null;
  decidedBy: string;
  decidedAt: string;
}

export interface OutboundDocument {
  id: string;
  documentTypeId: string;
  confidentialityLevelId: string;
  priorityLevelId: string;
  subject: string;
  summary: string | null;
  status: DocumentStatus;
  approvedVersionId: string | null;
  bookId: string | null;
  bookYear: number | null;
  bookNumber: number | null;
  issuedDate: string | null;
  organizationId: string;
  departmentId: string | null;
  dueDate: string | null;
  recalled: boolean;
  createdAt: string;
  createdBy: string | null;
  latestFiles: DocumentFileMeta[];
  versions: DocumentVersion[];
  approvals: ApprovalRecord[];
}
