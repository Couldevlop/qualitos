import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AuditPlanResponse,
  AuditStatus,
  AuditsPage,
  CreateAuditPlanRequest
} from './audits.types';

@Injectable({ providedIn: 'root' })
export class AuditsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/audits/plans`;

  private readonly mockStore: AuditPlanResponse[] = this.seedMockPlans();

  constructor(private readonly http: HttpClient) {}

  listPlans(page = 0, size = 50, status?: AuditStatus): Observable<AuditsPage> {
    if (environment.useMockApi) return of(this.mockPage(status)).pipe(delay(150));
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<AuditsPage>(this.endpoint, { params });
  }

  createPlan(input: CreateAuditPlanRequest): Observable<AuditPlanResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const plan: AuditPlanResponse = {
        id: 'a-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        title: input.title,
        scope: input.scope,
        type: input.type,
        status: 'PLANNED',
        standard: input.standard,
        leadAuditorId: input.leadAuditorId,
        scheduledDate: input.scheduledDate,
        createdAt: now,
        updatedAt: now
      };
      this.mockStore.unshift(plan);
      return of(plan).pipe(delay(200));
    }
    return this.http.post<AuditPlanResponse>(this.endpoint, input);
  }

  private mockPage(status?: AuditStatus): AuditsPage {
    const f = status ? this.mockStore.filter(a => a.status === status) : this.mockStore;
    return { content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length };
  }

  private seedMockPlans(): AuditPlanResponse[] {
    const now = new Date().toISOString();
    return [
      { id: 'a1', tenantId: 't', title: 'Audit interne ISO 9001 §9.2', type: 'INTERNAL',
        status: 'COMPLETED', standard: 'ISO_9001', leadAuditorId: 'u', scheduledDate: now,
        completedAt: now, conformityScore: 92, createdAt: now, updatedAt: now },
      { id: 'a2', tenantId: 't', title: 'Audit fournisseur Acme Forge', type: 'SUPPLIER',
        status: 'IN_PROGRESS', leadAuditorId: 'u', scheduledDate: now,
        conformityScore: 65, createdAt: now, updatedAt: now },
      { id: 'a3', tenantId: 't', title: 'Pré-audit certification ISO 27001', type: 'CERTIFICATION',
        status: 'PLANNED', standard: 'ISO_27001', leadAuditorId: 'u', scheduledDate: now,
        createdAt: now, updatedAt: now }
    ];
  }
}
