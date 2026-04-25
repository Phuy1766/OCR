'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type {
  Certificate,
  CertificateType,
  DigitalSignature,
  SignatureType,
  VerificationResult,
} from '@/types/signature';

export function useCertificates(type?: CertificateType, ownerUserId?: string) {
  return useQuery({
    queryKey: ['certificates', { type, ownerUserId }],
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<Certificate[]>>('/certificates', {
          params: { type, ownerUserId },
        }),
      ),
  });
}

export function useDocumentSignatures(documentId: string | null) {
  return useQuery({
    queryKey: ['signatures', documentId],
    enabled: !!documentId,
    queryFn: () =>
      unwrap(
        apiClient.get<ApiResponse<DigitalSignature[]>>(
          `/outbound-documents/${documentId}/signatures`,
        ),
      ),
  });
}

export function useSignDocument(documentId: string, type: SignatureType) {
  const qc = useQueryClient();
  const path = type === 'PERSONAL' ? 'sign-personal' : 'sign-organization';
  return useMutation({
    mutationFn: (body: {
      certificateId: string;
      pkcs12Password: string;
      reason?: string;
      location?: string;
    }) =>
      unwrap(
        apiClient.post<ApiResponse<DigitalSignature>>(
          `/outbound-documents/${documentId}/${path}`,
          body,
        ),
      ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['signatures', documentId] });
      qc.invalidateQueries({ queryKey: ['outbound-documents', documentId] });
    },
  });
}

export function useVerifySignatures(documentId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () =>
      unwrap(
        apiClient.post<ApiResponse<VerificationResult[]>>(
          `/outbound-documents/${documentId}/signatures/verify`,
        ),
      ),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['signatures', documentId] }),
  });
}
