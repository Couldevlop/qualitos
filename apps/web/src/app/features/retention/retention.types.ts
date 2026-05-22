/**
 * GDPR Retention Rules — Art. 5§1.e RGPD (limitation de conservation).
 * Backend /api/v1/gdpr/retention-rules.
 *
 * Workflow : DRAFT → ACTIVE → ARCHIVED.
 * ACTIVE est immutable (toute modification = nouvelle règle).
 *
 * `retentionPeriod` est sérialisé en ISO-8601 Duration côté backend
 * (Java Duration : `PnD`, `PTnH`, etc.). Le helper TS le convertit en
 * "N jours" pour l'UI.
 */

export type RetentionRuleStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export type RetentionUnit = 'DAY' | 'MONTH' | 'YEAR';

export interface RetentionRuleView {
  id: string;
  tenantId: string;
  dataCategoryCode: string;
  dataCategoryLabel?: string;
  retentionPeriod: string;       // ISO-8601 Duration (e.g. "P30D", "PT720H")
  legalBasis: string;
  lawfulBasisReference?: string;
  status: RetentionRuleStatus;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateRetentionRuleRequest {
  dataCategoryCode: string;
  dataCategoryLabel?: string;
  retentionPeriod: string;       // ISO-8601 Duration
  legalBasis: string;
  lawfulBasisReference?: string;
  createdByUserId: string;
}

export interface EditRetentionRuleRequest {
  dataCategoryLabel?: string;
  retentionPeriod: string;
  legalBasis: string;
  lawfulBasisReference?: string;
}

export interface ErasureEvaluation {
  dataCategoryCode: string;
  recordCreatedAt: string;
  erasureAt: string;
  dueNow: boolean;
  ruleId: string;
  retentionPeriod: string;
}
