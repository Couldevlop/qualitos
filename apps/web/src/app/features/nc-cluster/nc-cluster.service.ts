import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { NcClusterRequest, NcClusterResponse } from './nc-cluster.types';

/**
 * Service de clustering de NC (§4.3) : relaie les textes vers l'engine
 * (`POST /api/v1/ai/nc-clusters`), qui applique les garde-fous IA (OWASP LLM04) et relaie
 * vers ai-service (TF-IDF + DBSCAN, NumPy pur). Le jeton d'auth est attaché par l'intercepteur
 * HTTP ; le tenant est dérivé du JWT côté serveur (jamais dans le body).
 */
@Injectable({ providedIn: 'root' })
export class NcClusterService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/nc-clusters`;

  constructor(private readonly http: HttpClient) {}

  cluster(req: NcClusterRequest): Observable<NcClusterResponse> {
    return this.http.post<NcClusterResponse>(this.endpoint, req);
  }
}
