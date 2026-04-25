'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { OcrJob } from '@/types/ocr';

/** Fetch OCR job mới nhất của document. Refetch mỗi 3s khi đang PENDING/PROCESSING. */
export function useOcrJob(documentId: string | null) {
  return useQuery({
    queryKey: ['ocr', documentId],
    enabled: !!documentId,
    queryFn: async () => {
      // API trả ApiResponse<OcrJob | null>
      const r = await apiClient.get<ApiResponse<OcrJob | null>>(
        `/inbound-documents/${documentId}/ocr`,
      );
      // Không dùng unwrap vì data có thể null hợp lệ
      if (!r.data.success) {
        throw new Error(r.data.errors?.[0]?.message ?? 'Không tải được OCR job');
      }
      return r.data.data;
    },
    refetchInterval: (query) => {
      const data = query.state.data;
      if (!data) return false;
      if (data.status === 'PENDING' || data.status === 'PROCESSING') return 3000;
      return false;
    },
  });
}

export function useAcceptOcr(documentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: {
      jobId: string;
      externalReferenceNumber?: string;
      externalIssuer?: string;
      externalIssuedDate?: string;
      subject?: string;
      summary?: string;
    }) => {
      const { jobId, ...body } = input;
      return apiClient.post(`/ocr-jobs/${jobId}/accept`, body);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['ocr', documentId] });
      qc.invalidateQueries({ queryKey: ['inbound-documents', documentId] });
    },
  });
}
