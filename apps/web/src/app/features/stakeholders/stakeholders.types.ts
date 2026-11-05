/**
 * Le registre des parties intéressées (ISO 9001 §4.2), tel que
 * `/api/v1/stakeholders` le rend et le reçoit.
 */

/** Les familles de parties intéressées : des rôles, pas des secteurs. */
export type StakeholderCategory =
  | 'CUSTOMERS'
  | 'SUPPLIERS'
  | 'EMPLOYEES'
  | 'GOVERNANCE'
  | 'REGULATORS'
  | 'CERTIFICATION_BODIES'
  | 'BUSINESS_PARTNERS'
  | 'SERVICE_PROVIDERS'
  | 'FINANCIAL_PARTNERS'
  | 'LOCAL_COMMUNITIES'
  | 'OTHER';

/** D'où la partie intéressée regarde l'organisme. */
export type StakeholderSide = 'INTERNAL' | 'EXTERNAL';

/** Trois crans : au-delà, la cotation devient une discussion sur l'échelle. */
export type ImpactLevel = 'LOW' | 'MEDIUM' | 'HIGH';

/** « À déterminer » est un état plein : l'auditeur lit la décision. */
export type Relevance = 'RELEVANT' | 'NOT_RELEVANT' | 'TO_DETERMINE';

/** Ce qui rend une exigence opposable. */
export type MandatoryNature = 'CONTRACTUAL' | 'REGULATORY' | 'NORMATIVE' | 'VOLUNTARY' | 'INTERNAL';

/** Où en est le traitement d'une exigence. */
export type RequirementStatus = 'TO_ANALYSE' | 'APPLICABLE' | 'CONTROLLED' | 'TO_REASSESS';

/** Les objets d'autres modules auxquels une exigence se rattache. */
export type RequirementLink = 'RISK' | 'NON_CONFORMITY' | 'AUDIT' | 'DOCUMENT';

export const STAKEHOLDER_CATEGORIES: readonly StakeholderCategory[] = [
  'CUSTOMERS', 'SUPPLIERS', 'EMPLOYEES', 'GOVERNANCE', 'REGULATORS', 'CERTIFICATION_BODIES',
  'BUSINESS_PARTNERS', 'SERVICE_PROVIDERS', 'FINANCIAL_PARTNERS', 'LOCAL_COMMUNITIES', 'OTHER'
];
export const IMPACT_LEVELS: readonly ImpactLevel[] = ['LOW', 'MEDIUM', 'HIGH'];
export const RELEVANCES: readonly Relevance[] = ['RELEVANT', 'NOT_RELEVANT', 'TO_DETERMINE'];
export const MANDATORY_NATURES: readonly MandatoryNature[] =
  ['CONTRACTUAL', 'REGULATORY', 'NORMATIVE', 'VOLUNTARY', 'INTERNAL'];
export const REQUIREMENT_STATUSES: readonly RequirementStatus[] =
  ['TO_ANALYSE', 'APPLICABLE', 'CONTROLLED', 'TO_REASSESS'];

/** Les bornes du serveur, pour arrêter une saisie avant l'aller-retour. */
export const STAKEHOLDER_LIMITS = {
  name: 255,
  text: 4000,
  line: 500,
  requirement: 500,
  requirements: 100,
  /** Sous ce seuil, il n'y a rien à lire : l'IA inventerait. */
  documentMin: 40,
  /**
   * Le texte d'un document soumis à l'assistant. Aligné sur
   * `StakeholderPorts.Assistant.DOCUMENT_MAX` : au-delà, la passerelle IA refuse
   * l'invite entière, et l'utilisateur lirait « rien trouvé » sur un document
   * parfaitement lisible. Mieux vaut l'arrêter à la saisie.
   */
  document: 17_000,
  /** Chacune des deux versions comparées : elles partagent la même invite. */
  comparedDocument: 8_000
} as const;

/** Une personne de l'annuaire : son compte, et son nom recopié. */
export interface StakeholderPerson {
  accountId: string;
  name: string;
}

