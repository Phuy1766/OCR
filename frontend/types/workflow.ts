export type AssignmentStatus = 'ACTIVE' | 'COMPLETED' | 'CANCELLED' | 'REASSIGNED';

export type NotificationType =
  | 'ASSIGNMENT'
  | 'APPROVAL_REQUEST'
  | 'DEADLINE_WARNING'
  | 'STATUS_CHANGE'
  | 'DOCUMENT_RECALLED'
  | 'INFO';

export interface Assignment {
  id: string;
  documentId: string;
  workflowId: string;
  assignedToUserId: string;
  assignedToDeptId: string | null;
  assignedBy: string;
  assignedAt: string;
  dueDate: string | null;
  status: AssignmentStatus;
  note: string | null;
  completedAt: string | null;
  completedBy: string | null;
  resultSummary: string | null;
}

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  body: string | null;
  entityType: string | null;
  entityId: string | null;
  metadata: string | null;
  createdAt: string;
  readAt: string | null;
}
