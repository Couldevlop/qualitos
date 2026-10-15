import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RequirementsMatrix, SmiDashboard, StandardCandidate, StandardRef } from './smi.types';

/**
 * Le tableau de bord SMI ; le client vient du jeton. Lecture, sauf l'ajout
 * d'une norme, réservé à l'administrateur du client (le serveur refuse les
 * autres en 403).
 */
@Injectable({ providedIn: 'root' })
export class SmiService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/smi`;

  constructor(private readonly http: HttpClient) {}

  /** @param standard code d'une norme adoptée ; absent : toutes. */
  dashboard(standard?: string | null): Observable<SmiDashboard> {
    const params = standard ? new HttpParams().set('standard', standard) : undefined;
    return this.http.get<SmiDashboard>(`${this.endpoint}/dashboard`, { params });
  }

  requirementsMatrix(): Observable<RequirementsMatrix> {
    return this.http.get<RequirementsMatrix>(`${this.endpoint}/requirements-matrix`);
  }

  // ---------- ajout d'une norme (administrateur du client) ----------

  /** Les normes de système de management encore à adopter, les suggérées en tête. */
  candidates(): Observable<StandardCandidate[]> {
    return this.http.get<StandardCandidate[]>(`${this.endpoint}/standards/candidates`);
  }

  /** Adopte la norme : la même adoption que dans le Standards Hub, feuille de route comprise. */
  addStandard(standardId: string): Observable<StandardRef> {
    return this.http.post<StandardRef>(`${this.endpoint}/standards`, { standardId });
  }
}
