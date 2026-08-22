import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { delay } from 'rxjs/operators';

import { environment } from '../../../environments/environment';
import {
  AdoptionResponse, AdoptionsPage, AdoptRequest, AiDraftResponse, AlignmentReport,
  AuditBlancReport, CertificationBlancReport, ClauseRequest, DocumentTemplate, DossierResponse,
  EvidenceResponse, LinkEvidenceRequest, ProcessTemplate, RequirementRequest, RoadmapSummary,
  SectionRequest, StandardDetail, StandardRevision, StandardSummary, StandardsPage,
  StoryboardResponse, UpdateStageRequest
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

  /**
   * Crée un référentiel d'audit à partir d'une procédure approuvée de la GED (§8).
   * Le serveur en tire le code, le titre et la version ; l'arborescence naît vide.
   *
   * Les refus remontent tels quels : 409 (référentiel déjà créé) et 422 (procédure
   * non approuvée) ne disent pas la même chose, et c'est à l'écran de choisir le
   * message — pas au service de les aplatir en une erreur unique.
   */
  createProcedureReferential(documentId: string): Observable<void> {
    return this.http.post<void>(`${this.baseEndpoint}/from-document`, { documentId });
  }

  /**
   * Supprime un référentiel du tenant avec toute son arborescence. Refusé (409)
   * tant qu'un projet de conformité le suit.
   */
  deleteProcedureReferential(standardId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseEndpoint}/${standardId}`);
  }

  // ---- Arborescence d'un référentiel du tenant (§8) ----
  //
  // Aucune de ces écritures ne rend le nœud créé : le serveur n'attribue son
  // identifiant qu'à l'écriture en base. L'appelant relit la fiche, ce qui le
  // garde fidèle à l'ordre et aux codes réellement retenus.

  addSection(standardId: string, req: SectionRequest): Observable<void> {
    return this.http.post<void>(`${this.baseEndpoint}/${standardId}/sections`, req);
  }

  updateSection(standardId: string, sectionId: string, req: SectionRequest): Observable<void> {
    return this.http.patch<void>(`${this.baseEndpoint}/${standardId}/sections/${sectionId}`, req);
  }

  deleteSection(standardId: string, sectionId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseEndpoint}/${standardId}/sections/${sectionId}`);
  }

  addClause(standardId: string, sectionId: string, req: ClauseRequest): Observable<void> {
    return this.http.post<void>(
      `${this.baseEndpoint}/${standardId}/sections/${sectionId}/clauses`, req);
  }

  updateClause(standardId: string, clauseId: string, req: ClauseRequest): Observable<void> {
    return this.http.patch<void>(`${this.baseEndpoint}/${standardId}/clauses/${clauseId}`, req);
  }

  deleteClause(standardId: string, clauseId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseEndpoint}/${standardId}/clauses/${clauseId}`);
  }

  addRequirement(standardId: string, clauseId: string, req: RequirementRequest): Observable<void> {
    return this.http.post<void>(
      `${this.baseEndpoint}/${standardId}/clauses/${clauseId}/requirements`, req);
  }

  updateRequirement(standardId: string, requirementId: string,
                    req: RequirementRequest): Observable<void> {
    return this.http.patch<void>(
      `${this.baseEndpoint}/${standardId}/requirements/${requirementId}`, req);
  }

  deleteRequirement(standardId: string, requirementId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.baseEndpoint}/${standardId}/requirements/${requirementId}`);
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

  /** Récit narratif IA de l'état d'avancement (§7.4). */
  generateStoryboard(id: string): Observable<StoryboardResponse> {
    return this.http.post<StoryboardResponse>(
      `${this.baseEndpoint}/adoptions/${id}/storyboard`, {});
  }

  // ---- Catalogue : bibliothèque / processus / veille (§8.4, par standardId) ----

  listDocumentTemplates(standardId: string): Observable<DocumentTemplate[]> {
    return this.http.get<DocumentTemplate[]>(`${this.baseEndpoint}/${standardId}/document-templates`);
  }

  /** Télécharge le modèle (blob via l'intercepteur → token attaché). */
  downloadDocumentTemplate(standardId: string, templateId: string): Observable<HttpResponse<Blob>> {
    return this.http.get(
      `${this.baseEndpoint}/${standardId}/document-templates/${templateId}/download`,
      { observe: 'response', responseType: 'blob' });
  }

  /** Génère un brouillon de document par LLM (via api-quality-engine → ai-service). */
  generateAiDraft(standardId: string, templateId: string): Observable<AiDraftResponse> {
    return this.http.post<AiDraftResponse>(
      `${this.baseEndpoint}/${standardId}/document-templates/${templateId}/ai-draft`, {});
  }

  listProcessTemplates(standardId: string): Observable<ProcessTemplate[]> {
    return this.http.get<ProcessTemplate[]>(`${this.baseEndpoint}/${standardId}/process-templates`);
  }

  listRevisions(standardId: string): Observable<StandardRevision[]> {
    return this.http.get<StandardRevision[]>(`${this.baseEndpoint}/${standardId}/revisions`);
  }

  // ---- Mocks (mode démo sans backend) ----

  private mockCatalog(): StandardsPage {
    const items: StandardSummary[] = [
      { id: 's1', code: 'iso-9001', fullName: 'ISO 9001:2015 — Management de la qualité',
        publisher: 'ISO', currentVersion: '2015', family: 'HLS', applicableIndustries: 'all',
        status: 'PUBLISHED', recertificationCycleMonths: 36, owned: false },
      { id: 's2', code: 'iso-27001', fullName: 'ISO/IEC 27001:2022 — Sécurité de l\'information',
        publisher: 'ISO/IEC', currentVersion: '2022', family: 'HLS', applicableIndustries: 'all',
        status: 'PUBLISHED', recertificationCycleMonths: 36, owned: false },
      // Un référentiel du tenant, pour que le mode démonstration montre aussi ce
      // que la fonctionnalité apporte : une procédure interne devenue auditable.
      { id: 's3', code: 'PRO-002', fullName: 'Procédure d\'audit interne',
        currentVersion: 'v3', family: 'INTERNAL_PROCEDURE', status: 'PUBLISHED', owned: true }
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
