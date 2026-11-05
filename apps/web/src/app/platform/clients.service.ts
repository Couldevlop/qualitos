import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  OnboardRequest, OnboardResponse, TenantIdentityRequest, TenantIdentityResponse, TenantPage
} from '../features/admin/admin.types';

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

  /**
   * L'identité et le thème d'un client nommé.
   *
   * <p>L'éditeur passe par cette route et non par celle de la marque
   * (`/api/v1/branding`) : celle-là lit le client DANS le jeton, et à la
   * création aucun membre du client n'a encore de jeton.
   */
  saveIdentity(tenantId: string, input: TenantIdentityRequest): Observable<TenantIdentityResponse> {
    return this.http.put<TenantIdentityResponse>(`${this.endpoint}/${tenantId}/identity`, input);
  }

  /** Le logo, en multipart. Le serveur vérifie le type par ses octets, pas par son nom. */
  saveLogo(tenantId: string, file: File): Observable<void> {
    const corps = new FormData();
    corps.append('file', file, file.name);
    return this.http.post<void>(`${this.endpoint}/${tenantId}/identity/logo`, corps);
  }
}
