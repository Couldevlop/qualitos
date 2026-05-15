import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { PdcaCycleResponse, SpringPage } from './pdca.types';

@Injectable({ providedIn: 'root' })
export class PdcaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/pdca/cycles`;

  constructor(private readonly http: HttpClient) {}

  listCycles(page = 0, size = 20, status?: string): Observable<SpringPage<PdcaCycleResponse>> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(200));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<SpringPage<PdcaCycleResponse>>(this.endpoint, { params });
  }

  getCycle(id: string): Observable<PdcaCycleResponse> {
    if (environment.useMockApi) {
      const cycle = this.mockCycles().find(c => c.id === id);
      return of(cycle ?? this.mockCycles()[0]).pipe(delay(150));
    }
    return this.http.get<PdcaCycleResponse>(`${this.endpoint}/${id}`);
  }

  private mockPage(status?: string): SpringPage<PdcaCycleResponse> {
    const all = this.mockCycles();
    const filtered = status ? all.filter(c => c.status === status) : all;
    return {
      content: filtered,
      totalElements: filtered.length,
      totalPages: 1,
      number: 0,
      size: filtered.length
    };
  }

  private mockCycles(): PdcaCycleResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'demo-1', tenantId: 'demo-tenant',
        title: 'Réduction des défauts de soudure — ligne 3',
        description: 'Pilote: équipe production. Objectif: -30% NC en 90j.',
        status: 'DO',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        steps: [
          { id: 's1', cycleId: 'demo-1', phase: 'PLAN', title: 'Analyse Pareto',
            status: 'DONE', createdAt: now, updatedAt: now },
          { id: 's2', cycleId: 'demo-1', phase: 'DO', title: 'Mise en place Poka-Yoke',
            status: 'IN_PROGRESS', createdAt: now, updatedAt: now }
        ]
      },
      {
        id: 'demo-2', tenantId: 'demo-tenant',
        title: 'Amélioration satisfaction patient — service ambulatoire',
        description: 'CHU. Objectif: NPS +10 points en 6 mois.',
        status: 'PLAN',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        steps: []
      },
      {
        id: 'demo-3', tenantId: 'demo-tenant',
        title: 'Réduction MTTR incidents P1',
        description: 'SRE. Objectif: < 30 min médiane.',
        status: 'CHECK',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        steps: []
      }
    ];
  }
}
