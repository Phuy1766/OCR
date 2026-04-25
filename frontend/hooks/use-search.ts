'use client';

import { useQuery } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { SearchResponse } from '@/types/search';

export interface SearchParams {
  q?: string;
  direction?: 'INBOUND' | 'OUTBOUND';
  status?: string;
  organizationId?: string;
  bookId?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export function useSearchDocuments(params: SearchParams, enabled = true) {
  return useQuery({
    queryKey: ['search', params],
    enabled,
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<SearchResponse>>('/search/documents', { params }),
      ),
  });
}
