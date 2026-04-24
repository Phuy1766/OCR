'use client';

import { useMutation, useQuery } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import { useAuthStore } from '@/stores/auth-store';
import type { ApiResponse } from '@/types/api';
import type { TokenPairResponse, UserDto } from '@/types/auth';
import type { LoginFormValues } from '@/schemas/auth';

export function useLogin() {
  const setSession = useAuthStore((s) => s.setSession);
  return useMutation({
    mutationFn: async (values: LoginFormValues) => {
      const tokens = await unwrap(
        apiClient.post<ApiResponse<TokenPairResponse>>('/auth/login', values),
      );
      setSession(tokens.accessToken, tokens.user);
      return tokens;
    },
  });
}

export function useLogout() {
  const clearSession = useAuthStore((s) => s.clearSession);
  return useMutation({
    mutationFn: async () => {
      try {
        await apiClient.post('/auth/logout');
      } finally {
        clearSession();
      }
    },
  });
}

/** Gọi /auth/me khi store có token; auto refresh nếu 401 nhờ interceptor. */
export function useMe(enabled = true) {
  const accessToken = useAuthStore((s) => s.accessToken);
  return useQuery({
    queryKey: ['auth', 'me'],
    enabled: enabled && !!accessToken,
    queryFn: () => unwrap(apiClient.get<ApiResponse<UserDto>>('/auth/me')),
  });
}
