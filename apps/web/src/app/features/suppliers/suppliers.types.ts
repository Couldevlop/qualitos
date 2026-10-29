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
  /** Le fournisseur de la NC — renseigné pour la liste transverse des NC. */
  supplierCode?: string;
  supplierName?: string;
  lotReference?: string;
  description?: string;
  severity: NonConformitySeverity;
  status: NonConformityStatus;
  detectedOn: string;
  resolvedOn?: string;
  resolution?: string;
  /** Référence de la pièce touchée ; absente des NC saisies avant qu'elle soit exigée. */
  partNumber?: string;
  partQuantity?: number;
  eightDRequested: boolean;
  eightDReceived: boolean;
  documentCount: number;
  createdAt: string;
  updatedAt: string;
}

export type NonConformityPage = SpringPage<NonConformityResponse>;

export interface CreateNonConformityRequest {
  /** Ouverte par défaut ; une NC se saisit aussi déjà clôturée ou annulée. */
  status?: NonConformityStatus;
  lotReference?: string;
  description?: string;
  severity: NonConformitySeverity;
  detectedOn: string;
  partNumber: string;
  partQuantity: number;
  eightDRequested?: boolean;
  eightDReceived?: boolean;
}

export interface UpdateNonConformityRequest {
  lotReference?: string;
  description?: string;
  severity?: NonConformitySeverity;
  status?: NonConformityStatus;
  resolvedOn?: string;
  resolution?: string;
  partNumber?: string;
  partQuantity?: number;
  eightDRequested?: boolean;
  eightDReceived?: boolean;
}

/** Filtres de la liste transverse des NC fournisseur. */
export interface NonConformityFilters {
  supplierId?: string;
  status?: NonConformityStatus;
  severity?: NonConformitySeverity;
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
  documentCount: number;
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

/** Ce qui porte un document fournisseur. */
export type SupplierDocumentOwnerType = 'NON_CONFORMITY' | 'CERTIFICATE';

/** Nature d'un document : elle se lit dans la fiche NC imprimée et dans la GED. */
export type SupplierDocumentCategory = 'EIGHT_D' | 'CAPA_EVIDENCE' | 'CERTIFICATE' | 'OTHER';

export interface SupplierDocument {
  id: string;
  supplierId: string;
  ownerType: SupplierDocumentOwnerType;
  ownerId: string;
  category: SupplierDocumentCategory;
  contentType: string;
  sizeBytes: number;
  originalFilename?: string;
  uploadedBy?: string;
  createdAt: string;
  /** Lien de lecture présigné, de courte durée ; absent de la réponse de dépôt. */
  url?: string;
}

/** Ligne de la vue « Documents fournisseurs » de la GED. */
export interface SupplierDocumentCatalogItem extends SupplierDocument {
  supplierCode?: string;
  supplierName?: string;
  /** Référence de pièce de la NC, ou norme et référence du certificat. */
  ownerLabel?: string;
}

export type SupplierDocumentCatalogPage = SpringPage<SupplierDocumentCatalogItem>;

export interface SupplierDocumentFilters {
  supplierId?: string;
  ownerType?: SupplierDocumentOwnerType;
  category?: SupplierDocumentCategory;
  q?: string;
}
