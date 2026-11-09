import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateStakeholderRequest,
  DetectedChange,
  ExtractedRequirement,
  RequirementLink,
  RequirementRequest,
  RequirementStatus,
  StakeholderRating,
  StakeholderRequirement,
  StakeholderRow,
  StakeholderSheet,
  StakeholderSide,
  StakeholderStatistics,
  SuggestedStakeholder,
  UpdateStakeholderRequest
} from './stakeholders.types';

/**
 * Le registre des parties intéressées.
 *
 * <p>Les trois appels d'assistance sont en POST et ne modifient RIEN : POST
 * parce qu'ils reçoivent un corps — le texte d'un contrat ne passe pas dans une
 * URL. Inscrire une suggestion, ajouter une exigence extraite et marquer des
 * exigences à réévaluer sont trois appels distincts, faits par quelqu'un.
 */
@Injectable({ providedIn: 'root' })
export class StakeholdersService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/stakeholders`;

  constructor(private readonly http: HttpClient) {}

  // ---------- registre ----------

  list(side?: StakeholderSide, search?: string): Observable<StakeholderRow[]> {
    let params = new HttpParams();
    if (side) params = params.set('side', side);
    if (search?.trim()) params = params.set('search', search.trim());
    return this.http.get<StakeholderRow[]>(this.endpoint, { params });
  }

  statistics(): Observable<StakeholderStatistics> {
    return this.http.get<StakeholderStatistics>(`${this.endpoint}/statistics`);
  }

  get(id: string): Observable<StakeholderSheet> {
    return this.http.get<StakeholderSheet>(`${this.endpoint}/${id}`);
  }

  create(request: CreateStakeholderRequest): Observable<StakeholderSheet> {
    return this.http.post<StakeholderSheet>(this.endpoint, request);
  }

  update(id: string, request: UpdateStakeholderRequest): Observable<StakeholderSheet> {
    return this.http.put<StakeholderSheet>(`${this.endpoint}/${id}`, request);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  markReviewed(id: string): Observable<StakeholderSheet> {
    return this.http.post<StakeholderSheet>(`${this.endpoint}/${id}/review`, {});
  }

  // ---------- matrice ----------

  /** Cote plusieurs lignes d'un coup : c'est le geste de la matrice. */
  rate(ratings: Record<string, StakeholderRating>): Observable<StakeholderRow[]> {
    return this.http.put<StakeholderRow[]>(`${this.endpoint}/ratings`, { ratings });
  }

  // ---------- exigences ----------

  addRequirement(id: string, request: RequirementRequest): Observable<StakeholderRequirement> {
    return this.http.post<StakeholderRequirement>(`${this.endpoint}/${id}/requirements`, request);
  }

  setRequirementStatus(id: string, requirementId: string,
                       status: RequirementStatus): Observable<StakeholderRequirement> {
    return this.http.put<StakeholderRequirement>(
      `${this.endpoint}/${id}/requirements/${requirementId}/status`, { status });
  }

  linkRequirement(id: string, requirementId: string, link: RequirementLink,
                  target: string): Observable<StakeholderRequirement> {
    return this.http.put<StakeholderRequirement>(
      `${this.endpoint}/${id}/requirements/${requirementId}/link`, { link, target });
  }

  removeRequirement(id: string, requirementId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}/requirements/${requirementId}`);
  }

  openCapa(id: string, requirementId: string, title: string, ownerId: string,
           ownerName: string | undefined, dueDate: string): Observable<StakeholderRequirement> {
    return this.http.post<StakeholderRequirement>(
      `${this.endpoint}/${id}/requirements/${requirementId}/capa`,
      { title, ownerId, ownerName, dueDate });
  }

  /**
   * Inscrit au registre des risques le risque qu'une exigence fait courir.
   *
   * <p>Le corps est court parce que l'intitulé, la cause et l'effet sont tirés
   * de l'exigence par le serveur : il la relit dans le client du jeton, et
   * c'est la même phrase qui part au registre.
   */
  openRisk(id: string, requirementId: string, type: string, process: string, severity: number,
           probability: number, ownerId: string,
           ownerName: string): Observable<StakeholderRequirement> {
    return this.http.post<StakeholderRequirement>(
      `${this.endpoint}/${id}/requirements/${requirementId}/risk`,
      { type, process, severity, probability, ownerId, ownerName });
  }

  // ---------- assistant : propose, n'écrit pas ----------

  suggest(sector: string, characteristics: string): Observable<SuggestedStakeholder[]> {
    return this.http.post<SuggestedStakeholder[]>(`${this.endpoint}/assistant/suggestions`,
      { sector, characteristics });
  }

  extract(documentName: string, documentText: string): Observable<ExtractedRequirement[]> {
    return this.http.post<ExtractedRequirement[]>(`${this.endpoint}/assistant/requirements`,
      { documentName, documentText });
  }

  changes(documentName: string, previousText: string,
          currentText: string): Observable<DetectedChange[]> {
    return this.http.post<DetectedChange[]>(`${this.endpoint}/assistant/changes`,
      { documentName, previousText, currentText });
  }

  /** La validation humaine du troisième assistant : lesquelles sont remises en cause. */
  markToReassess(id: string, requirementIds: string[]): Observable<StakeholderRequirement[]> {
    return this.http.post<StakeholderRequirement[]>(
      `${this.endpoint}/${id}/requirements/reassess`, { requirementIds });
  }
}
