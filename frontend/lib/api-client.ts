import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import type { ApiResponse } from '@/types/api';
import type { TokenPairResponse } from '@/types/auth';
import { useAuthStore } from '@/stores/auth-store';

const baseURL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api';

export const apiClient: AxiosInstance = axios.create({
  baseURL,
  withCredentials: true, // gửi kèm HttpOnly cookie chứa refresh token
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Attach Bearer từ Zustand store cho mỗi request (trừ login/refresh).
apiClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken;
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Khi nhận 401 lần đầu → thử gọi /auth/refresh rồi retry.
let refreshInFlight: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  if (refreshInFlight) return refreshInFlight;
  refreshInFlight = (async () => {
    try {
      const { data } = await apiClient.post<ApiResponse<TokenPairResponse>>(
        '/auth/refresh',
        {},
        { headers: { Authorization: '' } as never }, // đừng gửi Bearer cũ
      );
      if (data.success && data.data) {
        useAuthStore.getState().setSession(data.data.accessToken, data.data.user);
        return data.data.accessToken;
      }
      return null;
    } catch {
      return null;
    } finally {
      refreshInFlight = null;
    }
  })();
  return refreshInFlight;
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const status = error.response?.status;
    const original = error.config as InternalAxiosRequestConfig & { _retried?: boolean };

    // Bỏ qua refresh endpoint để không vòng lặp.
    const isRefreshCall = original?.url?.includes('/auth/refresh');
    const isLoginCall = original?.url?.includes('/auth/login');

    if (status === 401 && !original._retried && !isRefreshCall && !isLoginCall) {
      original._retried = true;
      const newToken = await refreshAccessToken();
      if (newToken) {
        original.headers = original.headers ?? {};
        original.headers.Authorization = `Bearer ${newToken}`;
        return apiClient(original);
      }
      useAuthStore.getState().clearSession();
    }
    return Promise.reject(error);
  },
);

/** Unwrap helper: ném lỗi nếu `success=false`, trả `data` nếu OK. */
export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const { data } = await promise;
  if (!data.success || data.data == null) {
    const first = data.errors?.[0];
    throw new ApiCallError(first?.code ?? 'UNKNOWN', first?.message ?? 'Đã xảy ra lỗi.');
  }
  return data.data;
}

export class ApiCallError extends Error {
  constructor(
    public code: string,
    message: string,
  ) {
    super(message);
  }
}
