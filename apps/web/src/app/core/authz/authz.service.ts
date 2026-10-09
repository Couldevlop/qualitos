import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, take } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { AuthzMe, CatalogEntry, MemberRoles, PermissionCode, RoleCommand, RoleView } from './authz.types';

/** Ce qu'on sait des droits de l'utilisateur courant : pas encore lus, ou lus. */
type State = { loaded: false } | { loaded: true; me: AuthzMe };

/**
 * Les droits de l'utilisateur courant, et l'administration des droits du client.
 *
 * <p>L'écran n'affiche que ce qui servira : un bouton dont l'utilisateur n'a
 * pas l'action ne s'affiche pas. Ce n'est qu'un confort — le serveur reste
 * seul juge et refuse en 403 ce que l'écran aurait laissé passer.
 *
 * <p>Les droits sont lus une fois à la première demande, puis relus après
 * chaque changement fait dans l'administration ({@link refresh}). Si la lecture
 * échoue, l'utilisateur n'a AUCUN droit affiché : mieux vaut un bouton absent
 * qu'un bouton qui mènera à un refus.
 */
@Injectable({ providedIn: 'root' })
export class AuthzService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/authz`;
  private readonly state$ = new BehaviorSubject<State>({ loaded: false });
  private requested = false;

  constructor(private readonly http: HttpClient) {}

  /** Les droits de l'utilisateur courant, dès qu'ils sont connus. */
  me(): Observable<AuthzMe> {
    this.ensureLoaded();
    return this.state$.pipe(
      filter((s): s is { loaded: true; me: AuthzMe } => s.loaded),
      map(s => s.me)
    );
  }

  /** Vrai si l'utilisateur a au moins une de ces actions. Émet à chaque relecture. */
  can(codes: PermissionCode | PermissionCode[]): Observable<boolean> {
    const voulues = Array.isArray(codes) ? codes : [codes];
    return this.me().pipe(
      map(me => me.permissions.includes('*') || voulues.some(c => me.permissions.includes(c))),
      distinctUntilChanged()
    );
  }

  /** Relit les droits — après un changement de rôle, ou une reconnexion. */
  refresh(): void {
    this.requested = true;
    this.load();
  }

  private ensureLoaded(): void {
    if (this.requested) return;
    this.requested = true;
    this.load();
  }

  private load(): void {
    if (environment.useMockApi) {
      // Démonstration sans serveur : l'utilisateur fictif a tout.
      this.state$.next({ loaded: true, me: { userId: null, roles: ['QUALITY_MANAGER'], permissions: ['*'] } });
      return;
    }
    this.http.get<AuthzMe>(`${this.endpoint}/me`).pipe(
      catchError(() => of<AuthzMe>({ userId: null, roles: [], permissions: [] })),
      take(1)
    ).subscribe(me => this.state$.next({ loaded: true, me }));
  }

  // ---------- administration (exige authz.manage côté serveur) ----------

  catalog(): Observable<CatalogEntry[]> {
    return this.http.get<CatalogEntry[]>(`${this.endpoint}/catalog`);
  }

  roles(): Observable<RoleView[]> {
    return this.http.get<RoleView[]>(`${this.endpoint}/roles`);
  }

  createRole(cmd: RoleCommand): Observable<RoleView> {
    return this.http.post<RoleView>(`${this.endpoint}/roles`, cmd);
  }

  updateRole(code: string, cmd: RoleCommand): Observable<RoleView> {
    return this.http.put<RoleView>(`${this.endpoint}/roles/${encodeURIComponent(code)}`, cmd);
  }

  /** Supprime un rôle sur mesure, ou rend à un rôle système ses droits livrés. */
  deleteRole(code: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/roles/${encodeURIComponent(code)}`);
  }

  members(): Observable<MemberRoles[]> {
    return this.http.get<MemberRoles[]>(`${this.endpoint}/members`);
  }

  setMemberRoles(userId: string, roles: string[]): Observable<MemberRoles> {
    return this.http.put<MemberRoles>(`${this.endpoint}/members/${encodeURIComponent(userId)}/roles`, { roles });
  }
}
