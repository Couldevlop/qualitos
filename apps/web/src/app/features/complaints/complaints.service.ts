import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { ComplaintsPage } from './complaints.types';

/**
 * Le registre des réclamations clients en lecture (§4.9).
 *
 * <p>Le tenant vient du jeton, jamais d'ici (§18.2 #2).
 */
@Injectable({ providedIn: 'root' })
export class ComplaintsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/complaints`;

  constructor(private readonly http: HttpClient) {}

  /**
   * Les réclamations, page par page.
   *
   * @param page rang de la page, à partir de 0
   * @param size taille de la page
   */
  list(page = 0, size = 50): Observable<ComplaintsPage> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ComplaintsPage>(this.endpoint, { params });
  }
}
