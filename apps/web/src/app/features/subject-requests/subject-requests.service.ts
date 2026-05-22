import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CompleteSubjectRequest,
  ExtendDeadlineRequest,
  ReceiveSubjectRequest,
  RejectSubjectRequest,
  StartProcessingRequest,
  SubjectRequestStatus,
  SubjectRequestType,
  SubjectRequestView
} from './subject-requests.types';

/**
 * Single HTTP boundary for the GDPR Subject Requests (DSAR) feature.
 * Mirrors the privacy invariant from the Consents module:
 *  - the backend hashes the subjectIdentifier (Art. 15-22 RGPD)
 *  - the API never returns the plaintext identifier
 *  - the mock branch hashes locally too (FNV-1a 32-bit) so the demo
 *    in-memory store only ever holds hashes, never PII in clear.
 */
@Injectable({ providedIn: 'root' })
export class SubjectRequestsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/subject-requests`;
  private readonly mockStore: SubjectRequestView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: SubjectRequestStatus): Observable<SubjectRequestView[]> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      const f = status ? this.mockStore.filter(r => r.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<SubjectRequestView[]>(this.endpoint, { params });
  }

  searchBySubject(subjectIdentifier: string): Observable<SubjectRequestView[]> {
    if (environment.useMockApi) {
      const hash = this.hashFnv1a(subjectIdentifier);
      return of(this.mockStore.filter(r => r.subjectIdentifierHash === hash)).pipe(delay(120));
    }
    const params = new HttpParams().set('subjectIdentifier', subjectIdentifier);
    return this.http.get<SubjectRequestView[]>(`${this.endpoint}/search`, { params });
  }

  overdue(limit = 100): Observable<SubjectRequestView[]> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      const arr = this.mockStore.filter(r => r.overdue
        && r.status !== 'COMPLETED' && r.status !== 'REJECTED').slice(0, limit);
      return of(arr).pipe(delay(120));
    }
    const params = new HttpParams().set('limit', limit);
    return this.http.get<SubjectRequestView[]>(`${this.endpoint}/overdue`, { params });
  }

  get(id: string): Observable<SubjectRequestView> {
    if (environment.useMockApi) {
      this.recomputeOverdue();
      return of(this.mockStore.find(r => r.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<SubjectRequestView>(`${this.endpoint}/${id}`);
  }

  receive(input: ReceiveSubjectRequest): Observable<SubjectRequestView> {
    if (environment.useMockApi) {
      const now = new Date();
      const deadline = new Date(now.getTime() + 30 * 86400000); // RGPD Art. 12§3 — 1 mois
      const r: SubjectRequestView = {
        id: 'sr-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        type: input.type,
        subjectIdentifierHash: this.hashFnv1a(input.subjectIdentifier),
        subjectIdentifierLabel: input.subjectIdentifierLabel,
        status: 'RECEIVED',
        receivedAt: now.toISOString(),
        deadlineAt: deadline.toISOString(),
        extended: false,
        requestedByUserId: input.requestedByUserId,
        updatedAt: now.toISOString(),
        overdue: false
      };
      this.mockStore.unshift(r);
      return of(r).pipe(delay(150));
    }
    return this.http.post<SubjectRequestView>(this.endpoint, input);
  }

  start(id: string, input: StartProcessingRequest): Observable<SubjectRequestView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (r) {
        r.status = 'IN_PROGRESS';
        r.inProgressAt = now;
        r.handledByUserId = input.handledByUserId;
        r.updatedAt = now;
        return of(r).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<SubjectRequestView>(`${this.endpoint}/${id}/start`, input);
  }

  complete(id: string, input: CompleteSubjectRequest): Observable<SubjectRequestView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (r) {
        r.status = 'COMPLETED';
        r.completedAt = now;
        r.resolutionNotes = input.resolutionNotes;
        r.evidenceUrl = input.evidenceUrl;
        if (input.handledByUserId) r.handledByUserId = input.handledByUserId;
        r.updatedAt = now;
        return of(r).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<SubjectRequestView>(`${this.endpoint}/${id}/complete`, input);
  }

  reject(id: string, input: RejectSubjectRequest): Observable<SubjectRequestView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (r) {
        r.status = 'REJECTED';
        r.completedAt = now;
        r.rejectionReason = input.reason;
        if (input.handledByUserId) r.handledByUserId = input.handledByUserId;
        r.updatedAt = now;
        return of(r).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<SubjectRequestView>(`${this.endpoint}/${id}/reject`, input);
  }

  extend(id: string, input: ExtendDeadlineRequest): Observable<SubjectRequestView> {
    if (environment.useMockApi) {
      const r = this.mockStore.find(x => x.id === id);
      if (r) {
        r.deadlineAt = input.newDeadline;
        r.extended = true;
        r.updatedAt = new Date().toISOString();
        return of(r).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<SubjectRequestView>(`${this.endpoint}/${id}/extend`, input);
  }

  // ---------- Internals ----------

  private recomputeOverdue(): void {
    const now = Date.now();
    for (const r of this.mockStore) {
      const live = r.status === 'RECEIVED' || r.status === 'IN_PROGRESS';
      r.overdue = live && new Date(r.deadlineAt).getTime() < now;
    }
  }

  private hashFnv1a(s: string): string {
    let h = 0x811c9dc5;
    for (let i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 0x01000193);
    }
    return 'fnv1a:' + (h >>> 0).toString(16).padStart(8, '0');
  }

  private seed(): SubjectRequestView[] {
    const now = Date.now();
    const past = (d: number) => new Date(now - d * 86400000).toISOString();
    const fut  = (d: number) => new Date(now + d * 86400000).toISOString();
    const hashFor = (s: string) => this.hashFnv1a(s);
    return [
      {
        id: 'sr-1', tenantId: 'demo-tenant',
        type: 'ACCESS',
        subjectIdentifierHash: hashFor('alice@example.fr'),
        subjectIdentifierLabel: 'client#A-001',
        status: 'IN_PROGRESS',
        receivedAt: past(8), deadlineAt: fut(22), extended: false,
        inProgressAt: past(6),
        requestedByUserId: 'demo-user', handledByUserId: 'demo-user',
        updatedAt: past(2), overdue: false
      },
      {
        id: 'sr-2', tenantId: 'demo-tenant',
        type: 'ERASURE',
        subjectIdentifierHash: hashFor('bob@example.fr'),
        subjectIdentifierLabel: 'visiteur#B-204',
        status: 'COMPLETED',
        receivedAt: past(45), deadlineAt: past(15), extended: false,
        inProgressAt: past(43), completedAt: past(20),
        resolutionNotes: 'Suppression effectuée sur CRM, sauvegardes purgées (TTL), confirmation envoyée par e-mail.',
        evidenceUrl: 'https://www.qualitos.io/audit/sr-sr-2.pdf',
        requestedByUserId: 'demo-user', handledByUserId: 'demo-user',
        updatedAt: past(20), overdue: false
      },
      {
        id: 'sr-3', tenantId: 'demo-tenant',
        type: 'PORTABILITY',
        subjectIdentifierHash: hashFor('charlie@example.fr'),
        subjectIdentifierLabel: 'client#C-118',
        status: 'RECEIVED',
        receivedAt: past(2), deadlineAt: fut(28), extended: false,
        requestedByUserId: 'demo-user',
        updatedAt: past(2), overdue: false
      },
      {
        id: 'sr-4', tenantId: 'demo-tenant',
        type: 'OBJECTION',
        subjectIdentifierHash: hashFor('dora@example.fr'),
        subjectIdentifierLabel: 'prospect#D-77',
        status: 'IN_PROGRESS',
        receivedAt: past(35), deadlineAt: past(5), extended: true,
        inProgressAt: past(30),
        requestedByUserId: 'demo-user', handledByUserId: 'demo-user',
        updatedAt: past(5), overdue: true
      }
    ];
  }
}
