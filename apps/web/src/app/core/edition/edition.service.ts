import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { Edition, EditionView, SAAS } from './edition.types';

/**
 * L'édition de l'installation et l'état de sa licence (ADR 0082).
 *
 * <p>Lue une fois par session : une licence se renouvelle rarement, et le
 * bandeau se met à jour au prochain chargement. Une lecture qui échoue ne cache
 * rien et ne bloque rien : l'écran retombe sur l'édition SaaS, et le serveur
 * reste seul juge — il refusera en 403 ou 404 ce que l'écran aurait montré.
 */
@Injectable({ providedIn: 'root' })
export class EditionService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/edition`;
  private cached$?: Observable<EditionView>;

  constructor(private readonly http: HttpClient) {}

  current(): Observable<EditionView> {
    if (!this.cached$) {
      this.cached$ = (environment.useMockApi ? of(SAAS) : this.http.get<EditionView>(this.endpoint)).pipe(
        catchError(() => of(SAAS)),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.cached$;
  }

  edition(): Observable<Edition> {
    return this.current().pipe(map(v => v.edition));
  }
}
