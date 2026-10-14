import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, shareReplay, tap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  DIRECTORY_MAX_PAGE_SIZE,
  DIRECTORY_MAX_TERM_LENGTH,
  DirectoryPage,
  DirectoryUser
} from './directory.types';

/** Durée de vie d'un nom en cache : assez pour une page de liste, assez court pour suivre un renommage. */
export const DIRECTORY_CACHE_TTL_MS = 60_000;
/** Au-delà, les plus anciennes entrées sont oubliées. */
export const DIRECTORY_CACHE_MAX = 500;

/** Personnes fictives du mode démonstration sans serveur. */
const DEMO_PEOPLE: DirectoryUser[] = [
  { id: '00000000-0000-4000-8000-000000000001', displayName: 'Camille Bernard', email: 'camille.bernard@demo.qualitos.io', active: true },
  { id: '00000000-0000-4000-8000-000000000002', displayName: 'Élodie Martin', email: 'elodie.martin@demo.qualitos.io', active: true },
  { id: '00000000-0000-4000-8000-000000000003', displayName: 'Karim Haddad', email: 'karim.haddad@demo.qualitos.io', active: true },
  { id: '00000000-0000-4000-8000-000000000004', displayName: 'Sophie Laurent', email: 'sophie.laurent@demo.qualitos.io', active: true }
];

/** Même forme de comparaison que le serveur : sans accents, minuscules, espaces resserrés. */
export function normalizeSearch(text: string): string {
  return text.normalize('NFD').replace(/\p{M}+/gu, '').toLowerCase().replace(/\s+/g, ' ').trim();
}

/**
 * L'annuaire du client en lecture restreinte (ADR 0091), partagé par tous les
 * écrans qui désignent un responsable (audits, CAPA, changements…).
 *
 * <p>Ouvert à tout membre authentifié, à la différence de `/api/v1/users`
 * réservé aux administrateurs. Le client vient du jeton côté serveur : aucune
 * méthode ne le prend en paramètre.
 *
 * <p>`get(id)` garde le nom quelques instants : une liste de vingt dossiers au
 * même responsable ne fait qu'un appel. Les résultats d'une recherche
 * alimentent ce cache.
 */
@Injectable({ providedIn: 'root' })
export class DirectoryService {

  // Chemin servi par api-core : l'ingress et le proxy on-premise routent
  // `/api/v1/directory` vers lui, le reste de `/api` vers le moteur qualité.
  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/directory/users`;
  private readonly byId = new Map<string, { expires: number; user$: Observable<DirectoryUser> }>();

  constructor(private readonly http: HttpClient) {}

  /**
   * Membres actifs dont le nom ou l'adresse contient `q` (accents et casse
   * ignorés), triés par nom. Le terme est rogné à la borne du serveur plutôt
   * que de provoquer un refus.
   */
  search(q: string, size = 20, page = 0): Observable<DirectoryPage> {
    const term = (q ?? '').trim().slice(0, DIRECTORY_MAX_TERM_LENGTH);
    const boundedSize = Math.min(Math.max(1, Math.floor(size)), DIRECTORY_MAX_PAGE_SIZE);
    const boundedPage = Math.max(0, Math.floor(page));
    if (environment.useMockApi) {
      return of(this.demoSearch(term, boundedSize, boundedPage)).pipe(tap(p => this.remember(p.content)));
    }
    let params = new HttpParams().set('page', String(boundedPage)).set('size', String(boundedSize));
    if (term) {
      params = params.set('q', term);
    }
    return this.http.get<DirectoryPage>(this.endpoint, { params }).pipe(tap(p => this.remember(p.content)));
  }

  /** Une personne du client, active ou non ; erreur 404 si elle n'en fait pas partie. */
  get(id: string): Observable<DirectoryUser> {
    const now = Date.now();
    const hit = this.byId.get(id);
    if (hit && hit.expires > now) {
      return hit.user$;
    }
    const user$ = this.fetch(id).pipe(
      catchError(err => {
        // Un échec ne se met pas en cache : le prochain affichage réessaie.
        this.byId.delete(id);
        return throwError(() => err);
      }),
      shareReplay({ bufferSize: 1, refCount: false })
    );
    this.store(id, user$, now);
    return user$;
  }

  /** Oublie les noms gardés (changement de session, de client). */
  clearCache(): void {
    this.byId.clear();
  }

  private fetch(id: string): Observable<DirectoryUser> {
    if (environment.useMockApi) {
      const person = DEMO_PEOPLE.find(p => p.id === id);
      return person ? of(person) : throwError(() => ({ status: 404 }));
    }
    return this.http.get<DirectoryUser>(`${this.endpoint}/${encodeURIComponent(id)}`);
  }

  private remember(users: DirectoryUser[]): void {
    const now = Date.now();
    for (const u of users) {
      this.store(u.id, of(u), now);
    }
  }

  private store(id: string, user$: Observable<DirectoryUser>, now: number): void {
    this.byId.delete(id);
    this.byId.set(id, { expires: now + DIRECTORY_CACHE_TTL_MS, user$ });
    while (this.byId.size > DIRECTORY_CACHE_MAX) {
      const oldest = this.byId.keys().next().value as string;
      this.byId.delete(oldest);
    }
  }

  private demoSearch(term: string, size: number, page: number): DirectoryPage {
    const needle = normalizeSearch(term);
    const all = DEMO_PEOPLE
      .filter(p => normalizeSearch(`${p.displayName} ${p.email}`).includes(needle))
      .sort((a, b) => a.displayName.localeCompare(b.displayName));
    return {
      content: all.slice(page * size, page * size + size),
      page,
      size,
      totalElements: all.length,
      totalPages: Math.ceil(all.length / size)
    };
  }
}
