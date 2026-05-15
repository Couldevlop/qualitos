import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { IshikawaDiagramResponse, IshikawaPage, IshikawaStatus } from './ishikawa.types';

@Injectable({ providedIn: 'root' })
export class IshikawaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ishikawa/diagrams`;

  constructor(private readonly http: HttpClient) {}

  listDiagrams(page = 0, size = 50, status?: IshikawaStatus): Observable<IshikawaPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<IshikawaPage>(this.endpoint, { params });
  }

  private mockPage(status?: IshikawaStatus): IshikawaPage {
    const all = this.mockDiagrams();
    const filtered = status ? all.filter(d => d.status === status) : all;
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private mockDiagrams(): IshikawaDiagramResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'ish-1', tenantId: 'demo-tenant',
        problemStatement: 'Hausse des défauts de soudure ligne 3 (+18% sur 30j)',
        description: 'Analyse menée avec l\'équipe production et qualité.',
        mode: 'SIX_M', status: 'VALIDATED', ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        causes: [
          { id: 'c1', diagramId: 'ish-1', category: 'MACHINES',
            label: 'Calibration robot soudure dérivée', rootCauseScore: 0.85,
            createdAt: now, updatedAt: now },
          { id: 'c2', diagramId: 'ish-1', category: 'MATERIALS',
            label: 'Lot de fil de soudure non conforme', rootCauseScore: 0.30,
            createdAt: now, updatedAt: now }
        ]
      },
      {
        id: 'ish-2', tenantId: 'demo-tenant',
        problemStatement: 'Annulations chirurgie programmée — bloc B',
        mode: 'SEVEN_M', status: 'IN_REVIEW', ownerId: 'demo-user',
        createdAt: now, updatedAt: now, causes: []
      }
    ];
  }
}
