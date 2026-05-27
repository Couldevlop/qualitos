import { SpringPage } from '../pdca/pdca.types';

export type StandardStatus = 'PUBLISHED' | 'DEPRECATED' | 'WITHDRAWN';
export type AdoptionStatus = 'PLANNING' | 'IN_PROGRESS' | 'CERTIFIED' | 'SURVEILLANCE' | 'EXPIRED' | 'WITHDRAWN';
export type ObligationLevel = 'MUST' | 'SHOULD' | 'MAY';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type StageStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'DONE' | 'SKIPPED';

export interface StandardSummary {
  id: string;
  code: string;
  fullName: string;
  publisher?: string;
  currentVersion: string;
  family?: string;
  applicableIndustries?: string;
  status: StandardStatus;
  recertificationCycleMonths?: number;
}

export interface RequirementDetail {
  id: string;
  code: string;
  text: string;
  obligation: ObligationLevel;
  evidenceTypes?: string;
  measurableCriteria?: string;
  riskIfMissing?: RiskLevel;
  orderIndex: number;
}
export interface ClauseDetail {
  id: string; code: string; title: string; description?: string;
  orderIndex: number; requirements: RequirementDetail[];
}
export interface SectionDetail {
  id: string; code: string; title: string; description?: string;
  orderIndex: number; clauses: ClauseDetail[];
}
export interface StandardDetail {
  id: string; code: string; fullName: string; publisher?: string;
  currentVersion: string; publicationDate?: string; family?: string;
  applicableIndustries?: string; description?: string;
  certificationBodyRequired: boolean; recertificationCycleMonths?: number;
  relatedNormCodes?: string; status: StandardStatus; sections: SectionDetail[];
}

