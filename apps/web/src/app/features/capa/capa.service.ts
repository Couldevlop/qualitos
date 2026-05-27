import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CapaActionResponse,
  CapaCaseResponse,
  CapaPage,
  CapaStatus,
  CreateCapaActionRequest,
  CreateCapaCaseRequest,
  SuggestedAction,
  UpdateCapaCaseRequest
} from './capa.types';

@Injectable({ providedIn: 'root' })
export class CapaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  private readonly mockStore: CapaCaseResponse[] = this.seedMockCases();

  constructor(private readonly http: HttpClient) {}

  listCases(page = 0, size = 50, status?: CapaStatus): Observable<CapaPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<CapaPage>(this.endpoint, { params });
  }

  getCase(id: string): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(c => c.id === id);
      return of(found ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<CapaCaseResponse>(`${this.endpoint}/${id}`);
  }

  createCase(input: CreateCapaCaseRequest): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const c: CapaCaseResponse = {
        id: 'capa-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        title: input.title,
        description: input.description,
        type: input.type,
        criticity: input.criticity,
        status: 'OPEN',
        sourceType: input.sourceType,
        sourceRef: input.sourceRef,
        ownerId: input.ownerId,
        dueDate: input.dueDate,
        createdAt: now,
        updatedAt: now,
        actions: []
      };
      this.mockStore.unshift(c);
      return of(c).pipe(delay(200));
    }
    return this.http.post<CapaCaseResponse>(this.endpoint, input);
  }

  updateCase(id: string, input: UpdateCapaCaseRequest): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        if (input.title !== undefined) c.title = input.title;
        if (input.description !== undefined) c.description = input.description;
        if (input.criticity !== undefined) c.criticity = input.criticity;
        if (input.sourceRef !== undefined) c.sourceRef = input.sourceRef;
        if (input.dueDate !== undefined) c.dueDate = input.dueDate;
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CapaCaseResponse>(`${this.endpoint}/${id}`, input);
  }

  deleteCase(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(c => c.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  addAction(caseId: string, input: CreateCapaActionRequest): Observable<CapaActionResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === caseId);
      const action: CapaActionResponse = {
        id: 'act-' + Math.random().toString(36).slice(2, 9),
        capaId: caseId,
        title: input.title,
        status: input.status ?? 'PENDING',
        assigneeId: input.assigneeId,
        dueDate: input.dueDate
      };
      if (c) {
        c.actions = [...c.actions, action];
        c.updatedAt = new Date().toISOString();
      }
      return of(action).pipe(delay(120));
    }
    return this.http.post<CapaActionResponse>(`${this.endpoint}/${caseId}/actions`, input);
  }

  /** Suggestions d'actions correctives/préventives par l'IA (via api-quality-engine → ai-service). §4.2 */
  suggestActions(caseId: string): Observable<SuggestedAction[]> {
    if (environment.useMockApi) {
      return of(<SuggestedAction[]>[
        { title: 'Auditer le fournisseur sur site et revoir le PPAP' },
        { title: 'Renforcer le plan de contrôle réception (échantillonnage 100%)' },
        { title: 'Mettre à jour la fiche de non-conformité et informer les parties prenantes' },
        { title: 'Former les opérateurs au nouveau critère de contrôle' }
      ]).pipe(delay(500));
    }
    return this.http.post<SuggestedAction[]>(`${this.endpoint}/${caseId}/suggest-actions`, {});
  }

  verifyEffectiveness(id: string, effective: boolean): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (c) {
        c.effectivenessVerified = effective;
        c.updatedAt = now;
        if (effective) c.status = 'CLOSED';
        c.closedAt = effective ? now : c.closedAt;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CapaCaseResponse>(
      `${this.endpoint}/${id}/effectiveness`,
      { effective }
    );
  }

  startCase(id: string): Observable<CapaCaseResponse> {
    return this.transition(id, 'IN_PROGRESS', 'start');
  }

  resolveCase(id: string): Observable<CapaCaseResponse> {
    return this.transition(id, 'RESOLVED', 'resolve');
  }

  rejectCase(id: string): Observable<CapaCaseResponse> {
    return this.transition(id, 'REJECTED', 'reject');
  }

  private transition(
    id: string,
    targetStatus: CapaStatus,
    pathSegment: 'start' | 'resolve' | 'reject'
  ): Observable<CapaCaseResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        c.status = targetStatus;
        c.updatedAt = new Date().toISOString();
        if (targetStatus === 'RESOLVED') c.resolvedAt = c.updatedAt;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<CapaCaseResponse>(`${this.endpoint}/${id}/${pathSegment}`, {});
  }

  private mockPage(status?: CapaStatus): CapaPage {
    const filtered = status ? this.mockStore.filter(c => c.status === status) : this.mockStore;
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private seedMockCases(): CapaCaseResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'capa-1', tenantId: 'demo-tenant',
        title: 'Recalibration robot soudure cobot-3',
        description: 'Suite NC répétée sur ligne 3.',
        type: 'CORRECTIVE', criticity: 'HIGH', status: 'IN_PROGRESS',
        sourceType: 'NON_CONFORMITY', sourceRef: 'NC-2026-018',
        ownerId: 'demo-user', dueDate: '2026-05-30',
        createdAt: now, updatedAt: now, actions: []
      },
      {
        id: 'capa-2', tenantId: 'demo-tenant',
        title: 'Audit anti-fraude LCB-FT trimestriel — déficit contrôle KYC',
        type: 'PREVENTIVE', criticity: 'CRITICAL', status: 'OPEN',
        sourceType: 'AUDIT', sourceRef: 'AUD-2026-Q2',
        ownerId: 'demo-user',
        createdAt: now, updatedAt: now, actions: []
      },
      {
        id: 'capa-3', tenantId: 'demo-tenant',
        title: 'Mise à jour procédure stérilisation autoclave 4',
        type: 'CORRECTIVE', criticity: 'MEDIUM', status: 'RESOLVED',
        sourceType: 'INTERNAL',
        ownerId: 'demo-user',
        resolvedAt: now, createdAt: now, updatedAt: now, actions: []
      }
    ];
  }
}
