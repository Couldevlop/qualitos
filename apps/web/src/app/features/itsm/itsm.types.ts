import { SpringPage } from '../pdca/pdca.types';

export type ItsmProvider = 'SERVICENOW' | 'JIRA_SM';

export type ConnectionStatus = 'ACTIVE' | 'DISABLED' | 'DISABLED_ON_ERRORS';

export interface ConnectionResponse {
  id: string;
  tenantId: string;
  name: string;
  provider: ItsmProvider;
  baseUrl: string;
  username?: string;
  externalScope?: string;
  status: ConnectionStatus;
  consecutiveFailures: number;
  lastSyncAt?: string;
  lastSuccessAt?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export type ConnectionPage = SpringPage<ConnectionResponse>;

export interface CreateConnectionRequest {
  name: string;
  provider: ItsmProvider;
  baseUrl: string;       // https only
  username?: string;
  secret: string;        // ≥ 4, ≤ 1024 chars — never echoed back
  externalScope?: string;
  createdBy: string;
}

export interface UpdateConnectionRequest {
  name?: string;
  baseUrl?: string;
  username?: string;
  secret?: string;       // optional on update — only sent when rotating
  externalScope?: string;
  status?: ConnectionStatus;
}

export interface SyncReport {
  connectionId: string;
  totalFetched: number;
  newImports: number;
  alreadyKnown: number;
  ranAt: string;
  errorMessage?: string;
}

export interface MappingResponse {
  id: string;
  tenantId: string;
  connectionId: string;
  externalId: string;
  externalUrl?: string;
  externalStatus?: string;
  externalPriority?: string;
  externalTitle?: string;
  internalEntityType?: string;
  internalEntityId?: string;
  firstImportedAt: string;
  lastSeenAt: string;
}

export type MappingPage = SpringPage<MappingResponse>;
