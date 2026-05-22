/**
 * GDPR ROPA — registre des activités de traitement (Art. 30 RGPD).
 * Source de vérité backend : /api/v1/gdpr/processing-activities.
 */

export type LawfulBasis =
  | 'CONSENT'              // Art. 6.1.a
  | 'CONTRACT'             // Art. 6.1.b
  | 'LEGAL_OBLIGATION'     // Art. 6.1.c
  | 'VITAL_INTERESTS'      // Art. 6.1.d
  | 'PUBLIC_TASK'          // Art. 6.1.e
  | 'LEGITIMATE_INTERESTS'; // Art. 6.1.f (LIA requise)

export type ProcessingActivityStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';

export interface ProcessingActivityView {
  id: string;
  tenantId: string;
  reference: string;
  name: string;
  purposes: string;
  lawfulBasis: LawfulBasis;
  lawfulBasisDetails?: string;
  controllerName: string;
  controllerContact: string;
  dpoContact?: string;
  jointControllerName?: string;
  jointControllerContact?: string;
  dataSubjectCategories: string[];
  dataCategories: string[];
  specialCategoriesProcessed: boolean;
  specialCategoriesJustification?: string;
  recipientCategories: string[];
  thirdCountryTransfers: string[];
  transferSafeguards?: string;
  linkedRetentionRuleIds: string[];
  technicalMeasures?: string;
  organizationalMeasures?: string;
  status: ProcessingActivityStatus;
  effectiveFrom?: string;
  effectiveTo?: string;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProcessingActivityRequest {
  reference: string;
  name: string;
  purposes: string;
  lawfulBasis: LawfulBasis;
  lawfulBasisDetails?: string;
  controllerName: string;
  controllerContact: string;
  dpoContact?: string;
  jointControllerName?: string;
  jointControllerContact?: string;
  dataSubjectCategories?: string[];
  dataCategories?: string[];
  specialCategoriesProcessed: boolean;
  specialCategoriesJustification?: string;
  recipientCategories?: string[];
  thirdCountryTransfers?: string[];
  transferSafeguards?: string;
  linkedRetentionRuleIds?: string[];
  technicalMeasures?: string;
  organizationalMeasures?: string;
  createdByUserId: string;
}

export interface EditProcessingActivityRequest {
  name: string;
  purposes: string;
  lawfulBasis: LawfulBasis;
  lawfulBasisDetails?: string;
  controllerName: string;
  controllerContact: string;
  dpoContact?: string;
  jointControllerName?: string;
  jointControllerContact?: string;
  dataSubjectCategories?: string[];
  dataCategories?: string[];
  specialCategoriesProcessed: boolean;
  specialCategoriesJustification?: string;
  recipientCategories?: string[];
  thirdCountryTransfers?: string[];
  transferSafeguards?: string;
  linkedRetentionRuleIds?: string[];
  technicalMeasures?: string;
  organizationalMeasures?: string;
}
