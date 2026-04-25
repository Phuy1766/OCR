'use client';

import { useQuery } from '@tanstack/react-query';
import { apiClient, unwrap } from '@/lib/api-client';
import type { ApiResponse } from '@/types/api';
import type { DashboardStats } from '@/types/dashboard';

export function useDashboardStats() {
  return useQuery({
    queryKey: ['dashboard', 'stats'],
    queryFn: () =>
      unwrap(apiClient.get<ApiResponse<DashboardStats>>('/dashboard/stats')),
    staleTime: 30_000,
  });
}
