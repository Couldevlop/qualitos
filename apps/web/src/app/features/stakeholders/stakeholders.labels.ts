import {
  ImpactLevel,
  MandatoryNature,
  Relevance,
  RequirementStatus,
  StakeholderCategory,
  StakeholderSide
} from './stakeholders.types';

/**
 * Les libellés du registre des parties intéressées.
 *
 * <p>Écrits ici une fois : la liste, la fiche, la matrice et l'assistant
 * affichaient sinon la valeur brute de l'énumération (« CERTIFICATION_BODIES »),
 * qui ne se traduit dans aucune langue. Les traductions vivent dans
 * `scripts/i18n/stakeholders.py`.
 */
const CATEGORIES: Readonly<Record<StakeholderCategory, string>> = {
  CUSTOMERS: $localize`:@@stakeholders.category.customers:Clients et utilisateurs finaux`,
  SUPPLIERS: $localize`:@@stakeholders.category.suppliers:Fournisseurs et sous-traitants`,
  EMPLOYEES: $localize`:@@stakeholders.category.employees:Collaborateurs et représentants du personnel`,
  GOVERNANCE: $localize`:@@stakeholders.category.governance:Direction et actionnaires`,
  REGULATORS: $localize`:@@stakeholders.category.regulators:Autorités réglementaires`,
  CERTIFICATION_BODIES: $localize`:@@stakeholders.category.certification:Organismes de certification et d'accréditation`,
  BUSINESS_PARTNERS: $localize`:@@stakeholders.category.partners:Partenaires commerciaux`,
  SERVICE_PROVIDERS: $localize`:@@stakeholders.category.providers:Prestataires de services`,
  FINANCIAL_PARTNERS: $localize`:@@stakeholders.category.financial:Assureurs et partenaires financiers`,
  LOCAL_COMMUNITIES: $localize`:@@stakeholders.category.communities:Communautés locales et acteurs externes`,
  OTHER: $localize`:@@stakeholders.category.other:Autres parties intéressées`
};

const SIDES: Readonly<Record<StakeholderSide, string>> = {
  INTERNAL: $localize`:@@stakeholders.side.internal:Interne`,
  EXTERNAL: $localize`:@@stakeholders.side.external:Externe`
};

const IMPACTS: Readonly<Record<ImpactLevel, string>> = {
  LOW: $localize`:@@stakeholders.impact.low:Faible`,
  MEDIUM: $localize`:@@stakeholders.impact.medium:Moyen`,
  HIGH: $localize`:@@stakeholders.impact.high:Élevé`
};

const RELEVANCE_LABELS: Readonly<Record<Relevance, string>> = {
  RELEVANT: $localize`:@@stakeholders.relevance.relevant:Pertinente`,
  NOT_RELEVANT: $localize`:@@stakeholders.relevance.not-relevant:Non pertinente`,
  TO_DETERMINE: $localize`:@@stakeholders.relevance.to-determine:À déterminer`
};

const NATURES: Readonly<Record<MandatoryNature, string>> = {
  CONTRACTUAL: $localize`:@@stakeholders.nature.contractual:Contractuel`,
  REGULATORY: $localize`:@@stakeholders.nature.regulatory:Réglementaire`,
  NORMATIVE: $localize`:@@stakeholders.nature.normative:Normatif`,
  VOLUNTARY: $localize`:@@stakeholders.nature.voluntary:Volontaire`,
  INTERNAL: $localize`:@@stakeholders.nature.internal:Interne`
};

const STATUSES: Readonly<Record<RequirementStatus, string>> = {
  TO_ANALYSE: $localize`:@@stakeholders.status.to-analyse:À analyser`,
  APPLICABLE: $localize`:@@stakeholders.status.applicable:Applicable`,
  CONTROLLED: $localize`:@@stakeholders.status.controlled:Maîtrisée`,
  TO_REASSESS: $localize`:@@stakeholders.status.to-reassess:À réévaluer`
};

export function categoryLabel(code: StakeholderCategory | string | null | undefined): string {
  if (!code) return '—';
  return CATEGORIES[code as StakeholderCategory] ?? String(code);
}

export function sideLabel(code: StakeholderSide | string | null | undefined): string {
  if (!code) return '—';
  return SIDES[code as StakeholderSide] ?? String(code);
}

export function impactLabel(code: ImpactLevel | string | null | undefined): string {
  if (!code) return '—';
  return IMPACTS[code as ImpactLevel] ?? String(code);
}

export function relevanceLabel(code: Relevance | string | null | undefined): string {
  if (!code) return '—';
  return RELEVANCE_LABELS[code as Relevance] ?? String(code);
}

export function natureLabel(code: MandatoryNature | string | null | undefined): string {
  if (!code) return '—';
  return NATURES[code as MandatoryNature] ?? String(code);
}

export function requirementStatusLabel(code: RequirementStatus | string | null | undefined): string {
  if (!code) return '—';
  return STATUSES[code as RequirementStatus] ?? String(code);
}

/**
 * La classe du jeton de criticité. Le liseré de couleur ne suffit pas : le
 * libellé est toujours écrit à côté, pour que l'information ne repose pas sur
 * la seule couleur (WCAG 1.4.1).
 */
export function impactChip(code: ImpactLevel | null | undefined): string {
  switch (code) {
    case 'HIGH': return 'chip chip--danger';
    case 'MEDIUM': return 'chip chip--warn';
    case 'LOW': return 'chip chip--ok';
    default: return 'chip';
  }
}

/** La classe du jeton de statut d'une exigence. */
export function statusChip(code: RequirementStatus | null | undefined): string {
  switch (code) {
    case 'CONTROLLED': return 'chip chip--ok';
    case 'TO_REASSESS': return 'chip chip--warn';
    case 'APPLICABLE': return 'chip chip--accent';
    default: return 'chip';
  }
}
