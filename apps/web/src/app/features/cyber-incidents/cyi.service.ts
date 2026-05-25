import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CyiCloseRequest,
  CyiDetectRequest,
  CyiLinkBreachRequest,
  CyiMitigateRequest,
  CyiNotificationRequest,
  CyiRejectRequest,
  CyiStartAssessmentRequest,
  CyiStatus,
  CyiUpdateSeverityRequest,
  CyiView
} from './cyi.types';

@Injectable({ providedIn: 'root' })
export class CyberIncidentsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents`;
  private readonly mockStore: CyiView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: CyiStatus): Observable<CyiView[]> {
    if (environment.useMockApi) {
      this.recompute();
      const f = status ? this.mockStore.filter(i => i.status === status) : this.mockStore;
      return of([...f]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<CyiView[]>(this.endpoint, { params });
  }

  earlyWarningOverdue(limit = 100): Observable<CyiView[]> {
    if (environment.useMockApi) {
      this.recompute();
      return of(this.mockStore.filter(i => i.earlyWarningOverdue).slice(0, limit)).pipe(delay(120));
    }
    return this.http.get<CyiView[]>(`${this.endpoint}/early-warning-overdue`, {
      params: new HttpParams().set('limit', String(limit))
    });
  }

  initialAssessmentOverdue(limit = 100): Observable<CyiView[]> {
    if (environment.useMockApi) {
      this.recompute();
      return of(this.mockStore.filter(i => i.initialAssessmentOverdue).slice(0, limit)).pipe(delay(120));
    }
    return this.http.get<CyiView[]>(`${this.endpoint}/initial-assessment-overdue`, {
      params: new HttpParams().set('limit', String(limit))
    });
  }

  finalReportOverdue(limit = 100): Observable<CyiView[]> {
    if (environment.useMockApi) {
      this.recompute();
      return of(this.mockStore.filter(i => i.finalReportOverdue).slice(0, limit)).pipe(delay(120));
    }
    return this.http.get<CyiView[]>(`${this.endpoint}/final-report-overdue`, {
      params: new HttpParams().set('limit', String(limit))
    });
  }

  get(id: string): Observable<CyiView> {
    if (environment.useMockApi) {
      this.recompute();
      const i = this.mockStore.find(x => x.id === id);
      return i ? of(structuredClone(i)).pipe(delay(80))
               : throwError(() => this.err(404, 'Incident introuvable.'));
    }
    return this.http.get<CyiView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.reference === reference);
      return i ? of(structuredClone(i)).pipe(delay(80))
               : throwError(() => this.err(404, 'Référence introuvable.'));
    }
    return this.http.get<CyiView>(`${this.endpoint}/by-reference`, {
      params: new HttpParams().set('reference', reference)
    });
  }

  detect(req: CyiDetectRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      if (this.mockStore.some(i => i.reference === req.reference)) {
        return throwError(() => this.err(409, 'Référence déjà utilisée.'));
      }
      const significant = req.severity === 'HIGH' || req.severity === 'CRITICAL';
      const detectedMs = new Date(req.detectedAt).getTime();
      const view: CyiView = {
        id: this.uuid(),
        tenantId: '00000000-0000-0000-0000-000000000001',
        reference: req.reference, title: req.title,
        description: req.description ?? null,
        detectedAt: req.detectedAt,
        occurredAt: req.occurredAt ?? null,
        earlyWarningDeadlineAt:     significant ? new Date(detectedMs + 24 * 3600 * 1000).toISOString() : null,
        initialAssessmentDeadlineAt:significant ? new Date(detectedMs + 72 * 3600 * 1000).toISOString() : null,
        finalReportDeadlineAt:      significant ? new Date(detectedMs + 30 * 86400000).toISOString() : null,
        incidentType: req.incidentType, severity: req.severity,
        status: 'DETECTED',
        estimatedAffectedUsers: req.estimatedAffectedUsers,
        affectedAssets: req.affectedAssets ?? [],
        affectedServices: req.affectedServices ?? [],
        linkedBreachId: req.linkedBreachId ?? null,
        containmentMeasures: null, impactDescription: null,
        earlyWarningSentAt: null, earlyWarningReference: null,
        initialAssessmentSentAt: null, initialAssessmentReference: null,
        finalReportSentAt: null, finalReportReference: null,
        closureNotes: null, rejectionReason: null,
        reportedByUserId: req.reportedByUserId, handledByUserId: null,
        closedAt: null, updatedAt: new Date().toISOString(),
        earlyWarningOverdue: false, initialAssessmentOverdue: false,
        finalReportOverdue: false, significant
      };
      this.mockStore.unshift(view);
      return of(structuredClone(view)).pipe(delay(140));
    }
    return this.http.post<CyiView>(this.endpoint, req);
  }

  startAssessment(id: string, req: CyiStartAssessmentRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (i.status !== 'DETECTED') return throwError(() => this.err(409, 'Démarrage possible uniquement depuis DETECTED.'));
      i.status = 'ASSESSING';
      i.handledByUserId = req.handledByUserId;
      i.updatedAt = new Date().toISOString();
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/start-assessment`, req);
  }

  mitigate(id: string, req: CyiMitigateRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (i.status !== 'ASSESSING') return throwError(() => this.err(409, 'Endiguement possible uniquement depuis ASSESSING.'));
      i.status = 'MITIGATED';
      i.containmentMeasures = req.containmentMeasures;
      i.impactDescription = req.impactDescription ?? null;
      if (req.handledByUserId) i.handledByUserId = req.handledByUserId;
      i.updatedAt = new Date().toISOString();
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/mitigate`, req);
  }

  recordEarlyWarning(id: string, req: CyiNotificationRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (!i.significant) return throwError(() => this.err(409, 'Notification CSIRT requise uniquement pour HIGH/CRITICAL.'));
      i.earlyWarningSentAt = req.sentAt;
      i.earlyWarningReference = req.reference;
      i.earlyWarningOverdue = false;
      i.updatedAt = new Date().toISOString();
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/early-warning`, req);
  }

  recordInitialAssessment(id: string, req: CyiNotificationRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (!i.significant) return throwError(() => this.err(409, 'Notification CSIRT requise uniquement pour HIGH/CRITICAL.'));
      i.initialAssessmentSentAt = req.sentAt;
      i.initialAssessmentReference = req.reference;
      i.initialAssessmentOverdue = false;
      i.updatedAt = new Date().toISOString();
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/initial-assessment`, req);
  }

  recordFinalReport(id: string, req: CyiNotificationRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (!i.significant) return throwError(() => this.err(409, 'Notification CSIRT requise uniquement pour HIGH/CRITICAL.'));
      i.finalReportSentAt = req.sentAt;
      i.finalReportReference = req.reference;
      i.finalReportOverdue = false;
      i.updatedAt = new Date().toISOString();
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/final-report`, req);
  }

  close(id: string, req: CyiCloseRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (i.status !== 'MITIGATED') return throwError(() => this.err(409, 'Clôture possible uniquement après MITIGATED.'));
      const now = new Date().toISOString();
      i.status = 'CLOSED'; i.closureNotes = req.closureNotes ?? null;
      i.closedAt = now; i.updatedAt = now;
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/close`, req);
  }

  reject(id: string, req: CyiRejectRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (i.status !== 'DETECTED' && i.status !== 'ASSESSING') {
        return throwError(() => this.err(409, 'Rejet possible uniquement en DETECTED/ASSESSING.'));
      }
      const now = new Date().toISOString();
      i.status = 'REJECTED'; i.rejectionReason = req.reason;
      i.closedAt = now; i.updatedAt = now;
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/reject`, req);
  }

  updateSeverity(id: string, req: CyiUpdateSeverityRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      if (i.status === 'CLOSED' || i.status === 'REJECTED') {
        return throwError(() => this.err(409, 'Sévérité figée — incident terminal.'));
      }
      i.severity = req.severity;
      const sig = req.severity === 'HIGH' || req.severity === 'CRITICAL';
      i.significant = sig;
      if (sig && !i.earlyWarningDeadlineAt) {
        const t0 = new Date(i.detectedAt).getTime();
        i.earlyWarningDeadlineAt      = new Date(t0 + 24 * 3600 * 1000).toISOString();
        i.initialAssessmentDeadlineAt = new Date(t0 + 72 * 3600 * 1000).toISOString();
        i.finalReportDeadlineAt       = new Date(t0 + 30 * 86400000).toISOString();
      }
      i.updatedAt = new Date().toISOString();
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/severity`, req);
  }

  linkBreach(id: string, req: CyiLinkBreachRequest): Observable<CyiView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (!i) return throwError(() => this.err(404, 'Incident introuvable.'));
      i.linkedBreachId = req.breachId;
      i.updatedAt = new Date().toISOString();
      return of(structuredClone(i)).pipe(delay(120));
    }
    return this.http.post<CyiView>(`${this.endpoint}/${id}/link-breach`, req);
  }

  private recompute(): void {
    const now = Date.now();
    for (const i of this.mockStore) {
      if (i.status === 'CLOSED' || i.status === 'REJECTED' || !i.significant) {
        i.earlyWarningOverdue = false;
        i.initialAssessmentOverdue = false;
        i.finalReportOverdue = false;
        continue;
      }
      i.earlyWarningOverdue      = !i.earlyWarningSentAt      && !!i.earlyWarningDeadlineAt      && new Date(i.earlyWarningDeadlineAt).getTime() < now;
      i.initialAssessmentOverdue = !i.initialAssessmentSentAt && !!i.initialAssessmentDeadlineAt && new Date(i.initialAssessmentDeadlineAt).getTime() < now;
      i.finalReportOverdue       = !i.finalReportSentAt       && !!i.finalReportDeadlineAt       && new Date(i.finalReportDeadlineAt).getTime() < now;
    }
  }

  private err(status: number, title: string) {
    return { status, error: { type: 'about:blank', title, status, detail: title } };
  }

  private uuid(): string {
    return 'cyi-' + Math.random().toString(16).slice(2, 10) + '-' + Date.now().toString(16);
  }

  private seed(): CyiView[] {
    const now = Date.now();
    const hr = 3600000; const day = 86400000;
    return [
      {
        id: 'cyi-seed-001', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'NIS2-INC-2026-001', title: 'Tentative ransomware sur partage RH',
        description: 'Détection EDR — chiffrement de fichiers sur un endpoint isolé immédiatement.',
        detectedAt: new Date(now - 6 * hr).toISOString(),
        occurredAt: new Date(now - 8 * hr).toISOString(),
        earlyWarningDeadlineAt:     new Date(now - 6 * hr + 24 * hr).toISOString(),
        initialAssessmentDeadlineAt:new Date(now - 6 * hr + 72 * hr).toISOString(),
        finalReportDeadlineAt:      new Date(now - 6 * hr + 30 * day).toISOString(),
        incidentType: 'RANSOMWARE', severity: 'CRITICAL',
        status: 'ASSESSING',
        estimatedAffectedUsers: 1,
        affectedAssets: ['LAPTOP-RH-042'], affectedServices: ['Partage RH'],
        linkedBreachId: null,
        containmentMeasures: null, impactDescription: null,
        earlyWarningSentAt: null, earlyWarningReference: null,
        initialAssessmentSentAt: null, initialAssessmentReference: null,
        finalReportSentAt: null, finalReportReference: null,
        closureNotes: null, rejectionReason: null,
        reportedByUserId: '00000000-0000-0000-0000-000000000999',
        handledByUserId: '00000000-0000-0000-0000-000000000999',
        closedAt: null, updatedAt: new Date(now - 4 * hr).toISOString(),
        earlyWarningOverdue: false, initialAssessmentOverdue: false,
        finalReportOverdue: false, significant: true
      },
      {
        id: 'cyi-seed-002', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'NIS2-INC-2026-002', title: 'DDoS sur passerelle API publique',
        description: 'Saturation lien WAN — basculement CDN + mode dégradé activé. Trafic légitime restauré sous 35 minutes.',
        detectedAt: new Date(now - 5 * day).toISOString(),
        occurredAt: new Date(now - 5 * day - 10 * 60000).toISOString(),
        earlyWarningDeadlineAt:     new Date(now - 5 * day + 24 * hr).toISOString(),
        initialAssessmentDeadlineAt:new Date(now - 5 * day + 72 * hr).toISOString(),
        finalReportDeadlineAt:      new Date(now - 5 * day + 30 * day).toISOString(),
        incidentType: 'DDOS', severity: 'HIGH',
        status: 'MITIGATED',
        estimatedAffectedUsers: 12000,
        affectedAssets: ['EDGE-NGINX'], affectedServices: ['API publique'],
        linkedBreachId: null,
        containmentMeasures: 'Activation rate-limit IP, bascule CDN, ACL drop sur IP source.',
        impactDescription: '35 minutes de dégradation API publique — 12 000 utilisateurs impactés.',
        earlyWarningSentAt:     new Date(now - 5 * day + 5 * hr).toISOString(),
        earlyWarningReference: 'CSIRT-FR-2026-INC-A14',
        initialAssessmentSentAt:new Date(now - 5 * day + 60 * hr).toISOString(),
        initialAssessmentReference: 'CSIRT-FR-2026-INC-A14/2',
        finalReportSentAt: null, finalReportReference: null,
        closureNotes: null, rejectionReason: null,
        reportedByUserId: '00000000-0000-0000-0000-000000000999',
        handledByUserId: '00000000-0000-0000-0000-000000000999',
        closedAt: null, updatedAt: new Date(now - 2 * day).toISOString(),
        earlyWarningOverdue: false, initialAssessmentOverdue: false,
        finalReportOverdue: false, significant: true
      },
      {
        id: 'cyi-seed-003', tenantId: '00000000-0000-0000-0000-000000000001',
        reference: 'NIS2-INC-2026-003', title: 'Phishing ciblé direction financière',
        description: 'Email usurpant un fournisseur, tentative virement frauduleux 45k€ — bloqué par contrôle 4 yeux.',
        detectedAt: new Date(now - 2 * day).toISOString(),
        occurredAt: new Date(now - 2 * day - 30 * 60000).toISOString(),
        earlyWarningDeadlineAt: null, initialAssessmentDeadlineAt: null, finalReportDeadlineAt: null,
        incidentType: 'PHISHING', severity: 'MEDIUM',
        status: 'DETECTED',
        estimatedAffectedUsers: 0,
        affectedAssets: [], affectedServices: ['Compte CFO'],
        linkedBreachId: null,
        containmentMeasures: null, impactDescription: null,
        earlyWarningSentAt: null, earlyWarningReference: null,
        initialAssessmentSentAt: null, initialAssessmentReference: null,
        finalReportSentAt: null, finalReportReference: null,
        closureNotes: null, rejectionReason: null,
        reportedByUserId: '00000000-0000-0000-0000-000000000999',
        handledByUserId: null,
        closedAt: null, updatedAt: new Date(now - 2 * day).toISOString(),
        earlyWarningOverdue: false, initialAssessmentOverdue: false,
        finalReportOverdue: false, significant: false
      }
    ];
  }
}
