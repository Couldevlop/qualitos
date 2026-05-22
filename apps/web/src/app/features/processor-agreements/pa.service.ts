import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreatePaRequest,
  EditPaRequest,
  PaStatus,
  PaView,
  TerminatePaRequest
} from './pa.types';

@Injectable({ providedIn: 'root' })
export class PaService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/processor-agreements`;
  private readonly mockStore: PaView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: PaStatus): Observable<PaView[]> {
    if (environment.useMockApi) {
      this.recomputeExpired();
      const f = status ? this.mockStore.filter(p => p.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<PaView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<PaView> {
    if (environment.useMockApi) {
      this.recomputeExpired();
      return of(this.mockStore.find(p => p.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<PaView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<PaView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.reference === reference);
      return of(p ?? this.mockStore[0]).pipe(delay(100));
    }
    const params = new HttpParams().set('reference', reference);
    return this.http.get<PaView>(`${this.endpoint}/by-reference`, { params });
  }

  create(input: CreatePaRequest): Observable<PaView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const p: PaView = {
        id: 'pa-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference,
        processorName: input.processorName,
        processorLegalEntity: input.processorLegalEntity,
        processorContact: input.processorContact,
        processorDpoContact: input.processorDpoContact,
        processorCountry: input.processorCountry,
        servicesDescription: input.servicesDescription,
        subProcessorCategories:      input.subProcessorCategories      ?? [],
        linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? [],
        thirdCountryTransfers:       input.thirdCountryTransfers       ?? [],
        transferSafeguards: input.transferSafeguards,
        contractDocumentUrl: input.contractDocumentUrl,
        signedAt: input.signedAt,
        effectiveFrom: input.effectiveFrom,
        expirationDate: input.expirationDate,
        securityMeasures: input.securityMeasures,
        breachNotificationCommitmentHours: input.breachNotificationCommitmentHours,
        auditRights: input.auditRights,
        auditRightsNotes: input.auditRightsNotes,
        dataReturnOrDeletionTerms: input.dataReturnOrDeletionTerms,
        status: 'DRAFT',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(p);
      return of(p).pipe(delay(150));
    }
    return this.http.post<PaView>(this.endpoint, input);
  }

  edit(id: string, input: EditPaRequest): Observable<PaView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      if (p) {
        Object.assign(p, input, {
          subProcessorCategories:      input.subProcessorCategories      ?? [],
          linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? [],
          thirdCountryTransfers:       input.thirdCountryTransfers       ?? []
        });
        p.updatedAt = new Date().toISOString();
        return of(p).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<PaView>(`${this.endpoint}/${id}`, input);
  }

  activate(id: string): Observable<PaView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (p) {
        p.status = 'ACTIVE';
        if (!p.effectiveFrom) p.effectiveFrom = now;
        p.updatedAt = now;
        return of(p).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<PaView>(`${this.endpoint}/${id}/activate`, {});
  }

  terminate(id: string, body: TerminatePaRequest): Observable<PaView> {
    if (environment.useMockApi) {
      const p = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (p) {
        p.status = 'TERMINATED';
        p.terminationReason = body.reason;
        p.updatedAt = now;
        return of(p).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<PaView>(`${this.endpoint}/${id}/terminate`, body);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(p => p.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  expireDue(limit = 200): Observable<{ expired: number }> {
    if (environment.useMockApi) {
      const now = Date.now();
      let count = 0;
      for (const p of this.mockStore) {
        if (p.status === 'ACTIVE' && p.expirationDate
            && new Date(p.expirationDate).getTime() <= now) {
          p.status = 'EXPIRED';
          p.updatedAt = new Date().toISOString();
          count++;
          if (count >= limit) break;
        }
      }
      return of({ expired: count }).pipe(delay(200));
    }
    const params = new HttpParams().set('limit', limit);
    return this.http.post<{ expired: number }>(`${this.endpoint}/expire-due`, null, { params });
  }

  // ---- Internals ----

  private recomputeExpired(): void {
    const now = Date.now();
    for (const p of this.mockStore) {
      if (p.status === 'ACTIVE' && p.expirationDate
          && new Date(p.expirationDate).getTime() <= now) {
        p.status = 'EXPIRED';
      }
    }
  }

  private seed(): PaView[] {
    const now = new Date();
    const fut = (d: number) => new Date(now.getTime() + d * 86400000).toISOString();
    const past = (d: number) => new Date(now.getTime() - d * 86400000).toISOString();
    return [
      {
        id: 'pa-1', tenantId: 'demo-tenant',
        reference: 'DPA-AWS-EU',
        processorName: 'Amazon Web Services EMEA SARL',
        processorLegalEntity: 'AWS EMEA SARL (Luxembourg)',
        processorContact: 'aws-customer-support@amazon.com',
        processorDpoContact: 'eu-privacy@amazon.com',
        processorCountry: 'LU',
        servicesDescription: 'Hébergement cloud (S3, EC2, RDS) pour services de production QualitOS — région eu-west-3 (Paris) avec failover eu-west-1 (Dublin).',
        subProcessorCategories: ['Sous-traitants infrastructure AWS', 'Connectivité tier-1'],
        linkedProcessingActivityIds: [],
        thirdCountryTransfers: [],
        contractDocumentUrl: 'https://www.qualitos.io/audit/dpa-aws-2026.pdf',
        signedAt: past(120), effectiveFrom: past(120), expirationDate: fut(605),
        securityMeasures: 'Chiffrement AES-256-GCM at-rest, TLS 1.3 in-transit, MFA console, audit logs CloudTrail, certifications ISO 27001/27017/27018, HDS.',
        breachNotificationCommitmentHours: 24,
        auditRights: true,
        auditRightsNotes: 'Audit annuel + accès rapports SOC 2 Type II. Audit ad-hoc sous préavis 30 jours en cas d\'incident.',
        dataReturnOrDeletionTerms: 'Restitution en format standard (S3 export) sous 30 jours puis suppression complète dans les 90 jours.',
        status: 'ACTIVE',
        createdByUserId: 'demo-user',
        createdAt: past(125), updatedAt: past(120)
      },
      {
        id: 'pa-2', tenantId: 'demo-tenant',
        reference: 'DPA-PAYROLL-FR',
        processorName: 'PaiePartner SAS',
        processorLegalEntity: 'PaiePartner SAS (RCS Paris)',
        processorContact: 'support@paiepartner.fr',
        processorCountry: 'FR',
        servicesDescription: 'Calcul de paie + déclarations sociales pour 500 salariés.',
        subProcessorCategories: ['DSN', 'URSSAF connect'],
        linkedProcessingActivityIds: [],
        thirdCountryTransfers: [],
        signedAt: now.toISOString(), effectiveFrom: now.toISOString(),
        expirationDate: fut(730),
        securityMeasures: 'Chiffrement, MFA, sauvegardes quotidiennes, hébergement France.',
        breachNotificationCommitmentHours: 48,
        auditRights: true,
        dataReturnOrDeletionTerms: 'Restitution CSV + suppression sécurisée 60 jours.',
        status: 'DRAFT',
        createdByUserId: 'demo-user',
        createdAt: now.toISOString(), updatedAt: now.toISOString()
      },
      {
        id: 'pa-3', tenantId: 'demo-tenant',
        reference: 'DPA-EMAIL-MARKETING',
        processorName: 'MailWave Inc.',
        processorLegalEntity: 'MailWave Inc. (Delaware, USA)',
        processorCountry: 'US',
        servicesDescription: 'Envoi de newsletters (anciennement utilisé).',
        subProcessorCategories: [],
        linkedProcessingActivityIds: [],
        thirdCountryTransfers: ['USA'],
        transferSafeguards: 'SCC 2021 + mesures supplémentaires.',
        signedAt: past(800), effectiveFrom: past(800), expirationDate: past(60),
        breachNotificationCommitmentHours: 72,
        auditRights: false,
        status: 'EXPIRED',
        createdByUserId: 'demo-user',
        createdAt: past(810), updatedAt: past(60)
      }
    ];
  }
}
