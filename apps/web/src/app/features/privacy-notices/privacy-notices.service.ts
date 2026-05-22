import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreatePrivacyNoticeRequest,
  EditPrivacyNoticeRequest,
  PrivacyNoticeStatus,
  PrivacyNoticeView,
  PublishPrivacyNoticeRequest
} from './privacy-notices.types';

@Injectable({ providedIn: 'root' })
export class PrivacyNoticesService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/privacy-notices`;
  private readonly mockStore: PrivacyNoticeView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: PrivacyNoticeStatus): Observable<PrivacyNoticeView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(n => n.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<PrivacyNoticeView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<PrivacyNoticeView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(n => n.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<PrivacyNoticeView>(`${this.endpoint}/${id}`);
  }

  versions(reference: string): Observable<PrivacyNoticeView[]> {
    if (environment.useMockApi) {
      return of(this.mockStore.filter(n => n.reference === reference)).pipe(delay(120));
    }
    const params = new HttpParams().set('reference', reference);
    return this.http.get<PrivacyNoticeView[]>(`${this.endpoint}/versions`, { params });
  }

  findPublished(reference: string, language: string): Observable<PrivacyNoticeView | null> {
    if (environment.useMockApi) {
      const found = this.mockStore.find(n =>
        n.reference === reference && n.language === language && n.status === 'PUBLISHED');
      return of(found ?? null).pipe(delay(100));
    }
    const params = new HttpParams().set('reference', reference).set('language', language);
    return this.http.get<PrivacyNoticeView | null>(`${this.endpoint}/published`, { params });
  }

  create(input: CreatePrivacyNoticeRequest): Observable<PrivacyNoticeView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const n: PrivacyNoticeView = {
        id: 'pn-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference, version: input.version, language: input.language,
        title: input.title, summary: input.summary, contentMarkdown: input.contentMarkdown,
        linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? [],
        publishUrl: input.publishUrl,
        contactName: input.contactName, contactEmail: input.contactEmail,
        status: 'DRAFT',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(n);
      return of(n).pipe(delay(150));
    }
    return this.http.post<PrivacyNoticeView>(this.endpoint, input);
  }

  edit(id: string, input: EditPrivacyNoticeRequest): Observable<PrivacyNoticeView> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      if (n) {
        Object.assign(n, input, {
          linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? []
        });
        n.updatedAt = new Date().toISOString();
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<PrivacyNoticeView>(`${this.endpoint}/${id}`, input);
  }

  publish(id: string, body: PublishPrivacyNoticeRequest): Observable<PrivacyNoticeView> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (n) {
        // OWASP A04 — mirror backend invariant: archive any previous PUBLISHED
        // version with the same reference+language to enforce "one published
        // notice per locale" at a time.
        for (const other of this.mockStore) {
          if (other.id !== n.id
              && other.reference === n.reference
              && other.language === n.language
              && other.status === 'PUBLISHED') {
            other.status = 'ARCHIVED';
            other.effectiveTo = now;
            other.updatedAt = now;
          }
        }
        n.status = 'PUBLISHED';
        n.publishedAt = now;
        n.publishedByUserId = body.publishedByUserId;
        n.effectiveFrom = now;
        n.updatedAt = now;
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<PrivacyNoticeView>(`${this.endpoint}/${id}/publish`, body);
  }

  archive(id: string): Observable<PrivacyNoticeView> {
    if (environment.useMockApi) {
      const n = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (n) {
        n.status = 'ARCHIVED';
        n.effectiveTo = now;
        n.updatedAt = now;
        return of(n).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<PrivacyNoticeView>(`${this.endpoint}/${id}/archive`, {});
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(n => n.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  // ---- Mock seed ----

  private seed(): PrivacyNoticeView[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'pn-1', tenantId: 'demo-tenant',
        reference: 'PUBLIC_WEB_NOTICE', version: '2026.1', language: 'fr',
        title: 'Politique de confidentialité QualitOS — Site public',
        summary: 'Mentions Art. 13 RGPD applicables aux visiteurs du site qualitos.io.',
        contentMarkdown: '# Politique de confidentialité\n\n## Identité du responsable\n\nQualitOS SAS, contact : dpo@qualitos.io.\n\n## Finalités\n\n- Mesure d\'audience anonymisée…\n\n## Bases légales\n\n- Intérêt légitime (Art. 6.1.f) pour la mesure d\'audience.\n- Consentement (Art. 6.1.a) pour les cookies non strictement nécessaires.',
        linkedProcessingActivityIds: [],
        publishUrl: 'https://www.qualitos.io/privacy',
        contactName: 'DPO QualitOS', contactEmail: 'dpo@qualitos.io',
        status: 'PUBLISHED',
        effectiveFrom: now, publishedAt: now, publishedByUserId: 'demo-user',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'pn-2', tenantId: 'demo-tenant',
        reference: 'PUBLIC_WEB_NOTICE', version: '2026.1', language: 'en',
        title: 'QualitOS Privacy Notice — Public website',
        summary: 'GDPR Art. 13 notice for visitors of qualitos.io.',
        contentMarkdown: '# Privacy notice\n\n## Controller\n\nQualitOS SAS, contact: dpo@qualitos.io.\n\n## Purposes\n\n- Anonymous audience measurement…',
        linkedProcessingActivityIds: [],
        publishUrl: 'https://www.qualitos.io/en/privacy',
        contactName: 'QualitOS DPO', contactEmail: 'dpo@qualitos.io',
        status: 'PUBLISHED',
        effectiveFrom: now, publishedAt: now, publishedByUserId: 'demo-user',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'pn-3', tenantId: 'demo-tenant',
        reference: 'HR_RECRUITMENT_NOTICE', version: '2026.1', language: 'fr',
        title: 'Mention RH — Recrutement (candidats)',
        summary: 'Mention Art. 13 destinée aux candidats déposant leur CV.',
        contentMarkdown: '# Recrutement\n\nLe traitement de votre candidature a pour finalité…',
        linkedProcessingActivityIds: [],
        contactName: 'DPO QualitOS', contactEmail: 'dpo@qualitos.io',
        status: 'DRAFT',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }
}
