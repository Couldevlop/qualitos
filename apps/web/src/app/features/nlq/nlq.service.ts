import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { NlqAskResponse } from './nlq.types';

/**
 * Service NLQ : pose une question en langage naturel à l'engine
 * (POST /api/v1/ai/nlq/ask), qui relaie vers ai-service (génération SQL validée,
 * filtre tenant, exécution read-only). §7.3.
 */
@Injectable({ providedIn: 'root' })
export class NlqService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/nlq/ask`;

  constructor(private readonly http: HttpClient) {}

  ask(question: string, maxRows = 100): Observable<NlqAskResponse> {
    if (environment.useMockApi) {
      return of(this.mock(question)).pipe(delay(650));
    }
    return this.http.post<NlqAskResponse>(this.endpoint, { question, maxRows });
  }

  private mock(question: string): NlqAskResponse {
    return {
      question,
      sql: "SELECT status, COUNT(*) AS count FROM capa_cases\n"
        + "WHERE tenant_id = %(tenant_id)s GROUP BY status",
      tenantFilterApplied: true,
      tablesUsed: ['capa_cases'],
      functionsUsed: ['count'],
      rows: [
        { status: 'OPEN', count: 4 },
        { status: 'IN_PROGRESS', count: 7 },
        { status: 'RESOLVED', count: 3 },
        { status: 'CLOSED', count: 12 }
      ],
      rowCount: 4,
      confidence: 0.85,
      chart: { chart_type: 'bar', title: 'CAPA par statut' },
      narrative: 'Démo : 26 CAPA au total, dont 11 encore actives (OPEN + IN_PROGRESS). '
        + 'La majorité (12) sont clôturées.'
    };
  }
}
