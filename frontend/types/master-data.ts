export type BookType = 'INBOUND' | 'OUTBOUND';
export type ConfidentialityScope = 'NORMAL' | 'SECRET';

export interface DocumentTypeDto {
  id: string;
  code: string;
  abbreviation: string;
  name: string;
  description: string | null;
  displayOrder: number;
  system: boolean;
  active: boolean;
}

export interface ConfidentialityLevelDto {
  id: string;
  code: string;
  name: string;
  level: number;
  color: string | null;
  description: string | null;
  displayOrder: number;
  requiresSecretBook: boolean;
}

export interface PriorityLevelDto {
  id: string;
  code: string;
  name: string;
  level: number;
  color: string | null;
  slaHours: number | null;
  description: string | null;
  displayOrder: number;
  urgent: boolean;
}

export interface DocumentBookDto {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  bookType: BookType;
  confidentialityScope: ConfidentialityScope;
  prefix: string | null;
  description: string | null;
  active: boolean;
}

export interface OrganizationDto {
  id: string;
  code: string;
  name: string;
  fullName: string | null;
  taxCode: string | null;
  address: string | null;
  phone: string | null;
  email: string | null;
  parentId: string | null;
  active: boolean;
}

export interface DepartmentDto {
  id: string;
  organizationId: string;
  code: string;
  name: string;
  parentId: string | null;
  headUserId: string | null;
  active: boolean;
}
