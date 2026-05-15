import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuditPlanResponse, AuditStatus, AuditsPage } from './audits.types';

@Injectable({ providedIn: 'root' })
export class AuditsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/audits/plans`;

  constructor(private readonly http: HttpClient) {}

  listPlans(page = 0, size = 50, status?: AuditStatus): Observable<AuditsPage> {
    if (environment.useMockApi) return of(this.mockPage(status)).pipe(delay(150));
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<AuditsPage>(this.endpoint, { params });
  }

  private mockPage(status?: AuditStatus): AuditsPage {
    const all = this.mockPlans();
    const f = status ? all.filter(a => a.status === status) : all;
    return { content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length };
  }

  private mockPlans(): AuditPlanResponse[] {
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
