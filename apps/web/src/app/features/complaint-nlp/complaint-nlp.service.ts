import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ComplaintAnalyzeRequest, ComplaintAnalyzeResponse } from './complaint-nlp.types';

/**
 * Service d'analyse NLP des réclamations (§4.9) : relaie le lot vers l'engine
 * (`POST /api/v1/ai/complaints/analyze`), qui applique les garde-fous IA (OWASP LLM04) et
 * relaie vers ai-service (sentiment + classification, pur). Le jeton d'auth est attaché par
 * l'intercepteur HTTP ; le tenant est dérivé du JWT côté serveur (jamais dans le body).
 */
@Injectable({ providedIn: 'root' })
export class ComplaintNlpService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/complaints/analyze`;

  constructor(private readonly http: HttpClient) {}

  analyze(req: ComplaintAnalyzeRequest): Observable<ComplaintAnalyzeResponse> {
    return this.http.post<ComplaintAnalyzeResponse>(this.endpoint, req);
  }
}
