export type OcrJobStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'TIMEOUT'
  | 'SERVICE_UNAVAILABLE';

export interface OcrExtractedField {
  id: string;
  fieldName: string;
  fieldValue: string | null;
  confidence: number | null;
  bbox: string | null;
  pageNumber: number | null;
}

export interface OcrResult {
  id: string;
  rawText: string | null;
  confidenceAvg: number | null;
  processingMs: number | null;
  engineVersion: string | null;
  pageCount: number | null;
  accepted: boolean;
  acceptedAt: string | null;
  acceptedBy: string | null;
  fields: OcrExtractedField[];
}

export interface OcrJob {
  jobId: string;
  documentId: string;
  fileId: string;
  status: OcrJobStatus;
  retryCount: number;
  errorMessage: string | null;
  enqueuedAt: string;
  completedAt: string | null;
  result: OcrResult | null;
}
