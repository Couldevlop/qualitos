import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AuditPlanResponse,
  AuditStatus,
  AuditsPage,
  ChecklistItemResponse,
  CreateAuditPlanRequest,
  CreateChecklistItemRequest
} from './audits.types';

@Injectable({ providedIn: 'root' })
export class AuditsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/audits/plans`;

  private readonly mockStore: AuditPlanResponse[] = this.seedMockPlans();

  constructor(private readonly http: HttpClient) {}

  listPlans(page = 0, size = 50, status?: AuditStatus): Observable<AuditsPage> {
    if (environment.useMockApi) return of(this.mockPage(status)).pipe(delay(150));
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<AuditsPage>(this.endpoint, { params });
  }

  getPlan(id: string): Observable<AuditPlanResponse> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(p => p.id === id);
      return of(found ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<AuditPlanResponse>(`${this.endpoint}/${id}`);
  }

  createPlan(input: CreateAuditPlanRequest): Observable<AuditPlanResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const plan: AuditPlanResponse = {
        id: 'a-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        title: input.title,
        scope: input.scope,
        type: input.type,
        status: 'PLANNED',
        standard: input.standard,
        leadAuditorId: input.leadAuditorId,
        scheduledDate: input.scheduledDate,
        createdAt: now,
        updatedAt: now,
        checklist: [],
        findings: []
      };
      this.mockStore.unshift(plan);
      return of(plan).pipe(delay(200));
    }
    return this.http.post<AuditPlanResponse>(this.endpoint, input);
  }

  deletePlan(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(p => p.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  addChecklistItem(
    planId: string,
    input: CreateChecklistItemRequest
  ): Observable<ChecklistItemResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const item: ChecklistItemResponse = {
        id: 'cli-' + Math.random().toString(36).slice(2, 9),
        planId,
        question: input.question,
        clauseRef: input.clauseRef,
        expectedEvidence: input.expectedEvidence,
        weight: input.weight,
        orderIndex: input.orderIndex,
        createdAt: now,
        updatedAt: now
      };
      const plan = this.mockStore.find(p => p.id === planId);
      if (plan) {
        plan.checklist = [...(plan.checklist ?? []), item];
        plan.updatedAt = now;
      }
      return of(item).pipe(delay(120));
    }
    return this.http.post<ChecklistItemResponse>(`${this.endpoint}/${planId}/checklist`, input);
  }

  startPlan(id: string): Observable<AuditPlanResponse> {
    return this.transition(id, 'IN_PROGRESS', 'start');
  }

  completePlan(id: string, reportSummary?: string): Observable<AuditPlanResponse> {
    if (environment.useMockApi) {
      const plan = this.mockStore.find(p => p.id === id);
      if (plan) {
        plan.status = 'COMPLETED';
        plan.completedAt = new Date().toISOString();
        plan.updatedAt = plan.completedAt;
        plan.reportSummary = reportSummary;
        return of(plan).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<AuditPlanResponse>(
      `${this.endpoint}/${id}/complete`,
      reportSummary ? { reportSummary } : {}
    );
  }

  cancelPlan(id: string): Observable<AuditPlanResponse> {
    return this.transition(id, 'CANCELLED', 'cancel');
  }

  private transition(
    id: string,
    targetStatus: AuditStatus,
    pathSegment: 'start' | 'cancel'
  ): Observable<AuditPlanResponse> {
    if (environment.useMockApi) {
      const plan = this.mockStore.find(p => p.id === id);
      if (plan) {
        plan.status = targetStatus;
        plan.updatedAt = new Date().toISOString();
        if (targetStatus === 'IN_PROGRESS') plan.startedAt = plan.updatedAt;
        return of(plan).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<AuditPlanResponse>(`${this.endpoint}/${id}/${pathSegment}`, {});
  }

  private mockPage(status?: AuditStatus): AuditsPage {
    const f = status ? this.mockStore.filter(a => a.status === status) : this.mockStore;
    return { content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length };
  }

  private seedMockPlans(): AuditPlanResponse[] {
    const now = new Date().toISOString();
    return [
      { id: 'a1', tenantId: 't', title: 'Audit interne ISO 9001 §9.2', type: 'INTERNAL',
        status: 'COMPLETED', standard: 'ISO_9001', leadAuditorId: 'u', scheduledDate: now,
        completedAt: now, conformityScore: 92, createdAt: now, updatedAt: now,
        checklist: [], findings: [] },
      { id: 'a2', tenantId: 't', title: 'Audit fournisseur Acme Forge', type: 'SUPPLIER',
        status: 'IN_PROGRESS', leadAuditorId: 'u', scheduledDate: now,
        conformityScore: 65, createdAt: now, updatedAt: now,
        checklist: [], findings: [] },
      { id: 'a3', tenantId: 't', title: 'Pré-audit certification ISO 27001', type: 'CERTIFICATION',
        status: 'PLANNED', standard: 'ISO_27001', leadAuditorId: 'u', scheduledDate: now,
        createdAt: now, updatedAt: now,
        checklist: [], findings: [] }
    ];
  }
}
