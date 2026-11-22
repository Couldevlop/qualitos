import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ApproveInput, CustomerDecisionInput, Derogation, DerogationPage,
  DerogationSaveInput, DerogationSearchInput, PendingApprovalCount, SendToCustomerInput
} from './derogations.types';

/**
 * La dérogation client.
 *
 * L'état du dossier, ce qui est modifiable et qui peut agir viennent du
 * serveur : l'écran les affiche, il ne les décide pas. Chaque geste est un
 * appel distinct, parce que chacun porte sa propre règle — l'agrégat vérifie
 * l'état ET l'acteur, et un « enregistrer » générique les contournerait.
 */
@Injectable({ providedIn: 'root' })
export class DerogationsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/derogations`;

  constructor(private readonly http: HttpClient) {}

  /** Le registre, filtré par créateur, pièce, client et statut. */
  search(input: DerogationSearchInput): Observable<DerogationPage> {
    let params = new HttpParams()
      .set('page', input.page ?? 0)
      .set('size', input.size ?? 20);
    if (input.owner) {
      params = params.set('owner', input.owner);
    }
    if (input.partReference) {
      params = params.set('partReference', input.partReference);
    }
    if (input.customerName) {
      params = params.set('customerName', input.customerName);
    }
    if (input.status) {
      params = params.set('status', input.status);
    }
    return this.http.get<DerogationPage>(this.endpoint, { params });
  }

  get(id: string): Observable<Derogation> {
    return this.http.get<Derogation>(`${this.endpoint}/${id}`);
  }

  create(input: DerogationSaveInput): Observable<Derogation> {
    return this.http.post<Derogation>(this.endpoint, input);
  }

  update(id: string, input: DerogationSaveInput): Observable<Derogation> {
    return this.http.put<Derogation>(`${this.endpoint}/${id}`, input);
  }

  /** « Validation manager » : la demande part au manager choisi dans l'annuaire. */
  requestApproval(id: string, managerAccountId: string): Observable<Derogation> {
    return this.http.post<Derogation>(
      `${this.endpoint}/${id}/request-approval`, { managerAccountId });
  }

  withdraw(id: string): Observable<Derogation> {
    return this.http.post<Derogation>(`${this.endpoint}/${id}/withdraw`, {});
  }

  /** Le renvoi exige un motif : sans lui, l'initiateur ne sait pas quoi corriger. */
  sendBack(id: string, reason: string): Observable<Derogation> {
    return this.http.post<Derogation>(`${this.endpoint}/${id}/send-back`, { reason });
  }

  approve(id: string, input: ApproveInput): Observable<Derogation> {
    return this.http.post<Derogation>(`${this.endpoint}/${id}/approve`, input);
  }

  reopen(id: string): Observable<Derogation> {
    return this.http.post<Derogation>(`${this.endpoint}/${id}/reopen`, {});
  }

  /** « Validation client » : le seul geste qui sort de l'organisation. */
  sendToCustomer(id: string, input: SendToCustomerInput): Observable<Derogation> {
    return this.http.post<Derogation>(`${this.endpoint}/${id}/send-to-customer`, input);
  }

  recordCustomerDecision(id: string, input: CustomerDecisionInput): Observable<Derogation> {
    return this.http.post<Derogation>(`${this.endpoint}/${id}/customer-decision`, input);
  }

  /** « Mes validations » : les dossiers qui attendent ma signature. */
  myApprovals(page = 0, size = 20): Observable<DerogationPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<DerogationPage>(`${this.endpoint}/my-approvals`, { params });
  }

  /** Le nombre derrière l'alerte rouge de l'onglet. */
  myApprovalCount(): Observable<PendingApprovalCount> {
    return this.http.get<PendingApprovalCount>(`${this.endpoint}/my-approvals/count`);
  }

  /**
   * Le document, tel qu'il part au client.
   *
   * Rendu en `blob` : on l'ouvre pour le relire AVANT de l'envoyer. C'est le
   * seul moyen de vérifier ce qu'un tiers va lire.
   */
  pdf(id: string): Observable<Blob> {
    return this.http.get(`${this.endpoint}/${id}/pdf`, { responseType: 'blob' });
  }
}
