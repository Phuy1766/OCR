export interface ApiError {
  code: string;
  field: string | null;
  message: string;
}

export interface ApiMeta {
  page?: number;
  size?: number;
  totalElements?: number;
  totalPages?: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  errors: ApiError[] | null;
  meta: ApiMeta | null;
  timestamp: string;
}
