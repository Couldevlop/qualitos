import {
  CapaKind, OpportunityActionStatus, OpportunityDecision, OpportunityLevel, OpportunityStatus, RegisterEvent,
  RegisterOrigin, RegisterRequirement, RegisterType, RiskDecision, RiskLevel, RiskStatus
} from './risk-register.types';

/**
 * Les textes du registre, écrits une fois.
 *
 * <p>Le serveur ne renvoie que des codes ; c'est ici qu'ils deviennent des mots
 * dans la langue de l'écran. Une valeur inconnue s'affiche telle quelle plutôt
 * que de disparaître.
 */

export interface Choice<T> { value: T; label: string; }

export const TYPES: ReadonlyArray<Choice<RegisterType>> = [
  { value: 'QUALITY', label: $localize`:@@rr.type.quality:Qualité` },
  { value: 'ENVIRONMENT', label: $localize`:@@rr.type.environment:Environnement` },
  { value: 'HEALTH_SAFETY', label: $localize`:@@rr.type.health-safety:SST` },
  { value: 'INFORMATION_SECURITY', label: $localize`:@@rr.type.information-security:Sécurité de l'information` },
  { value: 'LEGAL', label: $localize`:@@rr.type.legal:Exigences légales` }
];

/** Les origines d'un risque. Les quatre dernières pointent un objet réel de la plateforme. */
export const RISK_ORIGINS: ReadonlyArray<Choice<RegisterOrigin>> = [
  { value: 'DIRECT', label: $localize`:@@rr.origin.direct:Saisie directe` },
  { value: 'MANAGEMENT_REVIEW', label: $localize`:@@rr.origin.management-review:Revue de direction` },
  { value: 'AUDIT', label: $localize`:@@rr.origin.audit:Audit` },
  { value: 'CUSTOMER_FEEDBACK', label: $localize`:@@rr.origin.customer-feedback:Retour client` },
  { value: 'MONITORING', label: $localize`:@@rr.origin.monitoring:Veille` },
  { value: 'FMEA', label: $localize`:@@rr.origin.fmea:AMDEC` },
  { value: 'NON_CONFORMITY', label: $localize`:@@rr.origin.non-conformity:Non-conformité` },
  { value: 'INCIDENT', label: $localize`:@@rr.origin.incident:Incident` },
  { value: 'CHANGE', label: $localize`:@@rr.origin.change:Changement (MOC)` }
];

/** Une opportunité ne naît ni d'une AMDEC, ni d'une NC, ni d'un incident, ni d'un changement. */
export const OPPORTUNITY_ORIGINS: ReadonlyArray<Choice<RegisterOrigin>> =
  RISK_ORIGINS.filter(o => ['DIRECT', 'MANAGEMENT_REVIEW', 'AUDIT', 'CUSTOMER_FEEDBACK', 'MONITORING']
    .includes(o.value));

export const RISK_REQUIREMENTS: ReadonlyArray<Choice<RegisterRequirement>> = [
  { value: 'ISO_9001_6_1', label: 'ISO 9001 · 6.1' },
  { value: 'ISO_14001_6_1', label: 'ISO 14001 · 6.1' },
  { value: 'ISO_45001_6_1', label: 'ISO 45001 · 6.1' },
  { value: 'IATF_16949_6_1_2', label: 'IATF 16949 · 6.1.2' }
];

export const OPPORTUNITY_REQUIREMENTS: ReadonlyArray<Choice<RegisterRequirement>> = [
  { value: 'ISO_9001_6_1', label: 'ISO 9001 · 6.1' },
  { value: 'ISO_9001_10_3', label: 'ISO 9001 · 10.3' },
  { value: 'ISO_14001_6_1', label: 'ISO 14001 · 6.1' },
  { value: 'ISO_45001_6_1', label: 'ISO 45001 · 6.1' }
];

