import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  ConsentView,
  GrantConsentRequest,
  WithdrawConsentRequest
} from './consents.types';

/**
 * Single HTTP boundary for the Consents feature.
 *
 * OWASP A02 — sensitive identifier (subjectIdentifier in plaintext) is
 * only forwarded to the backend in the grant payload, never persisted
 * locally and never logged. The mock branch immediately hashes via a
 * lightweight FNV-1a so the in-memory store mirrors the production
 * privacy invariant (only hashes live in client state).
 */
@Injectable({ providedIn: 'root' })
export class ConsentsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/consents`;

  private readonly mockStore: ConsentView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  get(id: string): Observable<ConsentView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(c => c.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<ConsentView>(`${this.endpoint}/${id}`);
  }

  searchBySubject(subjectIdentifier: string): Observable<ConsentView[]> {
    if (environment.useMockApi) {
      const hash = this.hashFnv1a(subjectIdentifier);
      const arr = this.mockStore.filter(c => c.subjectIdentifierHash === hash);
      return of(arr).pipe(delay(120));
    }
    const params = new HttpParams().set('subjectIdentifier', subjectIdentifier);
    return this.http.get<ConsentView[]>(`${this.endpoint}/search`, { params });
  }

  searchByPurpose(purposeCode: string): Observable<ConsentView[]> {
    if (environment.useMockApi) {
      const arr = this.mockStore.filter(c => c.purposeCode === purposeCode);
      return of(arr).pipe(delay(120));
    }
    const params = new HttpParams().set('purposeCode', purposeCode);
    return this.http.get<ConsentView[]>(`${this.endpoint}/by-purpose`, { params });
  }

  active(subjectIdentifier: string, purposeCode: string): Observable<ConsentView | null> {
    if (environment.useMockApi) {
      const hash = this.hashFnv1a(subjectIdentifier);
      const found = this.mockStore.find(c =>
        c.subjectIdentifierHash === hash && c.purposeCode === purposeCode && c.active
      );
      return of(found ?? null).pipe(delay(100));
    }
    const params = new HttpParams()
      .set('subjectIdentifier', subjectIdentifier)
      .set('purposeCode', purposeCode);
    return this.http.get<ConsentView | null>(`${this.endpoint}/active`, { params });
  }

  grant(input: GrantConsentRequest): Observable<ConsentView> {
    if (environment.useMockApi) {
      const now = new Date();
      const hash = this.hashFnv1a(input.subjectIdentifier);
      const c: ConsentView = {
        id: 'cons-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        subjectIdentifierHash: hash,
        subjectIdentifierLabel: input.subjectIdentifierLabel,
        purposeCode: input.purposeCode,
        purposeVersion: input.purposeVersion,
        source: input.source,
        evidenceUrl: input.evidenceUrl,
        ipAddress: input.ipAddress,
        userAgent: input.userAgent,
        grantedByUserId: input.grantedByUserId,
        grantedAt: now.toISOString(),
        expiresAt: input.expiresAt,
        status: 'GRANTED',
        updatedAt: now.toISOString(),
        active: !input.expiresAt || new Date(input.expiresAt) > now
      };
      this.mockStore.unshift(c);
      return of(c).pipe(delay(150));
    }
    return this.http.post<ConsentView>(this.endpoint, input);
  }

  withdraw(id: string, input: WithdrawConsentRequest): Observable<ConsentView> {
    if (environment.useMockApi) {
      const c = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (c) {
        c.status = 'WITHDRAWN';
        c.withdrawnAt = now;
        c.withdrawnByUserId = input.actorUserId;
        c.withdrawalReason = input.reason;
        c.active = false;
        c.updatedAt = now;
        return of(c).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<ConsentView>(`${this.endpoint}/${id}/withdraw`, input);
  }

  expireDue(limit = 200): Observable<{ expired: number }> {
    if (environment.useMockApi) {
      const now = Date.now();
      let count = 0;
      for (const c of this.mockStore) {
        if (c.status === 'GRANTED' && c.expiresAt && new Date(c.expiresAt).getTime() <= now) {
          c.status = 'EXPIRED';
          c.active = false;
          c.updatedAt = new Date().toISOString();
          count++;
          if (count >= limit) break;
        }
      }
      return of({ expired: count }).pipe(delay(200));
    }
    const params = new HttpParams().set('limit', limit);
    return this.http.post<{ expired: number }>(`${this.endpoint}/expire-due`, null, { params });
  }

  // ---------- Mock helpers ----------

  /**
   * Lightweight FNV-1a 32-bit hash — mirrors the privacy invariant that
   * the client never holds the plaintext identifier. NOT a cryptographic
   * hash; only used in the mock branch to make the demo realistic.
   * The real backend uses SubjectIdentifierHasher with a salted SHA-256.
   */
  private hashFnv1a(s: string): string {
    let h = 0x811c9dc5;
    for (let i = 0; i < s.length; i++) {
      h ^= s.charCodeAt(i);
      h = Math.imul(h, 0x01000193);
    }
    return 'fnv1a:' + (h >>> 0).toString(16).padStart(8, '0');
  }

  private seed(): ConsentView[] {
    const now = new Date();
    const days = (n: number) => new Date(now.getTime() + n * 86400000).toISOString();
    const past = (n: number) => new Date(now.getTime() - n * 86400000).toISOString();
    const hashFor = (s: string) => this.hashFnv1a(s);
    return [
      {
        id: 'cons-1', tenantId: 'demo-tenant',
        subjectIdentifierHash: hashFor('alice@example.fr'),
        subjectIdentifierLabel: 'client#A-001',
        purposeCode: 'newsletter.marketing',
        purposeVersion: '2026.1',
        source: 'WEB_FORM',
        evidenceUrl: 'https://www.qualitos.io/audit/consent-cons-1.pdf',
        ipAddress: '203.0.113.42', userAgent: 'Mozilla/5.0',
        grantedByUserId: 'demo-user',
        grantedAt: past(30), expiresAt: days(335),
        status: 'GRANTED',
        updatedAt: past(30), active: true
      },
      {
        id: 'cons-2', tenantId: 'demo-tenant',
        subjectIdentifierHash: hashFor('alice@example.fr'),
        subjectIdentifierLabel: 'client#A-001',
        purposeCode: 'analytics.optional',
        purposeVersion: '2025.4',
        source: 'WEB_FORM',
        grantedByUserId: 'demo-user',
        grantedAt: past(120),
        status: 'WITHDRAWN',
        withdrawnAt: past(15),
        withdrawnByUserId: 'demo-user',
        withdrawalReason: 'Demande explicite via formulaire opt-out',
        updatedAt: past(15), active: false
      },
      {
        id: 'cons-3', tenantId: 'demo-tenant',
        subjectIdentifierHash: hashFor('bob@example.fr'),
        subjectIdentifierLabel: 'patient#B-014',
        purposeCode: 'telemedicine.session',
        purposeVersion: '2026.1',
        source: 'MOBILE_APP',
        evidenceUrl: 'https://www.qualitos.io/audit/consent-cons-3.pdf',
        grantedByUserId: 'demo-user',
        grantedAt: past(2), expiresAt: days(363),
        status: 'GRANTED',
        updatedAt: past(2), active: true
      }
    ];
  }
}
