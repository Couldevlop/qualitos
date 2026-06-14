import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ForecastRequest, ForecastResponse } from './forecast.types';

/**
 * Service de prévision KPI (§6.5) : relaie la série vers l'engine
 * (`POST /api/v1/ai/forecast/kpi`), qui applique les garde-fous IA (OWASP LLM04) et relaie
 * vers ai-service (lissage exponentiel Holt-Winters, NumPy pur). Le jeton d'auth est attaché
 * par l'intercepteur HTTP ; le tenant est dérivé du JWT côté serveur (jamais dans le body).
 */
@Injectable({ providedIn: 'root' })
export class ForecastService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/forecast/kpi`;

  constructor(private readonly http: HttpClient) {}

  forecast(req: ForecastRequest): Observable<ForecastResponse> {
    return this.http.post<ForecastResponse>(this.endpoint, req);
  }
}
