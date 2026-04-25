'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { PageResponse } from '@/types/document';
import type { ApprovalDecision, OutboundDocument } from '@/types/outbound';

export function useOutboundDocuments(params?: { page?: number; size?: number }) {
  return useQuery({
    queryKey: ['outbound-documents', params],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<PageResponse<OutboundDocument>>>(
          '/outbound-documents',
          { params },
        ),
      ),
  });
}

export function useOutboundDocument(id: string | null) {
  return useQuery({
    queryKey: ['outbound-documents', id],
    enabled: !!id,
    queryFn: () =>
      unwrap(apiClient.get<ApiResponse<OutboundDocument>>(`/outbound-documents/${id}`)),
  });
}

export function useCreateOutboundDraft() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: { data: object; files: File[] }) => {
      const fd = new FormData();
      fd.append('data', new Blob([JSON.stringify(input.data)], { type: 'application/json' }));
      for (const f of input.files) fd.append('files', f);
      return unwrap(
        apiClient.post<ApiResponse<OutboundDocument>>('/outbound-documents', fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
        }),
      );
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['outbound-documents'] }),
  });
}

export function useSubmitOutbound(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => apiClient.post(`/outbound-documents/${id}/submit`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['outbound-documents', id] });
      qc.invalidateQueries({ queryKey: ['outbound-documents'] });
    },
  });
}

export function useApproveDept(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { decision: ApprovalDecision; comment?: string }) =>
      apiClient.post(`/outbound-documents/${id}/approvals/dept`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['outbound-documents', id] }),
  });
}

export function useApproveLeader(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { decision: ApprovalDecision; comment?: string }) =>
      apiClient.post(`/outbound-documents/${id}/approvals/leader`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['outbound-documents', id] }),
  });
}

export function useIssueOutbound(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { bookId: string; issuedDate?: string }) =>
      apiClient.post(`/outbound-documents/${id}/issue`, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['outbound-documents', id] }),
  });
}
