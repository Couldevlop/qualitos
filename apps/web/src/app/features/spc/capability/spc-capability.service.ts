import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { SpcSettings, StudyRequest, StudySheet, StudySummary } from './spc-capability.types';

/**
 * Les études de capabilité SPC (ADR 0096). Le tenant et l'auteur viennent du
 * jeton côté serveur, jamais d'ici ; Cp et Cpk sont recalculés par le serveur.
 */
@Injectable({ providedIn: 'root' })
export class SpcCapabilityService {

  private readonly base = `${environment.apiBaseUrl}/api/v1/spc`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<StudySummary[]> {
    return this.http.get<StudySummary[]>(`${this.base}/studies`);
  }

  get(id: string): Observable<StudySheet> {
    return this.http.get<StudySheet>(`${this.base}/studies/${encodeURIComponent(id)}`);
  }

  create(req: StudyRequest): Observable<StudySheet> {
    return this.http.post<StudySheet>(`${this.base}/studies`, req);
  }

  update(id: string, req: StudyRequest): Observable<StudySheet> {
    return this.http.put<StudySheet>(`${this.base}/studies/${encodeURIComponent(id)}`, req);
  }

  setActionPlan(id: string, actionPlan: boolean): Observable<StudySummary> {
    return this.http.patch<StudySummary>(`${this.base}/studies/${encodeURIComponent(id)}/action-plan`,
      { actionPlan });
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/studies/${encodeURIComponent(id)}`);
  }

  settings(): Observable<SpcSettings> {
    return this.http.get<SpcSettings>(`${this.base}/settings`);
  }

  updateSettings(capableThreshold: number, watchThreshold: number): Observable<SpcSettings> {
    return this.http.put<SpcSettings>(`${this.base}/settings`, { capableThreshold, watchThreshold });
  }
}
