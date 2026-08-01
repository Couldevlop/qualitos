import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, defer, throwError } from 'rxjs';
import { map } from 'rxjs/operators';

import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';
import {
  API_KEY_EXPIRE_DUE_DEFAULT, API_KEY_EXPIRE_DUE_MAX, API_KEY_EXPIRE_DUE_MIN,
  API_KEY_EXPIRING_SOON_DAYS, ApiKeyCounts, ApiKeyCreateInput, ApiKeyStatus, ApiKeyView,
  ApiKeysOverview, IssuedApiKey, MissingActorError
} from './api-keys.types';

/**
 * Clés d'API du tenant (§13.1).
 *
 * L'API `/api/v1/api-keys` n'avait aucun consommateur : émettre ou révoquer une clé
 * imposait un appel HTTP manuel, ce qui est intenable pour une action de sécurité.
 *
 * Le tenant est dérivé du JWT côté serveur (règle §18.2 #2) : aucune méthode ne le
 * prend en paramètre. L'`actor` — exigé non nul par `ApiKeyWebDto` — est résolu ici à
 * partir de la session, jamais fourni par l'écran : une UI ne doit pas pouvoir
 * désigner un autre utilisateur comme auteur d'une révocation (OWASP A01).
 */
@Injectable({ providedIn: 'root' })
export class ApiKeysService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/api-keys`;

  constructor(
    private readonly http: HttpClient,
    private readonly auth: AuthService
  ) {}

  /** Clés du tenant courant, dans l'ordre brut du serveur. */
  list(): Observable<ApiKeyView[]> {
    return this.http.get<ApiKeyView[]>(this.endpoint);
  }

  /** Détail d'une clé. Le serveur renvoie 404 sur une clé d'un autre tenant. */
  get(id: string): Observable<ApiKeyView> {
    return this.http.get<ApiKeyView>(`${this.endpoint}/${encodeURIComponent(id)}`);
  }

  /**
   * Vue de l'écran : liste ordonnée + compteurs. Les compteurs sont calculés ici et
   * non côté serveur (aucune route ne les expose) ; les dériver de la même liste
   * garantit qu'un chiffre affiché correspond toujours aux lignes visibles.
   */
  overview(): Observable<ApiKeysOverview> {
    return this.list().pipe(map(keys => {
      const sorted = [...keys].sort(compareKeys);
      return { keys: sorted, counts: countKeys(sorted, Date.now()) };
    }));
  }

  /**
   * Émet une clé. La réponse porte le secret en clair : elle ne doit ni être
   * journalisée ni être conservée par l'appelant au-delà de son affichage unique.
   */
  create(input: ApiKeyCreateInput): Observable<IssuedApiKey> {
    return this.withActor(actor => this.http.post<IssuedApiKey>(this.endpoint, {
      name: input.name,
      scopes: input.scopes,
      expiresAt: input.expiresAt,
      actor
    }));
  }

  /**
   * Rotation : le serveur révoque la clé visée et en émet une NOUVELLE (nouvel `id`,
   * nouveau préfixe) qui hérite du nom, des scopes et de l'échéance. L'ancienne cesse
   * donc d'être utilisable immédiatement — c'est irréversible côté appelants.
   */
  rotate(id: string): Observable<IssuedApiKey> {
    return this.withActor(actor =>
      this.http.post<IssuedApiKey>(`${this.endpoint}/${encodeURIComponent(id)}/rotate`, { actor }));
  }

  /** Révocation définitive : le serveur n'offre aucun chemin de retour vers ACTIVE. */
  revoke(id: string): Observable<ApiKeyView> {
    return this.withActor(actor =>
      this.http.post<ApiKeyView>(`${this.endpoint}/${encodeURIComponent(id)}/revoke`, { actor }));
  }

  /**
   * Passe en EXPIRED les clés actives dont l'échéance est dépassée, et renvoie le
   * nombre traité. Le serveur borne `limit` à [1, 500] : on borne aussi ici pour ne
   * pas provoquer un 400 depuis un appel programmatique.
   */
  expireDue(limit: number = API_KEY_EXPIRE_DUE_DEFAULT): Observable<number> {
    const bounded = Math.max(API_KEY_EXPIRE_DUE_MIN,
      Math.min(API_KEY_EXPIRE_DUE_MAX, Math.trunc(limit) || API_KEY_EXPIRE_DUE_DEFAULT));
    return this.http
      .post<{ expired: number }>(`${this.endpoint}/expire-due`, null,
        { params: new HttpParams().set('limit', bounded) })
      .pipe(map(res => res?.expired ?? 0));
  }

  /**
   * `defer` : l'acteur est lu à l'ABONNEMENT, pas à la construction du flux. Sans
   * cela, une action préparée avant un rafraîchissement de session partirait avec
   * l'ancien identifiant.
   */
  private withActor<T>(call: (actor: string) => Observable<T>): Observable<T> {
    return defer(() => {
      const actor = this.auth.snapshot()?.userId;
      if (!actor) return throwError(() => new MissingActorError());
      return call(actor);
    });
  }
}

/** Rang d'affichage : ce qui est encore utilisable d'abord, l'historique ensuite. */
const STATUS_RANK: Record<ApiKeyStatus, number> = { ACTIVE: 0, EXPIRED: 1, REVOKED: 2 };

/** Actives en tête, puis les plus récentes d'abord à statut égal. */
function compareKeys(a: ApiKeyView, b: ApiKeyView): number {
  const byStatus = (STATUS_RANK[a.status] ?? 9) - (STATUS_RANK[b.status] ?? 9);
  if (byStatus !== 0) return byStatus;
  return (b.createdAt ?? '').localeCompare(a.createdAt ?? '');
}

function countKeys(keys: ApiKeyView[], nowMs: number): ApiKeyCounts {
  const horizon = nowMs + API_KEY_EXPIRING_SOON_DAYS * 24 * 60 * 60 * 1000;
  const counts: ApiKeyCounts = { total: keys.length, active: 0, expiringSoon: 0, expired: 0, revoked: 0 };
  for (const key of keys) {
    if (key.status === 'ACTIVE') {
      counts.active++;
      const due = key.expiresAt ? Date.parse(key.expiresAt) : NaN;
      if (!Number.isNaN(due) && due <= horizon) counts.expiringSoon++;
    } else if (key.status === 'EXPIRED') {
      counts.expired++;
    } else if (key.status === 'REVOKED') {
      counts.revoked++;
    }
  }
  return counts;
}
