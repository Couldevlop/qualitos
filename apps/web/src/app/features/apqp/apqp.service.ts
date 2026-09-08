import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  ApqpDeliverableRequest,
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
 */
@Injectable({ providedIn: 'root' })
export class ApqpService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/apqp/phases`;

  constructor(private readonly http: HttpClient) {}

  /** Le cycle, amorcé côté serveur à la première lecture. */
  cycle(): Observable<ApqpPhase[]> {
    return this.http.get<ApqpPhase[]>(this.endpoint);
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
}
