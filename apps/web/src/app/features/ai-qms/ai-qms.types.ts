/**
 * AI Act — Quality Management System (Art. 17 RGPD AI Act).
 * Backend /api/v1/ai-act/qms.
 * Workflow : DRAFT → APPROVED → IN_FORCE → SUPERSEDED, ou ARCHIVED.
 */

export type AiQmsStatus = 'DRAFT' | 'APPROVED' | 'IN_FORCE' | 'SUPERSEDED' | 'ARCHIVED';

export interface AiQmsView {
  id: string;
  tenantId: string;
  reference: string;
  version: string;
  name: string;
  description?: string;
  // Art. 17 — les 9 sections du QMS IA
  regulatoryComplianceStrategy?: string;
  designControlDescription?: string;
  qualityControlDescription?: string;
  dataManagementDescription?: string;
  riskManagementDescription?: string;
  pmmDescription?: string;
  regulatorCommunicationDescription?: string;
  resourceManagementDescription?: string;
  supplierMonitoringDescription?: string;
  coveredAiSystemIds: string[];
  status: AiQmsStatus;
  submittedByUserId?: string;
  approvedByUserId?: string;
  approvalNotes?: string;
  approvedAt?: string;
  inForceFrom?: string;
  supersededAt?: string;
  supersededByQmsId?: string;
  archiveReason?: string;
  archivedAt?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface DraftAiQmsRequest {
  reference: string;
  version: string;
  name: string;
  description?: string;
  regulatoryComplianceStrategy?: string;
  designControlDescription?: string;
  qualityControlDescription?: string;
  dataManagementDescription?: string;
  riskManagementDescription?: string;
  pmmDescription?: string;
  regulatorCommunicationDescription?: string;
  resourceManagementDescription?: string;
  supplierMonitoringDescription?: string;
  coveredAiSystemIds?: string[];
  createdByUserId: string;
}

export interface EditAiQmsRequest {
  name: string;
  description?: string;
  regulatoryComplianceStrategy?: string;
  designControlDescription?: string;
  qualityControlDescription?: string;
  dataManagementDescription?: string;
  riskManagementDescription?: string;
  pmmDescription?: string;
  regulatorCommunicationDescription?: string;
  resourceManagementDescription?: string;
  supplierMonitoringDescription?: string;
  coveredAiSystemIds?: string[];
}

export interface ApproveAiQmsRequest {
  submittedByUserId: string;
  approvedByUserId: string;
  approvalNotes?: string;
}

export interface SupersedeAiQmsRequest { supersededByQmsId: string; }
export interface ArchiveAiQmsRequest   { reason: string; }
