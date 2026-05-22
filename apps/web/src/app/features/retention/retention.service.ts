import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateRetentionRuleRequest,
  EditRetentionRuleRequest,
  ErasureEvaluation,
  RetentionRuleStatus,
  RetentionRuleView,
  RetentionUnit
} from './retention.types';

/**
 * Helpers ISO-8601 Duration ↔ UI (amount + unit).
 *
 * Java Duration accepte uniquement les unités temps-based : days (PnD)
 * et plus petit (hours/min/sec). Pas de mois/années. Pour offrir une
 * UX naturelle, on convertit :
 *   - "MONTH" en N*30 jours
 *   - "YEAR"  en N*365 jours
 * Le serializer back-end produit du PnD canonique.
 */
export function durationToDays(iso: string): number {
  if (!iso) return 0;
  const m = iso.match(/^P(?:(\d+)D)?(?:T(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?$/);
  if (!m) return 0;
  const days  = parseInt(m[1] ?? '0', 10);
  const hours = parseInt(m[2] ?? '0', 10);
  return days + Math.floor(hours / 24);
}

export function daysToDuration(amount: number, unit: RetentionUnit): string {
  let days = amount;
  if (unit === 'MONTH') days = amount * 30;
  if (unit === 'YEAR')  days = amount * 365;
  return 'P' + Math.max(0, Math.round(days)) + 'D';
}

/** Returns a human-readable label like "3 ans (1095 j)". */
export function describeDuration(iso: string): string {
  const days = durationToDays(iso);
  if (days >= 365 && days % 365 === 0) {
    const y = days / 365;
    return `${y} an${y > 1 ? 's' : ''} (${days} j)`;
  }
  if (days >= 30 && days % 30 === 0) {
    const m = days / 30;
    return `${m} mois (${days} j)`;
  }
  return `${days} jour${days > 1 ? 's' : ''}`;
}

@Injectable({ providedIn: 'root' })
export class RetentionService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/retention-rules`;
  private readonly mockStore: RetentionRuleView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: RetentionRuleStatus): Observable<RetentionRuleView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(r => r.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<RetentionRuleView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<RetentionRuleView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(r => r.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<RetentionRuleView>(`${this.endpoint}/${id}`);
  }

  create(input: CreateRetentionRuleRequest): Observable<RetentionRuleView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const r: RetentionRuleView = {
        id: 'ret-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        dataCategoryCode: input.dataCategoryCode,
        dataCategoryLabel: input.dataCategoryLabel,
        retentionPeriod: input.retentionPeriod,
        legalBasis: input.legalBasis,
        lawfulBasisReference: input.lawfulBasisReference,
        status: 'DRAFT',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(r);
      return of(r).pipe(delay(150));
    }
    return this.http.post<RetentionRuleView>(this.endpoint, input);
  }

  edit(id: string, input: EditRetentionRuleRequest): Observable<RetentionRuleView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (r) {
        r.dataCategoryLabel = input.dataCategoryLabel;
        r.retentionPeriod = input.retentionPeriod;
        r.legalBasis = input.legalBasis;
        r.lawfulBasisReference = input.lawfulBasisReference;
        r.updatedAt = new Date().toISOString();
        return of(r).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<RetentionRuleView>(`${this.endpoint}/${id}`, input);
  }

  activate(id: string): Observable<RetentionRuleView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (r) {
        // OWASP A04 — mirror backend invariant : on activation, archive any
        // other currently ACTIVE rule for the same dataCategoryCode (one
        // active rule per category at a time).
        for (const other of this.mockStore) {
          if (other.id !== r.id
              && other.dataCategoryCode === r.dataCategoryCode
              && other.status === 'ACTIVE') {
            other.status = 'ARCHIVED';
            other.effectiveTo = now;
            other.updatedAt = now;
          }
        }
        r.status = 'ACTIVE';
        r.effectiveFrom = now;
        r.updatedAt = now;
        return of(r).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<RetentionRuleView>(`${this.endpoint}/${id}/activate`, {});
  }

  archive(id: string): Observable<RetentionRuleView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (r) {
        r.status = 'ARCHIVED';
        r.effectiveTo = now;
        r.updatedAt = now;
        return of(r).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<RetentionRuleView>(`${this.endpoint}/${id}/archive`, {});
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(r => r.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  evaluateErasure(dataCategoryCode: string, recordCreatedAt: string): Observable<ErasureEvaluation | null> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x =>
        x.dataCategoryCode === dataCategoryCode && x.status === 'ACTIVE');
      if (!r) return of(null).pipe(delay(120));
      const created = new Date(recordCreatedAt);
      const days = durationToDays(r.retentionPeriod);
      const erasure = new Date(created.getTime() + days * 86400000);
      return of({
        dataCategoryCode,
        recordCreatedAt,
        erasureAt: erasure.toISOString(),
        dueNow: erasure.getTime() <= Date.now(),
        ruleId: r.id,
        retentionPeriod: r.retentionPeriod
      }).pipe(delay(120));
    }
    const params = new HttpParams()
      .set('dataCategoryCode', dataCategoryCode)
      .set('recordCreatedAt', recordCreatedAt);
    return this.http.get<ErasureEvaluation | null>(`${this.endpoint}/erasure-evaluation`, { params });
  }

  // ---- Mock seed ----

  private seed(): RetentionRuleView[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'ret-1', tenantId: 'demo-tenant',
        dataCategoryCode: 'rh.bulletin_paie',
        dataCategoryLabel: 'Bulletins de paie salariés',
        retentionPeriod: 'P1825D', // 5 ans
        legalBasis: 'Code du travail L3243-4 (durée minimale de conservation des bulletins de paie). RGPD Art. 6.1.c (obligation légale).',
        lawfulBasisReference: 'https://www.legifrance.gouv.fr/codes/article_lc/LEGIARTI000033019975',
        status: 'ACTIVE',
        effectiveFrom: now,
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'ret-2', tenantId: 'demo-tenant',
        dataCategoryCode: 'crm.prospect',
        dataCategoryLabel: 'Données de prospection commerciale B2B',
        retentionPeriod: 'P1095D', // 3 ans
        legalBasis: 'Recommandation CNIL — 3 ans à compter du dernier contact actif. RGPD Art. 6.1.f (intérêt légitime).',
        lawfulBasisReference: 'https://www.cnil.fr/fr/la-prospection-commerciale-par-courrier-electronique',
        status: 'ACTIVE',
        effectiveFrom: now,
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'ret-3', tenantId: 'demo-tenant',
        dataCategoryCode: 'cctv.video',
        dataCategoryLabel: 'Enregistrements vidéosurveillance',
        retentionPeriod: 'P30D',
        legalBasis: 'Recommandation CNIL : 1 mois maximum sauf besoin spécifique sécurité.',
        status: 'DRAFT',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }
}
