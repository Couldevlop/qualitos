export type AdmStatus = 'DRAFT' | 'ACTIVE' | 'DEPRECATED' | 'ARCHIVED';
export type AdmType = 'PROFILING_ONLY' | 'AUTOMATED_DECISION' | 'AUTOMATED_DECISION_WITH_LEGAL_EFFECT';
export type Art22Basis = 'EXPLICIT_CONSENT' | 'CONTRACTUAL_NECESSITY' | 'AUTHORIZED_BY_LAW';

export const TYPE_LABEL: Record<AdmType, string> = {
  PROFILING_ONLY:                       $localize`:@@automated-decisions.type.profiling-only:Profilage seul (Art. 4.4)`,
  AUTOMATED_DECISION:                   $localize`:@@automated-decisions.type.automated-decision:Décision automatisée — sans effet juridique`,
  AUTOMATED_DECISION_WITH_LEGAL_EFFECT: $localize`:@@automated-decisions.type.legal-effect:Décision automatisée — avec effet juridique (Art. 22.1)`
};

export const BASIS_LABEL: Record<Art22Basis, string> = {
  EXPLICIT_CONSENT:      $localize`:@@automated-decisions.basis.explicit-consent:Consentement explicite`,
  CONTRACTUAL_NECESSITY: $localize`:@@automated-decisions.basis.contractual-necessity:Nécessité contractuelle`,
  AUTHORIZED_BY_LAW:     $localize`:@@automated-decisions.basis.authorized-by-law:Autorisée par la loi UE/État membre`
};

export interface AdmView {
  id: string;
  tenantId: string;
  reference: string;
  name: string;
  description?: string | null;
  decisionType: AdmType;
  art22LawfulBasis?: Art22Basis | null;
  lawfulBasisDetails?: string | null;
  inputDataCategories?: string[] | null;
  linkedProcessingActivityIds?: string[] | null;
  linkedDpiaId?: string | null;
  algorithmDescription?: string | null;
  significanceForSubject?: string | null;
  humanReviewMechanism?: string | null;
  objectionMechanism?: string | null;
  status: AdmStatus;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  createdByUserId: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdmCreateRequest {
  reference: string;
  name: string;
  description?: string;
  decisionType: AdmType;
  art22LawfulBasis?: Art22Basis;
  lawfulBasisDetails?: string;
  inputDataCategories?: string[];
  linkedProcessingActivityIds?: string[];
  linkedDpiaId?: string;
  algorithmDescription?: string;
  significanceForSubject?: string;
  humanReviewMechanism?: string;
  objectionMechanism?: string;
  createdByUserId: string;
}

export interface AdmEditRequest {
  name: string;
  description?: string;
  decisionType: AdmType;
  art22LawfulBasis?: Art22Basis;
  lawfulBasisDetails?: string;
  inputDataCategories?: string[];
  linkedProcessingActivityIds?: string[];
  linkedDpiaId?: string;
  algorithmDescription?: string;
  significanceForSubject?: string;
  humanReviewMechanism?: string;
  objectionMechanism?: string;
}
