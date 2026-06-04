import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { ActivityEntry, ActivityPage } from './activity-feed.types';

/**
 * Accès au flux d'activité (read-model projeté par le consommateur Kafka).
 * L'authentification est ajoutée par l'ApiInterceptor (OWASP A01/A07) ; l'isolation
 * tenant est appliquée côté serveur via le claim JWT.
 */
@Injectable({ providedIn: 'root' })
export class ActivityFeedService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/activity-feed`;

  constructor(private readonly http: HttpClient) {}

  /** Les `limit` événements les plus récents du tenant courant. */
  recent(limit = 10): Observable<ActivityEntry[]> {
    if (environment.useMockApi) {
      return of(this.seed().slice(0, limit)).pipe(delay(120));
    }
    const params = new HttpParams().set('size', String(limit));
    return this.http
      .get<ActivityPage>(this.endpoint, { params })
      .pipe(map(page => page.content ?? []));
  }

  private seed(): ActivityEntry[] {
    return [
      {
        id: '11111111-1111-1111-1111-111111111111', sequenceNo: 3,
        occurredAt: '2026-06-03T10:00:00Z', recordedAt: '2026-06-03T10:00:01Z',
        action: 'capa.created', resourceType: 'capa',
        resourceId: '22222222-2222-2222-2222-222222222222', actorUserId: null,
        summary: 'Ouverture CAPA — dérive process ligne 3'
      },
      {
        id: '33333333-3333-3333-3333-333333333333', sequenceNo: 2,
        occurredAt: '2026-06-03T09:40:00Z', recordedAt: '2026-06-03T09:40:01Z',
        action: 'audit.plan.completed', resourceType: 'audit',
        resourceId: null, actorUserId: null,
        summary: 'Audit interne ISO 9001 §9.2 clôturé'
      },
      {
        id: '44444444-4444-4444-4444-444444444444', sequenceNo: 1,
        occurredAt: '2026-06-03T09:10:00Z', recordedAt: '2026-06-03T09:10:01Z',
        action: 'documents.version.published', resourceType: 'documents',
        resourceId: null, actorUserId: null,
        summary: 'Procédure PR-QUA-007 v3 publiée'
      }
    ];
  }
}
