import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { CircuitRun, CircuitStep, CircuitSubject, CircuitView } from './circuit.types';

/**
 * Les circuits de validation : les régler (exige `authz.manage`), et voir où
 * en est un objet (ouvert à tout membre du client). Les décisions passent par
 * le module métier — approuver ou refuser une version de document.
 */
@Injectable({ providedIn: 'root' })
export class CircuitService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/circuits`;

  constructor(private readonly http: HttpClient) {}

  circuit(subject: CircuitSubject): Observable<CircuitView> {
    return this.http.get<CircuitView>(`${this.endpoint}/${subject}`);
  }

  /** Remplace le circuit ; une liste vide le retire (retour à l'approbation simple). */
  save(subject: CircuitSubject, steps: CircuitStep[]): Observable<CircuitView> {
    return this.http.put<CircuitView>(`${this.endpoint}/${subject}`, { steps });
  }

  /** Le dernier passage de l'objet, ou `null` s'il n'est jamais entré dans un circuit. */
  run(subject: CircuitSubject, subjectId: string): Observable<CircuitRun | null> {
    return this.http.get<CircuitRun>(`${this.endpoint}/${subject}/runs/${encodeURIComponent(subjectId)}`,
      { observe: 'response' }).pipe(map(r => r.status === 204 ? null : r.body));
  }
}
