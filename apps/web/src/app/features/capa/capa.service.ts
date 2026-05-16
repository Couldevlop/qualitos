import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { CapaCaseResponse, CapaPage, CapaStatus } from './capa.types';

@Injectable({ providedIn: 'root' })
export class CapaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  constructor(private readonly http: HttpClient) {}

  listCases(page = 0, size = 50, status?: CapaStatus): Observable<CapaPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<CapaPage>(this.endpoint, { params });
  }

  private mockPage(status?: CapaStatus): CapaPage {
    const all = this.mockCases();
    const filtered = status ? all.filter(c => c.status === status) : all;
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private mockCases(): CapaCaseResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'capa-1', tenantId: 'demo-tenant',
        title: 'Recalibration robot soudure cobot-3',
        description: 'Suite NC répétée sur ligne 3.',
        type: 'CORRECTIVE', criticity: 'HIGH', status: 'IN_PROGRESS',
        sourceType: 'NON_CONFORMITY', sourceRef: 'NC-2026-018',
        ownerId: 'demo-user', dueDate: '2026-05-30',
        createdAt: now, updatedAt: now, actions: []
      },
      {
        id: 'capa-2', tenantId: 'demo-tenant',
        title: 'Audit anti-fraude LCB-FT trimestriel — déficit contrôle KYC',
        type: 'PREVENTIVE', criticity: 'CRITICAL', status: 'OPEN',
        sourceType: 'AUDIT', sourceRef: 'AUD-2026-Q2',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now, actions: []
      },
      {
        id: 'capa-3', tenantId: 'demo-tenant',
        title: 'Mise à jour procédure stérilisation autoclave 4',
        type: 'CORRECTIVE', criticity: 'MEDIUM', status: 'RESOLVED',
        sourceType: 'INTERNAL',
        ownerId: 'demo-user',
        resolvedAt: now, createdAt: now, updatedAt: now, actions: []
      }
    ];
  }
}
