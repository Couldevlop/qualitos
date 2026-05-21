import { SpringPage } from '../pdca/pdca.types';

export type IncidentType =
  | 'INJURY' | 'NEAR_MISS' | 'ENVIRONMENTAL'
  | 'SECURITY' | 'PROPERTY_DAMAGE' | 'OTHER';

export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type IncidentStatus =
  | 'REPORTED' | 'INVESTIGATING' | 'MITIGATED' | 'CLOSED' | 'CANCELLED';

export interface IncidentView {
  id: string;
  tenantId: string;
  code: string;
  title: string;
  description?: string;
  type: IncidentType;
  severity: IncidentSeverity;
  status: IncidentStatus;
  occurredAt?: string;
  reportedAt: string;
  mitigatedAt?: string;
  closedAt?: string;
  location?: string;
  personsInvolved?: string;
  rootCause?: string;
  correctiveActions?: string;
  standardsCsv?: string;
  capaCaseId?: string;
  ncId?: string;
  ownerUserId?: string;
  reportedBy: string;
  createdAt: string;
  updatedAt: string;
}

export type IncidentPage = SpringPage<IncidentView>;

export interface ReportRequest {
  code: string;
  title: string;
  description?: string;
  type: IncidentType;
  severity?: IncidentSeverity;
  occurredAt?: string;
  location?: string;
  reportedBy: string;
}

export interface EditRequest {
  title?: string;
  description?: string;
  location?: string;
  personsInvolved?: string;
  severity?: IncidentSeverity;
  standardsCsv?: string;
}

export interface InvestigateRequest { ownerUserId?: string; }

export interface MitigateRequest {
  rootCause: string;
  correctiveActions: string;
}

export interface LinkCapaRequest { capaCaseId: string; }
export interface LinkNcRequest   { ncId: string; }

export interface Statistics {
  tenantId: string;
  reported: number;
  investigating: number;
  mitigated: number;
  closed: number;
  cancelled: number;
  injuries: number;
  nearMisses: number;
  environmental: number;
  security: number;
  propertyDamage: number;
  other: number;
}
