import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateIshikawaCauseRequest,
  CreateIshikawaDiagramRequest,
  IshikawaCauseResponse,
  IshikawaDiagramResponse,
  IshikawaPage,
  IshikawaStatus,
  SuggestedCause,
  UpdateIshikawaDiagramRequest
} from './ishikawa.types';

@Injectable({ providedIn: 'root' })
export class IshikawaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ishikawa/diagrams`;

  private readonly mockStore: IshikawaDiagramResponse[] = this.seedMockDiagrams();

  constructor(private readonly http: HttpClient) {}

  listDiagrams(page = 0, size = 50, status?: IshikawaStatus): Observable<IshikawaPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) {
      params = params.set('status', status);
    }
    return this.http.get<IshikawaPage>(this.endpoint, { params });
  }

  getDiagram(id: string): Observable<IshikawaDiagramResponse> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(d => d.id === id);
      return of(found ?? this.mockStore[0]).pipe(delay(150));
    }
    return this.http.get<IshikawaDiagramResponse>(`${this.endpoint}/${id}`);
  }

  createDiagram(input: CreateIshikawaDiagramRequest): Observable<IshikawaDiagramResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const diagram: IshikawaDiagramResponse = {
        id: 'ish-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        problemStatement: input.problemStatement,
        description: input.description,
        mode: input.mode,
        status: 'DRAFT',
        ownerId: input.ownerId,
        createdAt: now,
        updatedAt: now,
        causes: []
      };
      this.mockStore.unshift(diagram);
      return of(diagram).pipe(delay(200));
    }
    return this.http.post<IshikawaDiagramResponse>(this.endpoint, input);
  }

  updateDiagram(id: string, input: UpdateIshikawaDiagramRequest): Observable<IshikawaDiagramResponse> {
    if (environment.useMockApi) {
      const d = this.mockStore.find(x => x.id === id);
      if (d) {
        if (input.problemStatement !== undefined) d.problemStatement = input.problemStatement;
        if (input.description !== undefined) d.description = input.description;
        if (input.mode !== undefined) d.mode = input.mode;
        if (input.status !== undefined) d.status = input.status;
        d.updatedAt = new Date().toISOString();
        return of(d).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<IshikawaDiagramResponse>(`${this.endpoint}/${id}`, input);
  }

  deleteDiagram(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(d => d.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  addCause(diagramId: string, input: CreateIshikawaCauseRequest): Observable<IshikawaCauseResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const cause: IshikawaCauseResponse = {
        id: 'cause-' + Math.random().toString(36).slice(2, 9),
        diagramId,
        parentId: input.parentId,
        category: input.category,
        label: input.label,
        description: input.description,
        rootCauseScore: input.rootCauseScore,
        createdAt: now,
        updatedAt: now
      };
      const diagram = this.mockStore.find(d => d.id === diagramId);
      if (diagram) {
        diagram.causes = [...diagram.causes, cause];
        diagram.updatedAt = now;
      }
      return of(cause).pipe(delay(150));
    }
    return this.http.post<IshikawaCauseResponse>(`${this.endpoint}/${diagramId}/causes`, input);
  }

  /** Suggestions de causes par l'IA (via api-quality-engine → ai-service). §3.5 */
  suggestCauses(diagramId: string): Observable<SuggestedCause[]> {
    if (environment.useMockApi) {
      return of(<SuggestedCause[]>[
        { category: 'METHODS', label: 'Procédure de contrôle obsolète ou non suivie' },
        { category: 'MANPOWER', label: 'Formation insuffisante des opérateurs' },
        { category: 'MACHINES', label: 'Maintenance préventive non respectée' },
        { category: 'MATERIALS', label: 'Variabilité du lot de matière première' }
      ]).pipe(delay(500));
    }
    return this.http.post<SuggestedCause[]>(`${this.endpoint}/${diagramId}/suggest-causes`, {});
  }

  private mockPage(status?: IshikawaStatus): IshikawaPage {
    const filtered = status ? this.mockStore.filter(d => d.status === status) : this.mockStore;
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private seedMockDiagrams(): IshikawaDiagramResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'ish-1', tenantId: 'demo-tenant',
        problemStatement: 'Hausse des défauts de soudure ligne 3 (+18% sur 30j)',
        description: 'Analyse menée avec l\'équipe production et qualité.',
        mode: 'SIX_M', status: 'VALIDATED', ownerId: 'demo-user',
        createdAt: now, updatedAt: now,
        causes: [
          { id: 'c1', diagramId: 'ish-1', category: 'MACHINES',
            label: 'Calibration robot soudure dérivée', rootCauseScore: 0.85,
            createdAt: now, updatedAt: now },
          { id: 'c2', diagramId: 'ish-1', category: 'MATERIALS',
            label: 'Lot de fil de soudure non conforme', rootCauseScore: 0.30,
            createdAt: now, updatedAt: now }
        ]
      },
      {
        id: 'ish-2', tenantId: 'demo-tenant',
        problemStatement: 'Annulations chirurgie programmée — bloc B',
        mode: 'SEVEN_M', status: 'IN_REVIEW', ownerId: 'demo-user',
        createdAt: now, updatedAt: now, causes: []
      }
    ];
  }
}
