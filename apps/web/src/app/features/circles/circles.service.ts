import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { CircleResponse, CircleStatus, CirclesPage } from './circles.types';

@Injectable({ providedIn: 'root' })
export class CirclesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/circles`;

  constructor(private readonly http: HttpClient) {}

  listCircles(page = 0, size = 50, status?: CircleStatus): Observable<CirclesPage> {
    if (environment.useMockApi) return of(this.mockPage(status)).pipe(delay(150));
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<CirclesPage>(this.endpoint, { params });
  }

  private mockPage(status?: CircleStatus): CirclesPage {
    const all = this.mockCircles();
    const f = status ? all.filter(c => c.status === status) : all;
    return { content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length };
  }

  private mockCircles(): CircleResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'c1', tenantId: 't', name: 'Cercle production ligne 3',
        description: 'Amélioration continue du process soudure',
        topic: 'production-soudure', status: 'ACTIVE', memberCount: 7,
        createdAt: now, updatedAt: now,
        members: [
          { id: 'm1', userId: 'u1', role: 'FACILITATOR', joinedAt: now },
          { id: 'm2', userId: 'u2', role: 'SECRETARY', joinedAt: now }
        ],
        meetings: [
          { id: 'mt1', title: 'Réunion mensuelle Avril', status: 'HELD', scheduledAt: now }
        ],
        proposals: [
          { id: 'p1', title: 'Ajout poka-yoke positionnement pièce', status: 'APPROVED' },
          { id: 'p2', title: 'Maintenance préventive cobot-3', status: 'MEASURED' }
        ]
      },
      {
        id: 'c2', tenantId: 't', name: 'Cercle qualité fournisseurs',
        description: 'Notation et amélioration relation fournisseurs',
        topic: 'fournisseurs', status: 'PAUSED', memberCount: 5,
        createdAt: now, updatedAt: now,
        members: [], meetings: [], proposals: []
      }
    ];
  }
}
