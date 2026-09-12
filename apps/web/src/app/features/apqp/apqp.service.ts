import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ApqpCompletionRequest,
  ApqpCycle,
  ApqpDeliverableRequest,
  ApqpEvidence,
  ApqpPhase,
  ApqpReorderRequest,
  CreateApqpPhaseRequest,
  UpdateApqpPhaseRequest
} from './apqp.types';

/**
 * Le cycle APQP d'un client.
 *
 * <p>Les écritures rendent la PHASE entière, pas seulement l'élément touché :
 * ajouter un livrable renumérote ses voisins, en retirer un resserre les rangs.
 * Rendre l'ensemble évite à l'écran de recomposer un état qu'il devinerait, et
 * de le deviner faux.
 *
 * <p>Deux écritures font exception et rendent le CYCLE entier : cocher un
 * livrable et réinitialiser. Cocher change le compte du dossier PPAP affiché
 * sous le schéma, qui n'appartient à aucune phase en particulier.
 */
@Injectable({ providedIn: 'root' })
export class ApqpService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/apqp/phases`;

  constructor(private readonly http: HttpClient) {}

  /** Le cycle et l'état de son dossier PPAP, amorcé côté serveur à la première lecture. */
  cycle(): Observable<ApqpCycle> {
    return this.http.get<ApqpCycle>(this.endpoint);
  }

  /**
   * Rend au client le cycle du référentiel, en effaçant le sien.
   *
   * <p>Destructif : l'écran demande confirmation, le serveur ne la redemande pas.
   */
  reset(): Observable<ApqpCycle> {
    return this.http.post<ApqpCycle>(`${this.endpoint}/reset`, {});
  }

  createPhase(input: CreateApqpPhaseRequest): Observable<ApqpPhase> {
    return this.http.post<ApqpPhase>(this.endpoint, input);
  }

  updatePhase(phaseId: string, input: UpdateApqpPhaseRequest): Observable<ApqpPhase> {
    return this.http.put<ApqpPhase>(`${this.endpoint}/${phaseId}`, input);
  }

  deletePhase(phaseId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${phaseId}`);
  }

  /** Le cycle entier dans son nouvel ordre — un ordre partiel est refusé. */
  reorder(input: ApqpReorderRequest): Observable<ApqpPhase[]> {
    return this.http.put<ApqpPhase[]>(`${this.endpoint}/order`, input);
  }

  addDeliverable(phaseId: string, input: ApqpDeliverableRequest): Observable<ApqpPhase> {
    return this.http.post<ApqpPhase>(`${this.endpoint}/${phaseId}/deliverables`, input);
  }

  updateDeliverable(
    phaseId: string, deliverableId: string, input: ApqpDeliverableRequest
  ): Observable<ApqpPhase> {
    return this.http.put<ApqpPhase>(
      `${this.endpoint}/${phaseId}/deliverables/${deliverableId}`, input);
  }

  deleteDeliverable(phaseId: string, deliverableId: string): Observable<ApqpPhase> {
    return this.http.delete<ApqpPhase>(
      `${this.endpoint}/${phaseId}/deliverables/${deliverableId}`);
  }

  /**
   * Déclare où en est un livrable.
   *
   * <p>Rend le cycle entier, parce que le compte du dossier PPAP en dépend.
   */
  completeDeliverable(
    phaseId: string, deliverableId: string, input: ApqpCompletionRequest
  ): Observable<ApqpCycle> {
    return this.http.put<ApqpCycle>(
      `${this.endpoint}/${phaseId}/deliverables/${deliverableId}/completion`, input);
  }

  /** Les pièces d'un livrable, chacune avec son URL de lecture à vie courte. */
  evidences(phaseId: string, deliverableId: string): Observable<ApqpEvidence[]> {
    return this.http.get<ApqpEvidence[]>(this.evidenceEndpoint(phaseId, deliverableId));
  }

  /**
   * Verse une pièce.
   *
   * <p>Le champ s'appelle `file`, comme le serveur l'attend : c'est le seul nom
   * qu'il lit, et un autre produirait un 400 sans rien dire de lisible.
   */
  uploadEvidence(phaseId: string, deliverableId: string, file: File): Observable<ApqpEvidence> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApqpEvidence>(this.evidenceEndpoint(phaseId, deliverableId), form);
  }

  deleteEvidence(
    phaseId: string, deliverableId: string, evidenceId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.evidenceEndpoint(phaseId, deliverableId)}/${evidenceId}`);
  }

  private evidenceEndpoint(phaseId: string, deliverableId: string): string {
    return `${this.endpoint}/${phaseId}/deliverables/${deliverableId}/evidences`;
  }
}