/** Une ligne du registre. */
export interface StakeholderRow {
  id: string;
  name: string;
  category: StakeholderCategory;
  side: StakeholderSide;
  qualityImpact: ImpactLevel;
  regulatoryImpact: ImpactLevel;
  /** Calculée par le serveur : le plus élevé des deux impacts. */
  criticality: ImpactLevel;
  relevance: Relevance;
  lastReviewedOn: string | null;
  nextReviewOn: string | null;
  reviewOverdue: boolean;
  requirementCount: number;
}

/** Une exigence portée par une partie intéressée. */
export interface StakeholderRequirement {
  id: string;
  need: string | null;
  requirement: string;
  origin: string | null;
  nature: MandatoryNature | null;
  risk: string | null;
  riskLevel: ImpactLevel | null;
  control: string | null;
  indicator: string | null;
  responsible: string | null;
  evidence: string | null;
  status: RequirementStatus;
  riskId: string | null;
  ncId: string | null;
  auditId: string | null;
  documentId: string | null;
  capaId: string | null;
}

/** La fiche complète, dans l'ordre où elle se remplit. */
export interface StakeholderSheet {
  id: string;
  name: string;
  category: StakeholderCategory;
  side: StakeholderSide;
  description: string | null;
  contact: string | null;
  sites: string | null;
  needs: string | null;
  requirementsSummary: string | null;
  relevance: Relevance;
  relevanceJustification: string | null;
  risksAndOpportunities: string | null;
  qualityImpact: ImpactLevel;
  regulatoryImpact: ImpactLevel;
  criticality: ImpactLevel;
  owner: StakeholderPerson | null;
  controls: string | null;
  indicators: string | null;
  documents: string | null;
  actions: string | null;
  nextReviewOn: string | null;
  lastReviewedOn: string | null;
  reviewOverdue: boolean;
  requirements: StakeholderRequirement[];
}

/** Les dénombrements au-dessus du registre. */
export interface StakeholderStatistics {
  total: number;
  internal: number;
  external: number;
  critical: number;
  overdue: number;
  toDetermine: number;
  requirementsToReassess: number;
}

export interface CreateStakeholderRequest {
  name: string;
  category: StakeholderCategory;
  side: StakeholderSide;
}

/**
 * La fiche enregistrée d'un BLOC : un champ absent est un champ effacé, pas un
 * champ inchangé. L'écran envoie donc toujours la fiche entière.
 */
export interface UpdateStakeholderRequest {
  name: string;
  category: StakeholderCategory;
  side: StakeholderSide;
  description: string | null;
  contact: string | null;
  sites: string | null;
  needs: string | null;
  requirementsSummary: string | null;
  relevance: Relevance;
  relevanceJustification: string | null;
  risksAndOpportunities: string | null;
  qualityImpact: ImpactLevel;
  regulatoryImpact: ImpactLevel;
  owner: StakeholderPerson | null;
  controls: string | null;
  indicators: string | null;
  documents: string | null;
  actions: string | null;
  nextReviewOn: string | null;
}

export interface RequirementRequest {
  need?: string | null;
  requirement: string;
  origin?: string | null;
  nature?: MandatoryNature | null;
  risk?: string | null;
  riskLevel?: ImpactLevel | null;
  control?: string | null;
  indicator?: string | null;
  responsible?: string | null;
  evidence?: string | null;
  status?: RequirementStatus | null;
}

/** Les deux notes d'une ligne de la matrice. */
export interface StakeholderRating {
  qualityImpact: ImpactLevel;
  regulatoryImpact: ImpactLevel;
}

/** Une partie intéressée proposée par l'IA : pas encore inscrite. */
export interface SuggestedStakeholder {
  name: string;
  category: StakeholderCategory;
  rationale: string | null;
}

/** Une exigence tirée d'un document, avec le passage qui la prouve. */
export interface ExtractedRequirement {
  quote: string;
  reference: string | null;
  requirement: string;
  nature: MandatoryNature | null;
  control: string | null;
}

/** Un changement détecté entre deux versions. */
export interface DetectedChange {
  title: string;
  detail: string | null;
  source: string | null;
  impact: ImpactLevel | null;
}
