import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  ConnectionPage,
  ConnectionResponse,
  CreateConnectionRequest,
  MappingPage,
  MappingResponse,
  SyncReport,
  UpdateConnectionRequest
} from './itsm.types';

@Injectable({ providedIn: 'root' })
export class ItsmService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/itsm`;

  private readonly mockConnections: ConnectionResponse[] = this.seedConnections();
  private readonly mockMappings: Record<string, MappingResponse[]> = this.seedMappings();

  constructor(private readonly http: HttpClient) {}

  // ---------- Connections ----------

  list(page = 0, size = 20): Observable<ConnectionPage> {
    if (environment.useMockApi) {
      return of({
        content: this.mockConnections,
        totalElements: this.mockConnections.length,
        totalPages: 1, number: 0, size: this.mockConnections.length
      }).pipe(delay(120));
    }
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ConnectionPage>(`${this.endpoint}/connections`, { params });
  }

  get(id: string): Observable<ConnectionResponse> {
    if (environment.useMockApi) {
      return of(this.mockConnections.find(c => c.id === id) ?? this.mockConnections[0]).pipe(delay(100));
    }
    return this.http.get<ConnectionResponse>(`${this.endpoint}/connections/${id}`);
  }

  create(input: CreateConnectionRequest): Observable<ConnectionResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      // OWASP A02 — we never persist the plaintext secret in mockStore;
      // the backend stores an encrypted version, the API responses never contain it.
      const c: ConnectionResponse = {
        id: 'itsm-' + (this.mockConnections.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        name: input.name, provider: input.provider,
        baseUrl: input.baseUrl, username: input.username,
        externalScope: input.externalScope,
        status: 'ACTIVE', consecutiveFailures: 0,
        createdBy: input.createdBy,
        createdAt: now, updatedAt: now
      };
      this.mockConnections.unshift(c);
      this.mockMappings[c.id] = [];
      return of(c).pipe(delay(150));
    }
    return this.http.post<ConnectionResponse>(`${this.endpoint}/connections`, input);
  }

  update(id: string, input: UpdateConnectionRequest): Observable<ConnectionResponse> {
    if (environment.useMockApi) {
      const c = this.mockConnections.find(x => x.id === id);
      if (c) {
        // OWASP A02 — drop `secret` from the patch we record locally; the
        // backend handles encryption + rotation server-side.
        const { secret: _drop, ...rest } = input;
        Object.assign(c, rest);
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockConnections[0]).pipe(delay(120));
    }
    return this.http.patch<ConnectionResponse>(`${this.endpoint}/connections/${id}`, input);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockConnections.findIndex(c => c.id === id);
      if (i >= 0) this.mockConnections.splice(i, 1);
      delete this.mockMappings[id];
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/connections/${id}`);
  }

  sync(id: string): Observable<SyncReport> {
    if (environment.useMockApi) {
      const c = this.mockConnections.find(x => x.id === id);
      const now = new Date().toISOString();
      if (c) {
        c.lastSyncAt = now;
        c.lastSuccessAt = now;
        c.consecutiveFailures = 0;
        c.updatedAt = now;
      }
      return of({
        connectionId: id,
        totalFetched: 14, newImports: 3, alreadyKnown: 11,
        ranAt: now
      }).pipe(delay(400));
    }
    return this.http.post<SyncReport>(`${this.endpoint}/connections/${id}/sync`, {});
  }

  // ---------- Mappings ----------

  listMappings(connectionId?: string, page = 0, size = 50): Observable<MappingPage> {
    if (environment.useMockApi) {
      const all = connectionId
        ? (this.mockMappings[connectionId] ?? [])
        : Object.values(this.mockMappings).flat();
      return of({
        content: all, totalElements: all.length, totalPages: 1, number: 0, size: all.length
      }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (connectionId) params = params.set('connectionId', connectionId);
    return this.http.get<MappingPage>(`${this.endpoint}/mappings`, { params });
  }

  // ---------- Seeds ----------

  private seedConnections(): ConnectionResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'itsm-1', tenantId: 'demo-tenant',
        name: 'ServiceNow Production',
        provider: 'SERVICENOW',
        baseUrl: 'https://demo.service-now.com',
        username: 'integration_qualitos',
        externalScope: 'incident,problem,change',
        status: 'ACTIVE', consecutiveFailures: 0,
        lastSyncAt: now, lastSuccessAt: now,
        createdBy: 'demo-user', createdAt: now, updatedAt: now
      },
      {
        id: 'itsm-2', tenantId: 'demo-tenant',
        name: 'Jira SM — Équipe SRE',
        provider: 'JIRA_SM',
        baseUrl: 'https://qualitos.atlassian.net',
        username: 'sre-bot@qualitos.io',
        externalScope: 'project=SRE,issuetype=Incident',
        status: 'ACTIVE', consecutiveFailures: 0,
        lastSyncAt: now, lastSuccessAt: now,
        createdBy: 'demo-user', createdAt: now, updatedAt: now
      },
      {
        id: 'itsm-3', tenantId: 'demo-tenant',
        name: 'Jira SM — Sandbox',
        provider: 'JIRA_SM',
        baseUrl: 'https://qualitos-sandbox.atlassian.net',
        status: 'DISABLED_ON_ERRORS', consecutiveFailures: 5,
        lastSyncAt: now,
        createdBy: 'demo-user', createdAt: now, updatedAt: now
      }
    ];
  }

  private seedMappings(): Record<string, MappingResponse[]> {
    const now = new Date().toISOString();
    return {
      'itsm-1': [
        {
          id: 'map-1', tenantId: 'demo-tenant', connectionId: 'itsm-1',
          externalId: 'INC0012345',
          externalUrl: 'https://demo.service-now.com/incident.do?sys_id=INC0012345',
          externalStatus: 'In Progress', externalPriority: '2 - High',
          externalTitle: 'Latency spike on payments API after deploy',
          internalEntityType: 'CAPA', internalEntityId: 'capa-2',
          firstImportedAt: now, lastSeenAt: now
        },
        {
          id: 'map-2', tenantId: 'demo-tenant', connectionId: 'itsm-1',
          externalId: 'INC0012346',
          externalUrl: 'https://demo.service-now.com/incident.do?sys_id=INC0012346',
          externalStatus: 'Resolved', externalPriority: '3 - Moderate',
          externalTitle: 'Disk full on db-prod-04',
          firstImportedAt: now, lastSeenAt: now
        }
      ],
      'itsm-2': [
        {
          id: 'map-3', tenantId: 'demo-tenant', connectionId: 'itsm-2',
          externalId: 'SRE-1284',
          externalUrl: 'https://qualitos.atlassian.net/browse/SRE-1284',
          externalStatus: 'In Progress', externalPriority: 'High',
          externalTitle: 'Recurring timeouts on Keycloak OIDC discovery',
          firstImportedAt: now, lastSeenAt: now
        }
      ],
      'itsm-3': []
    };
  }
}
