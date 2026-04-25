'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { PageResponse } from '@/types/document';
import type { Assignment, AssignmentStatus, Notification } from '@/types/workflow';

export function useMyAssignments(status?: AssignmentStatus) {
  return useQuery({
    queryKey: ['assignments', 'me', status ?? 'ALL'],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<PageResponse<Assignment>>>('/assignments/me', {
          params: status ? { status } : {},
        }),
      ),
  });
}

export function useAssignDocument(documentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: {
      assignedToUserId: string;
      assignedToDeptId?: string;
      dueDate?: string;
      note?: string;
    }) =>
      unwrap(
        apiClient.post<ApiResponse<Assignment>>(
          `/inbound-documents/${documentId}/assign`,
          body,
        ),
      ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['inbound-documents'] });
      qc.invalidateQueries({ queryKey: ['assignments'] });
    },
  });
}

export function useCompleteAssignment(assignmentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { resultSummary: string }) =>
      unwrap(
        apiClient.post<ApiResponse<Assignment>>(
          `/assignments/${assignmentId}/complete`,
          body,
        ),
      ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['assignments'] });
      qc.invalidateQueries({ queryKey: ['inbound-documents'] });
    },
  });
}

export function useNotifications(unreadOnly = false) {
  return useQuery({
    queryKey: ['notifications', { unreadOnly }],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<PageResponse<Notification>>>('/notifications', {
          params: { unreadOnly, size: 20 },
        }),
      ),
    refetchInterval: 30_000,
  });
}

export function useUnreadCount() {
  return useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<{ count: number }>>('/notifications/unread-count'),
      ),
    refetchInterval: 30_000,
  });
}

export function useMarkNotificationRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiClient.patch(`/notifications/${id}/read`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });
}

export function useMarkAllRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiClient.post('/notifications/mark-all-read'),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });
}
