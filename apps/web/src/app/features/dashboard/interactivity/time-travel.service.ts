import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { DashboardSnapshot } from './time-travel.types';

/**
 * Time-travel de dashboard (§7.3) : récupère l'état réel des KPIs du tenant à
 * une date choisie via une requête backend (as-of sur kpi_measurements).
 * AUCUNE simulation côté front : la donnée vient de l'API, filtrée par tenant.
 */
@Injectable()
export class TimeTravelService {

  private readonly baseUrl = environment.apiBaseUrl + '/api/v1/dashboards/time-travel';

  constructor(private readonly http: HttpClient) {}

  /**
   * @param asOf instant ISO-8601 (ex. '2026-03-15T00:00:00.000Z')
   * @returns snapshot as-of de tous les KPIs ACTIVE du tenant
   */
  kpisAsOf(asOf: string): Observable<DashboardSnapshot> {
    const params = new HttpParams().set('asOf', asOf);
    return this.http.get<DashboardSnapshot>(`${this.baseUrl}/kpis`, { params });
  }
}
