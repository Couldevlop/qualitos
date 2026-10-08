import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { OnboardRequest, OnboardResponse, TenantPage } from './admin.types';

/**
 * Les clients de la plateforme, côté éditeur (super administrateur).
 *
 * L'ingress route `/api/v1/tenants` vers api-core. Le serveur réserve ces
 * appels au super administrateur ; l'écran ne fait que les rendre accessibles.
 */
@Injectable({ providedIn: 'root' })
export class ClientsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/tenants`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, size = 100): Observable<TenantPage> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size)).set('sort', 'createdAt,desc');
    return this.http.get<TenantPage>(this.endpoint, { params });
  }

  /** L'entreprise, le compte de son premier administrateur, ses modules — en une fois. */
  onboard(input: OnboardRequest): Observable<OnboardResponse> {
    return this.http.post<OnboardResponse>(`${this.endpoint}/onboard`, input);
  }
}
