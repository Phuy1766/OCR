'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { DepartmentDto, OrganizationDto } from '@/types/master-data';

export function useOrganizations() {
  return useQuery({
    queryKey: ['organizations'],
    queryFn: () => unwrap(apiClient.get<ApiResponse<OrganizationDto[]>>('/organizations')),
  });
}

export function useCreateOrganization() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: {
      code: string;
      name: string;
      fullName?: string;
      parentId?: string;
    }) =>
      unwrap(apiClient.post<ApiResponse<OrganizationDto>>('/organizations', body)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['organizations'] }),
  });
}

export function useDepartments(organizationId: string | null) {
  return useQuery({
    queryKey: ['departments', organizationId],
    enabled: !!organizationId,
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<DepartmentDto[]>>('/departments', {
          params: { organizationId },
        }),
      ),
  });
}

export function useCreateDepartment() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: {
      organizationId: string;
      code: string;
      name: string;
      parentId?: string;
    }) => unwrap(apiClient.post<ApiResponse<DepartmentDto>>('/departments', body)),
    onSuccess: (_, vars) =>
      qc.invalidateQueries({ queryKey: ['departments', vars.organizationId] }),
  });
}

export function useCreateDocumentBook() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: {
      organizationId: string;
      code: string;
      name: string;
      bookType: 'INBOUND' | 'OUTBOUND';
      confidentialityScope: 'NORMAL' | 'SECRET';
      prefix?: string;
      description?: string;
    }) =>
      unwrap(apiClient.post<ApiResponse<unknown>>('/master/document-books', body)),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['master', 'document-books'] }),
  });
}
