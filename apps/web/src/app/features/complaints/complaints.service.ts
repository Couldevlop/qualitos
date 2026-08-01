import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AddResponseRequest, Complaint, ComplaintDetail, ComplaintListCriteria, ComplaintOverview,
  ComplaintResponseEntry, ComplaintStatistics, CreateComplaintRequest, SpringPage,
  UpdateComplaintRequest
} from './complaints.types';

/** Le fil de discussion tient dans une page : le serveur en sert 100 par défaut. */
const RESPONSES_PAGE_SIZE = 100;

/**
 * Client HTTP des réclamations clients (§4.9).
 *
 * `/api/v1/complaints` (15 routes) n'avait aucun consommateur : la promesse
 * « saisie multi-canal + suivi jusqu'à la mesure de satisfaction » n'était
 * pilotable qu'en appelant l'API à la main.
 *
 * Le tenant est dérivé du JWT côté serveur (règle §18.2 #2) : aucune méthode ne
 * le prend en paramètre.
 */
@Injectable({ providedIn: 'root' })
export class ComplaintsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/complaints`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Liste paginée.
   *
   * Le serveur applique UN SEUL filtre, par précédence status > category >
   * supplierId : on n'envoie donc que le critère effectivement retenu, sinon
   * l'écran afficherait un filtre silencieusement ignoré.
   */
  list(criteria: ComplaintListCriteria): Observable<SpringPage<Complaint>> {
    let params = new HttpParams()
      .set('page', criteria.page)
      .set('size', criteria.size);
    if (criteria.status) params = params.set('status', criteria.status);
    else if (criteria.category) params = params.set('category', criteria.category);
    else if (criteria.supplierId) params = params.set('supplierId', criteria.supplierId);
    return this.http.get<SpringPage<Complaint>>(this.endpoint, { params });
  }

  statistics(): Observable<ComplaintStatistics> {
    return this.http.get<ComplaintStatistics>(`${this.endpoint}/statistics`);
  }

  /** Vue de l'écran de liste : page de résultats + compteurs, en un seul flux. */
  overview(criteria: ComplaintListCriteria): Observable<ComplaintOverview> {
    return combineLatest([this.list(criteria), this.statistics()]).pipe(
      map(([page, statistics]) => ({ page, statistics }))
    );
  }

  get(id: string): Observable<Complaint> {
    return this.http.get<Complaint>(`${this.endpoint}/${id}`);
  }

  responses(id: string, page = 0, size = RESPONSES_PAGE_SIZE): Observable<SpringPage<ComplaintResponseEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<SpringPage<ComplaintResponseEntry>>(`${this.endpoint}/${id}/responses`, { params });
  }

  /**
   * Vue de l'écran de détail : la réclamation et son fil de réponses.
   *
   * Le fil conditionne les actions offertes (la résolution exige une réponse
   * client) : le charger séparément exposerait un instant une barre d'actions fausse.
   */
  detail(id: string): Observable<ComplaintDetail> {
    return combineLatest([this.get(id), this.responses(id)]).pipe(
      map(([complaint, page]) => ({ complaint, responses: page.content }))
    );
  }

  create(req: CreateComplaintRequest): Observable<Complaint> {
    return this.http.post<Complaint>(this.endpoint, req);
  }

  update(id: string, req: UpdateComplaintRequest): Observable<Complaint> {
    return this.http.patch<Complaint>(`${this.endpoint}/${id}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  /** Assigner depuis RECEIVED déclenche aussi le passage en investigation. */
  assign(id: string, assigneeUserId: string): Observable<Complaint> {
    return this.http.post<Complaint>(`${this.endpoint}/${id}/assign`, { assigneeUserId });
  }

  reject(id: string, reason: string): Observable<Complaint> {
    return this.http.post<Complaint>(`${this.endpoint}/${id}/reject`, { reason });
  }

  /** Le rattachement d'un dossier CAPA est facultatif (§4.2 ↔ §4.9). */
  resolve(id: string, capaCaseId?: string): Observable<Complaint> {
    return this.http.post<Complaint>(`${this.endpoint}/${id}/resolve`,
      { capaCaseId: capaCaseId ?? null });
  }

  close(id: string): Observable<Complaint> {
    return this.http.post<Complaint>(`${this.endpoint}/${id}/close`, {});
  }

  reopen(id: string): Observable<Complaint> {
    return this.http.post<Complaint>(`${this.endpoint}/${id}/reopen`, {});
  }

  setSatisfaction(id: string, score: number): Observable<Complaint> {
    return this.http.post<Complaint>(`${this.endpoint}/${id}/satisfaction`, { score });
  }

  addResponse(id: string, req: AddResponseRequest): Observable<ComplaintResponseEntry> {
    return this.http.post<ComplaintResponseEntry>(`${this.endpoint}/${id}/responses`, req);
  }

  /** Seules les notes internes sont supprimables : le serveur refuse les autres. */
  deleteResponse(id: string, responseId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}/responses/${responseId}`);
  }
}
