import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CertifyRequest,
  ConformityProcedure,
  ConformityStatus,
  ConformityView,
  EditRequest,
  FailRequest,
  PlanRequest,
  RevokeRequest
} from './ai-conformity.types';

@Injectable({ providedIn: 'root' })
export class AiConformityService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/conformity-assessments`;
  private readonly mockStore: ConformityView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: ConformityStatus): Observable<ConformityView[]> {
    if (environment.useMockApi) {
      this.recomputeExpired();
      const f = status ? this.mockStore.filter(c => c.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<ConformityView[]>(this.endpoint, { params });
  }

  listByAiSystem(aiSystemId: string): Observable<ConformityView[]> {
    if (environment.useMockApi) {
      return of(this.mockStore.filter(c => c.aiSystemId === aiSystemId)).pipe(delay(120));
    }
    const params = new HttpParams().set('aiSystemId', aiSystemId);
    return this.http.get<ConformityView[]>(`${this.endpoint}/by-system`, { params });
  }

  listExpiring(limit = 200): Observable<ConformityView[]> {
    if (environment.useMockApi) {
      const now = Date.now();
      const horizon = now + 90 * 86400000;
      const arr = this.mockStore.filter(c =>
        c.status === 'CERTIFIED' && c.validUntil
        && new Date(c.validUntil).getTime() > now
        && new Date(c.validUntil).getTime() < horizon).slice(0, limit);
      return of(arr).pipe(delay(120));
    }
    const params = new HttpParams().set('limit', limit);
    return this.http.get<ConformityView[]>(`${this.endpoint}/expiring-certificates`, { params });
  }

  get(id: string): Observable<ConformityView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(c => c.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<ConformityView>(`${this.endpoint}/${id}`);
  }

  plan(input: PlanRequest): Observable<ConformityView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const c: ConformityView = {
        id: 'ca-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference, aiSystemId: input.aiSystemId,
        qmsId: input.qmsId, procedure: input.procedure,
        notifiedBodyId: input.notifiedBodyId, notifiedBodyName: input.notifiedBodyName,
        scope: input.scope, status: 'PLANNED',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(c);
      return of(c).pipe(delay(150));
    }
    return this.http.post<ConformityView>(this.endpoint, input);
  }

  edit(id: string, input: EditRequest): Observable<ConformityView> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        Object.assign(c, input);
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<ConformityView>(`${this.endpoint}/${id}`, input);
  }

  start(id: string): Observable<ConformityView> {
    return this.transition(id, 'start', c => { c.status = 'IN_PROGRESS'; c.startedAt = new Date().toISOString(); });
  }

  certify(id: string, body: CertifyRequest): Observable<ConformityView> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (c) {
        c.status = 'CERTIFIED';
        c.certificateNumber = body.certificateNumber;
        c.euDeclarationReference = body.euDeclarationReference;
        c.validUntil = body.validUntil;
        c.certifiedAt = now;
        c.updatedAt = now;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ConformityView>(`${this.endpoint}/${id}/certify`, body);
  }

  markExpired(id: string): Observable<ConformityView> {
    return this.transition(id, 'mark-expired', c => { c.status = 'EXPIRED'; c.expiredAt = new Date().toISOString(); });
  }

  revoke(id: string, body: RevokeRequest): Observable<ConformityView> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (c) {
        c.status = 'REVOKED';
        c.revokeReason = body.reason;
        c.revokedAt = now;
        c.updatedAt = now;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ConformityView>(`${this.endpoint}/${id}/revoke`, body);
  }

  fail(id: string, body: FailRequest): Observable<ConformityView> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (c) {
        c.status = 'FAILED';
        c.failReason = body.reason;
        c.failedAt = now;
        c.updatedAt = now;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ConformityView>(`${this.endpoint}/${id}/fail`, body);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(c => c.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private transition(id: string, op: string, mutator: (c: ConformityView) => void): Observable<ConformityView> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) { mutator(c); c.updatedAt = new Date().toISOString(); return of(c).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ConformityView>(`${this.endpoint}/${id}/${op}`, {});
  }

  private recomputeExpired(): void {
    const now = Date.now();
    for (const c of this.mockStore) {
      if (c.status === 'CERTIFIED' && c.validUntil && new Date(c.validUntil).getTime() <= now) {
        c.status = 'EXPIRED';
        c.expiredAt = new Date().toISOString();
      }
    }
  }

  private seed(): ConformityView[] {
    const now = new Date();
    const fut = (d: number) => new Date(now.getTime() + d * 86400000).toISOString();
    const past = (d: number) => new Date(now.getTime() - d * 86400000).toISOString();
    return [
      {
        id: 'ca-1', tenantId: 'demo-tenant',
        reference: 'CA-TELEMED-2026',
        aiSystemId: '00000000-0000-0000-0000-000000000001',
        procedure: 'NOTIFIED_BODY',
        notifiedBodyId: '0123', notifiedBodyName: 'BSI Notified Body — UK0123',
        scope: 'Système IA diagnostic assisté par image — high-risk Annexe III §5.b. Périmètre : modèles V3.2-V3.5 inclus, jeux d\'apprentissage versions 2026.Q1-Q2.',
        certificateNumber: 'NB0123-AI-2026-0042',
        euDeclarationReference: 'EU-DECL-2026-QualitOS-Telemed-001',
        validUntil: fut(900),
        status: 'CERTIFIED', certifiedAt: past(45),
        createdByUserId: 'demo-user',
        createdAt: past(90), updatedAt: past(45)
      },
      {
        id: 'ca-2', tenantId: 'demo-tenant',
        reference: 'CA-CHATBOT-INTERNAL',
        aiSystemId: '00000000-0000-0000-0000-000000000002',
        procedure: 'INTERNAL_CONTROL',
        scope: 'Chatbot RH non high-risk — auto-évaluation Annexe VI.',
        status: 'IN_PROGRESS', startedAt: past(5),
        createdByUserId: 'demo-user',
        createdAt: past(15), updatedAt: past(5)
      },
      {
        id: 'ca-3', tenantId: 'demo-tenant',
        reference: 'CA-FRAUD-DETECT',
        aiSystemId: '00000000-0000-0000-0000-000000000003',
        procedure: 'NOTIFIED_BODY',
        notifiedBodyId: '0987', notifiedBodyName: 'TÜV SÜD Product Service NB0987',
        scope: 'Détection de fraude bancaire — high-risk Annexe III §5.b.',
        status: 'PLANNED',
        createdByUserId: 'demo-user',
        createdAt: now.toISOString(), updatedAt: now.toISOString()
      }
    ];
  }
}
