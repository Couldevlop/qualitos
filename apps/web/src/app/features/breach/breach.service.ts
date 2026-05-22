import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, throwError } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  BreachCloseRequest,
  BreachContainRequest,
  BreachDetectRequest,
  BreachDpaNotificationRequest,
  BreachRejectRequest,
  BreachStartAssessmentRequest,
  BreachStatus,
  BreachSubjectsNotificationRequest,
  BreachUpdateSeverityRequest,
  BreachView
} from './breach.types';

@Injectable({ providedIn: 'root' })
export class BreachService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/breaches`;
  private readonly mockStore: BreachView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: BreachStatus): Observable<BreachView[]> {
    if (environment.useMockApi) {
      this.recompute();
      const f = status ? this.mockStore.filter(b => b.status === status) : this.mockStore;
      return of([...f]).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<BreachView[]>(this.endpoint, { params });
  }

  dpaOverdue(limit = 100): Observable<BreachView[]> {
    if (environment.useMockApi) {
      this.recompute();
      return of(this.mockStore.filter(b => b.dpaOverdue).slice(0, limit)).pipe(delay(120));
    }
    return this.http.get<BreachView[]>(`${this.endpoint}/dpa-overdue`, {
      params: new HttpParams().set('limit', String(limit))
    });
  }

  get(id: string): Observable<BreachView> {
    if (environment.useMockApi) {
      this.recompute();
      const b = this.mockStore.find(x => x.id === id);
      return b ? of(structuredClone(b)).pipe(delay(80))
               : throwError(() => this.err(404, 'Violation introuvable.'));
    }
    return this.http.get<BreachView>(`${this.endpoint}/${id}`);
  }

  detect(req: BreachDetectRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      if (this.mockStore.some(b => b.internalReference === req.internalReference)) {
        return throwError(() => this.err(409, 'Référence interne déjà utilisée.'));
      }
      const detectedMs = new Date(req.detectedAt).getTime();
      const requiresSubject = req.severity === 'HIGH' || req.severity === 'CRITICAL';
      const view: BreachView = {
        id: this.uuid(),
        tenantId: '00000000-0000-0000-0000-000000000001',
        internalReference: req.internalReference,
        title: req.title,
        description: req.description ?? null,
        detectedAt: req.detectedAt,
        occurredAt: req.occurredAt ?? null,
        dpaDeadlineAt: new Date(detectedMs + 72 * 3600 * 1000).toISOString(),
        severity: req.severity, status: 'DETECTED',
        affectedSubjectsCount: req.affectedSubjectsCount,
        affectedDataCategories: req.affectedDataCategories ?? [],
        riskOfHarmDescription: req.riskOfHarmDescription ?? null,
        containmentMeasures: null,
        dpaNotifiedAt: null, dpaReference: null,
        subjectsNotifiedAt: null, subjectsNotificationChannel: null,
        rejectionReason: null, closureNotes: null,
        reportedByUserId: req.reportedByUserId, handledByUserId: null,
        closedAt: null, updatedAt: new Date().toISOString(),
        dpaOverdue: false, subjectNotificationRequired: requiresSubject
      };
      this.mockStore.unshift(view);
      return of(structuredClone(view)).pipe(delay(140));
    }
    return this.http.post<BreachView>(this.endpoint, req);
  }

  startAssessment(id: string, req: BreachStartAssessmentRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      const b = this.mockStore.find(x => x.id === id);
      if (!b) return throwError(() => this.err(404, 'Violation introuvable.'));
      if (b.status !== 'DETECTED') return throwError(() => this.err(409, 'Démarrage possible uniquement depuis DETECTED.'));
      b.status = 'ASSESSING';
      b.handledByUserId = req.handledByUserId;
      b.updatedAt = new Date().toISOString();
      return of(structuredClone(b)).pipe(delay(120));
    }
    return this.http.post<BreachView>(`${this.endpoint}/${id}/start-assessment`, req);
  }

  contain(id: string, req: BreachContainRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      const b = this.mockStore.find(x => x.id === id);
      if (!b) return throwError(() => this.err(404, 'Violation introuvable.'));
      if (b.status !== 'ASSESSING') return throwError(() => this.err(409, 'Endiguement possible uniquement depuis ASSESSING.'));
      b.status = 'CONTAINED';
      b.containmentMeasures = req.containmentMeasures;
      if (req.handledByUserId) b.handledByUserId = req.handledByUserId;
      b.updatedAt = new Date().toISOString();
      return of(structuredClone(b)).pipe(delay(120));
    }
    return this.http.post<BreachView>(`${this.endpoint}/${id}/contain`, req);
  }

  notifyDpa(id: string, req: BreachDpaNotificationRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      const b = this.mockStore.find(x => x.id === id);
      if (!b) return throwError(() => this.err(404, 'Violation introuvable.'));
      if (b.status === 'CLOSED' || b.status === 'REJECTED') {
        return throwError(() => this.err(409, 'Notification CNIL impossible — incident terminal.'));
      }
      b.dpaNotifiedAt = req.notifiedAt;
      b.dpaReference = req.reference;
      b.dpaOverdue = false;
      b.updatedAt = new Date().toISOString();
      return of(structuredClone(b)).pipe(delay(120));
    }
    return this.http.post<BreachView>(`${this.endpoint}/${id}/notify-dpa`, req);
  }

  notifySubjects(id: string, req: BreachSubjectsNotificationRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      const b = this.mockStore.find(x => x.id === id);
      if (!b) return throwError(() => this.err(404, 'Violation introuvable.'));
      if (b.status === 'CLOSED' || b.status === 'REJECTED') {
        return throwError(() => this.err(409, 'Notification personnes impossible — incident terminal.'));
      }
      if (!b.subjectNotificationRequired) {
        return throwError(() => this.err(409, 'Notification personnes non requise (sévérité LOW/MEDIUM, Art. 34).'));
      }
      b.subjectsNotifiedAt = req.notifiedAt;
      b.subjectsNotificationChannel = req.channel;
      b.updatedAt = new Date().toISOString();
      return of(structuredClone(b)).pipe(delay(120));
    }
    return this.http.post<BreachView>(`${this.endpoint}/${id}/notify-subjects`, req);
  }

  close(id: string, req: BreachCloseRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      const b = this.mockStore.find(x => x.id === id);
      if (!b) return throwError(() => this.err(404, 'Violation introuvable.'));
      if (b.status !== 'CONTAINED') return throwError(() => this.err(409, 'Clôture possible uniquement après CONTAINED.'));
      const now = new Date().toISOString();
      b.status = 'CLOSED'; b.closureNotes = req.closureNotes ?? null;
      b.closedAt = now; b.updatedAt = now;
      return of(structuredClone(b)).pipe(delay(120));
    }
    return this.http.post<BreachView>(`${this.endpoint}/${id}/close`, req);
  }

  reject(id: string, req: BreachRejectRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      const b = this.mockStore.find(x => x.id === id);
      if (!b) return throwError(() => this.err(404, 'Violation introuvable.'));
      if (b.status !== 'DETECTED' && b.status !== 'ASSESSING') {
        return throwError(() => this.err(409, 'Rejet possible uniquement en DETECTED/ASSESSING.'));
      }
      const now = new Date().toISOString();
      b.status = 'REJECTED'; b.rejectionReason = req.reason;
      b.closedAt = now; b.updatedAt = now;
      return of(structuredClone(b)).pipe(delay(120));
    }
    return this.http.post<BreachView>(`${this.endpoint}/${id}/reject`, req);
  }

  updateSeverity(id: string, req: BreachUpdateSeverityRequest): Observable<BreachView> {
    if (environment.useMockApi) {
      const b = this.mockStore.find(x => x.id === id);
      if (!b) return throwError(() => this.err(404, 'Violation introuvable.'));
      if (b.status === 'CLOSED' || b.status === 'REJECTED') {
        return throwError(() => this.err(409, 'Sévérité figée — incident terminal.'));
      }
      b.severity = req.severity;
      b.subjectNotificationRequired = req.severity === 'HIGH' || req.severity === 'CRITICAL';
      b.updatedAt = new Date().toISOString();
      return of(structuredClone(b)).pipe(delay(120));
    }
    return this.http.post<BreachView>(`${this.endpoint}/${id}/severity`, req);
  }

  private recompute(): void {
    const now = Date.now();
    for (const b of this.mockStore) {
      if (b.status === 'CLOSED' || b.status === 'REJECTED' || b.dpaNotifiedAt) {
        b.dpaOverdue = false;
        continue;
      }
      b.dpaOverdue = !!b.dpaDeadlineAt && new Date(b.dpaDeadlineAt).getTime() < now;
    }
  }

  private err(status: number, title: string) {
    return { status, error: { type: 'about:blank', title, status, detail: title } };
  }

  private uuid(): string {
    return 'br-' + Math.random().toString(16).slice(2, 10) + '-' + Date.now().toString(16);
  }

  private seed(): BreachView[] {
    const now = Date.now();
    const hr = 3600000;
    return [
      {
        id: 'br-seed-001', tenantId: '00000000-0000-0000-0000-000000000001',
        internalReference: 'BR-2026-001',
        title: 'Email contenant tableau RH envoyé à mauvaise liste',
        description: 'Un export RH (nom, email, salaire de 47 collaborateurs) a été envoyé par erreur à une liste de diffusion publique. Rappel effectué, mais 3 destinataires hors entreprise auraient pu lire.',
        detectedAt: new Date(now - 30 * hr).toISOString(),
        occurredAt: new Date(now - 32 * hr).toISOString(),
        dpaDeadlineAt: new Date(now - 30 * hr + 72 * hr).toISOString(),
        severity: 'HIGH', status: 'ASSESSING',
        affectedSubjectsCount: 47,
        affectedDataCategories: ['identité', 'salaire', 'email pro'],
        riskOfHarmDescription: 'Risque réputationnel + atteinte vie privée. Pas de donnée bancaire ou santé.',
        containmentMeasures: null,
        dpaNotifiedAt: null, dpaReference: null,
        subjectsNotifiedAt: null, subjectsNotificationChannel: null,
        rejectionReason: null, closureNotes: null,
        reportedByUserId: '00000000-0000-0000-0000-000000000999',
        handledByUserId: '00000000-0000-0000-0000-000000000999',
        closedAt: null, updatedAt: new Date(now - 10 * hr).toISOString(),
        dpaOverdue: false, subjectNotificationRequired: true
      },
      {
        id: 'br-seed-002', tenantId: '00000000-0000-0000-0000-000000000001',
        internalReference: 'BR-2026-002',
        title: 'Clé USB perdue contenant un export client (chiffré AES-256)',
        description: 'Clé USB perdue lors d\'un déplacement. Contenait un export de 1 200 contacts clients (nom/téléphone/CA) — chiffré AES-256.',
        detectedAt: new Date(now - 4 * 24 * hr).toISOString(),
        occurredAt: new Date(now - 4 * 24 * hr - 6 * hr).toISOString(),
        dpaDeadlineAt: new Date(now - 4 * 24 * hr + 72 * hr).toISOString(),
        severity: 'MEDIUM', status: 'CONTAINED',
        affectedSubjectsCount: 1200,
        affectedDataCategories: ['nom', 'téléphone', 'chiffre d\'affaires'],
        riskOfHarmDescription: 'Chiffrement AES-256 actif — risque jugé limité.',
        containmentMeasures: 'Révocation des clés de déchiffrement, notification équipe sécurité, journalisation de la perte. Recherche infructueuse.',
        dpaNotifiedAt: new Date(now - 4 * 24 * hr + 40 * hr).toISOString(),
        dpaReference: 'CNIL-NOT-2026-12345',
        subjectsNotifiedAt: null, subjectsNotificationChannel: null,
        rejectionReason: null, closureNotes: null,
        reportedByUserId: '00000000-0000-0000-0000-000000000999',
        handledByUserId: '00000000-0000-0000-0000-000000000999',
        closedAt: null, updatedAt: new Date(now - 2 * 24 * hr).toISOString(),
        dpaOverdue: false, subjectNotificationRequired: false
      },
      {
        id: 'br-seed-003', tenantId: '00000000-0000-0000-0000-000000000001',
        internalReference: 'BR-2026-003',
        title: 'Accès non autorisé à dossier patient (interne)',
        description: 'Consultation d\'un dossier patient par un soignant non rattaché au service. Détecté par audit log.',
        detectedAt: new Date(now - 5 * hr).toISOString(),
        occurredAt: new Date(now - 6 * hr).toISOString(),
        dpaDeadlineAt: new Date(now - 5 * hr + 72 * hr).toISOString(),
        severity: 'CRITICAL', status: 'DETECTED',
        affectedSubjectsCount: 1,
        affectedDataCategories: ['santé', 'identité'],
        riskOfHarmDescription: 'Donnée de santé — catégorie particulière Art. 9. Atteinte avérée à la confidentialité.',
        containmentMeasures: null,
        dpaNotifiedAt: null, dpaReference: null,
        subjectsNotifiedAt: null, subjectsNotificationChannel: null,
        rejectionReason: null, closureNotes: null,
        reportedByUserId: '00000000-0000-0000-0000-000000000999',
        handledByUserId: null,
        closedAt: null, updatedAt: new Date(now - 5 * hr).toISOString(),
        dpaOverdue: false, subjectNotificationRequired: true
      }
    ];
  }
}