export interface AdoptionResponse {
  id: string;
  tenantId: string;
  standardId: string;
  standardCode: string;
  standardName: string;
  status: AdoptionStatus;
  scopeDescription?: string;
  targetCertificationDate?: string;
  leadAuditorId?: string;
  certificationBody?: string;
  certifiedAt?: string;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdoptRequest {
  standardId: string;
  scopeDescription?: string;
  targetCertificationDate?: string;
  leadAuditorId?: string;
  certificationBody?: string;
}

export interface RoadmapStageResponse {
  id: string; stepNumber: number; name: string; description?: string;
  typicalDuration?: string; deliverables?: string; responsibleRole?: string;
  involvedModules?: string; status: StageStatus; assigneeId?: string;
  plannedStartDate?: string; plannedEndDate?: string;
  actualStartDate?: string; actualEndDate?: string; notes?: string; orderIndex: number;
}
export interface RoadmapSummary {
  tenantStandardId: string; totalStages: number; doneStages: number;
  inProgressStages: number; skippedStages: number; completionPercent: number;
  stages: RoadmapStageResponse[];
}
export interface UpdateStageRequest {
  status?: StageStatus; assigneeId?: string;
  plannedStartDate?: string; plannedEndDate?: string;
  actualStartDate?: string; actualEndDate?: string; notes?: string;
}

export interface ClauseAlignment {
  clauseId: string; clauseCode: string; clauseTitle: string;
  score: number; totalRequirements: number; coveredRequirements: number;
}
export interface SectionAlignment {
  sectionId: string; sectionCode: string; sectionTitle: string;
  score: number; totalRequirements: number; coveredRequirements: number;
  clauses: ClauseAlignment[];
}
export interface AlignmentReport {
  tenantStandardId: string; standardId: string; standardCode: string;
  overallScore: number; totalRequirements: number; coveredRequirements: number;
  totalMustRequirements: number; coveredMustRequirements: number;
  sections: SectionAlignment[];
}

export interface AuditFinding {
  requirementId: string; sectionCode: string; clauseCode: string;
  requirementCode: string; requirementText: string;
  obligation: ObligationLevel; riskIfMissing?: RiskLevel;
  findingSeverity: string; expectedEvidence?: string;
  remediationAction: string; remediationPriority: number;
}
export interface AuditBlancReport {
  tenantStandardId: string; standardId: string; standardCode: string;
  standardName: string; generatedAt: string; readinessScore: number;
  totalRequirements: number; coveredRequirements: number;
  mustTotal: number; mustCovered: number;
  criticalGaps: number; majorGaps: number; minorGaps: number;
  verdict: string; findings: AuditFinding[];
}

export interface DossierResponse {
  tenantStandardId: string; standardCode: string; standardName: string;
  generatedAt: string; sha256: string; anchorTxRef: string;
  fileName: string; contentType: string;
  readinessScore: number; roadmapCompletion: number;
  evidenceCount: number; htmlContent: string;
}

// ---- Preuves (§8.4 onglet 6) ----
export type EvidenceType =
  | 'DOCUMENT' | 'AUDIT' | 'CAPA' | 'PDCA_CYCLE' | 'ISHIKAWA' | 'FIVES_AUDIT'
  | 'TRAINING_RECORD' | 'KPI_RECORD' | 'EXTERNAL_FILE' | 'OTHER';

export interface EvidenceResponse {
  id: string;
  tenantStandardId: string;
  requirementId: string;
  requirementCode: string;
  evidenceType: EvidenceType;
  evidenceRefId?: string;
  evidenceUri?: string;
  note?: string;
  linkedBy: string;
  linkedAt: string;
}

export interface LinkEvidenceRequest {
  requirementId: string;
  evidenceType: EvidenceType;
  evidenceRefId?: string;
  evidenceUri?: string;
  note?: string;
  linkedBy: string;
}

// ---- Certification à blanc (§8.5 étapes 14-15 ; ISO/IEC 17021-1) ----
export type CertificationDecision =
  | 'CERTIFIABLE' | 'CERTIFIABLE_SOUS_RESERVE' | 'NON_CERTIFIABLE' | 'AJOURNE';

export interface AuditStageResult {
  stageNumber: number;
  name: string;
  passed: boolean;
  score: number;
  summary: string;
  watchPoints: string[];
}

export interface NonConformity {
  requirementId: string;
  sectionCode: string;
  clauseCode: string;
  requirementCode: string;
  requirementText: string;
  type: 'MAJOR' | 'MINOR' | 'OBSERVATION';
  description: string;
  requiredCorrection: string;
}

export interface MockCertificate {
  certificateNumber: string;
  standardCode: string;
  scope: string;
  certificationBody: string;
  issuedAt: string;
  expiresAt: string;
  conditions: string;
  disclaimer: string;
}

// ---- Bibliothèque documentaire (§8.4 onglet 3) ----
export type DocumentObligation = 'MANDATORY' | 'RECOMMENDED' | 'OPTIONAL';
export interface DocumentTemplate {
  id: string;
  code: string;
  name: string;
  obligation: DocumentObligation;
  category?: string;
  format?: string;
  mapsToClauses?: string;
  description?: string;
  downloadable: boolean;
}

// ---- Génération IA d'un brouillon de document (§8.8) ----
export interface AiDraftResponse {
  templateId: string;
  templateCode: string;
  templateName: string;
  draft: string;
  provider: string;
  latencyMs: number;
}

// ---- Cartographie des processus (§8.4 onglet 4) ----
export interface ProcessTemplate {
  id: string;
  code: string;
  name: string;
  description?: string;
  mapsToClauses?: string;
  bpmnUri?: string;
  orderIndex: number;
}

// ---- Veille normative (§8.4 onglet 8) ----
export type RevisionStatus = 'CURRENT' | 'PLANNED' | 'SUPERSEDED';
export interface StandardRevision {
  id: string;
  version: string;
  status: RevisionStatus;
  publishedDate?: string;
  effectiveDate?: string;
  summary?: string;
  impactNote?: string;
  sourceUrl?: string;
  orderIndex: number;
}

export interface CertificationBlancReport {
  tenantStandardId: string;
  standardId: string;
  standardCode: string;
  standardName: string;
  generatedAt: string;
  stage1: AuditStageResult;
  stage2: AuditStageResult;
  majorNonConformities: number;
  minorNonConformities: number;
  observations: number;
  decision: CertificationDecision;
  decisionLabel: string;
  nonConformities: NonConformity[];
  certificate?: MockCertificate;
  sha256: string;
  anchorTxRef: string;
  signature: string;
}

export type StandardsPage = SpringPage<StandardSummary>;
export type AdoptionsPage = SpringPage<AdoptionResponse>;
