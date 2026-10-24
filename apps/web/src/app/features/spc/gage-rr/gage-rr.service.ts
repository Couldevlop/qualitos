import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { GageRrRequest, GageRrSheet, GageRrSummary } from './gage-rr.types';

/**
 * Les études Gage R&R (ADR 0097). Le tenant et l'auteur viennent du jeton
 * côté serveur, jamais d'ici ; le %GRR, le ndc et le verdict sont recalculés
 * par le serveur.
 */
@Injectable({ providedIn: 'root' })
export class GageRrService {

  private readonly base = `${environment.apiBaseUrl}/api/v1/spc/gage-rr`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<GageRrSummary[]> {
    return this.http.get<GageRrSummary[]>(this.base);
  }

  get(id: string): Observable<GageRrSheet> {
    return this.http.get<GageRrSheet>(`${this.base}/${encodeURIComponent(id)}`);
  }

  create(req: GageRrRequest): Observable<GageRrSheet> {
    return this.http.post<GageRrSheet>(this.base, req);
  }

  update(id: string, req: GageRrRequest): Observable<GageRrSheet> {
    return this.http.put<GageRrSheet>(`${this.base}/${encodeURIComponent(id)}`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${encodeURIComponent(id)}`);
  }
}
