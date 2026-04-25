export interface SearchHit {
  documentId: string;
  direction: 'INBOUND' | 'OUTBOUND';
  status: string;
  subject: string;
  summary: string | null;
  externalReferenceNumber: string | null;
  externalIssuer: string | null;
  bookNumber: number | null;
  bookYear: number | null;
  receivedDate: string | null;
  issuedDate: string | null;
  organizationId: string;
  departmentId: string | null;
  score: number;
  /** HTML highlight với <mark>...</mark> */
  headline: string;
  /** METADATA | OCR | FUZZY | LIST */
  matchSource: string;
}

export interface SearchResponse {
  hits: SearchHit[];
  totalElements: number;
  page: number;
  size: number;
  fuzzyFallback: boolean;
}
