import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ImpactDomain,
  MocDecisionForm,
  MocDetail,
  MocImpactForm,
  MocRequestForm,
  MocSettings,
  MocSummary
} from './moc.types';

/**
 * La gestion des changements (MOC, ADR 0087).
 *
 * <p>Le client et l'acteur viennent du jeton côté serveur : aucune méthode ne
 * les transmet. Le serveur reste seul juge des règles (soumission bloquée,
 * titulaire du rôle, refus motivé) ; l'écran ne fait que les annoncer.
 */
@Injectable({ providedIn: 'root' })
export class MocService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/changes`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<MocSummary[]> {
    return this.http.get<MocSummary[]>(this.endpoint);
  }

  get(id: string): Observable<MocDetail> {
    return this.http.get<MocDetail>(`${this.endpoint}/${id}`);
  }

  create(form: MocRequestForm): Observable<MocDetail> {
    return this.http.post<MocDetail>(this.endpoint, form);
  }

  update(id: string, form: MocRequestForm): Observable<MocDetail> {
    return this.http.put<MocDetail>(`${this.endpoint}/${id}`, form);
  }

  /** Seul un brouillon se supprime (refus 409 sinon). */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  sendToAnalysis(id: string): Observable<MocDetail> {
    return this.http.post<MocDetail>(`${this.endpoint}/${id}/analysis`, {});
  }

  assess(id: string, domain: ImpactDomain, form: MocImpactForm): Observable<MocDetail> {
    return this.http.put<MocDetail>(`${this.endpoint}/${id}/impacts/${domain}`, form);
  }

  submit(id: string): Observable<MocDetail> {
    return this.http.post<MocDetail>(`${this.endpoint}/${id}/submit`, {});
  }

  decide(id: string, validationId: string, form: MocDecisionForm): Observable<MocDetail> {
    return this.http.post<MocDetail>(`${this.endpoint}/${id}/validations/${validationId}/decision`, form);
  }

  reopen(id: string): Observable<MocDetail> {
    return this.http.post<MocDetail>(`${this.endpoint}/${id}/reopen`, {});
  }

  /**
   * Le classeur de toutes les demandes. `blob` + réponse complète : c'est le
   * serveur qui nomme le fichier (Content-Disposition).
   */
  exportXlsx(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.endpoint}/export.xlsx`, { responseType: 'blob', observe: 'response' });
  }

  // ---------- paramètres (administrateur du client, authz.manage) ----------

  settings(): Observable<MocSettings> {
    return this.http.get<MocSettings>(`${this.endpoint}/settings`);
  }

  saveSettings(settings: MocSettings): Observable<MocSettings> {
    return this.http.put<MocSettings>(`${this.endpoint}/settings`, settings);
  }
}
