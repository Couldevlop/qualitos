import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AiQmsStatus,
  AiQmsView,
  ApproveAiQmsRequest,
  ArchiveAiQmsRequest,
  DraftAiQmsRequest,
  EditAiQmsRequest,
  SupersedeAiQmsRequest
} from './ai-qms.types';

@Injectable({ providedIn: 'root' })
export class AiQmsService {

  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/qms`;
  private readonly mockStore: AiQmsView[] = this.seed();

  constructor(private readonly http: HttpClient) {}

  list(status?: AiQmsStatus): Observable<AiQmsView[]> {
    if (environment.useMockApi) {
      const f = status ? this.mockStore.filter(q => q.status === status) : this.mockStore;
      return of(f).pipe(delay(120));
    }
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<AiQmsView[]>(this.endpoint, { params });
  }

  get(id: string): Observable<AiQmsView> {
    if (environment.useMockApi) {
      return of(this.mockStore.find(q => q.id === id) ?? this.mockStore[0]).pipe(delay(100));
    }
    return this.http.get<AiQmsView>(`${this.endpoint}/${id}`);
  }

  getByReference(reference: string): Observable<AiQmsView> {
    if (environment.useMockApi) {
      const q = this.mockStore.find(x => x.reference === reference);
      return of(q ?? this.mockStore[0]).pipe(delay(100));
    }
    const params = new HttpParams().set('reference', reference);
    return this.http.get<AiQmsView>(`${this.endpoint}/by-reference`, { params });
  }

  draft(input: DraftAiQmsRequest): Observable<AiQmsView> {
    if (environment.useMockApi) {
      const now = new Date().toISOString();
      const q: AiQmsView = {
        id: 'qms-' + (this.mockStore.length + 1) + '-' + Math.random().toString(36).slice(2, 7),
        tenantId: 'demo-tenant',
        reference: input.reference, version: input.version, name: input.name,
        description: input.description,
        regulatoryComplianceStrategy:      input.regulatoryComplianceStrategy,
        designControlDescription:          input.designControlDescription,
        qualityControlDescription:         input.qualityControlDescription,
        dataManagementDescription:         input.dataManagementDescription,
        riskManagementDescription:         input.riskManagementDescription,
        pmmDescription:                    input.pmmDescription,
        regulatorCommunicationDescription: input.regulatorCommunicationDescription,
        resourceManagementDescription:     input.resourceManagementDescription,
        supplierMonitoringDescription:     input.supplierMonitoringDescription,
        coveredAiSystemIds: input.coveredAiSystemIds ?? [],
        status: 'DRAFT',
        createdByUserId: input.createdByUserId,
        createdAt: now, updatedAt: now
      };
      this.mockStore.unshift(q);
      return of(q).pipe(delay(150));
    }
    return this.http.post<AiQmsView>(this.endpoint, input);
  }

  edit(id: string, input: EditAiQmsRequest): Observable<AiQmsView> {
    if (environment.useMockApi) {
      const q = this.mockStore.find(x => x.id === id);
      if (q) {
        Object.assign(q, input, { coveredAiSystemIds: input.coveredAiSystemIds ?? [] });
        q.updatedAt = new Date().toISOString();
        return of(q).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.put<AiQmsView>(`${this.endpoint}/${id}`, input);
  }

  approve(id: string, body: ApproveAiQmsRequest): Observable<AiQmsView> {
    if (environment.useMockApi) {
      const q = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (q) {
        q.status = 'APPROVED';
        q.submittedByUserId = body.submittedByUserId;
        q.approvedByUserId = body.approvedByUserId;
        q.approvalNotes = body.approvalNotes;
        q.approvedAt = now;
        q.updatedAt = now;
        return of(q).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiQmsView>(`${this.endpoint}/${id}/approve`, body);
  }

  putInForce(id: string): Observable<AiQmsView> {
    if (environment.useMockApi) {
      const q = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (q) {
        // OWASP A04 — mirror backend : only one IN_FORCE per reference.
        for (const other of this.mockStore) {
          if (other.id !== q.id && other.reference === q.reference && other.status === 'IN_FORCE') {
            other.status = 'SUPERSEDED';
            other.supersededAt = now;
            other.supersededByQmsId = q.id;
            other.updatedAt = now;
          }
        }
        q.status = 'IN_FORCE';
        q.inForceFrom = now;
        q.updatedAt = now;
        return of(q).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiQmsView>(`${this.endpoint}/${id}/put-in-force`, {});
  }

  supersede(id: string, body: SupersedeAiQmsRequest): Observable<AiQmsView> {
    if (environment.useMockApi) {
      const q = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (q) {
        q.status = 'SUPERSEDED';
        q.supersededAt = now;
        q.supersededByQmsId = body.supersededByQmsId;
        q.updatedAt = now;
        return of(q).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiQmsView>(`${this.endpoint}/${id}/supersede`, body);
  }

  archive(id: string, body: ArchiveAiQmsRequest): Observable<AiQmsView> {
    if (environment.useMockApi) {
      const q = this.mockStore.find(x => x.id === id);
      const now = new Date().toISOString();
      if (q) {
        q.status = 'ARCHIVED';
        q.archiveReason = body.reason;
        q.archivedAt = now;
        q.updatedAt = now;
        return of(q).pipe(delay(120));
      }
      return of(this.mockStore[0]).pipe(delay(120));
    }
    return this.http.post<AiQmsView>(`${this.endpoint}/${id}/archive`, body);
  }

  delete(id: string): Observable<void> {
    if (environment.useMockApi) {
      const i = this.mockStore.findIndex(q => q.id === id);
      if (i >= 0) this.mockStore.splice(i, 1);
      return of(undefined).pipe(delay(120));
    }
    return this.http.delete<void>(`${this.endpoint}/${id}`);
  }

  private seed(): AiQmsView[] {
    const now = new Date().toISOString();
    return [
      {
        id: 'qms-1', tenantId: 'demo-tenant',
        reference: 'QMS-AI-MEDPHARM',
        version: '2026.1',
        name: 'QMS IA — Plateforme téléconsultation IA',
        description: 'Système de management qualité couvrant les systèmes IA high-risk de diagnostic assistant.',
        regulatoryComplianceStrategy: 'Veille EDPB + EU AI Office. Mapping AI Act ↔ ISO 42001 ↔ ISO 13485 maintenu trimestriellement.',
        designControlDescription: 'Revues de conception aux jalons (V&V, validation clinique, audit éthique). Documentation technique Art. 11 maintenue.',
        qualityControlDescription: 'KPI : taux de faux positifs, dérive de modèle, drift dataset. Seuils d\'alerte automatisés.',
        dataManagementDescription: 'Gouvernance data Art. 10 : datasets versionnés, contrôle biais, traçabilité provenance.',
        riskManagementDescription: 'FMEA IA continu. Risques fondamentaux + risques sectoriels santé.',
        pmmDescription: 'Plan PMM Art. 72 — surveillance post-market, incidents Art. 73, KPI clinique.',
        regulatorCommunicationDescription: 'Point d\'entrée unique CNIL + autorité notifiée. Reporting incidents Art. 73 < 15 jours.',
        resourceManagementDescription: 'Comité IA Act mensuel. Ressources : 1 ML lead, 2 data engineers, 1 DPO, 1 clinical validator.',
        supplierMonitoringDescription: 'Audits annuels fournisseurs modèles tiers + datasets. Clauses contractuelles AI Act dérivées.',
        coveredAiSystemIds: [],
        status: 'IN_FORCE',
        approvedByUserId: 'demo-user',
        approvedAt: now, inForceFrom: now,
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      },
      {
        id: 'qms-2', tenantId: 'demo-tenant',
        reference: 'QMS-AI-MEDPHARM',
        version: '2026.2',
        name: 'QMS IA — V2026.2 (intégration nouvelles obligations EU AI Office)',
        description: 'Version révisée — intègre les nouvelles guidances EU AI Office Q1 2026.',
        coveredAiSystemIds: [],
        status: 'DRAFT',
        createdByUserId: 'demo-user',
        createdAt: now, updatedAt: now
      }
    ];
  }
}
