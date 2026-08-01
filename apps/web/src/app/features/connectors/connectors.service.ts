import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CONNECTOR_MAX_PAGE_SIZE,
  CommConnection, CommCreateRequest, CommTestResult, CommUpdateRequest,
  ConnectorPage,
  EhrConnection, EhrCreateRequest, EhrSyncReport, EhrUpdateRequest,
  ErpConnection, ErpCreateRequest, ErpSyncReport, ErpUpdateRequest
} from './connectors.types';

/**
 * Configuration des connecteurs tiers (CLAUDE.md §13.3).
 *
 * Trois contrôleurs (`/api/v1/erp`, `/api/v1/ehr`, `/api/v1/comm`) n'avaient aucun
 * consommateur : brancher un SAP, un serveur FHIR ou un salon Teams imposait de
 * parler HTTP à la main. Ces routes sont verrouillées côté serveur aux rôles
 * ADMIN / ADMIN_TENANT / SUPER_ADMIN (`SecurityConfig`).
 *
 * Le tenant est dérivé du JWT côté serveur (règle §18.2 #2) : aucune méthode ne le
 * prend en paramètre. Les secrets ne transitent QUE dans le sens client → serveur ;
 * aucune réponse n'en contient, donc rien n'est à masquer à l'affichage.
 */
@Injectable({ providedIn: 'root' })
export class ConnectorsService {

  private readonly erpBase = `${environment.apiBaseUrl}/api/v1/erp/connections`;
  private readonly ehrBase = `${environment.apiBaseUrl}/api/v1/ehr/connections`;
  private readonly commBase = `${environment.apiBaseUrl}/api/v1/comm/connections`;

  constructor(private readonly http: HttpClient) {}

  // ---- ERP -------------------------------------------------------------------

  listErp(page: number, size: number): Observable<ConnectorPage<ErpConnection>> {
    return this.http.get<ConnectorPage<ErpConnection>>(this.erpBase, { params: this.pageParams(page, size) });
  }

  createErp(input: ErpCreateRequest): Observable<ErpConnection> {
    return this.http.post<ErpConnection>(this.erpBase, input);
  }

  updateErp(id: string, input: ErpUpdateRequest): Observable<ErpConnection> {
    return this.http.patch<ErpConnection>(`${this.erpBase}/${id}`, input);
  }

  deleteErp(id: string): Observable<void> {
    return this.http.delete<void>(`${this.erpBase}/${id}`);
  }

  /** Import fournisseurs + mesures KPI. Le serveur répond 200 avec un rapport, même en échec. */
  syncErp(id: string): Observable<ErpSyncReport> {
    return this.http.post<ErpSyncReport>(`${this.erpBase}/${id}/sync`, {});
  }

  // ---- EHR -------------------------------------------------------------------

  listEhr(page: number, size: number): Observable<ConnectorPage<EhrConnection>> {
    return this.http.get<ConnectorPage<EhrConnection>>(this.ehrBase, { params: this.pageParams(page, size) });
  }

  createEhr(input: EhrCreateRequest): Observable<EhrConnection> {
    return this.http.post<EhrConnection>(this.ehrBase, input);
  }

  updateEhr(id: string, input: EhrUpdateRequest): Observable<EhrConnection> {
    return this.http.patch<EhrConnection>(`${this.ehrBase}/${id}`, input);
  }

  deleteEhr(id: string): Observable<void> {
    return this.http.delete<void>(`${this.ehrBase}/${id}`);
  }

  /** Import incrémental des ressources FHIR depuis le dernier succès. */
  syncEhr(id: string): Observable<EhrSyncReport> {
    return this.http.post<EhrSyncReport>(`${this.ehrBase}/${id}/sync`, {});
  }

  // ---- Communication ---------------------------------------------------------

  listComm(page: number, size: number): Observable<ConnectorPage<CommConnection>> {
    return this.http.get<ConnectorPage<CommConnection>>(this.commBase, { params: this.pageParams(page, size) });
  }

  createComm(input: CommCreateRequest): Observable<CommConnection> {
    return this.http.post<CommConnection>(this.commBase, input);
  }

  updateComm(id: string, input: CommUpdateRequest): Observable<CommConnection> {
    return this.http.patch<CommConnection>(`${this.commBase}/${id}`, input);
  }

  deleteComm(id: string): Observable<void> {
    return this.http.delete<void>(`${this.commBase}/${id}`);
  }

  /**
   * Envoie un vrai message de test dans le salon configuré : c'est le seul moyen de
   * valider l'URL webhook, qui n'est jamais relue. Le serveur répond 200 avec
   * `success: false` quand l'envoi échoue — pas d'erreur HTTP à intercepter.
   */
  testComm(id: string): Observable<CommTestResult> {
    return this.http.post<CommTestResult>(`${this.commBase}/${id}/test`, {});
  }

  // ---- Interne ---------------------------------------------------------------

  /**
   * Le serveur plafonne la taille de page à 100 (`spring.data.web.pageable.max-page-size`)
   * et rabote SANS erreur au-delà : on borne ici pour que l'index de page demandé reste
   * cohérent avec la fenêtre réellement servie.
   */
  private pageParams(page: number, size: number): HttpParams {
    return new HttpParams()
      .set('page', Math.max(0, Math.trunc(page)))
      .set('size', Math.min(CONNECTOR_MAX_PAGE_SIZE, Math.max(1, Math.trunc(size))));
  }
}