export const SEVERITIES: ReadonlyArray<Choice<number>> = [
  { value: 1, label: $localize`:@@rr.severity.1:1 · Négligeable` },
  { value: 2, label: $localize`:@@rr.severity.2:2 · Mineure` },
  { value: 3, label: $localize`:@@rr.severity.3:3 · Significative` },
  { value: 4, label: $localize`:@@rr.severity.4:4 · Grave` },
  { value: 5, label: $localize`:@@rr.severity.5:5 · Catastrophique` }
];

export const PROBABILITIES: ReadonlyArray<Choice<number>> = [
  { value: 1, label: $localize`:@@rr.probability.1:1 · Rare` },
  { value: 2, label: $localize`:@@rr.probability.2:2 · Peu probable` },
  { value: 3, label: $localize`:@@rr.probability.3:3 · Possible` },
  { value: 4, label: $localize`:@@rr.probability.4:4 · Probable` },
  { value: 5, label: $localize`:@@rr.probability.5:5 · Quasi certaine` }
];

export const GAINS: ReadonlyArray<Choice<number>> = [
  { value: 1, label: $localize`:@@rr.gain.1:1 · Faible` },
  { value: 2, label: $localize`:@@rr.gain.2:2 · Modéré` },
  { value: 3, label: $localize`:@@rr.gain.3:3 · Significatif` },
  { value: 4, label: $localize`:@@rr.gain.4:4 · Fort` },
  { value: 5, label: $localize`:@@rr.gain.5:5 · Très fort` }
];

export const FEASIBILITIES: ReadonlyArray<Choice<number>> = [
  { value: 1, label: $localize`:@@rr.feasibility.1:1 · Très difficile` },
  { value: 2, label: $localize`:@@rr.feasibility.2:2 · Difficile` },
  { value: 3, label: $localize`:@@rr.feasibility.3:3 · Réaliste` },
  { value: 4, label: $localize`:@@rr.feasibility.4:4 · Facile` },
  { value: 5, label: $localize`:@@rr.feasibility.5:5 · Très facile` }
];

export const RISK_DECISIONS: ReadonlyArray<Choice<RiskDecision>> = [
  { value: 'UNDECIDED', label: $localize`:@@rr.risk-decision.undecided:À décider` },
  { value: 'REDUCE', label: $localize`:@@rr.risk-decision.reduce:Réduire` },
  { value: 'ACCEPT', label: $localize`:@@rr.risk-decision.accept:Accepter` },
  { value: 'AVOID', label: $localize`:@@rr.risk-decision.avoid:Éviter` },
  { value: 'TRANSFER', label: $localize`:@@rr.risk-decision.transfer:Transférer` }
];

export const RISK_STATUSES: ReadonlyArray<Choice<RiskStatus>> = [
  { value: 'TO_TREAT', label: $localize`:@@rr.risk-status.to-treat:À traiter` },
  { value: 'IN_TREATMENT', label: $localize`:@@rr.risk-status.in-treatment:En traitement` },
  { value: 'MONITORED', label: $localize`:@@rr.risk-status.monitored:Surveillé` },
  { value: 'ACCEPTED', label: $localize`:@@rr.risk-status.accepted:Accepté` },
  { value: 'CLOSED', label: $localize`:@@rr.risk-status.closed:Clos` }
];

export const OPPORTUNITY_DECISIONS: ReadonlyArray<Choice<OpportunityDecision>> = [
  { value: 'UNDECIDED', label: $localize`:@@rr.opp-decision.undecided:À décider` },
  { value: 'PLAN', label: $localize`:@@rr.opp-decision.plan:Planifier` },
  { value: 'STUDY', label: $localize`:@@rr.opp-decision.study:Étudier` },
  { value: 'POSTPONE', label: $localize`:@@rr.opp-decision.postpone:Reporter` },
  { value: 'DISCARD', label: $localize`:@@rr.opp-decision.discard:Écarter` }
];

