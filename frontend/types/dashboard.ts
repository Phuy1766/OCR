export interface DashboardStats {
  counts: {
    inboundThisWeek: number;
    outboundIssued: number;
    myActiveAssignments: number;
    unreadNotifications: number;
  };
  recentInbound: RecentDocument[];
  recentOutbound: RecentDocument[];
  myPendingTasks: MyTask[];
  statusBreakdown: Record<string, number>;
}

export interface RecentDocument {
  id: string;
  direction: 'INBOUND' | 'OUTBOUND';
  status: string;
  subject: string;
  bookNumber: number | null;
  bookYear: number | null;
  createdAt: string;
}

export interface MyTask {
  assignmentId: string;
  documentId: string;
  subject: string;
  dueDate: string | null;
}
