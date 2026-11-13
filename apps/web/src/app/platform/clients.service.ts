import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  OnboardRequest, OnboardResponse, TenantIdentityRequest, TenantIdentityResponse, TenantPage,
  TenantSummary, TenantUpdateRequest
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
   * Corrige un client existant : son nom, son offre, son état.
   *
   * <p>Le serveur journalise l'écriture dans la piste d'audit de la plateforme
   * (ADR 0109) : un changement d'offre est un acte commercial, et une
   * réactivation rouvre l'accès de tout un établissement.
   */
  update(tenantId: string, input: TenantUpdateRequest): Observable<TenantSummary> {
    return this.http.put<TenantSummary>(`${this.endpoint}/${tenantId}`, input);
  }

  /**
   * Suspend un client : ses comptes n'entrent plus, ses données restent.
   *
   * <p>Il n'y a pas d'effacement, et c'est délibéré : les dossiers d'un client
   * sont des preuves d'audit, souvent soumises à une durée de conservation
   * réglementaire. Un bouton « supprimer » qui les emporterait rendrait
   * l'éditeur responsable d'une destruction que personne ne peut annuler.
   * La réactivation passe par {@link update} avec `active: true`.
   */
  suspend(tenantId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${tenantId}`);
  }

  /**
   * L'identité et le thème d'un client nommé.
   *
   * <p>L'éditeur passe par cette route et non par celle de la marque
   * (`/api/v1/branding`) : celle-là lit le client DANS le jeton, et à la
   * création aucun membre du client n'a encore de jeton.
   */
  /**
   * L'identité déjà posée sur un client, pour la rouvrir et la corriger.
   *
   * <p>Un formulaire de correction qui s'ouvre vide n'est pas une correction :
   * c'est une ressaisie, et le premier enregistrement efface ce que l'éditeur
   * n'a pas retapé. La fenêtre part donc de l'existant.
   */
  identity(tenantId: string): Observable<TenantIdentityResponse> {
    return this.http.get<TenantIdentityResponse>(`${this.endpoint}/${tenantId}/identity`);
  }

  saveIdentity(tenantId: string, input: TenantIdentityRequest): Observable<TenantIdentityResponse> {
    return this.http.put<TenantIdentityResponse>(`${this.endpoint}/${tenantId}/identity`, input);
  }

  /** Le logo, en multipart. Le serveur vérifie le type par ses octets, pas par son nom. */
  saveLogo(tenantId: string, file: File): Observable<void> {
    const corps = new FormData();
    corps.append('file', file, file.name);
    return this.http.post<void>(`${this.endpoint}/${tenantId}/identity/logo`, corps);
  }

  /**
   * Retire le logo d'un client : il revient à celui du thème.
   *
   * <p>Distinct d'un envoi de logo vide, qui serait refusé : un client peut
   * décider de ne plus en avoir, et jusqu'ici un logo posé une fois ne pouvait
   * plus que se remplacer.
   */
  deleteLogo(tenantId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${tenantId}/identity/logo`);
  }
}
