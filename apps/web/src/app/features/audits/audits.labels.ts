import { AuditType, FindingType } from './audits.types';

/**
 * Libellés des types d'audit et de constat pour l'AFFICHAGE (liste, planning,
 * fiche) : ces écrans montraient le code de l'API (« INTERNAL », « MAJOR_NC »),
 * qui ne se traduit dans aucune langue. Les sélecteurs des dialogues gardent
 * leurs propres listes, avec la version longue de LPA.
 */
const AUDIT_TYPES: Readonly<Record<AuditType, string>> = {
  INTERNAL: $localize`:@@audits.type.internal:Interne`,
  EXTERNAL: $localize`:@@audits.type.external:Externe`,
  SUPPLIER: $localize`:@@audits.type.supplier:Fournisseur`,
  LPA: $localize`:@@audits.type.lpa-short:LPA`,
  CERTIFICATION: $localize`:@@audits.type.certification:Certification`,
  SURVEILLANCE: $localize`:@@audits.type.surveillance:Surveillance`
};

const FINDING_TYPES: Readonly<Record<FindingType, string>> = {
  MAJOR_NC: $localize`:@@audits.finding.major-nc:Non-conformité majeure`,
  MINOR_NC: $localize`:@@audits.finding.minor-nc:Non-conformité mineure`,
  OBSERVATION: $localize`:@@audits.finding.observation:Observation`,
  OPPORTUNITY: $localize`:@@audits.finding.opportunity:Opportunité`,
  CONFORMITY: $localize`:@@audits.finding.conformity:Conformité`
};

/** Libellé d'un type d'audit ; la valeur brute si elle est inconnue, un tiret si elle manque. */
export function auditTypeLabel(type: AuditType | string | undefined | null): string {
  if (!type) return '—';
  return AUDIT_TYPES[type as AuditType] ?? String(type);
}

/** Libellé d'un type de constat ; la valeur brute si elle est inconnue, un tiret si elle manque. */
export function findingTypeLabel(type: FindingType | string | undefined | null): string {
  if (!type) return '—';
  return FINDING_TYPES[type as FindingType] ?? String(type);
}
