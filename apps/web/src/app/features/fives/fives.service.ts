import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { catchError, delay, map } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import { ConnectivityService } from '../../core/offline/connectivity.service';
import { OfflineQueueService } from '../../core/offline/offline-queue.service';
import {
  CreateFiveSAuditRequest,
  FiveSAuditResponse,
  FiveSAuditStatus,
  FiveSItemResponse,
  FiveSPage,
  ScorePillarRequest,
  UpdateFiveSAuditRequest
} from './fives.types';

@Injectable({ providedIn: 'root' })
export class FivesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/fives/audits`;

  private readonly mockStore: FiveSAuditResponse[] = this.seedMockAudits();

  constructor(
    private readonly http: HttpClient,
    private readonly connectivity: ConnectivityService,
    private readonly offlineQueue: OfflineQueueService
  ) {}

  listAudits(page = 0, size = 50, status?: FiveSAuditStatus): Observable<FiveSPage> {
    if (environment.useMockApi) {
      return of(this.mockPage(status)).pipe(delay(150));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<FiveSPage>(this.endpoint, { params });
  }

  getAudit(id: string): Observable<FiveSAuditResponse> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(a => a.id === id);
      return of(found ?? this.mockStore[0]).pipe(delay(120));
    }
    return this.http.get<FiveSAuditResponse>(`${this.endpoint}/${id}`);
  }

  createAudit(input: CreateFiveSAuditRequest): Observable<FiveSAuditResponse> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const audit: FiveSAuditResponse = {
        id: '5s-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        zone: input.zone,
        description: input.description,
        status: 'DRAFT',
        auditorId: input.auditorId,
        scheduledAt: input.scheduledAt,
        createdAt: now,
        updatedAt: now,
        items: []
      };
      this.mockStore.unshift(audit);
      return of(audit).pipe(delay(200));
    }
    // Offline-first terrain (§15.2-15.3) : hors-ligne, l'audit est mis en file
    // et créé pour de vrai à la resynchronisation — réponse optimiste en attendant.
    if (!this.connectivity.isOnline()) {
      return this.enqueueCreate(input);
    }
    return this.http.post<FiveSAuditResponse>(this.endpoint, input).pipe(
      catchError(err => this.isNetworkError(err) ? this.enqueueCreate(input) : throwError(() => err))
    );
  }

  updateAudit(id: string, input: UpdateFiveSAuditRequest): Observable<FiveSAuditResponse> {
    if (environment.useMockApi) {
      const a = this.mockStore.find(x => x.id === id);
      if (a) {
        if (input.zone !== undefined) a.zone = input.zone;
        if (input.description !== undefined) a.description = input.description;
        if (input.scheduledAt !== undefined) a.scheduledAt = input.scheduledAt;
        a.updatedAt = new Date().toISOString();
        return of(a).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<FiveSAuditResponse>(`${this.endpoint}/${id}`, input);
  }

  deleteAudit(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(a => a.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  scorePillar(auditId: string, input: ScorePillarRequest): Observable<FiveSItemResponse> {
    if (environment.useMockApi) {
      const audit = this.mockStore.find(a => a.id === auditId);
      const now = new Date().toISOString();
      let item: FiveSItemResponse | undefined;
      if (audit) {
        item = audit.items.find(i => i.pillar === input.pillar);
        if (item) {
          item.score = input.score;
          item.note = input.note;
          item.photoUrl = input.photoUrl;
        } else {
          item = {
            id: 'item-' + Math.random().toString(36).slice(2, 9),
            auditId,
            pillar: input.pillar,
            score: input.score,
            note: input.note,
            photoUrl: input.photoUrl
          };
          audit.items = [...audit.items, item];
        }
        audit.overallScore = Math.round(
          (audit.items.reduce((s, i) => s + i.score, 0) / audit.items.length) * 10
        );
        audit.updatedAt = now;
      }
      return of(item ?? {
        id: 'orphan', auditId, pillar: input.pillar, score: input.score
      }).pipe(delay(120));
    }
    if (!this.connectivity.isOnline()) {
      return this.enqueueScore(auditId, input);
    }
    return this.http.put<FiveSItemResponse>(`${this.endpoint}/${auditId}/score`, input).pipe(
      catchError(err => this.isNetworkError(err) ? this.enqueueScore(auditId, input) : throwError(() => err))
    );
  }

  startAudit(id: string): Observable<FiveSAuditResponse> {
    return this.transition(id, 'IN_PROGRESS', 'start');
  }

  completeAudit(id: string): Observable<FiveSAuditResponse> {
    return this.transition(id, 'COMPLETED', 'complete');
  }

  cancelAudit(id: string): Observable<FiveSAuditResponse> {
    return this.transition(id, 'CANCELLED', 'cancel');
  }

  private transition(
    id: string,
    targetStatus: FiveSAuditStatus,
    pathSegment: 'start' | 'complete' | 'cancel'
  ): Observable<FiveSAuditResponse> {
    if (environment.useMockApi) {
      const audit = this.mockStore.find(a => a.id === id);
      if (audit) {
        audit.status = targetStatus;
        audit.updatedAt = new Date().toISOString();
        if (targetStatus === 'COMPLETED') audit.completedAt = audit.updatedAt;
        return of(audit).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<FiveSAuditResponse>(`${this.endpoint}/${id}/${pathSegment}`, {});
  }

  // ---- offline (file d'attente + réponses optimistes) -------------------------

  private enqueueCreate(input: CreateFiveSAuditRequest): Observable<FiveSAuditResponse> {
    const now = new Date().toISOString();
    return this.offlineQueue
      .enqueue('POST', this.endpoint, input, `Création audit 5S — ${input.zone}`)
      .pipe(map(op => ({
        id: 'offline-' + op.id,
        tenantId: '',
        zone: input.zone,
        description: input.description,
        status: 'DRAFT' as FiveSAuditStatus,
        auditorId: input.auditorId,
        scheduledAt: input.scheduledAt,
        createdAt: now,
        updatedAt: now,
        items: [],
        pendingSync: true
      })));
  }

  private enqueueScore(auditId: string, input: ScorePillarRequest): Observable<FiveSItemResponse> {
    return this.offlineQueue
      .enqueue('PUT', `${this.endpoint}/${auditId}/score`, input, `Score 5S ${input.pillar}`)
      .pipe(map(op => ({
        id: 'offline-' + op.id,
        auditId,
        pillar: input.pillar,
        score: input.score,
        note: input.note,
        photoUrl: input.photoUrl,
        pendingSync: true
      })));
  }

  /** status 0 = la requête n'a pas atteint le serveur (coupure pendant l'envoi). */
  private isNetworkError(err: unknown): boolean {
    return err instanceof HttpErrorResponse && err.status === 0;
  }

  private mockPage(status?: FiveSAuditStatus): FiveSPage {
    const filtered = status ? this.mockStore.filter(a => a.status === status) : this.mockStore;
    return {
      content: filtered, totalElements: filtered.length, totalPages: 1,
      number: 0, size: filtered.length
    };
  }

  private seedMockAudits(): FiveSAuditResponse[] {
    const now = new Date().toISOString();
    return [
      {
        id: '5s-1', tenantId: 'demo-tenant', zone: 'Atelier mécanique A',
        description: 'Audit mensuel ligne 1', status: 'COMPLETED',
        auditorId: 'demo-user', scheduledAt: now, completedAt: now,
        overallScore: 78, createdAt: now, updatedAt: now,
        items: []
      },
      {
        id: '5s-2', tenantId: 'demo-tenant', zone: 'Bloc opératoire B',
        description: 'CHU - audit hebdo', status: 'IN_PROGRESS',
        auditorId: 'demo-user', scheduledAt: now,
        createdAt: now, updatedAt: now, items: []
      },
      {
        id: '5s-3', tenantId: 'demo-tenant', zone: 'Entrepôt logistique nord',
        status: 'DRAFT',
        auditorId: 'demo-user', scheduledAt: now,
        createdAt: now, updatedAt: now, items: []
      }
    ];
  }
}
