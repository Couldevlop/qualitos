import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  Nis2MeasureCategory,
  Nis2MeasureEditRequest,
  Nis2MeasurePlanRequest,
  Nis2MeasureReviewRequest,
  Nis2MeasureStatus,
  Nis2MeasureVerifyRequest,
  Nis2MeasureView
} from './nis2m.types';

@Injectable({ providedIn: 'root' })
export class Nis2MeasuresService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/nis2/risk-measures`;
  private readonly mockStore: Nis2MeasureView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: Nis2MeasureStatus): Observable<Nis2MeasureView[]> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      const f = status ? this.mockStore.filter(m => m.status === status) : this.mockStore;
      return of([...f]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<Nis2MeasureView[]>(this.endpoint, { params });
  }

  listByCategory(category: Nis2MeasureCategory): Observable<Nis2MeasureView[]> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      return of(this.mockStore.filter(m => m.category === category)).pipe(delay(120));
    }
    return this.http.get<Nis2MeasureView[]>(`${this.endpoint}/by-category`, {
      params: new HttpParams().set('category', category)
    });
  }

  reviewOverdue(limit = 100): Observable<Nis2MeasureView[]> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      return of(this.mockStore.filter(m => m.reviewOverdue).slice(0, limit)).pipe(delay(120));
    }
    return this.http.get<Nis2MeasureView[]>(`${this.endpoint}/review-overdue`, {
      params: new HttpParams().set('limit', String(limit))
    });
  }

  get(id: string): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      const m = this.mockStore.find(x => x.id === id);
      return m ? of(structuredClone(m)).pipe(delay(80))
               : throwError(() => this.err(404, 'Mesure introuvable.'));
    }
    return this.http.get<Nis2MeasureView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      const m = this.mockStore.find(x => x.reference === reference);
      return m ? of(structuredClone(m)).pipe(delay(80))
               : throwError(() => this.err(404, 'Référence introuvable.'));
    }
    return this.http.get<Nis2MeasureView>(`${this.endpoint}/by-reference`, {
      params: new HttpParams().set('reference', reference)
    });
  }

  plan(req: Nis2MeasurePlanRequest): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      if (this.mockStore.some(m => m.reference === req.reference)) {
        return throwError(() => this.err(409, 'Référence déjà utilisée.'));
      }
      if (req.residualRiskRating === 'CRITICAL' && !req.criticalRiskJustification?.trim()) {
        return throwError(() => this.err(422, 'Justification obligatoire pour un risque résiduel CRITICAL.'));
      }
      const now = new Date().toISOString();
      const m: Nis2MeasureView = {
        id: this.uuid(),
        tenantId: '00000000-0000-0000-0000-000000000001',
        reference: req.reference,
        category: req.category,
        title: req.title,
        description: req.description ?? null,
        status: 'PLANNED',
        ownerUserId: req.ownerUserId ?? null,
        maturityLevel: req.maturityLevel,
        residualRiskRating: req.residualRiskRating,
        criticalRiskJustification: req.criticalRiskJustification ?? null,
        reviewIntervalDays: req.reviewIntervalDays,
        effectiveFrom: null, effectiveTo: null,
        lastReviewedAt: null, reviewedByUserId: null, nextReviewDueAt: null,
        evidenceUrls: req.evidenceUrls ?? [],
        linkedProcessingActivityIds: req.linkedProcessingActivityIds ?? [],
        linkedProcessorAgreementIds: req.linkedProcessorAgreementIds ?? [],
        notes: req.notes ?? null,
        createdByUserId: req.createdByUserId,
        createdAt: now, updatedAt: now,
        reviewOverdue: false,
        criticalResidualRisk: req.residualRiskRating === 'CRITICAL'
      };
      this.mockStore.unshift(m);
      return of(structuredClone(m)).pipe(delay(140));
    }
    return this.http.post<Nis2MeasureView>(this.endpoint, req);
  }

  edit(id: string, req: Nis2MeasureEditRequest): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      const m = this.mockStore.find(x => x.id === id);
      if (!m) return throwError(() => this.err(404, 'Mesure introuvable.'));
      if (m.status === 'DEPRECATED') return throwError(() => this.err(409, 'Mesure désactivée — édition impossible.'));
      if (req.residualRiskRating === 'CRITICAL' && !req.criticalRiskJustification?.trim()) {
        return throwError(() => this.err(422, 'Justification obligatoire pour un risque résiduel CRITICAL.'));
      }
      m.title = req.title;
      m.description = req.description ?? null;
      m.ownerUserId = req.ownerUserId ?? null;
      m.maturityLevel = req.maturityLevel;
      m.residualRiskRating = req.residualRiskRating;
      m.criticalRiskJustification = req.criticalRiskJustification ?? null;
      m.reviewIntervalDays = req.reviewIntervalDays;
      m.evidenceUrls = req.evidenceUrls ?? [];
      m.linkedProcessingActivityIds = req.linkedProcessingActivityIds ?? [];
      m.linkedProcessorAgreementIds = req.linkedProcessorAgreementIds ?? [];
      m.notes = req.notes ?? null;
      m.criticalResidualRisk = req.residualRiskRating === 'CRITICAL';
      m.updatedAt = new Date().toISOString();
      return of(structuredClone(m)).pipe(delay(120));
    }
    return this.http.put<Nis2MeasureView>(`${this.endpoint}/${id}`, req);
  }

  startImplementation(id: string): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      return this.transition(id, 'PLANNED', 'IN_PROGRESS', 'Démarrage possible uniquement depuis PLANNED.');
    }
    return this.http.post<Nis2MeasureView>(`${this.endpoint}/${id}/start`, {});
  }

  markImplemented(id: string): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      const m = this.mockStore.find(x => x.id === id);
      if (!m) return throwError(() => this.err(404, 'Mesure introuvable.'));
      if (m.status !== 'IN_PROGRESS') return throwError(() => this.err(409, 'Marquage impossible — statut courant : ' + m.status + '.'));
      m.status = 'IMPLEMENTED';
      m.effectiveFrom = new Date().toISOString();
      m.updatedAt = new Date().toISOString();
      return of(structuredClone(m)).pipe(delay(120));
    }
    return this.http.post<Nis2MeasureView>(`${this.endpoint}/${id}/implemented`, {});
  }

  verify(id: string, req: Nis2MeasureVerifyRequest): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      const m = this.mockStore.find(x => x.id === id);
      if (!m) return throwError(() => this.err(404, 'Mesure introuvable.'));
      if (m.status !== 'IMPLEMENTED') return throwError(() => this.err(409, 'Vérification possible uniquement depuis IMPLEMENTED.'));
      m.status = 'VERIFIED';
      m.lastReviewedAt = req.reviewedAt;
      m.reviewedByUserId = req.reviewedByUserId;
      m.nextReviewDueAt = this.computeNextReview(req.reviewedAt, m.reviewIntervalDays);
      m.updatedAt = new Date().toISOString();
      m.reviewOverdue = false;
      return of(structuredClone(m)).pipe(delay(120));
    }
    return this.http.post<Nis2MeasureView>(`${this.endpoint}/${id}/verify`, req);
  }

  review(id: string, req: Nis2MeasureReviewRequest): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      const m = this.mockStore.find(x => x.id === id);
      if (!m) return throwError(() => this.err(404, 'Mesure introuvable.'));
      if (m.status !== 'VERIFIED') return throwError(() => this.err(409, 'Revue périodique possible uniquement après VERIFIED.'));
      m.lastReviewedAt = req.reviewedAt;
      m.reviewedByUserId = req.reviewedByUserId;
      m.nextReviewDueAt = this.computeNextReview(req.reviewedAt, m.reviewIntervalDays);
      m.updatedAt = new Date().toISOString();
      m.reviewOverdue = false;
      return of(structuredClone(m)).pipe(delay(120));
    }
    return this.http.post<Nis2MeasureView>(`${this.endpoint}/${id}/review`, req);
  }

  deprecate(id: string): Observable<Nis2MeasureView> {
    if (environment.useMockApi) {
      const m = this.mockStore.find(x => x.id === id);
      if (!m) return throwError(() => this.err(404, 'Mesure introuvable.'));
      if (m.status === 'DEPRECATED') return throwError(() => this.err(409, 'Mesure déjà désactivée.'));
      m.status = 'DEPRECATED';
      m.effectiveTo = new Date().toISOString();
      m.updatedAt = new Date().toISOString();
      return of(structuredClone(m)).pipe(delay(120));
    }
    return this.http.post<Nis2MeasureView>(`${this.endpoint}/${id}/deprecate`, {});
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(m => m.id === id);
      if (i < 0) return throwError(() => this.err(404, 'Mesure introuvable.'));
      this.mockStore.splice(i, 1);
      return of(void 0).pipe(delay(100));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private transition(
    id: string,
    from: Nis2MeasureStatus,
    to: Nis2MeasureStatus,
    msg: string
  ): Observable<Nis2MeasureView> {
    const m = this.mockStore.find(x => x.id === id);
    if (!m) return throwError(() => this.err(404, 'Mesure introuvable.'));
    if (m.status !== from) return throwError(() => this.err(409, msg));
    m.status = to;
    m.updatedAt = new Date().toISOString();
    return of(structuredClone(m)).pipe(delay(120));
  }

  private computeNextReview(reviewedAt: string, intervalDays: number): string {
    const t = new Date(reviewedAt).getTime() + intervalDays * 86400000;
    return new Date(t).toISOString();
  }

  private recomputeOverdue(): void {
    const now = Date.now();
    for (const m of this.mockStore) {
      m.reviewOverdue = m.status === 'VERIFIED' && !!m.nextReviewDueAt
                        && new Date(m.nextReviewDueAt).getTime() < now;
    }
  }

  private err(status: number, title: string) {
    return { status, error: { type: 'about:blank', title, status, detail: title } };
  }

  private uuid(): string {
    return 'nis2m-' + Math.random().toString(16).slice(2, 10) + '-' + Date.now().toString(16);
  }

  private seed(): Nis2MeasureView[] {
    const now = Date.now();
    const day = 86400000;
    const iso = (n: number) => new Date(now - n * day).toISOString();
    return [
      {
        id: 'nis2m-seed-001', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'NIS2-MFA-001', category: 'MFA_AND_COMMUNICATIONS',
        title: 'MFA obligatoire pour les administrateurs et accès distants',
        description: 'Authentification multi-facteurs FIDO2/WebAuthn imposée sur Keycloak pour tout rôle privilégié.',
        status: 'VERIFIED', ownerUserId: '00000000-0000-0000-0000-000000000999',
        maturityLevel: 4, residualRiskRating: 'LOW', criticalRiskJustification: null,
        reviewIntervalDays: 180,
        effectiveFrom: iso(120), effectiveTo: null,
        lastReviewedAt: iso(15), reviewedByUserId: '00000000-0000-0000-0000-000000000999',
        nextReviewDueAt: new Date(now + 165 * day).toISOString(),
        evidenceUrls: ['https://wiki.qualitos.local/security/mfa-policy.pdf'],
        linkedProcessingActivityIds: [], linkedProcessorAgreementIds: [],
        notes: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(200), updatedAt: iso(15),
        reviewOverdue: false, criticalResidualRisk: false
      },
      {
        id: 'nis2m-seed-002', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'NIS2-BCP-002', category: 'BUSINESS_CONTINUITY',
        title: 'Plan de continuité d\'activité — sauvegarde 3-2-1 + PRA testé',
        description: 'Sauvegardes chiffrées triple-site, restauration testée chaque trimestre.',
        status: 'IN_PROGRESS', ownerUserId: '00000000-0000-0000-0000-000000000999',
        maturityLevel: 3, residualRiskRating: 'MEDIUM', criticalRiskJustification: null,
        reviewIntervalDays: 90,
        effectiveFrom: null, effectiveTo: null,
        lastReviewedAt: null, reviewedByUserId: null, nextReviewDueAt: null,
        evidenceUrls: [],
        linkedProcessingActivityIds: [], linkedProcessorAgreementIds: [],
        notes: 'PRA à valider avec DSI courant 2026.',
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(40), updatedAt: iso(5),
        reviewOverdue: false, criticalResidualRisk: false
      },
      {
        id: 'nis2m-seed-003', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'NIS2-SUPCHAIN-003', category: 'SUPPLY_CHAIN_SECURITY',
        title: 'Audit de sécurité des sous-traitants critiques',
        description: 'Évaluation cyber des fournisseurs hébergeurs et SaaS critiques (audit annuel + clauses sécurité contractuelles).',
        status: 'VERIFIED', ownerUserId: '00000000-0000-0000-0000-000000000999',
        maturityLevel: 2, residualRiskRating: 'CRITICAL',
        criticalRiskJustification: 'Dépendance forte à un hébergeur cloud unique — risque concentration nécessitant attention direction.',
        reviewIntervalDays: 90,
        effectiveFrom: iso(180), effectiveTo: null,
        lastReviewedAt: iso(100), reviewedByUserId: '00000000-0000-0000-0000-000000000999',
        nextReviewDueAt: iso(10),
        evidenceUrls: [],
        linkedProcessingActivityIds: [], linkedProcessorAgreementIds: [],
        notes: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(200), updatedAt: iso(100),
        reviewOverdue: true, criticalResidualRisk: true
      }
    ];
  }
}
