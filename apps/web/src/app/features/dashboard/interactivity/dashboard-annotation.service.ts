import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import { CreateAnnotationRequest, DashboardAnnotation } from './dashboard-annotation.types';

/**
 * Accès HTTP aux annotations de dashboard (§7.3).
 *
 * tenantId et authorId sont résolus côté API depuis le JWT validé — jamais
 * envoyés dans le corps de requête (§18.2 #2). Le corps des annotations est du
 * texte brut, échappé à l'affichage par Angular (interpolation, pas innerHTML).
 */
@Injectable()
export class DashboardAnnotationService {

  private readonly baseUrl = environment.apiBaseUrl + '/api/v1/dashboards/annotations';

  constructor(private readonly http: HttpClient) {}

  /** Annotations d'un graphique (chartKey), plus récentes d'abord. */
  list(chartKey: string): Observable<DashboardAnnotation[]> {
    const params = new HttpParams().set('chartKey', chartKey);
    return this.http.get<DashboardAnnotation[]>(this.baseUrl, { params });
  }

  /** Crée une annotation sur un graphique. */
  create(req: CreateAnnotationRequest): Observable<DashboardAnnotation> {
    return this.http.post<DashboardAnnotation>(this.baseUrl, req);
  }

  /** Supprime une annotation (autorisé seulement si l'API le permet : 403 sinon). */
  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
