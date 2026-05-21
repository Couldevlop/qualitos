import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AddApproverRequest,
  AddImpactRequest,
  ApprovalResponse,
  ChangePage,
  ChangeResponse,
  ChangeRequestStatus,
  ChangeRequestType,
  ChangeSummary,
  CreateChangeRequest,
  DecisionRequest,
  ImpactResponse,
  ImplementRequest,
  UpdateChangeRequest
} from './changes.types';

@Injectable({ providedIn: 'root' })
export class ChangesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/changes`;

  private readonly mockStore: ChangeResponse[] = this.seedMockChanges();
  private readonly mockApprovals: Record<string, ApprovalResponse[]> = this.seedApprovals();
  private readonly mockImpacts: Record<string, ImpactResponse[]> = {};

  constructor(private readonly http: HttpClient) {}

  // ---- CRUD ----

  list(page = 0, size = 50, status?: ChangeRequestStatus, type?: ChangeRequestType): Observable<ChangePage> {
    if (environment.useMockApi) {
      const f = this.mockStore
        .filter(c => !status || c.status === status)
        .filter(c => !type   || c.type   === type);
      return of({ content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (type)   params = params.set('type',   type);
    return this.http.get<ChangePage>(this.endpoint, { params });
  }

  get(id: string): Observable<ChangeResponse> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(x => x.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<ChangeResponse>(`${this.endpoint}/${id}`);
  }

  create(input: CreateChangeRequest): Observable<ChangeResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const c: ChangeResponse = {
        id: 'chg-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        code: input.code, title: input.title, description: input.description,
        type: input.type, priority: input.priority ?? 'MEDIUM',
        status: 'DRAFT',
        requesterUserId: input.requesterUserId, ownerUserId: input.ownerUserId,
        plannedFor: input.plannedFor,
        impactSummary: input.impactSummary, riskAssessment: input.riskAssessment,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(c);
      this.mockApprovals[c.id] = [];
      this.mockImpacts[c.id] = [];
      return of(c).pipe(delay(150));
    }
    return this.http.post<ChangeResponse>(this.endpoint, input);
  }

  update(id: string, input: UpdateChangeRequest): Observable<ChangeResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) { Object.assign(c, input); c.updatedAt = new Date().toISOString(); return of(c).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<ChangeResponse>(`${this.endpoint}/${id}`, input);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(c => c.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      delete this.mockApprovals[id]; delete this.mockImpacts[id];
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  // ---- Workflow ----

  submit(id: string): Observable<ChangeResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) { c.status = 'SUBMITTED'; c.updatedAt = new Date().toISOString(); return of(c).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ChangeResponse>(`${this.endpoint}/${id}/submit`, {});
  }

  cancel(id: string, reason?: string): Observable<ChangeResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        c.status = 'CANCELLED';
        if (reason) c.rejectionReason = reason;
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (reason) params = params.set('reason', reason);
    return this.http.post<ChangeResponse>(`${this.endpoint}/${id}/cancel`, {}, { params });
  }

  implement(id: string, body: ImplementRequest): Observable<ChangeResponse> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      if (c) {
        c.status = 'IMPLEMENTED';
        c.implementedAt = body.implementedAt;
        c.updatedAt = new Date().toISOString();
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ChangeResponse>(`${this.endpoint}/${id}/implement`, body);
  }

  summary(id: string): Observable<ChangeSummary> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      const approvals = this.mockApprovals[id] ?? [];
      const impacts = this.mockImpacts[id] ?? [];
      return of({
        changeId: id, status: c?.status ?? 'DRAFT',
        totalApprovers: approvals.length,
        approved: approvals.filter(a => a.decision === 'APPROVED').length,
        rejected: approvals.filter(a => a.decision === 'REJECTED').length,
        pending:  approvals.filter(a => a.decision === 'PENDING').length,
        impactCount: impacts.length,
        approvals, impacts
      }).pipe(delay(120));
    }
    return this.http.get<ChangeSummary>(`${this.endpoint}/${id}/summary`);
  }

  // ---- Approvers ----

  listApprovals(id: string): Observable<ApprovalResponse[]> {
    if (environment.useMockApi) return of(this.mockApprovals[id] ?? []).pipe(delay(100));
    return this.http.get<ApprovalResponse[]>(`${this.endpoint}/${id}/approvals`);
  }

  addApprover(id: string, body: AddApproverRequest): Observable<ApprovalResponse> {
    if (environment.useMockApi) {
      const a: ApprovalResponse = {
        id: 'app-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant', changeId: id,
        approverUserId: body.approverUserId,
        approvalLevel: body.approvalLevel ?? 1,
        decision: 'PENDING',
        createdAt: new Date().toISOString()
      };
      const arr = this.mockApprovals[id] ?? [];
      arr.push(a); this.mockApprovals[id] = arr;
      return of(a).pipe(delay(120));
    }
    return this.http.post<ApprovalResponse>(`${this.endpoint}/${id}/approvers`, body);
  }

  removeApprover(id: string, approverUserId: string): Observable<void> {
    if (environment.useMockApi) {
      const arr = this.mockApprovals[id] ?? [];
      const i = arr.findIndex(a => a.approverUserId === approverUserId);
      if (i >= 0) arr.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}/approvers/${approverUserId}`);
  }

  decide(id: string, body: DecisionRequest): Observable<ApprovalResponse> {
    if (environment.useMockApi) {
      const arr = this.mockApprovals[id] ?? [];
      const a = arr.find(x => x.approverUserId === body.approverUserId);
      const now = new Date().toISOString();
      if (a) {
        a.decision = body.decision;
        a.comment = body.comment;
        a.decidedAt = now;
        // recompute parent status mirror to backend invariants
        const c = this.mockStore.find(x => x.id === id);
        if (c) {
          const allApproved = arr.length > 0 && arr.every(x => x.decision === 'APPROVED');
          const anyRejected = arr.some(x => x.decision === 'REJECTED');
          if (anyRejected) { c.status = 'REJECTED'; c.rejectionReason = body.comment; }
          else if (allApproved) c.status = 'APPROVED';
          else c.status = 'UNDER_REVIEW';
          c.updatedAt = now;
        }
        return of(a).pipe(delay(120));
      }
      return of(arr[0]).pipe(delay(120));
    }
    return this.http.post<ApprovalResponse>(`${this.endpoint}/${id}/decisions`, body);
  }

  // ---- Impacts ----

  listImpacts(id: string): Observable<ImpactResponse[]> {
    if (environment.useMockApi) return of(this.mockImpacts[id] ?? []).pipe(delay(100));
    return this.http.get<ImpactResponse[]>(`${this.endpoint}/${id}/impacts`);
  }

  addImpact(id: string, body: AddImpactRequest): Observable<ImpactResponse> {
    if (environment.useMockApi) {
      const im: ImpactResponse = {
        id: 'imp-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant', changeId: id,
        targetType: body.targetType, targetId: body.targetId, notes: body.notes,
        createdAt: new Date().toISOString()
      };
      const arr = this.mockImpacts[id] ?? [];
      arr.push(im); this.mockImpacts[id] = arr;
      return of(im).pipe(delay(120));
    }
    return this.http.post<ImpactResponse>(`${this.endpoint}/${id}/impacts`, body);
  }

  removeImpact(id: string, impactId: string): Observable<void> {
    if (environment.useMockApi) {
      const arr = this.mockImpacts[id] ?? [];
      const i = arr.findIndex(x => x.id === impactId);
      if (i >= 0) arr.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}/impacts/${impactId}`);
  }

  // ---- Mock seeds ----

  private seedMockChanges(): ChangeResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'chg-1', tenantId: 'demo-tenant',
        code: 'CHG-2026-014', title: 'Mise à jour procédure stérilisation autoclave 4',
        description: 'Aligne la procédure sur la nouvelle norme ISO 13485:2026 §7.5.7.',
        type: 'DOCUMENT', priority: 'HIGH', status: 'UNDER_REVIEW',
        requesterUserId: 'demo-user', ownerUserId: 'demo-user',
        plannedFor: '2026-06-05',
        impactSummary: 'Procédure PROC-STER-004 + formation opérateurs.',
        riskAssessment: 'Risque résiduel faible avec formation préalable et double-check.',
        createdAt: now, updatedAt: now
      },
      {
        id: 'chg-2', tenantId: 'demo-tenant',
        code: 'CHG-2026-015', title: 'Migration LMS interne vers EduCloud',
        description: 'Bascule de la plateforme de formation interne (10 000 utilisateurs).',
        type: 'IT_SYSTEM', priority: 'CRITICAL', status: 'SUBMITTED',
        requesterUserId: 'demo-user',
        plannedFor: '2026-07-15',
        createdAt: now, updatedAt: now
      },
      {
        id: 'chg-3', tenantId: 'demo-tenant',
        code: 'CHG-2026-016', title: 'Changement fournisseur acier (qualif AcierFrance SA)',
        type: 'SUPPLIER', priority: 'MEDIUM', status: 'DRAFT',
        requesterUserId: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }

  private seedApprovals(): Record<string, ApprovalResponse[]> {
    const now = new Date().toISOString();
    return {
      'chg-1': [
        {
          id: 'app-1', tenantId: 'demo-tenant', changeId: 'chg-1',
          approverUserId: 'qa-manager', approvalLevel: 1,
          decision: 'APPROVED', comment: 'OK sous réserve formation', decidedAt: now,
          createdAt: now
        },
        {
          id: 'app-2', tenantId: 'demo-tenant', changeId: 'chg-1',
          approverUserId: 'reg-officer', approvalLevel: 2,
          decision: 'PENDING',
          createdAt: now
        }
      ]
    };
  }
}
