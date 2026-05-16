import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AdoptionResponse, AdoptionsPage, StandardSummary, StandardsPage } from './standards.types';

@Injectable({ providedIn: 'root' })
export class StandardsService {

  private readonly baseEndpoint = `${environment.apiBaseUrl}/api/v1/standards`;

  constructor(private readonly http: HttpClient) {}

  listCatalog(page = 0, size = 50): Observable<StandardsPage> {
    if (environment.useMockApi) return of(this.mockCatalog()).pipe(delay(120));
    return this.http.get<StandardsPage>(this.baseEndpoint,
      { params: new HttpParams().set('page', page).set('size', size) });
  }

  listAdoptions(): Observable<AdoptionsPage> {
    if (environment.useMockApi) return of(this.mockAdoptions()).pipe(delay(120));
    return this.http.get<AdoptionsPage>(`${this.baseEndpoint}/adoptions`);
  }

  private mockCatalog(): StandardsPage {
    const items: StandardSummary[] = [
      { id: 's1', code: 'iso-9001', fullName: 'ISO 9001:2015 — Management de la qualité',
        publisher: 'ISO', currentVersion: '2015', family: 'HLS', applicableIndustries: 'all',
        status: 'PUBLISHED', recertificationCycleMonths: 36 },
      { id: 's2', code: 'iso-27001', fullName: 'ISO/IEC 27001:2022 — Sécurité de l\'information',
        publisher: 'ISO/IEC', currentVersion: '2022', family: 'HLS', applicableIndustries: 'all',
        status: 'PUBLISHED', recertificationCycleMonths: 36 }
    ];
    return { content: items, totalElements: items.length, totalPages: 1, number: 0, size: items.length };
  }

  private mockAdoptions(): AdoptionsPage {
    const now = new Date().toISOString();
    const items: AdoptionResponse[] = [
      { id: 'ad1', tenantId: 't', standardId: 's1', standardCode: 'iso-9001',
        standardName: 'ISO 9001:2015', status: 'IN_PROGRESS',
        scopeDescription: 'SMQ siège + 3 usines',
        targetCertificationDate: '2026-12-15', certificationBody: 'AFNOR',
        createdAt: now, updatedAt: now },
      { id: 'ad2', tenantId: 't', standardId: 's2', standardCode: 'iso-27001',
        standardName: 'ISO/IEC 27001:2022', status: 'PLANNING',
        scopeDescription: 'SMSI direction technique',
        certificationBody: 'BSI', createdAt: now, updatedAt: now }
    ];
    return { content: items, totalElements: items.length, totalPages: 1, number: 0, size: items.length };
  }
}
