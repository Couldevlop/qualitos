import { SpringPage } from '../pdca/pdca.types';

export type SupplierStatus =
  | 'PROSPECT' | 'APPROVED' | 'CONDITIONAL' | 'SUSPENDED' | 'BLACKLISTED';

export type SupplierType =
  | 'RAW_MATERIAL' | 'COMPONENT' | 'SERVICE'
  | 'CONTRACT_MANUFACTURER' | 'SOFTWARE' | 'LOGISTICS' | 'OTHER';

export type NonConformitySeverity = 'MINOR' | 'MAJOR' | 'CRITICAL';
export type NonConformityStatus   = 'OPEN' | 'IN_REVIEW' | 'RESOLVED' | 'REJECTED';

export interface SupplierResponse {
  id: string;
  tenantId: string;
  code: string;
  name: string;
  countryCode?: string;
  contactEmail?: string;
  supplierType: SupplierType;
  status: SupplierStatus;
  score: number;                 // 0-100
  lastAuditAt?: string;
  approvedAt?: string;
  approvedBy?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type SupplierPage = SpringPage<SupplierResponse>;

export interface CreateSupplierRequest {
  code: string;
  name: string;
  countryCode?: string;
  contactEmail?: string;
  supplierType: SupplierType;
  createdBy: string;
}

export interface UpdateSupplierRequest {
  name?: string;
  countryCode?: string;
  contactEmail?: string;
  supplierType?: SupplierType;
}

export interface StatusChangeRequest {
  actorUserId: string;
  reason?: string;
}

export interface AuditResponse {
  id: string;
  tenantId: string;
  supplierId: string;
  auditedOn: string;
  score: number;
  auditorUserId?: string;
  findingsSummary?: string;
  criticalFindingsCount: number;
  majorFindingsCount: number;
  minorFindingsCount: number;
  createdAt: string;
}

export type AuditPage = SpringPage<AuditResponse>;

export interface CreateAuditRequest {
  auditedOn: string;
  score: number;
  auditorUserId?: string;
  findingsSummary?: string;
  criticalFindingsCount?: number;
  majorFindingsCount?: number;
  minorFindingsCount?: number;
}

export interface NonConformityResponse {
  id: string;
  tenantId: string;
  supplierId: string;
  lotReference?: string;
  description?: string;
  severity: NonConformitySeverity;
  status: NonConformityStatus;
  detectedOn: string;
  resolvedOn?: string;
  resolution?: string;
  createdAt: string;
  updatedAt: string;
}

export type NonConformityPage = SpringPage<NonConformityResponse>;

export interface CreateNonConformityRequest {
  lotReference?: string;
  description?: string;
  severity: NonConformitySeverity;
  detectedOn: string;
}

export interface UpdateNonConformityRequest {
  lotReference?: string;
  description?: string;
  severity?: NonConformitySeverity;
  status?: NonConformityStatus;
  resolvedOn?: string;
  resolution?: string;
}

export interface CertificateResponse {
  id: string;
  tenantId: string;
  supplierId: string;
  standardCode: string;
  reference?: string;
  issuedOn: string;
  expiresOn: string;
  documentUrl?: string;
  expired: boolean;
  createdAt: string;
  updatedAt: string;
}

export type CertificatePage = SpringPage<CertificateResponse>;

export interface CreateCertificateRequest {
  standardCode: string;
  reference?: string;
  issuedOn: string;
  expiresOn: string;
  documentUrl?: string;
}

export interface SupplierStatistics {
  supplierId: string;
  score: number;
  status: SupplierStatus;
  openNonConformities: number;
  resolvedNonConformitiesRecent: number;
  expiredCertificates: number;
  lastAuditAt?: string;
}
