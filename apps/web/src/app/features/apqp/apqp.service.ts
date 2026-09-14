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
  ApqpProject,
  ApqpReorderRequest,
  CreateApqpPhaseRequest,
  CreateApqpProjectRequest,
  UpdateApqpPhaseRequest,
  UpdateApqpProjectRequest
} from './apqp.types';

/**
 * Les projets APQP, et le cycle de chacun.
 *
 * <p>Toutes les routes du cycle sont désormais NICHÉES sous leur projet : un
 * client mène plusieurs projets de front, et une route globale n'aurait pas su
 * duquel elle parlait. D'où le `projectId` en premier argument de chaque méthode
 * de cycle — la seule façon de rendre l'oubli impossible à compiler.
 *
 * <p>Les écritures rendent la PHASE entière, pas seulement l'élément touché :
 * ajouter un livrable renumérote ses voisins, en retirer un resserre les rangs.
 * Rendre l'ensemble évite à l'écran de recomposer un état qu'il devinerait, et
 * de le deviner faux.
 *
 * <p>Deux écritures font exception et rendent le CYCLE entier : déclarer où en
 * est un livrable et réinitialiser. Déclarer change le compte du dossier PPAP,
 * qui n'appartient à aucune phase en particulier.
 */
@Injectable({ providedIn: 'root' })
export class ApqpService {

  private readonly racine = `${environment.apiBaseUrl}/api/v1/apqp/projects`;

  constructor(private readonly http: HttpClient) {}

  // ---------- projets ----------

  /** Les projets du client, du plus récent au plus ancien (ordre posé par le serveur). */
  projects(): Observable<ApqpProject[]> {
    return this.http.get<ApqpProject[]>(this.racine);
  }

  project(projectId: string): Observable<ApqpProject> {
    return this.http.get<ApqpProject>(`${this.racine}/${projectId}`);
  }

  createProject(input: CreateApqpProjectRequest): Observable<ApqpProject> {
    return this.http.post<ApqpProject>(this.racine, input);
  }

  updateProject(projectId: string, input: UpdateApqpProjectRequest): Observable<ApqpProject> {
    return this.http.put<ApqpProject>(`${this.racine}/${projectId}`, input);
  }

  /** Destructif : emporte le cycle, ses livrables et les pièces qui les prouvent. */
  deleteProject(projectId: string): Observable<void> {
    return this.http.delete<void>(`${this.racine}/${projectId}`);
  }

  // ---------- cycle d'un projet ----------

  /** Le cycle du projet et l'état de son dossier PPAP, amorcé côté serveur à la première lecture. */
  cycle(projectId: string): Observable<ApqpCycle> {
    return this.http.get<ApqpCycle>(this.phases(projectId));
  }

  /**
   * Rend au projet le cycle du référentiel, en effaçant le sien.
   *
   * <p>Destructif : l'écran demande confirmation, le serveur ne la redemande pas.
   */
  reset(projectId: string): Observable<ApqpCycle> {
    return this.http.post<ApqpCycle>(`${this.phases(projectId)}/reset`, {});
  }

  createPhase(projectId: string, input: CreateApqpPhaseRequest): Observable<ApqpPhase> {
    return this.http.post<ApqpPhase>(this.phases(projectId), input);
  }

  updatePhase(
    projectId: string, phaseId: string, input: UpdateApqpPhaseRequest
  ): Observable<ApqpPhase> {
    return this.http.put<ApqpPhase>(`${this.phases(projectId)}/${phaseId}`, input);
  }

  deletePhase(projectId: string, phaseId: string): Observable<void> {
    return this.http.delete<void>(`${this.phases(projectId)}/${phaseId}`);
  }

  /** Le cycle entier dans son nouvel ordre — un ordre partiel est refusé. */
  reorder(projectId: string, input: ApqpReorderRequest): Observable<ApqpPhase[]> {
    return this.http.put<ApqpPhase[]>(`${this.phases(projectId)}/order`, input);
  }

  addDeliverable(
    projectId: string, phaseId: string, input: ApqpDeliverableRequest
  ): Observable<ApqpPhase> {
    return this.http.post<ApqpPhase>(
      `${this.phases(projectId)}/${phaseId}/deliverables`, input);
  }

  updateDeliverable(
    projectId: string, phaseId: string, deliverableId: string, input: ApqpDeliverableRequest
  ): Observable<ApqpPhase> {
    return this.http.put<ApqpPhase>(
      `${this.phases(projectId)}/${phaseId}/deliverables/${deliverableId}`, input);
  }

  deleteDeliverable(
    projectId: string, phaseId: string, deliverableId: string
  ): Observable<ApqpPhase> {
    return this.http.delete<ApqpPhase>(
      `${this.phases(projectId)}/${phaseId}/deliverables/${deliverableId}`);
  }

  /**
   * Déclare où en est un livrable.
   *
   * <p>Rend le cycle entier, parce que le compte du dossier PPAP en dépend.
   */
  completeDeliverable(
    projectId: string, phaseId: string, deliverableId: string, input: ApqpCompletionRequest
  ): Observable<ApqpCycle> {
    return this.http.put<ApqpCycle>(
      `${this.phases(projectId)}/${phaseId}/deliverables/${deliverableId}/completion`, input);
  }

  /** Les pièces d'un livrable, chacune avec son URL de lecture à vie courte. */
  evidences(
    projectId: string, phaseId: string, deliverableId: string
  ): Observable<ApqpEvidence[]> {
    return this.http.get<ApqpEvidence[]>(
      this.evidenceEndpoint(projectId, phaseId, deliverableId));
  }

  /**
   * Verse une pièce.
   *
   * <p>Le champ s'appelle `file`, comme le serveur l'attend : c'est le seul nom
   * qu'il lit, et un autre produirait un 400 sans rien dire de lisible.
   */
  uploadEvidence(
    projectId: string, phaseId: string, deliverableId: string, file: File
  ): Observable<ApqpEvidence> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ApqpEvidence>(
      this.evidenceEndpoint(projectId, phaseId, deliverableId), form);
  }

  deleteEvidence(
    projectId: string, phaseId: string, deliverableId: string, evidenceId: string
  ): Observable<void> {
    return this.http.delete<void>(
      `${this.evidenceEndpoint(projectId, phaseId, deliverableId)}/${evidenceId}`);
  }

  // ---------- interne ----------

  private phases(projectId: string): string {
    return `${this.racine}/${projectId}/phases`;
  }

  private evidenceEndpoint(
    projectId: string, phaseId: string, deliverableId: string
  ): string {
    return `${this.phases(projectId)}/${phaseId}/deliverables/${deliverableId}/evidences`;
  }
}
