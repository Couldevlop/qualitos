import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  EditRequest,
  IncidentPage,
  IncidentSeverity,
  IncidentStatus,
  IncidentType,
  IncidentView,
  InvestigateRequest,
  LinkCapaRequest,
  LinkNcRequest,
  MitigateRequest,
  ReportRequest,
  Statistics
} from './ehs.types';

@Injectable({ providedIn: 'root' })
export class EhsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ehs/incidents`;

  private readonly mockStore: IncidentView[] = this.seedIncidents();

  constructor(private readonly http: HttpClient) {}

  list(
    page = 0, size = 50,
    status?: IncidentStatus, type?: IncidentType, severity?: IncidentSeverity
  ): Observable<IncidentPage> {
    if (environment.useMockApi) {
      const f = this.mockStore
        .filter(i => !status   || i.status   === status)
        .filter(i => !type     || i.type     === type)
        .filter(i => !severity || i.severity === severity);
      return of({
        content: f, totalElements: f.length, totalPages: 1, number: 0, size: f.length
      }).pipe(delay(120));
    }
    let params = new HttpParams().set('page', page).set('size', size);
    if (status)   params = params.set('status',   status);
    if (type)     params = params.set('type',     type);
    if (severity) params = params.set('severity', severity);
    return this.http.get<IncidentPage>(this.endpoint, { params });
  }

  get(id: string): Observable<IncidentView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(x => x.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<IncidentView>(`${this.endpoint}/${id}`);
  }

  report(input: ReportRequest): Observable<IncidentView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const i: IncidentView = {
        id: 'ehs-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        code: input.code, title: input.title, description: input.description,
        type: input.type, severity: input.severity ?? 'MEDIUM',
        status: 'REPORTED',
        occurredAt: input.occurredAt, reportedAt: now,
        location: input.location,
        reportedBy: input.reportedBy,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(i);
      return of(i).pipe(delay(150));
    }
    return this.http.post<IncidentView>(this.endpoint, input);
  }

  edit(id: string, input: EditRequest): Observable<IncidentView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (i) { Object.assign(i, input); i.updatedAt = new Date().toISOString(); return of(i).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.patch<IncidentView>(`${this.endpoint}/${id}`, input);
  }

  investigate(id: string, body: InvestigateRequest): Observable<IncidentView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (i) {
        i.status = 'INVESTIGATING';
        if (body.ownerUserId) i.ownerUserId = body.ownerUserId;
        i.updatedAt = new Date().toISOString();
        return of(i).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<IncidentView>(`${this.endpoint}/${id}/investigate`, body);
  }

  mitigate(id: string, body: MitigateRequest): Observable<IncidentView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (i) {
        i.status = 'MITIGATED'; i.mitigatedAt = now;
        i.rootCause = body.rootCause; i.correctiveActions = body.correctiveActions;
        i.updatedAt = now;
        return of(i).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<IncidentView>(`${this.endpoint}/${id}/mitigate`, body);
  }

  close(id: string): Observable<IncidentView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (i) { i.status = 'CLOSED'; i.closedAt = now; i.updatedAt = now; return of(i).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<IncidentView>(`${this.endpoint}/${id}/close`, {});
  }

  cancel(id: string): Observable<IncidentView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (i) { i.status = 'CANCELLED'; i.updatedAt = new Date().toISOString(); return of(i).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<IncidentView>(`${this.endpoint}/${id}/cancel`, {});
  }

  linkCapa(id: string, body: LinkCapaRequest): Observable<IncidentView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (i) { i.capaCaseId = body.capaCaseId; i.updatedAt = new Date().toISOString(); return of(i).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<IncidentView>(`${this.endpoint}/${id}/link-capa`, body);
  }

  linkNc(id: string, body: LinkNcRequest): Observable<IncidentView> {
    if (environment.useMockApi) {
      const i = this.mockStore.find(x => x.id === id);
      if (i) { i.ncId = body.ncId; i.updatedAt = new Date().toISOString(); return of(i).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<IncidentView>(`${this.endpoint}/${id}/link-nc`, body);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(x => x.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  statistics(): Observable<Statistics> {
    if (environment.useMockApi) {
      const count = (pred: (i: IncidentView) => boolean) => this.mockStore.filter(pred).length;
      return of({
        tenantId: 'demo-tenant',
        reported:      count(i => i.status === 'REPORTED'),
        investigating: count(i => i.status === 'INVESTIGATING'),
        mitigated:     count(i => i.status === 'MITIGATED'),
        closed:        count(i => i.status === 'CLOSED'),
        cancelled:     count(i => i.status === 'CANCELLED'),
        injuries:       count(i => i.type === 'INJURY'),
        nearMisses:     count(i => i.type === 'NEAR_MISS'),
        environmental:  count(i => i.type === 'ENVIRONMENTAL'),
        security:       count(i => i.type === 'SECURITY'),
        propertyDamage: count(i => i.type === 'PROPERTY_DAMAGE'),
        other:          count(i => i.type === 'OTHER')
      }).pipe(delay(100));
    }
    return this.http.get<Statistics>(`${this.endpoint}/statistics`);
  }

  // ---- Mock seed ----

  private seedIncidents(): IncidentView[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'ehs-1', tenantId: 'demo-tenant',
        code: 'EHS-2026-014', title: 'Chute de plain-pied atelier B — gauche zone 3',
        description: 'Glissade sur sol mouillé suite à fuite légère pompe de refroidissement.',
        type: 'INJURY', severity: 'MEDIUM', status: 'INVESTIGATING',
        occurredAt: '2026-05-18T09:14:00Z', reportedAt: '2026-05-18T09:42:00Z',
        location: 'Atelier B, zone 3, allée nord',
        personsInvolved: '1 opérateur (entorse cheville, soins infirmerie)',
        ownerUserId: 'demo-user', reportedBy: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'ehs-2', tenantId: 'demo-tenant',
        code: 'EHS-2026-015', title: 'Presque-accident — palette mal arrimée',
        type: 'NEAR_MISS', severity: 'HIGH', status: 'MITIGATED',
        occurredAt: '2026-05-15T15:22:00Z', reportedAt: '2026-05-15T16:00:00Z',
        location: 'Quai expédition 2',
        rootCause: 'Procédure d\'arrimage incomplète + manque de formation cariste recent.',
        correctiveActions: 'Mise à jour procédure PROC-EXP-014 + formation 4 caristes.',
        ownerUserId: 'demo-user', reportedBy: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'ehs-3', tenantId: 'demo-tenant',
        code: 'EHS-2026-016', title: 'Déversement solvant 5 L — bac de rétention OK',
        type: 'ENVIRONMENTAL', severity: 'LOW', status: 'CLOSED',
        occurredAt: '2026-05-10T11:08:00Z', reportedAt: '2026-05-10T11:35:00Z',
        closedAt: '2026-05-12T10:00:00Z',
        location: 'Local stockage produits chimiques',
        rootCause: 'Bidon mal refermé.',
        correctiveActions: 'Rappel consigne + check-list ouverture/fermeture bidons.',
        ownerUserId: 'demo-user', reportedBy: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }
}
