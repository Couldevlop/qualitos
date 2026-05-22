import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  EudbDeclareUpdateRequest,
  EudbDraftRequest,
  EudbEditRequest,
  EudbMarkRegisteredRequest,
  EudbRejectRequest,
  EudbRetireRequest,
  EudbStatus,
  EudbSubmitRequest,
  EudbView
} from './eudb.types';

@Injectable({ providedIn: 'root' })
export class EudbService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/eudb`;
  private readonly mockStore: EudbView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: EudbStatus): Observable<EudbView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(r => r.status === status) : this.mockStore;
      return of([...f]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<EudbView[]>(this.endpoint, { params });
  }

  listByAiSystem(aiSystemId: string): Observable<EudbView[]> {
    if (environment.useMockApi) {
      return of(this.mockStore.filter(r => r.aiSystemId === aiSystemId)).pipe(delay(120));
    }
    return this.http.get<EudbView[]>(`${this.endpoint}/by-system`, {
      params: new HttpParams().set('aiSystemId', aiSystemId)
    });
  }

  get(id: string): Observable<EudbView> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(r => r.id === id);
      return found ? of(structuredClone(found)).pipe(delay(80))
                   : throwError(() => this.err(404, 'Enregistrement introuvable.'));
    }
    return this.http.get<EudbView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<EudbView> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(r => r.reference === reference);
      return found ? of(structuredClone(found)).pipe(delay(80))
                   : throwError(() => this.err(404, 'Référence introuvable.'));
    }
    return this.http.get<EudbView>(`${this.endpoint}/by-reference`, {
      params: new HttpParams().set('reference', reference)
    });
  }

  getByEudbId(eudbId: string): Observable<EudbView> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(r => r.eudbId === eudbId);
      return found ? of(structuredClone(found)).pipe(delay(80))
                   : throwError(() => this.err(404, 'EUDB ID introuvable.'));
    }
    return this.http.get<EudbView>(`${this.endpoint}/by-eudb-id`, {
      params: new HttpParams().set('eudbId', eudbId)
    });
  }

  draft(req: EudbDraftRequest): Observable<EudbView> {
    if (environment.useMockApi) {
      if (this.mockStore.some(r => r.reference === req.reference)) {
        return throwError(() => this.err(409, 'Référence déjà utilisée.'));
      }
      const now = new Date().toISOString();
      const r: EudbView = {
        id: this.uuid(),
        tenantId: '00000000-0000-0000-0000-000000000001',
        reference: req.reference,
        aiSystemId: req.aiSystemId,
        providerEntityName: req.providerEntityName ?? null,
        providerEuRepresentative: req.providerEuRepresentative ?? null,
        memberStateOfReference: req.memberStateOfReference ?? null,
        intendedPurposeSummary: req.intendedPurposeSummary ?? null,
        technicalDocumentationReference: req.technicalDocumentationReference ?? null,
        eudbId: null,
        status: 'DRAFT',
        submittedAt: null, submittedByUserId: null,
        registrationDate: null,
        lastUpdateDate: null, lastUpdateSummary: null,
        rejectedAt: null, rejectionReason: null,
        retiredAt: null, retirementReason: null,
        createdByUserId: req.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(r);
      return of(structuredClone(r)).pipe(delay(140));
    }
    return this.http.post<EudbView>(this.endpoint, req);
  }

  edit(id: string, req: EudbEditRequest): Observable<EudbView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Enregistrement introuvable.'));
      if (r.status !== 'DRAFT') return throwError(() => this.err(409, 'Édition possible uniquement en DRAFT.'));
      r.providerEntityName = req.providerEntityName ?? null;
      r.providerEuRepresentative = req.providerEuRepresentative ?? null;
      r.memberStateOfReference = req.memberStateOfReference ?? null;
      r.intendedPurposeSummary = req.intendedPurposeSummary ?? null;
      r.technicalDocumentationReference = req.technicalDocumentationReference ?? null;
      r.updatedAt = new Date().toISOString();
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.put<EudbView>(`${this.endpoint}/${id}`, req);
  }

  submit(id: string, req: EudbSubmitRequest): Observable<EudbView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Enregistrement introuvable.'));
      if (r.status !== 'DRAFT') return throwError(() => this.err(409, 'Soumission impossible — statut actuel : ' + r.status + '.'));
      const missing: string[] = [];
      if (!r.providerEntityName) missing.push('Nom du fournisseur');
      if (!r.memberStateOfReference) missing.push('État membre de référence');
      if (!r.intendedPurposeSummary) missing.push('Finalité prévue');
      if (missing.length > 0) return throwError(() => this.err(422, 'Champs requis manquants : ' + missing.join(', ')));
      const now = new Date().toISOString();
      r.status = 'SUBMITTED';
      r.submittedAt = now;
      r.submittedByUserId = req.submittedByUserId;
      r.updatedAt = now;
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<EudbView>(`${this.endpoint}/${id}/submit`, req);
  }

  markRegistered(id: string, req: EudbMarkRegisteredRequest): Observable<EudbView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Enregistrement introuvable.'));
      if (r.status !== 'SUBMITTED') return throwError(() => this.err(409, 'Marquage possible uniquement après SUBMITTED.'));
      if (this.mockStore.some(x => x.eudbId === req.eudbId && x.id !== id)) {
        return throwError(() => this.err(409, 'EUDB ID déjà attribué à un autre enregistrement.'));
      }
      r.status = 'REGISTERED';
      r.eudbId = req.eudbId;
      r.registrationDate = req.registrationDate;
      r.updatedAt = new Date().toISOString();
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<EudbView>(`${this.endpoint}/${id}/mark-registered`, req);
  }

  declareUpdate(id: string, req: EudbDeclareUpdateRequest): Observable<EudbView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Enregistrement introuvable.'));
      if (r.status !== 'REGISTERED' && r.status !== 'UPDATED') {
        return throwError(() => this.err(409, 'Déclaration de mise à jour réservée aux enregistrements REGISTERED/UPDATED.'));
      }
      r.status = 'UPDATED';
      r.lastUpdateSummary = req.updateSummary;
      r.lastUpdateDate = req.updateDate;
      r.updatedAt = new Date().toISOString();
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<EudbView>(`${this.endpoint}/${id}/declare-update`, req);
  }

  reject(id: string, req: EudbRejectRequest): Observable<EudbView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Enregistrement introuvable.'));
      if (r.status !== 'DRAFT' && r.status !== 'SUBMITTED') {
        return throwError(() => this.err(409, 'Rejet possible uniquement en DRAFT/SUBMITTED.'));
      }
      const now = new Date().toISOString();
      r.status = 'REJECTED';
      r.rejectedAt = now;
      r.rejectionReason = req.reason;
      r.updatedAt = now;
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<EudbView>(`${this.endpoint}/${id}/reject`, req);
  }

  retire(id: string, req: EudbRetireRequest): Observable<EudbView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (!r) return throwError(() => this.err(404, 'Enregistrement introuvable.'));
      if (r.status !== 'REGISTERED' && r.status !== 'UPDATED') {
        return throwError(() => this.err(409, 'Retrait possible uniquement en REGISTERED/UPDATED.'));
      }
      const now = new Date().toISOString();
      r.status = 'RETIRED';
      r.retiredAt = now;
      r.retirementReason = req.reason;
      r.updatedAt = now;
      return of(structuredClone(r)).pipe(delay(120));
    }
    return this.http.post<EudbView>(`${this.endpoint}/${id}/retire`, req);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(r => r.id === id);
      if (i < 0) return throwError(() => this.err(404, 'Enregistrement introuvable.'));
      this.mockStore.splice(i, 1);
      return of(void 0).pipe(delay(100));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private err(status: number, title: string) {
    return { status, error: { type: 'about:blank', title, status, detail: title } };
  }

  private uuid(): string {
    return 'eudb-' + Math.random().toString(16).slice(2, 10) + '-' + Date.now().toString(16);
  }

  private seed(): EudbView[] {
    const now = Date.now();
    const day = 86400000;
    const iso = (n: number) => new Date(now - n * day).toISOString();
    return [
      {
        id: 'eudb-seed-001', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'EUDB-2026-DIAG-001',
        aiSystemId: '11111111-1111-1111-1111-111111111111',
        providerEntityName: 'QualitOS SAS',
        providerEuRepresentative: 'QualitOS SAS — Paris',
        memberStateOfReference: 'FR',
        intendedPurposeSummary: 'Aide au diagnostic radiologique — détection lésions pulmonaires (hôpital, médecin radiologue).',
        technicalDocumentationReference: 'TDOC-RX-LUNG-v1.3',
        eudbId: 'EUDB-AI-A4F92K71',
        status: 'REGISTERED',
        submittedAt: iso(40), submittedByUserId: '00000000-0000-0000-0000-000000000999',
        registrationDate: iso(28),
        lastUpdateDate: null, lastUpdateSummary: null,
        rejectedAt: null, rejectionReason: null,
        retiredAt: null, retirementReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(50), updatedAt: iso(28)
      },
      {
        id: 'eudb-seed-002', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'EUDB-2026-HRGD-002',
        aiSystemId: '22222222-2222-2222-2222-222222222222',
        providerEntityName: 'QualitOS SAS',
        providerEuRepresentative: 'QualitOS SAS — Paris',
        memberStateOfReference: 'FR',
        intendedPurposeSummary: 'Tri automatisé CV pour aide au recrutement (RH, candidats UE).',
        technicalDocumentationReference: 'TDOC-HR-CVSORT-v0.9',
        eudbId: null,
        status: 'SUBMITTED',
        submittedAt: iso(5), submittedByUserId: '00000000-0000-0000-0000-000000000999',
        registrationDate: null,
        lastUpdateDate: null, lastUpdateSummary: null,
        rejectedAt: null, rejectionReason: null,
        retiredAt: null, retirementReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(15), updatedAt: iso(5)
      },
      {
        id: 'eudb-seed-003', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'EUDB-2026-CREDIT-003',
        aiSystemId: '33333333-3333-3333-3333-333333333333',
        providerEntityName: 'QualitOS SAS',
        providerEuRepresentative: null,
        memberStateOfReference: null,
        intendedPurposeSummary: 'Scoring crédit (brouillon en cours d\'instruction).',
        technicalDocumentationReference: null,
        eudbId: null,
        status: 'DRAFT',
        submittedAt: null, submittedByUserId: null,
        registrationDate: null,
        lastUpdateDate: null, lastUpdateSummary: null,
        rejectedAt: null, rejectionReason: null,
        retiredAt: null, retirementReason: null,
        createdByUserId: '00000000-0000-0000-0000-000000000999',
        createdAt: iso(2), updatedAt: iso(2)
      }
    ];
  }
}
