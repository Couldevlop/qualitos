import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { SpringPage } from '../pdca/pdca.types';
import {
  CreateFiveWhysRequest, FiveWhysAnalysis, FiveWhysStep
} from './five-whys.types';

/**
 * Analyses des 5 Pourquoi. Le tenant vient du jeton côté serveur : aucune
 * méthode ne le prend en paramètre (§18.2 #2).
 */
@Injectable({ providedIn: 'root' })
export class FiveWhysService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/five-whys`;

  constructor(private readonly http: HttpClient) {}

  list(page = 0, size = 20): Observable<SpringPage<FiveWhysAnalysis>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<SpringPage<FiveWhysAnalysis>>(this.endpoint, { params });
  }

  /** Analyses ouvertes sur une non-conformité donnée. */
  listForNc(ncId: string): Observable<FiveWhysAnalysis[]> {
    return this.http.get<FiveWhysAnalysis[]>(this.endpoint, {
      params: new HttpParams().set('ncId', ncId)
    });
  }

  get(id: string): Observable<FiveWhysAnalysis> {
    return this.http.get<FiveWhysAnalysis>(`${this.endpoint}/${id}`);
  }

  create(input: CreateFiveWhysRequest): Observable<FiveWhysAnalysis> {
    return this.http.post<FiveWhysAnalysis>(this.endpoint, input);
  }

  updateProblem(id: string, problem: string): Observable<FiveWhysAnalysis> {
    return this.http.patch<FiveWhysAnalysis>(`${this.endpoint}/${id}/problem`, { problem });
  }

  addStep(id: string, answer: string): Observable<FiveWhysStep> {
    return this.http.post<FiveWhysStep>(`${this.endpoint}/${id}/steps`, { answer });
  }

  updateStep(stepId: string, answer: string): Observable<FiveWhysStep> {
    return this.http.patch<FiveWhysStep>(`${this.endpoint}/steps/${stepId}`, { answer });
  }

  /** Seul le dernier pourquoi peut être retiré : la chaîne se lit dans l'ordre. */
  deleteStep(stepId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/steps/${stepId}`);
  }

  setRootCause(id: string, rootCause: string): Observable<FiveWhysAnalysis> {
    return this.http.put<FiveWhysAnalysis>(`${this.endpoint}/${id}/root-cause`, { rootCause });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }
}
