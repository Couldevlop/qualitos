import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateFiveSAuditRequest,
  FiveSAuditResponse,
  FiveSAuditStatus,
  FiveSPage
} from './fives.types';

@Injectable({ providedIn: 'root' })
export class FivesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/fives/audits`;

  private readonly mockStore: FiveSAuditResponse[] = this.seedMockAudits();

  constructor(private readonly http: HttpClient) {}

  listAudits(page = 0, size = 50, status?: FiveSAuditStatus): Observable<FiveSPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<FiveSPage>(this.endpoint, { params });
  }

  createAudit(input: CreateFiveSAuditRequest): Observable<FiveSAuditResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const audit: FiveSAuditResponse = {
        id: '5s-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        zone: input.zone,
        description: input.description,
        status: 'DRAFT',
        auditorId: input.auditorId,
        scheduledAt: input.scheduledAt,
        createdAt: now,
        updatedAt: now,
        items: []
      };
      this.mockStore.unshift(audit);
      return of(audit).pipe(delay(200));
    }
    return this.http.post<FiveSAuditResponse>(this.endpoint, input);
  }

  private mockPage(status?: FiveSAuditStatus): FiveSPage {
    const filtered = status ? this.mockStore.filter(a => a.status === status) : this.mockStore;
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private seedMockAudits(): FiveSAuditResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: '5s-1', tenantId: 'demo-tenant', zone: 'Atelier mécanique A',
        description: 'Audit mensuel ligne 1', status: 'COMPLETED',
        auditorId: 'demo-user', scheduledAt: now, completedAt: now,
        overallScore: 78, createdAt: now, updatedAt: now,
        items: []
      },
      {
        id: '5s-2', tenantId: 'demo-tenant', zone: 'Bloc opératoire B',
        description: 'CHU - audit hebdo', status: 'IN_PROGRESS',
        auditorId: 'demo-user', scheduledAt: now,
        createdAt: now, updatedAt: now, items: []
      },
      {
        id: '5s-3', tenantId: 'demo-tenant', zone: 'Entrepôt logistique nord',
        status: 'DRAFT',
        auditorId: 'demo-user', scheduledAt: now,
        createdAt: now, updatedAt: now, items: []
      }
    ];
  }
}
