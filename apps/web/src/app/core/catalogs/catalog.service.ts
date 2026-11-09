import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map, shareReplay, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  Catalog,
  CatalogEntry,
  CatalogEntryForm,
  CatalogName,
  CatalogSummary
} from './catalog.types';

/**
 * Les listes déroulantes du client : celles qu'il a réglées, sinon celles de la
 * plateforme.
 *
 * <p>Deux usages, et une règle qui les sépare.
 *
 * <ul>
 *   <li><b>Les écrans de saisie</b> appellent {@link labels} : ils reçoivent les
 *       mots du client, ou <code>null</code> quand il n'a rien réglé. Dans ce
 *       second cas ils gardent leurs propres libellés, traduits en six langues —
 *       afficher le défaut français à un utilisateur arabophone serait une
 *       régression, et traduire les mots d'un client serait les trahir.</li>
 *   <li><b>L'écran de paramétrage</b> appelle {@link catalog}, {@link replace} et
 *       {@link reset}.</li>
 * </ul>
 *
 * <p>Les lectures sont mises en cache : une liste déroulante est demandée par
 * plusieurs écrans, parfois plusieurs fois par écran, et ne change qu'au
 * paramétrage — qui vide le cache.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/settings/catalogs`;

  private readonly cache = new Map<CatalogName, Observable<Catalog | null>>();

  constructor(private readonly http: HttpClient) {}

  /** Toutes les listes ouvertes au paramétrage, avec leur état. */
  summaries(): Observable<CatalogSummary[]> {
    return this.http.get<CatalogSummary[]>(this.endpoint);
  }

  /** Une liste, telle que le serveur la rend. */
  catalog(name: CatalogName): Observable<Catalog> {
    return this.http.get<Catalog>(`${this.endpoint}/${name}`);
  }

  /**
   * Les entrées actives, dans l'ordre du client — ou <code>null</code> s'il n'a
   * rien réglé, et s'il faut donc garder les libellés de l'écran.
   *
   * <p>Un échec réseau rend aussi <code>null</code> : un formulaire doit rester
   * utilisable quand le paramétrage est injoignable. Ses libellés d'origine sont
   * alors employés, ce qui est exactement le comportement d'avant.
   */
  entries(name: CatalogName): Observable<CatalogEntry[] | null> {
    return this.resolve(name).pipe(
      map(c => (c && !c.defaults ? c.entries.filter(e => e.active) : null)));
  }

  /**
   * Les libellés du client, indexés par code — ou <code>null</code> s'il n'a
   * rien réglé.
   */
  labels(name: CatalogName): Observable<Record<string, string> | null> {
    return this.entries(name).pipe(map(entries => {
      if (!entries) {
        return null;
      }
      const table: Record<string, string> = {};
      entries.forEach(e => (table[e.code] = e.label));
      return table;
    }));
  }

  /** Remplace une liste. L'ordre envoyé est l'ordre d'affichage. */
  replace(name: CatalogName, entries: CatalogEntryForm[]): Observable<Catalog> {
    return this.http.put<Catalog>(`${this.endpoint}/${name}`, { entries })
      .pipe(tap(() => this.invalidate(name)));
  }

  /** Revient aux valeurs de la plateforme. */
  reset(name: CatalogName): Observable<Catalog> {
    return this.http.post<Catalog>(`${this.endpoint}/${name}/reset`, {})
      .pipe(tap(() => this.invalidate(name)));
  }

  /** Vide le cache d'une liste, ou de toutes. */
  invalidate(name?: CatalogName): void {
    if (name) {
      this.cache.delete(name);
    } else {
      this.cache.clear();
    }
  }

  private resolve(name: CatalogName): Observable<Catalog | null> {
    const connu = this.cache.get(name);
    if (connu) {
      return connu;
    }
    const lecture = this.catalog(name).pipe(
      catchError(() => of(null)),
      // `refCount: false` : le cache survit au dernier abonné, sinon chaque
      // écran qui se ferme relancerait l'appel pour le suivant.
      shareReplay({ bufferSize: 1, refCount: false }));
    this.cache.set(name, lecture);
    return lecture;
  }
}
