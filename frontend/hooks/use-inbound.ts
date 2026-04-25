'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { InboundDocument, PageResponse } from '@/types/document';

interface InboundListParams {
  status?: string;
  bookId?: string;
  q?: string;
  page?: number;
  size?: number;
}

export function useInboundDocuments(params: InboundListParams) {
  return useQuery({
    queryKey: ['inbound-documents', params],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<PageResponse<InboundDocument>>>('/inbound-documents', {
          params,
        }),
      ),
  });
}

export function useInboundDocument(id: string | null) {
  return useQuery({
    queryKey: ['inbound-documents', id],
    enabled: !!id,
    queryFn: () =>
      unwrap(apiClient.get<ApiResponse<InboundDocument>>(`/inbound-documents/${id}`)),
  });
}

export function useCreateInboundDocument() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async (input: { data: object; files: File[] }) => {
      const fd = new FormData();
      fd.append(
        'data',
        new Blob([JSON.stringify(input.data)], { type: 'application/json' }),
      );
      for (const f of input.files) fd.append('files', f);
      return unwrap(
        apiClient.post<ApiResponse<InboundDocument>>('/inbound-documents', fd, {
          headers: { 'Content-Type': 'multipart/form-data' },
        }),
      );
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['inbound-documents'] }),
  });
}

export function useRecallInboundDocument(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (reason: string) =>
      apiClient.post(`/inbound-documents/${id}/recall`, { reason }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['inbound-documents'] }),
  });
}

export function inboundFileDownloadUrl(documentId: string, fileId: string): string {
  const base = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api';
  return `${base}/inbound-documents/${documentId}/files/${fileId}/download`;
}
