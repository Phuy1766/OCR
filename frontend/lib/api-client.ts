import axios, { AxiosError, type AxiosInstance } from 'axios';

const baseURL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api';

export const apiClient: AxiosInstance = axios.create({
  baseURL,
  withCredentials: true,
  timeout: 30_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    // Phase 1 sẽ bổ sung: refresh token, toast lỗi, redirect 401.
    return Promise.reject(error);
  },
);
