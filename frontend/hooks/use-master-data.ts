'use client';

import { useQuery } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  BookType,
  ConfidentialityLevelDto,
  ConfidentialityScope,
  DocumentBookDto,
  DocumentTypeDto,
  PriorityLevelDto,
} from '@/types/master-data';

const FIVE_MIN = 5 * 60_000;

export function useDocumentTypes() {
  return useQuery({
    queryKey: ['master', 'document-types'],
    queryFn: () =>
      unwrap(apiClient.get<ApiResponse<DocumentTypeDto[]>>('/master/document-types')),
    staleTime: FIVE_MIN,
  });
}

export function useConfidentialityLevels() {
  return useQuery({
    queryKey: ['master', 'confidentiality-levels'],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<ConfidentialityLevelDto[]>>('/master/confidentiality-levels'),
      ),
    staleTime: FIVE_MIN,
  });
}

export function usePriorityLevels() {
  return useQuery({
    queryKey: ['master', 'priority-levels'],
    queryFn: () =>
      unwrap(apiClient.get<ApiResponse<PriorityLevelDto[]>>('/master/priority-levels')),
    staleTime: FIVE_MIN,
  });
}

export function useDocumentBooks(params: {
  organizationId?: string;
  bookType?: BookType;
  scope?: ConfidentialityScope;
}) {
  return useQuery({
    queryKey: ['master', 'document-books', params],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<DocumentBookDto[]>>('/master/document-books', { params }),
      ),
  });
}
