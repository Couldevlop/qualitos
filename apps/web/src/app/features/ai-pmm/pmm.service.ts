import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  FREQUENCY_DAYS,
  PmmCloseRequest,
  PmmDraftRequest,
  PmmEditRequest,
  PmmPlanStatus,
  PmmPlanView,
  PmmReviewRequest,
  PmmSuspendRequest
} from './pmm.types';

@Injectable({ providedIn: 'root' })
export class PmmService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/pmm`;
  private readonly mockStore: PmmPlanView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: PmmPlanStatus): Observable<PmmPlanView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(p => p.status === status) : this.mockStore;
      return of([...f]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<PmmPlanView[]>(this.endpoint, { params });
  }

  listByAiSystem(aiSystemId: string): Observable<PmmPlanView[]> {
    if (environment.useMockApi) {
      return of(this.mockStore.filter(p => p.aiSystemId === aiSystemId)).pipe(delay(120));
    }
    return this.http.get<PmmPlanView[]>(`${this.endpoint}/by-system`, {
      params: new HttpParams().set('aiSystemId', aiSystemId)
    });
  }

  overdueReviews(limit = 200): Observable<PmmPlanView[]> {
    if (environment.useMockApi) {
      const now = Date.now();
      return of(this.mockStore.filter(p =>
        p.status === 'ACTIVE' && p.nextReviewDueAt && new Date(p.nextReviewDueAt).getTime() < now
      ).slice(0, limit)).pipe(delay(120));
    }
    return this.http.get<PmmPlanView[]>(`${this.endpoint}/overdue-reviews`, {
      params: new HttpParams().set('limit', String(limit))
    });
  }

  get(id: string): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      return p ? of(structuredClone(p)).pipe(delay(80))
               : throwError(() => this.err(404, 'Plan PMM introuvable.'));
    }
    return this.http.get<PmmPlanView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.reference === reference);
      return p ? of(structuredClone(p)).pipe(delay(80))
               : throwError(() => this.err(404, 'Référence introuvable.'));
    }
    return this.http.get<PmmPlanView>(`${this.endpoint}/by-reference`, {
      params: new HttpParams().set('reference', reference)
    });
  }

  draft(req: PmmDraftRequest): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      if (this.mockStore.some(p => p.reference === req.reference)) {
        return throwError(() => this.err(409, 'Référence déjà utilisée.'));
      }
      const now = new Date().toISOString();
      const p: PmmPlanView = {
        id: this.uuid(),
        tenantId: '00000000-0000-0000-0000-000000000001',
        reference: req.reference, aiSystemId: req.aiSystemId,
        name: req.name, description: req.description ?? null,
        metricsMonitored: req.metricsMonitored ?? null,
        collectionMethod: req.collectionMethod ?? null,
        reviewFrequency: req.reviewFrequency,
        responsiblePartyDescription: req.responsiblePartyDescription ?? null,
        triggerCriteria: req.triggerCriteria ?? null,
        qmsLinkReference: req.qmsLinkReference ?? null,
        status: 'DRAFT',
        activatedAt: null,
        lastReviewedAt: null, lastReviewedByUserId: null,
        nextReviewDueAt: null,
        suspendedAt: null, suspensionReason: null,
        effectiveTo: null, closureReason: null,
        createdByUserId: req.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(p);
      return of(structuredClone(p)).pipe(delay(140));
    }
    return this.http.post<PmmPlanView>(this.endpoint, req);
  }

  edit(id: string, req: PmmEditRequest): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      if (!p) return throwError(() => this.err(404, 'Plan PMM introuvable.'));
      if (p.status === 'CLOSED') return throwError(() => this.err(409, 'Plan clos — édition impossible.'));
      p.name = req.name;
      p.description = req.description ?? null;
      p.metricsMonitored = req.metricsMonitored ?? null;
      p.collectionMethod = req.collectionMethod ?? null;
      p.reviewFrequency = req.reviewFrequency;
      p.responsiblePartyDescription = req.responsiblePartyDescription ?? null;
      p.triggerCriteria = req.triggerCriteria ?? null;
      p.qmsLinkReference = req.qmsLinkReference ?? null;
      if (p.lastReviewedAt) {
        p.nextReviewDueAt = this.nextReview(p.lastReviewedAt, req.reviewFrequency);
      }
      p.updatedAt = new Date().toISOString();
      return of(structuredClone(p)).pipe(delay(120));
    }
    return this.http.put<PmmPlanView>(`${this.endpoint}/${id}`, req);
  }

  activate(id: string): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      if (!p) return throwError(() => this.err(404, 'Plan PMM introuvable.'));
      if (p.status !== 'DRAFT' && p.status !== 'SUSPENDED') {
        return throwError(() => this.err(409, 'Activation possible uniquement depuis DRAFT/SUSPENDED.'));
      }
      const now = new Date().toISOString();
      if (p.status === 'DRAFT') p.activatedAt = now;
      p.status = 'ACTIVE';
      p.suspendedAt = null; p.suspensionReason = null;
      p.updatedAt = now;
      return of(structuredClone(p)).pipe(delay(120));
    }
    return this.http.post<PmmPlanView>(`${this.endpoint}/${id}/activate`, {});
  }

  recordReview(id: string, req: PmmReviewRequest): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      if (!p) return throwError(() => this.err(404, 'Plan PMM introuvable.'));
      if (p.status !== 'ACTIVE') return throwError(() => this.err(409, 'Revue possible uniquement sur plan ACTIVE.'));
      const now = new Date().toISOString();
      p.lastReviewedAt = now;
      p.lastReviewedByUserId = req.reviewedByUserId;
      p.nextReviewDueAt = this.nextReview(now, p.reviewFrequency);
      p.updatedAt = now;
      return of(structuredClone(p)).pipe(delay(120));
    }
    return this.http.post<PmmPlanView>(`${this.endpoint}/${id}/record-review`, req);
  }

  suspend(id: string, req: PmmSuspendRequest): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      if (!p) return throwError(() => this.err(404, 'Plan PMM introuvable.'));
      if (p.status !== 'ACTIVE') return throwError(() => this.err(409, 'Suspension possible uniquement depuis ACTIVE.'));
      const now = new Date().toISOString();
      p.status = 'SUSPENDED';
      p.suspendedAt = now;
      p.suspensionReason = req.reason;
      p.updatedAt = now;
      return of(structuredClone(p)).pipe(delay(120));
    }
    return this.http.post<PmmPlanView>(`${this.endpoint}/${id}/suspend`, req);
  }

  close(id: string, req: PmmCloseRequest): Observable<PmmPlanView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      if (!p) return throwError(() => this.err(404, 'Plan PMM introuvable.'));
      if (p.status === 'CLOSED') return throwError(() => this.err(409, 'Plan déjà clos.'));
      const now = new Date().toISOString();
      p.status = 'CLOSED';
      p.effectiveTo = now;
      p.closureReason = req.reason;
      p.updatedAt = now;
      return of(structuredClone(p)).pipe(delay(120));
    }
    return this.http.post<PmmPlanView>(`${this.endpoint}/${id}/close`, req);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(p => p.id === id);
      if (i < 0) return throwError(() => this.err(404, 'Plan PMM introuvable.'));
      this.mockStore.splice(i, 1);
      return of(void 0).pipe(delay(100));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private nextReview(from: string, freq: keyof typeof FREQUENCY_DAYS): string {
    return new Date(new Date(from).getTime() + FREQUENCY_DAYS[freq] * 86400000).toISOString();
  }

  private err(status: number, title: string) {
    return { status, error: { type: 'about:blank', title, status, detail: title } };
  }

  private uuid(): string {
    return 'pmm-' + Math.random().toString(16).slice(2, 10) + '-' + Date.now().toString(16);
  }

  private seed(): PmmPlanView[] {
    const now = Date.now();
    const day = 86400000;
    const iso = (n: number) => new Date(now - n * day).toISOString();
    return [
      {
        id: 'pmm-seed-001', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'PMM-2026-DIAG-001',
        aiSystemId: '11111111-1111-1111-1111-111111111111',
        name: 'Surveillance post-marché — Aide diagnostic radiologique',
        description: 'Plan de surveillance des performances cliniques et de la dérive du modèle après mise en production.',
        metricsMonitored: 'Sensibilité, spécificité, taux de faux positifs, dérive démographique (âge, sexe), taux d\'overrides radiologue.',
        collectionMethod: 'Logs d\'inférence + revue de cas chaque trimestre par binôme radiologue + data scientist.',
        reviewFrequency: 'QUARTERLY',
        responsiblePartyDescription: 'Équipe data science (responsable : Dr. X) + comité qualité hospitalier.',
        triggerCriteria: 'Baisse de sensibilité > 2 points OU dérive démographique > 5 points OU taux d\'overrides > 8%.',
        qmsLinkReference: 'AIQMS-2026-001/SOP-PMM',
        status: 'ACTIVE',
        activatedAt: iso(180),
        lastReviewedAt: iso(60), lastReviewedByUserId: '00000000-0000-0000-0000-000000000999',
        nextReviewDueAt: new Date(now + 30 * day).toISOString(),
        suspendedAt: null, suspensionReason: null,
        effectiveTo: null, closureReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(200), updatedAt: iso(60)
      },
      {
        id: 'pmm-seed-002', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'PMM-2026-HRGD-002',
        aiSystemId: '22222222-2222-2222-2222-222222222222',
        name: 'Surveillance post-marché — Tri CV',
        description: 'Suivi des biais discriminants et de la qualité du classement IA.',
        metricsMonitored: 'Taux de discrimination indirecte (TPR par genre/origine), taux d\'écart IA/RH, satisfaction recruteurs.',
        collectionMethod: 'Audit mensuel d\'échantillon + interviews recruteurs.',
        reviewFrequency: 'MONTHLY',
        responsiblePartyDescription: 'DPO + responsable RH + data scientist.',
        triggerCriteria: 'Écart TPR > 5 points entre groupes démographiques OU plaintes candidat > 3/mois.',
        qmsLinkReference: 'AIQMS-2026-002/SOP-PMM-HR',
        status: 'ACTIVE',
        activatedAt: iso(60),
        lastReviewedAt: iso(35), lastReviewedByUserId: '00000000-0000-0000-0000-000000000999',
        nextReviewDueAt: iso(5),
        suspendedAt: null, suspensionReason: null,
        effectiveTo: null, closureReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(80), updatedAt: iso(35)
      },
      {
        id: 'pmm-seed-003', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'PMM-2026-CREDIT-003',
        aiSystemId: '33333333-3333-3333-3333-333333333333',
        name: 'Plan de surveillance scoring crédit (brouillon)',
        description: 'À compléter avant déploiement.',
        metricsMonitored: 'PD, taux d\'override, équité par catégorie protégée.',
        collectionMethod: null,
        reviewFrequency: 'QUARTERLY',
        responsiblePartyDescription: null,
        triggerCriteria: null,
        qmsLinkReference: null,
        status: 'DRAFT',
        activatedAt: null,
        lastReviewedAt: null, lastReviewedByUserId: null,
        nextReviewDueAt: null,
        suspendedAt: null, suspensionReason: null,
        effectiveTo: null, closureReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(5), updatedAt: iso(5)
      }
    ];
  }
}
