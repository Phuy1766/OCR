export type CertificateType = 'PERSONAL' | 'ORGANIZATION';
export type SignatureType = 'PERSONAL' | 'ORGANIZATION';

export interface Certificate {
  id: string;
  type: CertificateType;
  ownerUserId: string | null;
  ownerOrganizationId: string | null;
  alias: string;
  subjectDn: string;
  issuerDn: string | null;
  serialNumber: string;
  validFrom: string;
  validTo: string;
  revoked: boolean;
  currentlyValid: boolean;
}

export interface DigitalSignature {
  id: string;
  documentId: string;
  versionId: string;
  signedFileId: string;
  certificateId: string;
  signatureType: SignatureType;
  signerUserId: string;
  signedAt: string;
  reason: string | null;
  location: string | null;
}

export interface VerificationResult {
  signatureId: string;
  valid: boolean;
  failureReason: string | null;
  signerName: string | null;
  certSerial: string | null;
  subjectDn: string | null;
  verifiedAt: string;
}
