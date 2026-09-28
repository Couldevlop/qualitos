import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CoqEntryRequest, CoqLabel, CoqLabelRequest, CoqLine, CoqReport
} from './cost-of-quality.types';

/**
 * Le coût de la qualité.
 *
 * <p>Les totaux, le ratio et l'ordre des lignes viennent du serveur : l'écran
 * les affiche, il ne les recalcule pas. Un chiffre de revue de direction ne
 * doit pas dépendre du navigateur qui le montre.
 */
@Injectable({ providedIn: 'root' })
export class CostOfQualityService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/cost-of-quality`;

  constructor(private readonly http: HttpClient) {}

  /** Le rapport d'un mois, ou de l'année entière quand `month` est nul. */
  report(year: number, month: number | null): Observable<CoqReport> {
    let params = new HttpParams().set('year', year);
    if (month !== null) {
      params = params.set('month', month);
    }
    return this.http.get<CoqReport>(this.endpoint, { params });
  }

  labels(): Observable<CoqLabel[]> {
    return this.http.get<CoqLabel[]>(`${this.endpoint}/labels`);
  }

  createLabel(input: CoqLabelRequest): Observable<CoqLabel> {
    return this.http.post<CoqLabel>(`${this.endpoint}/labels`, input);
  }

  record(input: CoqEntryRequest): Observable<CoqLine> {
    return this.http.post<CoqLine>(`${this.endpoint}/entries`, input);
  }

  revise(entryId: string, input: CoqEntryRequest): Observable<CoqLine> {
    return this.http.put<CoqLine>(`${this.endpoint}/entries/${entryId}`, input);
  }

  delete(entryId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/entries/${entryId}`);
  }

  setCurrency(currency: string): Observable<{ currency: string }> {
    return this.http.put<{ currency: string }>(`${this.endpoint}/currency`, { currency });
  }
}
