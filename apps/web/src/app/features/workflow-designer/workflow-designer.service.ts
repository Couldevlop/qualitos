import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateWorkflowRequest,
  EMPTY_BPMN_XML,
  UpdateWorkflowRequest,
  WorkflowDefinition,
  WorkflowPage,
  WorkflowStatus,
  WorkflowSummary
} from './workflow-designer.types';

export interface WorkflowListFilters {
  status?: WorkflowStatus;
}

/**
 * Service HTTP du Designer de workflow BPMN no-code (§5.4).
 * Endpoint backend : /api/v1/workflow/definitions. Tenant + acteur côté serveur (JWT).
 * Un mode mock (environment.useMockApi) permet la démo sans backend.
 */
@Injectable({ providedIn: 'root' })
export class WorkflowDesignerService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/workflow/definitions`;

  private readonly mockStore: WorkflowDefinition[] = this.seedMock();

  constructor(private readonly http: HttpClient) {}

  list(page = 0, size = 50, filters: WorkflowListFilters = {}): Observable<WorkflowPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(filters)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.status) params = params.set('status', filters.status);
    return this.http.get<WorkflowPage>(this.endpoint, { params });
  }

  get(id: string): Observable<WorkflowDefinition> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(w => w.id === id);
      return of(found ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<WorkflowDefinition>(`${this.endpoint}/${id}`);
  }

  create(input: CreateWorkflowRequest): Observable<WorkflowDefinition> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const wf: WorkflowDefinition = {
        id: 'wf-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        name: input.name,
        description: input.description,
        bpmnXml: input.bpmnXml,
        status: 'DRAFT',
        version: 1,
        createdAt: now,
        updatedAt: now
      };
      this.mockStore.unshift(wf);
      return of(wf).pipe(delay(200));
    }
    return this.http.post<WorkflowDefinition>(this.endpoint, input);
  }

  update(id: string, input: UpdateWorkflowRequest): Observable<WorkflowDefinition> {
    if (environment.useMockApi) {
      const wf = this.mockStore.find(w => w.id === id) ?? this.mockStore[0];
      if (input.name !== undefined) wf.name = input.name;
      if (input.description !== undefined) wf.description = input.description;
      if (input.bpmnXml !== undefined) wf.bpmnXml = input.bpmnXml;
      wf.version += 1;
      wf.updatedAt = new Date().toISOString();
      return of(wf).pipe(delay(150));
    }
    return this.http.put<WorkflowDefinition>(`${this.endpoint}/${id}`, input);
  }

  publish(id: string): Observable<WorkflowDefinition> {
    return this.transition(id, 'PUBLISHED', 'publish');
  }

  archive(id: string): Observable<WorkflowDefinition> {
    return this.transition(id, 'ARCHIVED', 'archive');
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const idx = this.mockStore.findIndex(w => w.id === id);
      if (idx >= 0) this.mockStore.splice(idx, 1);
      return of(void 0).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  /** Diagramme BPMN vide par défaut pour démarrer un nouveau workflow. */
  emptyDiagram(): string {
    return EMPTY_BPMN_XML;
  }

  private transition(
    id: string,
    targetStatus: WorkflowStatus,
    pathSegment: 'publish' | 'archive'
  ): Observable<WorkflowDefinition> {
    if (environment.useMockApi) {
      const wf = this.mockStore.find(w => w.id === id) ?? this.mockStore[0];
      wf.status = targetStatus;
      wf.updatedAt = new Date().toISOString();
      return of(wf).pipe(delay(120));
    }
    return this.http.post<WorkflowDefinition>(`${this.endpoint}/${id}/${pathSegment}`, {});
  }

  private mockPage(filters: WorkflowListFilters): WorkflowPage {
    const content: WorkflowSummary[] = this.mockStore
      .filter(w => !filters.status || w.status === filters.status)
      .map(({ bpmnXml, ...summary }) => summary);
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: content.length };
  }

  private seedMock(): WorkflowDefinition[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'wf-capa', tenantId: 'demo-tenant', name: 'Processus CAPA — 4 étapes',
        description: 'Détection → Analyse → Action corrective → Vérification efficacité.',
        bpmnXml: EMPTY_BPMN_XML, status: 'PUBLISHED', version: 3,
        createdAt: now, updatedAt: now
      },
      {
        id: 'wf-audit', tenantId: 'demo-tenant', name: 'Revue de direction ISO 9001 §9.3',
        description: 'Préparation → Revue → Décisions → Plan d\'action.',
        bpmnXml: EMPTY_BPMN_XML, status: 'DRAFT', version: 1,
        createdAt: now, updatedAt: now
      }
    ];
  }
}
