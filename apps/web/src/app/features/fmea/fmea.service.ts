import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateFmeaItemRequest,
  CreateFmeaProjectRequest,
  FmeaItemPage,
  FmeaItemResponse,
  FmeaProjectPage,
  FmeaProjectResponse,
  FmeaProjectStatistics,
  FmeaStatus,
  FmeaType,
  UpdateFmeaItemRequest,
  UpdateFmeaProjectRequest
} from './fmea.types';

@Injectable({ providedIn: 'root' })
export class FmeaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/fmea`;

  private readonly mockProjects: FmeaProjectResponse[] = this.seedMockProjects();
  private readonly mockItems: Record<string, FmeaItemResponse[]> = this.seedMockItems();

  constructor(private readonly http: HttpClient) {}

  // ---------- Projects ----------

  list(
    page = 0, size = 50, status?: FmeaStatus, type?: FmeaType
  ): Observable<FmeaProjectPage> {
    if (environment.useMockApi) {
      const filtered = this.mockProjects
        .filter(p => !status || p.status === status)
        .filter(p => !type   || p.type   === type);
      return of({
        content: filtered, totalElements: filtered.length,
        totalPages: 1, number: 0, size: filtered.length
      }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (type)   params = params.set('type',   type);
    return this.http.get<FmeaProjectPage>(`${this.endpoint}/projects`, { params });
  }

  get(id: string): Observable<FmeaProjectResponse> {
    if (environment.useMockApi) {
      const p = this.mockProjects.find(x => x.id === id);
      return of(p ?? this.mockProjects[0]).pipe(delay(120));
    }
    return this.http.get<FmeaProjectResponse>(`${this.endpoint}/projects/${id}`);
  }

  create(input: CreateFmeaProjectRequest): Observable<FmeaProjectResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const p: FmeaProjectResponse = {
        id: 'fmea-' + (this.mockProjects.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        code: input.code,
        name: input.name,
        scope: input.scope,
        type: input.type,
        status: 'DRAFT',
        criticalRpnThreshold: input.criticalRpnThreshold ?? 100,
        revision: 1,
        ownerUserId: input.ownerUserId,
        createdBy: input.createdBy,
        createdAt: now, updatedAt: now
      };
      this.mockProjects.unshift(p);
      this.mockItems[p.id] = [];
      return of(p).pipe(delay(150));
    }
    return this.http.post<FmeaProjectResponse>(`${this.endpoint}/projects`, input);
  }

  update(id: string, input: UpdateFmeaProjectRequest): Observable<FmeaProjectResponse> {
    if (environment.useMockApi) {
      const p = this.mockProjects.find(x => x.id === id);
      if (p) {
        Object.assign(p, input);
        p.updatedAt = new Date().toISOString();
        return of(p).pipe(delay(120));
      }
      return of(this.mockProjects[0]).pipe(delay(120));
    }
    return this.http.patch<FmeaProjectResponse>(`${this.endpoint}/projects/${id}`, input);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockProjects.findIndex(p => p.id === id);
      if (i >= 0) this.mockProjects.splice(i, 1);
      delete this.mockItems[id];
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/projects/${id}`);
  }

  activate(id: string): Observable<FmeaProjectResponse> { return this.transition(id, 'activate', 'ACTIVE'); }
  reopen(id: string):   Observable<FmeaProjectResponse> { return this.transition(id, 'reopen',   'DRAFT'); }
  archive(id: string):  Observable<FmeaProjectResponse> { return this.transition(id, 'archive',  'ARCHIVED'); }

  private transition(
    id: string, op: 'activate' | 'reopen' | 'archive',
    target: FmeaStatus
  ): Observable<FmeaProjectResponse> {
    if (environment.useMockApi) {
      const p = this.mockProjects.find(x => x.id === id);
      if (p) { p.status = target; p.updatedAt = new Date().toISOString(); return of(p).pipe(delay(120)); }
      return of(this.mockProjects[0]).pipe(delay(120));
    }
    return this.http.post<FmeaProjectResponse>(`${this.endpoint}/projects/${id}/${op}`, {});
  }

  statistics(id: string): Observable<FmeaProjectStatistics> {
    if (environment.useMockApi) {
      const items = this.mockItems[id] ?? [];
      const total = items.length;
      const critical = items.filter(i => i.critical).length;
      const max = items.reduce((m, i) => Math.max(m, i.rpn), 0);
      const avg = total > 0 ? items.reduce((s, i) => s + i.rpn, 0) / total : 0;
      const p = this.mockProjects.find(x => x.id === id);
      return of({
        projectId: id,
        totalItems: total,
        criticalItems: critical,
        maxRpn: max,
        averageRpn: avg,
        criticalRpnThreshold: p?.criticalRpnThreshold ?? 100
      }).pipe(delay(120));
    }
    return this.http.get<FmeaProjectStatistics>(`${this.endpoint}/projects/${id}/statistics`);
  }

  // ---------- Items ----------

  listItems(projectId: string, page = 0, size = 100): Observable<FmeaItemPage> {
    if (environment.useMockApi) {
      const items = this.mockItems[projectId] ?? [];
      return of({
        content: items, totalElements: items.length,
        totalPages: 1, number: 0, size: items.length
      }).pipe(delay(100));
    }
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<FmeaItemPage>(`${this.endpoint}/projects/${projectId}/items`, { params });
  }

  addItem(projectId: string, input: CreateFmeaItemRequest): Observable<FmeaItemResponse> {
    if (environment.useMockApi) {
      const items = this.mockItems[projectId] ?? [];
      const project = this.mockProjects.find(p => p.id === projectId);
      const rpn = input.severity * input.occurrence * input.detection;
      const rpnAfter = (input.resultingSeverity ?? 0) > 0
        ? (input.resultingSeverity! * (input.resultingOccurrence ?? 0) * (input.resultingDetection ?? 0)) || undefined
        : undefined;
      const item: FmeaItemResponse = {
        id: 'fmi-' + Math.random().toString(36).slice(2, 9),
        tenantId: 'demo-tenant',
        projectId,
        sequenceNo: items.length + 1,
        function: input.function,
        failureMode: input.failureMode,
        failureEffect: input.failureEffect,
        failureCause: input.failureCause,
        currentControls: input.currentControls,
        severity: input.severity,
        occurrence: input.occurrence,
        detection: input.detection,
        rpn,
        recommendedAction: input.recommendedAction,
        actionOwnerUserId: input.actionOwnerUserId,
        actionDueDate: input.actionDueDate,
        resultingSeverity: input.resultingSeverity,
        resultingOccurrence: input.resultingOccurrence,
        resultingDetection: input.resultingDetection,
        rpnAfter,
        critical: rpn >= (project?.criticalRpnThreshold ?? 100),
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      items.push(item);
      this.mockItems[projectId] = items;
      return of(item).pipe(delay(120));
    }
    return this.http.post<FmeaItemResponse>(`${this.endpoint}/projects/${projectId}/items`, input);
  }

  updateItem(
    projectId: string, itemId: string, input: UpdateFmeaItemRequest
  ): Observable<FmeaItemResponse> {
    if (environment.useMockApi) {
      const items = this.mockItems[projectId] ?? [];
      const item = items.find(x => x.id === itemId);
      const project = this.mockProjects.find(p => p.id === projectId);
      if (item) {
        Object.assign(item, input);
        item.rpn = item.severity * item.occurrence * item.detection;
        if (item.resultingSeverity && item.resultingOccurrence && item.resultingDetection) {
          item.rpnAfter = item.resultingSeverity * item.resultingOccurrence * item.resultingDetection;
        }
        item.critical = item.rpn >= (project?.criticalRpnThreshold ?? 100);
        item.updatedAt = new Date().toISOString();
        return of(item).pipe(delay(120));
      }
      return of(items[0]).pipe(delay(120));
    }
    return this.http.patch<FmeaItemResponse>(
      `${this.endpoint}/projects/${projectId}/items/${itemId}`, input
    );
  }

  deleteItem(projectId: string, itemId: string): Observable<void> {
    if (environment.useMockApi) {
      const items = this.mockItems[projectId] ?? [];
      const i = items.findIndex(x => x.id === itemId);
      if (i >= 0) items.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/projects/${projectId}/items/${itemId}`);
  }

  // ---------- Mock seeds ----------

  private seedMockProjects(): FmeaProjectResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'fmea-1', tenantId: 'demo-tenant',
        code: 'PFMEA-ASM-A', name: 'FMEA processus — ligne assemblage A',
        scope: 'Postes 1 à 7, OF série 50 000+ pièces/an.',
        type: 'PROCESS_FMEA', status: 'ACTIVE',
        criticalRpnThreshold: 100, revision: 2,
        ownerUserId: 'demo-user', createdBy: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'fmea-2', tenantId: 'demo-tenant',
        code: 'DFMEA-PRD-V3', name: 'FMEA conception — capteur V3',
        type: 'DESIGN_FMEA', status: 'DRAFT',
        criticalRpnThreshold: 120, revision: 1,
        ownerUserId: 'demo-user', createdBy: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'fmea-3', tenantId: 'demo-tenant',
        code: 'BOWTIE-CYBER-001', name: 'Bow-tie — fuite données client',
        scope: 'Périmètre RGPD + NIS 2.',
        type: 'BOW_TIE', status: 'ACTIVE',
        criticalRpnThreshold: 80, revision: 1,
        ownerUserId: 'demo-user', createdBy: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }

  private seedMockItems(): Record<string, FmeaItemResponse[]> {
    const now = new Date().toISOString();
    return {
      'fmea-1': [
        {
          id: 'fmi-1', tenantId: 'demo-tenant', projectId: 'fmea-1',
          sequenceNo: 1,
          function: 'Soudure cobot poste 3',
          failureMode: 'Soudure incomplète',
          failureEffect: 'Pièce non conforme — rebut',
          failureCause: 'Dérive paramètre courant cobot',
          currentControls: 'Vision post-soudure 1/10',
          severity: 8, occurrence: 4, detection: 5, rpn: 160,
          recommendedAction: 'Mettre en place SPC en continu sur courant + vision 100%',
          critical: true,
          createdAt: now, updatedAt: now
        },
        {
          id: 'fmi-2', tenantId: 'demo-tenant', projectId: 'fmea-1',
          sequenceNo: 2,
          function: 'Sertissage poste 5',
          failureMode: 'Force de sertissage insuffisante',
          failureEffect: 'Desserrage en exploitation',
          failureCause: 'Usure outil',
          currentControls: 'Contrôle force 1/100',
          severity: 7, occurrence: 3, detection: 4, rpn: 84,
          critical: false,
          createdAt: now, updatedAt: now
        }
      ],
      'fmea-2': [],
      'fmea-3': []
    };
  }
}
