import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AdoptionResponse, AdoptionsPage, AdoptRequest, AlignmentReport, AuditBlancReport,
  CertificationBlancReport, DossierResponse, EvidenceResponse, LinkEvidenceRequest,
  RoadmapSummary, StandardDetail, StandardSummary, StandardsPage, UpdateStageRequest
} from './standards.types';

@Injectable({ providedIn: 'root' })
export class StandardsService {

  private readonly baseEndpoint = `${environment.apiBaseUrl}/api/v1/standards`;

  constructor(private readonly http: HttpClient) {}

  // ---- Catalogue ----

  listCatalog(page = 0, size = 50): Observable<StandardsPage> {
    if (environment.useMockApi) return of(this.mockCatalog()).pipe(delay(120));
    return this.http.get<StandardsPage>(this.baseEndpoint,
      { params: new HttpParams().set('page', page).set('size', size) });
  }

  getStandardDetail(id: string): Observable<StandardDetail> {
    return this.http.get<StandardDetail>(`${this.baseEndpoint}/${id}`);
  }

  // ---- Adoptions ----

  listAdoptions(): Observable<AdoptionsPage> {
    if (environment.useMockApi) return of(this.mockAdoptions()).pipe(delay(120));
    return this.http.get<AdoptionsPage>(`${this.baseEndpoint}/adoptions`);
  }

  getAdoption(id: string): Observable<AdoptionResponse> {
    return this.http.get<AdoptionResponse>(`${this.baseEndpoint}/adoptions/${id}`);
  }

  adopt(req: AdoptRequest): Observable<AdoptionResponse> {
    return this.http.post<AdoptionResponse>(`${this.baseEndpoint}/adoptions`, req);
  }

  startProgress(id: string): Observable<AdoptionResponse> {
    return this.http.patch<AdoptionResponse>(`${this.baseEndpoint}/adoptions/${id}/start`, {});
  }

  // ---- Roadmap (§8.5) ----

  getRoadmap(id: string): Observable<RoadmapSummary> {
    return this.http.get<RoadmapSummary>(`${this.baseEndpoint}/adoptions/${id}/roadmap`);
  }

  updateStage(id: string, stageId: string, req: UpdateStageRequest): Observable<unknown> {
    return this.http.patch(`${this.baseEndpoint}/adoptions/${id}/roadmap/${stageId}`, req);
  }

  // ---- Alignement & audit blanc ----

  getAlignment(id: string): Observable<AlignmentReport> {
    return this.http.get<AlignmentReport>(`${this.baseEndpoint}/adoptions/${id}/alignment`);
  }

  getAuditBlanc(id: string): Observable<AuditBlancReport> {
    return this.http.get<AuditBlancReport>(`${this.baseEndpoint}/adoptions/${id}/audit-blanc`);
  }

  // ---- Preuves (§8.4 onglet 6) ----

  listEvidence(id: string): Observable<EvidenceResponse[]> {
    return this.http.get<EvidenceResponse[]>(`${this.baseEndpoint}/adoptions/${id}/evidence`);
  }

  linkEvidence(id: string, req: LinkEvidenceRequest): Observable<EvidenceResponse> {
    return this.http.post<EvidenceResponse>(`${this.baseEndpoint}/adoptions/${id}/evidence`, req);
  }

  unlinkEvidence(id: string, evidenceId: string): Observable<unknown> {
    return this.http.delete(`${this.baseEndpoint}/adoptions/${id}/evidence/${evidenceId}`);
  }

  // ---- Dossier de certification (§8.4) ----

  generateDossier(id: string): Observable<DossierResponse> {
    return this.http.post<DossierResponse>(`${this.baseEndpoint}/adoptions/${id}/dossier`, {});
  }

  // ---- Certification à blanc (§8.5 étapes 14-15) ----

  runCertificationBlanc(id: string): Observable<CertificationBlancReport> {
    return this.http.post<CertificationBlancReport>(
      `${this.baseEndpoint}/adoptions/${id}/certification-blanc`, {});
  }

  // ---- Mocks (mode démo sans backend) ----

  private mockCatalog(): StandardsPage {
    const items: StandardSummary[] = [
      { id: 's1', code: 'iso-9001', fullName: 'ISO 9001:2015 — Management de la qualité',
        publisher: 'ISO', currentVersion: '2015', family: 'HLS', applicableIndustries: 'all',
        status: 'PUBLISHED', recertificationCycleMonths: 36 },
      { id: 's2', code: 'iso-27001', fullName: 'ISO/IEC 27001:2022 — Sécurité de l\'information',
        publisher: 'ISO/IEC', currentVersion: '2022', family: 'HLS', applicableIndustries: 'all',
        status: 'PUBLISHED', recertificationCycleMonths: 36 }
    ];
    return { content: items, totalElements: items.length, totalPages: 1, number: 0, size: items.length };
  }

  private mockAdoptions(): AdoptionsPage {
    const now = new Date().toISOString();
    const items: AdoptionResponse[] = [
      { id: 'ad1', tenantId: 't', standardId: 's1', standardCode: 'iso-9001',
        standardName: 'ISO 9001:2015', status: 'IN_PROGRESS',
        scopeDescription: 'SMQ siège + 3 usines',
        targetCertificationDate: '2026-12-15', certificationBody: 'AFNOR',
        createdAt: now, updatedAt: now }
    ];
    return { content: items, totalElements: items.length, totalPages: 1, number: 0, size: items.length };
  }
}
