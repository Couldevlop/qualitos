import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  GrantRequest, PartnerConsultant, PartnerConsultantRequest, PartnerEngagement, PartnerFirm,
  PartnerFirmRequest, ProposeRequest
} from './partner.types';

/** Une page de cabinets, telle que le serveur la rend. */
export interface PartnerFirmPage {
  content: PartnerFirm[];
  totalElements: number;
}

/**
 * Les trois surfaces du domaine partenaire, et leurs trois chemins.
 *
 * <p>Les chemins sont séparés parce que les <b>autorisations</b> le sont : le
 * serveur réserve `/partner-firms` à l'éditeur, `/partner-access` à
 * l'administrateur d'un client, `/partner/portfolio` à un consultant. Un seul
 * chemin pour les trois aurait obligé à distinguer les droits à l'intérieur,
 * là où ils se lisent aujourd'hui dans l'URL et dans la configuration de
 * sécurité.
 *
 * <p>Aucun identifiant de client ne voyage jamais ici : il vient du jeton
 * (`CLAUDE.md` §18.2.2). La seule exception est la proposition de l'éditeur,
 * qui désigne explicitement le client visé — c'est précisément un accès
 * privilégié, et le serveur le journalise comme tel.
 */
@Injectable({ providedIn: 'root' })
export class PartnerService {

  private readonly firmsUrl = `${environment.apiBaseUrl}/api/v1/partner-firms`;
  private readonly accessUrl = `${environment.apiBaseUrl}/api/v1/partner-access`;
  private readonly portfolioUrl = `${environment.apiBaseUrl}/api/v1/partner/portfolio`;

  constructor(private readonly http: HttpClient) {}

  // ---------- côté éditeur ----------

  listFirms(page = 0, size = 50): Observable<PartnerFirmPage> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PartnerFirmPage>(this.firmsUrl, { params });
  }

  createFirm(input: PartnerFirmRequest): Observable<PartnerFirm> {
    return this.http.post<PartnerFirm>(this.firmsUrl, input);
  }

  updateFirm(firmId: string, input: PartnerFirmRequest): Observable<PartnerFirm> {
    return this.http.put<PartnerFirm>(`${this.firmsUrl}/${firmId}`, input);
  }

  setFirmActive(firmId: string, active: boolean): Observable<PartnerFirm> {
    return this.http.put<PartnerFirm>(`${this.firmsUrl}/${firmId}/active`, { active });
  }

  listConsultants(firmId: string): Observable<PartnerConsultant[]> {
    return this.http.get<PartnerConsultant[]>(`${this.firmsUrl}/${firmId}/consultants`);
  }

  addConsultant(firmId: string, input: PartnerConsultantRequest): Observable<PartnerConsultant> {
    return this.http.post<PartnerConsultant>(`${this.firmsUrl}/${firmId}/consultants`, input);
  }

  setConsultantActive(firmId: string, consultantId: string,
                      active: boolean): Observable<PartnerConsultant> {
    return this.http.put<PartnerConsultant>(
      `${this.firmsUrl}/${firmId}/consultants/${consultantId}/active`, { active });
  }

  portfolioOf(firmId: string): Observable<PartnerEngagement[]> {
    return this.http.get<PartnerEngagement[]>(`${this.firmsUrl}/${firmId}/portfolio`);
  }

  /** Propose un client au portefeuille. N'ouvre aucun accès : le client décide. */
  propose(firmId: string, input: ProposeRequest): Observable<PartnerEngagement> {
    return this.http.post<PartnerEngagement>(`${this.firmsUrl}/${firmId}/portfolio`, input);
  }

  // ---------- côté client ----------

  /** Les accords qui concernent le client du jeton. */
  myEngagements(): Observable<PartnerEngagement[]> {
    return this.http.get<PartnerEngagement[]>(this.accessUrl);
  }

  grant(engagementId: string, input: GrantRequest): Observable<PartnerEngagement> {
    return this.http.post<PartnerEngagement>(`${this.accessUrl}/${engagementId}/grant`, input);
  }

  decline(engagementId: string): Observable<PartnerEngagement> {
    return this.http.post<PartnerEngagement>(`${this.accessUrl}/${engagementId}/decline`, {});
  }

  revoke(engagementId: string): Observable<PartnerEngagement> {
    return this.http.post<PartnerEngagement>(`${this.accessUrl}/${engagementId}/revoke`, {});
  }

  // ---------- côté consultant ----------

  /** Les clients où ce consultant peut entrer aujourd'hui. */
  myPortfolio(): Observable<PartnerEngagement[]> {
    return this.http.get<PartnerEngagement[]>(this.portfolioUrl);
  }

  /**
   * Rattache le compte connecté à sa fiche, par le courriel que son cabinet a
   * inscrit. Rend `null` si aucune fiche ne l'attend — ce n'est pas une panne,
   * c'est un rattachement qui reste à faire.
   */
  linkAccount(): Observable<PartnerConsultant | null> {
    return this.http.post<PartnerConsultant | null>(`${this.portfolioUrl}/link`, {});
  }
}
