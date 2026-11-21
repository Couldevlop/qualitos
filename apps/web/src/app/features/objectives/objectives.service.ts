import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateObjectiveRequest,
  ObjectiveProposal,
  ObjectiveReview,
  QualityObjective,
  UpdateObjectiveRequest
} from './objectives.types';

/**
 * Les objectifs qualité : les définir, les revoir face aux KPI réels, et
 * trancher la CAPA qu'un écart propose (ISO 9001 §6.2, ADR 0133).
 *
 * <p>`evaluate` renvoie `null` quand la règle ne déclenche pas : le serveur
 * répond 204, et l'écran doit pouvoir distinguer « rien à proposer » d'une
 * réponse vide par erreur.
 */
@Injectable({ providedIn: 'root' })
export class ObjectivesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/quality-objectives`;

  constructor(private readonly http: HttpClient) {}

  /**
   * L'écran « Objectifs vs KPI réels ».
   *
   * @param asOf fin de la période observée (`AAAA-MM-JJ`) : une revue mensuelle
   *             se tient après la fin du mois et doit pouvoir se rejouer telle
   *             qu'elle a été lue.
   */
  review(processId?: string | null, asOf?: string | null): Observable<ObjectiveReview> {
    let params = new HttpParams();
    if (processId) params = params.set('processId', processId);
    if (asOf) params = params.set('asOf', asOf);
    return this.http.get<ObjectiveReview>(this.endpoint, { params });
  }

  get(id: string): Observable<QualityObjective> {
    return this.http.get<QualityObjective>(`${this.endpoint}/${id}`);
  }

  /** L'historique des propositions d'un objectif : acceptées, rejetées, en attente. */
  proposals(id: string): Observable<ObjectiveProposal[]> {
    return this.http.get<ObjectiveProposal[]>(`${this.endpoint}/${id}/proposals`);
  }

  create(request: CreateObjectiveRequest): Observable<QualityObjective> {
    return this.http.post<QualityObjective>(this.endpoint, request);
  }

  update(id: string, request: UpdateObjectiveRequest): Observable<QualityObjective> {
    return this.http.patch<QualityObjective>(`${this.endpoint}/${id}`, request);
  }

  /** Retire du suivi, sans effacer : l'historique des CAPA ouvertes y tient. */
  archive(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  /** Confronte un objectif à ses mesures ; `null` si la règle ne déclenche pas. */
  evaluate(id: string): Observable<ObjectiveProposal | null> {
    return this.http.post<ObjectiveProposal | null>(`${this.endpoint}/${id}/evaluate`, {});
  }

  /** Confronte tous les objectifs suivis, et rend les propositions nées du passage. */
  evaluateAll(): Observable<ObjectiveProposal[]> {
    return this.http.post<ObjectiveProposal[]>(`${this.endpoint}/evaluate`, {});
  }

  /** « Accepter et enregistrer la CAPA » : le dossier naît. */
  accept(proposalId: string): Observable<ObjectiveProposal> {
    return this.http.post<ObjectiveProposal>(
      `${this.endpoint}/proposals/${proposalId}/accept`, {});
  }

  /** « Rejeter avec justification » : le motif est obligatoire. */
  reject(proposalId: string, reason: string): Observable<ObjectiveProposal> {
    return this.http.post<ObjectiveProposal>(
      `${this.endpoint}/proposals/${proposalId}/reject`, { reason });
  }
}