export const OPPORTUNITY_STATUSES: ReadonlyArray<Choice<OpportunityStatus>> = [
  { value: 'UNDER_STUDY', label: $localize`:@@rr.opp-status.under-study:En étude` },
  { value: 'PLANNED', label: $localize`:@@rr.opp-status.planned:Planifiée` },
  { value: 'IN_PROGRESS', label: $localize`:@@rr.opp-status.in-progress:En cours` },
  { value: 'DONE', label: $localize`:@@rr.opp-status.done:Réalisée` },
  { value: 'DISCARDED', label: $localize`:@@rr.opp-status.discarded:Écartée` }
];

export const ACTION_STATUSES: ReadonlyArray<Choice<OpportunityActionStatus>> = [
  { value: 'TO_START', label: $localize`:@@rr.action-status.to-start:À démarrer` },
  { value: 'IN_PROGRESS', label: $localize`:@@rr.action-status.in-progress:En cours` },
  { value: 'DONE', label: $localize`:@@rr.action-status.done:Terminée` },
  { value: 'CANCELLED', label: $localize`:@@rr.action-status.cancelled:Annulée` }
];

const RISK_LEVELS: Record<RiskLevel, string> = {
  LOW: $localize`:@@rr.level.low:Faible`,
  MEDIUM: $localize`:@@rr.level.medium:Moyen`,
  HIGH: $localize`:@@rr.level.high:Élevé`,
  CRITICAL: $localize`:@@rr.level.critical:Critique`
};

const OPPORTUNITY_LEVELS: Record<OpportunityLevel, string> = {
  LOW: $localize`:@@rr.level.low:Faible`,
  MEDIUM: $localize`:@@rr.level.medium:Moyen`,
  HIGH: $localize`:@@rr.level.high:Élevé`,
  PRIORITY: $localize`:@@rr.level.priority:Prioritaire`
};

/** Statuts d'un dossier CAPA lié, tels que le tableau « Traitement » les nomme. */
const CAPA_STATUSES: Record<string, string> = {
  OPEN: $localize`:@@rr.capa-status.open:À démarrer`,
  IN_PROGRESS: $localize`:@@rr.capa-status.in-progress:En cours`,
  RESOLVED: $localize`:@@rr.capa-status.resolved:Résolue`,
  CLOSED: $localize`:@@rr.capa-status.closed:Clôturée`,
  REJECTED: $localize`:@@rr.capa-status.rejected:Rejetée`
};

function find<T>(list: ReadonlyArray<Choice<T>>, value: T | null | undefined): string {
  if (value === null || value === undefined) return '—';
  return list.find(c => c.value === value)?.label ?? String(value);
}

export const CAPA_KINDS: ReadonlyArray<Choice<CapaKind>> = [
  { value: 'PREVENTIVE', label: $localize`:@@rr.capa-kind.preventive:Préventive` },
  { value: 'CORRECTIVE', label: $localize`:@@rr.capa-kind.corrective:Corrective` }
];

export const capaKindLabel = (v: CapaKind | null | undefined) => find(CAPA_KINDS, v);
export const typeLabel = (v: RegisterType | null | undefined) => find(TYPES, v);
export const originLabel = (v: RegisterOrigin | null | undefined) => find(RISK_ORIGINS, v);
export const riskStatusLabel = (v: RiskStatus | null | undefined) => find(RISK_STATUSES, v);
export const riskDecisionLabel = (v: RiskDecision | null | undefined) => find(RISK_DECISIONS, v);
export const opportunityStatusLabel = (v: OpportunityStatus | null | undefined) => find(OPPORTUNITY_STATUSES, v);
export const opportunityDecisionLabel = (v: OpportunityDecision | null | undefined) =>
  find(OPPORTUNITY_DECISIONS, v);
export const actionStatusLabel = (v: OpportunityActionStatus | null | undefined) => find(ACTION_STATUSES, v);

export function requirementLabel(v: RegisterRequirement): string {
  return [...RISK_REQUIREMENTS, ...OPPORTUNITY_REQUIREMENTS].find(r => r.value === v)?.label ?? v;
}

