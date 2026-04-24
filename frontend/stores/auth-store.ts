'use client';

import { create } from 'zustand';
import type { UserDto } from '@/types/auth';

interface AuthState {
  accessToken: string | null;
  user: UserDto | null;
  setSession: (accessToken: string, user: UserDto) => void;
  clearSession: () => void;
  hasPermission: (perm: string) => boolean;
  hasRole: (role: string) => boolean;
}

/**
 * Access token nằm in-memory (Zustand state, không persist). Refresh token
 * nằm HttpOnly cookie do backend set — không đọc được từ JS.
 *
 * Khi user F5, store mất access token → interceptor gọi /api/auth/refresh
 * (cookie tự gửi theo) để lấy access mới.
 */
export const useAuthStore = create<AuthState>((set, get) => ({
  accessToken: null,
  user: null,
  setSession: (accessToken, user) => set({ accessToken, user }),
  clearSession: () => set({ accessToken: null, user: null }),
  hasPermission: (perm) => get().user?.permissions.includes(perm) ?? false,
  hasRole: (role) => get().user?.roles.includes(role) ?? false,
}));
