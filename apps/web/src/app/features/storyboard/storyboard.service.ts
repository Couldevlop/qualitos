import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { StoryboardRequest, StoryboardResponse } from './storyboard.types';

/**
 * Service du Storyboard IA (§7.4) : relaie les indicateurs vers l'engine
 * (`POST /api/v1/ai/storyboard`), qui applique les garde-fous IA (OWASP LLM04) et relaie vers
 * ai-service (LLM réel Ollama/Anthropic + fallback). Le jeton d'auth est attaché par
 * l'intercepteur HTTP ; le tenant est dérivé du JWT côté serveur (jamais dans le body).
 */
@Injectable({ providedIn: 'root' })
export class StoryboardService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai/storyboard`;

  constructor(private readonly http: HttpClient) {}

  generate(req: StoryboardRequest): Observable<StoryboardResponse> {
    return this.http.post<StoryboardResponse>(this.endpoint, req);
  }
}
