import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  AnomalyDetectRequest,
  AnomalyDetectResponse,
  AnomalyExplainRequest,
  AnomalyExplainResponse
} from './anomaly.types';

/**
 * Service de détection d'anomalies (§12.1) : relaie la matrice vers l'engine
 * (`POST /api/v1/ai/anomaly/detect`), qui applique les garde-fous IA (OWASP LLM04)
 * et relaie vers ai-service (Isolation Forest / reconstruction ACP, NumPy pur).
 * Le jeton d'auth est attaché par l'intercepteur HTTP ; le tenant est dérivé du JWT
 * côté serveur (jamais envoyé dans le body).
 */
@Injectable({ providedIn: 'root' })
export class AnomalyService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/anomaly/detect`;

  constructor(private readonly http: HttpClient) {}

  detect(req: AnomalyDetectRequest): Observable<AnomalyDetectResponse> {
    return this.http.post<AnomalyDetectResponse>(this.endpoint, req);
  }

  /** Explique le score d'anomalie d'un échantillon (Kernel SHAP). */
  explain(req: AnomalyExplainRequest): Observable<AnomalyExplainResponse> {
    return this.http.post<AnomalyExplainResponse>(
      this.endpoint.replace('/detect', '/explain'), req);
  }
}