export function riskLevelLabel(v: RiskLevel | null | undefined): string {
  return v ? RISK_LEVELS[v] ?? v : '—';
}

export function opportunityLevelLabel(v: OpportunityLevel | null | undefined): string {
  return v ? OPPORTUNITY_LEVELS[v] ?? v : '—';
}

export function capaStatusLabel(v: string | null | undefined): string {
  return v ? CAPA_STATUSES[v] ?? v : '—';
}

/** Le niveau d'un score, mêmes seuils que le serveur : sert à l'aperçu du formulaire avant enregistrement. */
export function levelOf(score: number): RiskLevel {
  if (score >= 15) return 'CRITICAL';
  if (score >= 10) return 'HIGH';
  if (score >= 5) return 'MEDIUM';
  return 'LOW';
}

/** « 4x3 » (code du suivi) → « 4 × 3 ». */
export function ratingText(code: string | null | undefined): string {
  return code ? code.replace('x', ' × ') : '—';
}

/**
 * La phrase d'une ligne du suivi.
 *
 * <p>Composée ici, dans la langue de l'écran, à partir des codes que le serveur
 * garde : une phrase stockée en base serait figée en français.
 */
export function eventText(e: RegisterEvent, kind: 'risk' | 'opportunity'): string {
  switch (e.type) {
    case 'CREATED': {
      const origine = originLabel(e.toValue as RegisterOrigin);
      const ref = e.detail ? ` ${e.detail}` : '';
      return kind === 'risk'
        ? $localize`:@@rr.event.risk-created:Risque créé — origine : ${origine}:origin:${ref}:ref:`
        : $localize`:@@rr.event.opportunity-created:Opportunité créée — origine : ${origine}:origin:${ref}:ref:`;
    }
    case 'RATING_CHANGED': {
      const avant = ratingText(e.fromValue);
      const apres = ratingText(e.toValue);
      return kind === 'risk'
        ? $localize`:@@rr.event.rating:Cotation brute passée de ${avant}:from: à ${apres}:to:`
        : $localize`:@@rr.event.evaluation:Évaluation passée de ${avant}:from: à ${apres}:to:`;
    }
    case 'RESIDUAL_CHANGED': {
      const avant = ratingText(e.fromValue);
      const apres = ratingText(e.toValue);
      return $localize`:@@rr.event.residual:Cotation résiduelle visée : ${avant}:from: → ${apres}:to:`;
    }
    case 'STATUS_CHANGED': {
      const avant = kind === 'risk' ? riskStatusLabel(e.fromValue as RiskStatus)
        : opportunityStatusLabel(e.fromValue as OpportunityStatus);
      const apres = kind === 'risk' ? riskStatusLabel(e.toValue as RiskStatus)
        : opportunityStatusLabel(e.toValue as OpportunityStatus);
      return $localize`:@@rr.event.status:Statut : ${avant}:from: → ${apres}:to:`;
    }
    case 'DECISION_CHANGED': {
      const avant = kind === 'risk' ? riskDecisionLabel(e.fromValue as RiskDecision)
        : opportunityDecisionLabel(e.fromValue as OpportunityDecision);
      const apres = kind === 'risk' ? riskDecisionLabel(e.toValue as RiskDecision)
        : opportunityDecisionLabel(e.toValue as OpportunityDecision);
      return $localize`:@@rr.event.decision:Décision : ${avant}:from: → ${apres}:to:`;
    }
    case 'ACTION_OPENED': {
      const titre = e.detail ?? '';
      return kind === 'risk'
        ? $localize`:@@rr.event.capa-opened:Action CAPA ouverte : ${titre}:title:`
        : $localize`:@@rr.event.action-opened:Action ${e.toValue ?? ''}:number: ouverte : ${titre}:title:`;
    }
    default:
      return String(e.type);
  }
}
