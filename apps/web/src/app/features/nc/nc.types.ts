import { SpringPage } from '../pdca/pdca.types';

export type NcCategory =
  | 'PRODUCT'
  | 'PROCESS'
  | 'DOCUMENTATION'
  | 'SUPPLIER'
  | 'SAFETY'
  | 'ENVIRONMENT'
  | 'OTHER';

export type NcSeverity = 'MINOR' | 'MAJOR' | 'CRITICAL';

export type NcStatus =
  | 'OPEN'
  | 'UNDER_ANALYSIS'
  | 'ACTION_DEFINED'
  | 'RESOLVED'
  | 'CLOSED'
  | 'CANCELLED';

export interface NcResponse {
  id: string;
  tenantId?: string;
  reference: string;
  title: string;
  description?: string;
  category: NcCategory;
  severity: NcSeverity;
  status: NcStatus;
  detectedAt: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne (chaîne unique côté backend). */
  photoUrls?: string;
  reporterId?: string;
  capaCaseId?: string;
  rootCause?: string;
  resolutionNote?: string;
  resolvedAt?: string;
  closedAt?: string;
  createdAt: string;
  updatedAt: string;
  /** true = saisie hors-ligne, en attente de synchronisation (réponse optimiste). */
  pendingSync?: boolean;
}

export type NcPage = SpringPage<NcResponse>;

export interface CreateNcRequest {
  title: string;
  description?: string;
  category: NcCategory;
  severity: NcSeverity;
  detectedAt?: string;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  /** URLs des photos, une par ligne. */
  photoUrls?: string;
  reporterId?: string;
}

export interface UpdateNcRequest {
  title?: string;
  description?: string;
  category?: NcCategory;
  severity?: NcSeverity;
  zone?: string;
  geoLat?: number;
  geoLng?: number;
  photoUrls?: string;
}

export interface ResolveNcRequest {
  resolutionNote: string;
}

export interface StartAnalysisNcRequest {
  rootCause?: string;
}

export interface EscalateCapaNcRequest {
  ownerId: string;
}
