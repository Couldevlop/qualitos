import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  CreateTransferRequest,
  EditTransferRequest,
  SuspendTransferRequest,
  TerminateTransferRequest,
  TransferMechanism,
  TransferStatus,
  TransferView
} from './transfers.types';

/** Art. 49 exige une justification — miroir du domaine backend. */
export function requiresDerogationJustification(m: TransferMechanism): boolean {
  return m === 'DEROGATION_ART49';
}

@Injectable({ providedIn: 'root' })
export class TransfersService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/cross-border-transfers`;
  private readonly mockStore: TransferView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: TransferStatus): Observable<TransferView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(t => t.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<TransferView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<TransferView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(t => t.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<TransferView>(`${this.endpoint}/${id}`);
  }

  create(input: CreateTransferRequest): Observable<TransferView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const t: TransferView = {
        id: 'cbt-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference,
        recipientName: input.recipientName,
        recipientLegalEntity: input.recipientLegalEntity,
        recipientContact: input.recipientContact,
        destinationCountries: input.destinationCountries ?? [],
        mechanism: input.mechanism,
        safeguardsDescription: input.safeguardsDescription,
        safeguardsDocumentUrl: input.safeguardsDocumentUrl,
        derogationJustification: input.derogationJustification,
        dataCategories: input.dataCategories ?? [],
        linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? [],
        linkedProcessorAgreementIds: input.linkedProcessorAgreementIds ?? [],
        status: 'DRAFT',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(t);
      return of(t).pipe(delay(150));
    }
    return this.http.post<TransferView>(this.endpoint, input);
  }

  edit(id: string, input: EditTransferRequest): Observable<TransferView> {
    if (environment.useMockApi) {
      const t = this.mockStore.find(x => x.id === id);
      if (t) {
        Object.assign(t, input, {
          destinationCountries:        input.destinationCountries        ?? [],
          dataCategories:              input.dataCategories              ?? [],
          linkedProcessingActivityIds: input.linkedProcessingActivityIds ?? [],
          linkedProcessorAgreementIds: input.linkedProcessorAgreementIds ?? []
        });
        t.updatedAt = new Date().toISOString();
        return of(t).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<TransferView>(`${this.endpoint}/${id}`, input);
  }

  activate(id: string): Observable<TransferView> { return this.transition(id, 'activate', 'ACTIVE'); }

  suspend(id: string, body: SuspendTransferRequest): Observable<TransferView> {
    if (environment.useMockApi) {
      const t = this.mockStore.find(x => x.id === id);
      if (t) { t.status = 'SUSPENDED'; t.suspendReason = body.reason; t.updatedAt = new Date().toISOString(); return of(t).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<TransferView>(`${this.endpoint}/${id}/suspend`, body);
  }

  terminate(id: string, body: TerminateTransferRequest): Observable<TransferView> {
    if (environment.useMockApi) {
      const t = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (t) { t.status = 'TERMINATED'; t.terminationReason = body.reason; t.effectiveTo = now; t.updatedAt = now; return of(t).pipe(delay(120)); }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<TransferView>(`${this.endpoint}/${id}/terminate`, body);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(t => t.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private transition(id: string, op: string, target: TransferStatus): Observable<TransferView> {
    if (environment.useMockApi) {
      const t = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (t) {
        t.status = target;
        if (target === 'ACTIVE' && !t.effectiveFrom) t.effectiveFrom = now;
        t.updatedAt = now;
        return of(t).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<TransferView>(`${this.endpoint}/${id}/${op}`, {});
  }

  private seed(): TransferView[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'cbt-1', tenantId: 'demo-tenant',
        reference: 'CRM-SALESFORCE-US',
        recipientName: 'Salesforce.com Inc.',
        recipientLegalEntity: 'Salesforce.com Inc. (Delaware, USA)',
        recipientContact: 'privacy@salesforce.com',
        destinationCountries: ['USA'],
        mechanism: 'STANDARD_CONTRACTUAL_CLAUSES',
        safeguardsDescription: 'SCC 2021 modules 2 (C2P) + 3 (P2P). Mesures supplémentaires : chiffrement E2E des données sensibles, contrôle d\'accès renforcé, audit annuel.',
        safeguardsDocumentUrl: 'https://www.qualitos.io/audit/scc-salesforce-2026.pdf',
        dataCategories: ['Identité prospects', 'Coordonnées professionnelles', 'Historique interactions'],
        linkedProcessingActivityIds: [],
        linkedProcessorAgreementIds: [],
        status: 'ACTIVE', effectiveFrom: now,
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'cbt-2', tenantId: 'demo-tenant',
        reference: 'BACKUP-UK-OFFSITE',
        recipientName: 'CloudSafe UK Ltd',
        recipientLegalEntity: 'CloudSafe UK Ltd (Londres, UK)',
        destinationCountries: ['Royaume-Uni'],
        mechanism: 'ADEQUACY_DECISION',
        safeguardsDescription: 'Décision d\'adéquation UE-UK du 28 juin 2021 (renouvelée 2025).',
        dataCategories: ['Sauvegardes chiffrées'],
        linkedProcessingActivityIds: [],
        linkedProcessorAgreementIds: [],
        status: 'ACTIVE', effectiveFrom: now,
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'cbt-3', tenantId: 'demo-tenant',
        reference: 'URGENT-MEDIC-EVAC-INDIA',
        recipientName: 'Apollo Hospital Chennai',
        destinationCountries: ['Inde'],
        mechanism: 'DEROGATION_ART49',
        derogationJustification: 'Évacuation médicale d\'urgence (Art. 49.1.f — intérêt vital de la personne concernée). Transfert ponctuel d\'un dossier médical pour prise en charge immédiate.',
        dataCategories: ['Données de santé'],
        linkedProcessingActivityIds: [],
        linkedProcessorAgreementIds: [],
        status: 'DRAFT',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }
}
