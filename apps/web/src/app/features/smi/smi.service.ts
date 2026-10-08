import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RequirementsMatrix, SmiDashboard } from './smi.types';

/** Le tableau de bord SMI. Lecture seule ; le client vient du jeton. */
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
}
