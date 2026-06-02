import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SpcAnalyzeRequest, SpcAnalyzeResponse } from './spc.types';

/**
 * Service SPC (§3.4) : relaie la série vers l'engine (`POST /api/v1/ai/spc/analyze`),
 * qui applique les garde-fous IA (OWASP LLM04) et relaie vers ai-service. Le jeton
 * d'auth est attaché par l'intercepteur HTTP ; le tenant est dérivé du JWT côté serveur
 * (jamais envoyé dans le body).
 */
@Injectable({ providedIn: 'root' })
export class SpcService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/spc/analyze`;

  constructor(private readonly http: HttpClient) {}

  analyze(req: SpcAnalyzeRequest): Observable<SpcAnalyzeResponse> {
    return this.http.post<SpcAnalyzeResponse>(this.endpoint, req);
  }
}
