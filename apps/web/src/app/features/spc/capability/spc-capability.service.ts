import { HttpClient, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  ImportedGrid, SpcSettings, StudyRequest, StudySheet, StudySummary
} from './spc-capability.types';

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

  /**
   * Le classeur modèle de la grille de mesures, aux dimensions de l'étude en cours.
   *
   * <p>La réponse complète est demandée, pas seulement son corps : c'est l'en-tête
   * `Content-Disposition` du serveur qui nomme le fichier enregistré.
   */
  measurementsTemplate(sampleSize: number, subgroupCount: number): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.base}/studies/measurements-template.xlsx`, {
      params: { sampleSize, subgroupCount },
      responseType: 'blob',
      observe: 'response'
    });
  }

  /**
   * Lit un classeur rempli et rend la grille, sans rien enregistrer.
   *
   * <p>Le fichier part en multipart ; le serveur vérifie son type, ses premiers
   * octets et sa taille avant d'ouvrir l'archive. Rien n'est enregistré : l'écran
   * pose la grille lue dans sa grille de saisie, et c'est l'utilisateur qui
   * enregistre l'étude après l'avoir relue.
   */
  importMeasurements(file: File): Observable<ImportedGrid> {
    const corps = new FormData();
    corps.append('file', file, file.name);
    return this.http.post<ImportedGrid>(`${this.base}/studies/measurements-import`, corps);
  }

  settings(): Observable<SpcSettings> {
    return this.http.get<SpcSettings>(`${this.base}/settings`);
  }

  updateSettings(capableThreshold: number, watchThreshold: number): Observable<SpcSettings> {
    return this.http.put<SpcSettings>(`${this.base}/settings`, { capableThreshold, watchThreshold });
  }
}
