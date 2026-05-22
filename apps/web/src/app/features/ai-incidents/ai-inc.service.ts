import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AiIncSeverity,
  AiIncStatus,
  AiIncView,
  CloseRequest,
  DetectRequest,
  DismissRequest,
  EditRequest,
  NotifyRegulatorRequest,
  SEVERITY_DEADLINE_DAYS,
  StartInvestigationRequest
} from './ai-inc.types';

@Injectable({ providedIn: 'root' })
export class AiIncidentsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/incidents`;
  private readonly mockStore: AiIncView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: AiIncStatus): Observable<AiIncView[]> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      const f = status ? this.mockStore.filter(i => i.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<AiIncView[]>(this.endpoint, { params });
  }

  listBySeverity(severity: AiIncSeverity): Observable<AiIncView[]> {
    if (environment.useMockApi) {
      return of(this.mockStore.filter(i => i.severity === severity)).pipe(delay(120));
    }
    const params = new HttpParams().set('severity', severity);
    return this.http.get<AiIncView[]>(`${this.endpoint}/by-severity`, { params });
  }

  listOverdue(limit = 200): Observable<AiIncView[]> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      const arr = this.mockStore.filter(i => i.overdueForRegulator).slice(0, limit);
      return of(arr).pipe(delay(120));
    }
    const params = new HttpParams().set('limit', limit);
    return this.http.get<AiIncView[]>(`${this.endpoint}/overdue-regulator-notification`, { params });
  }

  get(id: string): Observable<AiIncView> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      return of(this.mockStore.find(i => i.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<AiIncView>(`${this.endpoint}/${id}`);
  }

  detect(input: DetectRequest): Observable<AiIncView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const days = SEVERITY_DEADLINE_DAYS[input.severity];
      const deadline = new Date(new Date(input.detectedAt).getTime() + days * 86400000).toISOString();
      const i: AiIncView = {
        id: 'inc-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference, aiSystemId: input.aiSystemId,
        severity: input.severity,
        description: input.description,
        affectedPersonsDescription: input.affectedPersonsDescription,
        immediateActionsTaken: input.immediateActionsTaken,
        occurredAt: input.occurredAt, detectedAt: input.detectedAt,
        status: 'DETECTED', overdueForRegulator: false,
        regulatorNotificationDeadline: deadline,
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(i);
      return of(i).pipe(delay(150));
    }
    return this.http.post<AiIncView>(this.endpoint, input);
  }

  edit(id: string, input: EditRequest): Observable<AiIncView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (i) {
        i.description = input.description;
        i.affectedPersonsDescription = input.affectedPersonsDescription;
        i.immediateActionsTaken = input.immediateActionsTaken;
        i.updatedAt = new Date().toISOString();
        return of(i).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<AiIncView>(`${this.endpoint}/${id}`, input);
  }

  startInvestigation(id: string, body: StartInvestigationRequest): Observable<AiIncView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (i) {
        i.status = 'INVESTIGATING';
        i.investigationStartedAt = now;
        i.investigationLeadUserId = body.investigationLeadUserId;
        i.updatedAt = now;
        return of(i).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiIncView>(`${this.endpoint}/${id}/start-investigation`, body);
  }

  notifyRegulator(id: string, body: NotifyRegulatorRequest): Observable<AiIncView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (i) {
        i.status = 'NOTIFIED_REGULATOR';
        i.regulatorNotifiedAt = now;
        i.regulatorReference = body.regulatorReference;
        i.rootCauseAnalysis = body.rootCauseAnalysis;
        if (body.correctiveActions) i.correctiveActions = body.correctiveActions;
        i.overdueForRegulator = false;
        i.updatedAt = now;
        return of(i).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiIncView>(`${this.endpoint}/${id}/notify-regulator`, body);
  }

  close(id: string, body: CloseRequest): Observable<AiIncView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (i) {
        i.status = 'CLOSED';
        i.correctiveActions = body.correctiveActions;
        i.closedAt = now;
        i.updatedAt = now;
        return of(i).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiIncView>(`${this.endpoint}/${id}/close`, body);
  }

  dismiss(id: string, body: DismissRequest): Observable<AiIncView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (i) {
        i.status = 'DISMISSED';
        i.dismissReason = body.reason;
        i.dismissedAt = now;
        i.updatedAt = now;
        return of(i).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiIncView>(`${this.endpoint}/${id}/dismiss`, body);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const idx = this.mockStore.findIndex(i => i.id === id);
      if (idx >= 0) this.mockStore.splice(idx, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private recomputeOverdue(): void {
    const now = Date.now();
    for (const i of this.mockStore) {
      if (i.status === 'DETECTED' || i.status === 'INVESTIGATING') {
        const deadline = i.regulatorNotificationDeadline
          ? new Date(i.regulatorNotificationDeadline).getTime()
          : new Date(i.detectedAt).getTime() + SEVERITY_DEADLINE_DAYS[i.severity] * 86400000;
        i.overdueForRegulator = deadline < now;
      } else {
        i.overdueForRegulator = false;
      }
    }
  }

  private seed(): AiIncView[] {
    const now = Date.now();
    const past = (d: number) => new Date(now - d * 86400000).toISOString();
    const fut  = (d: number) => new Date(now + d * 86400000).toISOString();
    return [
      {
        id: 'inc-1', tenantId: 'demo-tenant',
        reference: 'AIINC-2026-001',
        aiSystemId: '00000000-0000-0000-0000-000000000001',
        severity: 'SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS',
        description: 'Discrimination détectée dans le modèle de scoring crédit : taux de refus disproportionné pour les demandes en provenance de certains quartiers.',
        affectedPersonsDescription: 'Environ 1 200 demandes refusées sur 3 mois (audit interne).',
        immediateActionsTaken: 'Gel temporaire du modèle. Bascule vers modèle V2.3 antérieur.',
        occurredAt: past(8), detectedAt: past(5),
        investigationStartedAt: past(4), investigationLeadUserId: 'demo-user',
        status: 'INVESTIGATING',
        overdueForRegulator: false,
        regulatorNotificationDeadline: fut(5),
        createdByUserId: 'demo-user',
        createdAt: past(5), updatedAt: past(4)
      },
      {
        id: 'inc-2', tenantId: 'demo-tenant',
        reference: 'AIINC-2026-002',
        aiSystemId: '00000000-0000-0000-0000-000000000002',
        severity: 'DEATH_OR_SERIOUS_HARM_TO_HEALTH',
        description: 'Diagnostic erroné par l\'IA assistant — patient orienté vers un suivi non urgent au lieu d\'une consultation immédiate. Patient hospitalisé en urgence 48h plus tard.',
        immediateActionsTaken: 'Modèle désactivé en production. Revue médicale humaine activée 100%.',
        occurredAt: past(3), detectedAt: past(2),
        investigationStartedAt: past(2), investigationLeadUserId: 'demo-user',
        regulatorNotifiedAt: past(1),
        regulatorReference: 'CNIL-IA-2026-INC-007',
        rootCauseAnalysis: 'Détection d\'une dérive de modèle non détectée par la PMM faute de seuils d\'alerte adaptés au sous-groupe concerné.',
        correctiveActions: 'Mise en place de seuils d\'alerte par sous-groupes démographiques. Audit indépendant prévu sous 30 jours.',
        status: 'NOTIFIED_REGULATOR',
        overdueForRegulator: false,
        regulatorNotificationDeadline: past(0),
        createdByUserId: 'demo-user',
        createdAt: past(2), updatedAt: past(1)
      },
      {
        id: 'inc-3', tenantId: 'demo-tenant',
        reference: 'AIINC-2026-003',
        aiSystemId: '00000000-0000-0000-0000-000000000003',
        severity: 'CRITICAL_INFRASTRUCTURE_DISRUPTION',
        description: 'Perte de service du routage IA pendant 4h — impact sur 30k requêtes.',
        immediateActionsTaken: 'Rollback automatique. Mode dégradé manuel activé.',
        occurredAt: past(20), detectedAt: past(20),
        status: 'DETECTED',
        overdueForRegulator: true,
        regulatorNotificationDeadline: past(5),
        createdByUserId: 'demo-user',
        createdAt: past(20), updatedAt: past(20)
      }
    ];
  }
}
