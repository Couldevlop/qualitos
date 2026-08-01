/**
 * Contrats des trois capacités Standards Hub restées sans interface :
 *
 * - matrice de co-couverture IMS  → `CoverageMatrixController` (§8.9) ;
 * - audit blanc IA persisté       → `MockAuditController` (§8.4 onglet 7) ;
 * - ancrage / vérification chaîne → `AnchoringController` (§11.3).
 *
 * Aucun type ne porte de `tenantId` en entrée : le serveur le dérive du JWT
 * (règle §18.2 #2). Les `tenantId` présents ci-dessous sont des champs de
 * RÉPONSE, affichés à titre de traçabilité uniquement.
 */

// ---------------------------------------------------------------------------
// §8.9 — Matrice de co-couverture (Integrated Management System)
// ---------------------------------------------------------------------------

/** Nature du lien entre une clause source et une clause d'une autre norme. */
export type CoverageRelation = 'EQUIVALENT' | 'COVERS' | 'RELATED' | 'REFERENCES';

/** Une clause cible atteinte depuis la clause source, avec sa confiance (0-100). */
export interface CoverageTarget {
  clauseCode: string;
  relation: CoverageRelation;
  confidence: number;
}

/** Cellule brute renvoyée par l'API : (clause source × norme cible). */
export interface CoverageCell {
  sourceStandardCode: string;
  sourceClauseCode: string;
  targetStandardCode: string;
  targets: CoverageTarget[];
}

/** Réponse de `GET /api/v1/standards/coverage-matrix`. */
export interface CoverageMatrixResponse {
  tenantId: string;
  standardCodes: string[];
  cells: CoverageCell[];
  totalSourceClauses: number;
  totalMappings: number;
  /** Taux de mutualisation en POURCENTAGE (0-100), déjà calculé par le serveur. */
  reuseRatioPercent: number;
}

/**
 * Cellule prête à afficher, TOUJOURS présente pour chaque colonne : une cellule
 * vide se dessine (tiret) au lieu de décaler la grille. Évite aussi d'indexer un
 * dictionnaire depuis le template (clé non typée en `strictTemplates`).
 */
export interface CoverageCellView {
  targetStandardCode: string;
  coverages: CoverageTarget[];
  /** Au moins un lien EQUIVALENT ou COVERS : la preuve est réellement réutilisable. */
  shared: boolean;
  /** Colonne de la norme d'origine de la clause : aucune couverture n'y est attendue. */
  self: boolean;
}

/** Ligne de la matrice : une clause source, une cellule par norme comparée. */
export interface CoverageRow {
  sourceStandardCode: string;
  sourceClauseCode: string;
  /** Aligné, index par index, sur `CoverageOverview.columns`. */
  cells: CoverageCellView[];
  /** Nombre de normes sur lesquelles la clause est mutualisable. */
  sharedCount: number;
}

/** Vue complète de l'onglet matrice : réponse brute + pivot prêt à rendre. */
export interface CoverageOverview {
  matrix: CoverageMatrixResponse;
  columns: string[];
  rows: CoverageRow[];
  /** Clauses mutualisées sur au moins une autre norme — l'argument d'effort du §8.9. */
  sharedClauseCount: number;
}

// ---------------------------------------------------------------------------
// §8.4 onglet 7 — Audit blanc IA (persisté et historisé)
// ---------------------------------------------------------------------------

/** Grille ISO/IEC 17021-1 restituée par le serveur (`MockAuditCriticality`). */
export type MockAuditCriticality = 'MAJOR' | 'MINOR' | 'OBSERVATION';

export interface MockAuditQuestion {
  clauseCode: string;
  question: string;
  rationale: string;
}

export interface MockAuditGap {
  clauseCode: string;
  title: string;
  criticality: MockAuditCriticality;
  /** Ratio de couverture des exigences de la clause, dans [0, 1]. */
  coverageRatio: number;
  totalRequirements: number;
  coveredRequirements: number;
  finding: string;
  questions: MockAuditQuestion[];
}

export interface MockAuditRemediation {
  clauseCode: string;
  criticality: MockAuditCriticality;
  /** Priorité CAPA associée : high / medium / low. */
  priority: string;
  /** Module QualitOS visé (PDCA, TRAINING, AUDIT, DOCUMENT_CONTROL). */
  targetModule: string;
  action: string;
}

/** Une exécution d'audit blanc IA, telle que persistée (`MockAuditDto.Report`). */
export interface MockAuditReport {
  id: string;
  adoptionId: string;
  standardId: string;
  standardCode: string;
  standardName: string;
  /** Niveau de préparation en POURCENTAGE (0-100). */
  readiness: number;
  majorCount: number;
  minorCount: number;
  observationCount: number;
  questionCount: number;
  questions: MockAuditQuestion[];
  gaps: MockAuditGap[];
  remediationPlan: MockAuditRemediation[];
  aiProvider: string;
  createdByUserId: string | null;
  createdAt: string;
}

// ---------------------------------------------------------------------------
// Normes & adoptions (matière première des deux onglets ci-dessus)
// ---------------------------------------------------------------------------

export type AdoptionStatus =
  'PLANNING' | 'IN_PROGRESS' | 'CERTIFIED' | 'SURVEILLANCE' | 'EXPIRED' | 'WITHDRAWN';

/** Sous-ensemble de `StandardsDto.AdoptionResponse` réellement utilisé ici. */
export interface StandardAdoption {
  id: string;
  standardId: string;
  standardCode: string;
  standardName: string;
  status: AdoptionStatus;
}

/** Sous-ensemble de `StandardsDto.StandardSummary` réellement utilisé ici. */
export interface StandardCatalogEntry {
  id: string;
  code: string;
  fullName: string;
  family: string | null;
}

/** Enveloppe `Page<T>` de Spring Data. */
export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  number: number;
  size: number;
}

// ---------------------------------------------------------------------------
// §11.3 — Ancrage blockchain & vérification de preuve
// ---------------------------------------------------------------------------

/**
 * Résultat d'un lot d'ancrage. Un lot vide est un cas NORMAL (rien à ancrer) :
 * `batchSize` vaut 0 et `merkleRoot`/`blockchainTxRef` sont nuls.
 */
export interface AnchorBatchResult {
  tenantId: string;
  batchSize: number;
  merkleRoot: string | null;
  blockchainTxRef: string | null;
  eventIds: string[];
  firstSequenceNo: number;
  lastSequenceNo: number;
  anchoredAt: string;
}

export type AnchorStatus = 'VERIFIED' | 'TAMPERED' | 'NOT_ANCHORED';

/** Verdict d'intégrité d'un événement ancré (ADR 0012). */
export interface AnchorVerification {
  status: AnchorStatus;
  detail: string;
  txRef: string | null;
  merkleRoot: string | null;
}

/**
 * Événement du journal d'audit, réduit à ce qui sert ici : proposer un hash
 * réel à vérifier et montrer s'il est déjà ancré.
 */
export interface AnchorableEvent {
  id: string;
  sequenceNo: number;
  occurredAt: string;
  action: string;
  resourceType: string;
  integrityHash: string;
  blockchainTxRef: string | null;
}
