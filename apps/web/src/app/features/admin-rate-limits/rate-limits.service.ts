import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CRITICAL_RATIO, RateLimitDecision, RateLimitOverview, RateLimitPolicy, RateLimitRow,
  RateLimitTotals, UNLIMITED_LIMIT, UpsertPolicyRequest, UsageLevel, WARNING_RATIO
} from './rate-limits.types';

/** Ordre de gravité : la ligne la plus tendue remonte en tête de tableau. */
const LEVEL_RANK: Record<UsageLevel, number> = {
  exceeded: 0, critical: 1, warning: 2, normal: 3, idle: 4, unmeasured: 5
};

/**
 * Quotas et limites d'API du tenant (§13.1).
 *
 * L'API `/api/v1/rate-limits` n'avait aucun consommateur : les quotas ne se pilotaient
 * qu'en appelant le service à la main, et surtout la CONSOMMATION courante
 * (`GET /check/{scope}`, lecture sans incrément) n'était visible nulle part — un tenant
 * pouvait frôler sa limite sans que personne ne le voie avant les premiers 429.
 *
 * Le tenant vient du JWT côté serveur (règle §18.2 #2) : aucune méthode ne le prend
 * en paramètre.
 */
@Injectable({ providedIn: 'root' })
export class RateLimitsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/rate-limits`;

  constructor(private readonly http: HttpClient) {}

  /** Politiques du tenant courant. */
  list(): Observable<RateLimitPolicy[]> {
    return this.http.get<RateLimitPolicy[]>(`${this.endpoint}/policies`);
  }

  get(id: string): Observable<RateLimitPolicy> {
    return this.http.get<RateLimitPolicy>(`${this.endpoint}/policies/${encodeURIComponent(id)}`);
  }

  /**
   * Consommation de la fenêtre courante pour un périmètre.
   *
   * Le périmètre est encodé : il transite dans le CHEMIN et peut contenir `.`, `:` ou `-`.
   * Le serveur le contraint par regex, mais un champ de formulaire reste une entrée
   * utilisateur — on n'envoie jamais de segment de chemin brut (OWASP A03).
   */
  check(scope: string): Observable<RateLimitDecision> {
    return this.http.get<RateLimitDecision>(`${this.endpoint}/check/${encodeURIComponent(scope)}`);
  }

  upsert(req: UpsertPolicyRequest): Observable<RateLimitPolicy> {
    return this.http.put<RateLimitPolicy>(`${this.endpoint}/policies`, req);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/policies/${encodeURIComponent(id)}`);
  }

  /**
   * Enregistre une politique (création ou mise à jour).
   *
   * Il n'y a pas de doublon à nettoyer : la contrainte d'unicité (tenant_id, scope)
   * garantit une seule ligne par périmètre. Une version antérieure de ce service
   * supprimait une prétendue « ligne résiduelle » — un scénario que la base rend
   * impossible, et dont trois tests validaient la fiction sur des mocks.
   *
   * Le vrai défaut était côté serveur : la mise à jour ne retrouvait pas une
   * politique suspendue et tentait une insertion, d'où un 409 qui rendait la
   * suspension irréversible. Il est corrigé dans `RateLimitService.upsert`.
   */
  savePolicy(req: UpsertPolicyRequest): Observable<RateLimitPolicy> {
    return this.upsert(req);
  }

  /**
   * Vue de l'écran : chaque politique appliquée est confrontée à sa consommation réelle.
   *
   * Une politique suspendue n'est pas sondée : le serveur répondrait « illimité » (aucune
   * politique ACTIVE pour ce périmètre), ce qui se lirait à tort comme « rien consommé ».
   */
  overview(): Observable<RateLimitOverview> {
    return this.list().pipe(
      switchMap(policies => {
        const probed = policies.filter(p => p.enabled);
        // forkJoin([]) COMPLÈTE sans jamais émettre : sans ce court-circuit, un tenant
        // n'ayant que des politiques suspendues verrait un écran vide et un spinner figé.
        const usages$: Observable<(RateLimitDecision | null)[]> = probed.length
          ? forkJoin(probed.map(p => this.check(p.scope).pipe(catchError(() => of(null)))))
          : of([]);
        return usages$.pipe(map(usages => {
          const byScope = new Map<string, RateLimitDecision | null>();
          probed.forEach((p, i) => byScope.set(p.scope, usages[i]));
          return buildOverview(policies, byScope);
        }));
      })
    );
  }
}

/** Assemble les lignes et les compteurs de synthèse à partir des données serveur. */
function buildOverview(
  policies: RateLimitPolicy[],
  usageByScope: Map<string, RateLimitDecision | null>
): RateLimitOverview {
  const rows = policies
    .map(policy => toRow(policy, usageByScope.get(policy.scope) ?? null))
    .sort((a, b) => LEVEL_RANK[a.level] - LEVEL_RANK[b.level]
      || b.ratio - a.ratio
      || a.policy.scope.localeCompare(b.policy.scope));

  const totals: RateLimitTotals = {
    policies: rows.length,
    enforced: rows.filter(r => r.policy.enabled).length,
    nearLimit: rows.filter(r => r.level === 'critical').length,
    exceeded: rows.filter(r => r.level === 'exceeded').length
  };
  return { rows, totals };
}

function toRow(policy: RateLimitPolicy, usage: RateLimitDecision | null): RateLimitRow {
  // Une politique suspendue, une sonde en échec ou une réponse « illimitée » ne
  // décrivent aucune consommation : on l'affiche comme non mesurée plutôt que 0 %.
  if (!policy.enabled || !usage || usage.limit >= UNLIMITED_LIMIT || usage.limit <= 0) {
    return { policy, usage, used: 0, ratio: 0, level: 'unmeasured' };
  }
  // Le restant est borné [0..limite] côté serveur, mais une jauge qui afficherait
  // « 120 / 100 » ou une barre à 130 % serait illisible : on borne aussi ici.
  const used = Math.min(usage.limit, Math.max(0, usage.limit - usage.remaining));
  const ratio = used / usage.limit;
  return { policy, usage, used, ratio, level: levelOf(usage.allowed, ratio, used) };
}

function levelOf(allowed: boolean, ratio: number, used: number): UsageLevel {
  if (!allowed) return 'exceeded';
  if (ratio >= CRITICAL_RATIO) return 'critical';
  if (ratio >= WARNING_RATIO) return 'warning';
  return used === 0 ? 'idle' : 'normal';
}
