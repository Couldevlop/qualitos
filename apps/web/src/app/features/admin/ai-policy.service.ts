import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AiPolicy, AiProviderName } from './ai-policy.types';

/**
 * La politique IA du tenant courant (ADR 0102). Le tenant n'est jamais envoyé :
 * le serveur le lit dans le jeton.
 */
@Injectable({ providedIn: 'root' })
export class AiPolicyService {

  private readonly base = `${environment.apiBaseUrl}/api/v1/ai/policy`;

  constructor(private readonly http: HttpClient) {}

  get(): Observable<AiPolicy> {
    return this.http.get<AiPolicy>(this.base);
  }

  pin(provider: AiProviderName): Observable<AiPolicy> {
    return this.http.put<AiPolicy>(this.base, { provider });
  }

  /** Rend le tenant au défaut de la plateforme. */
  release(): Observable<AiPolicy> {
    return this.http.delete<AiPolicy>(this.base);
  }
}
