import { CoverageStatus, UpcomingKind } from './smi.types';

/**
 * Les mots du tableau de bord SMI. Le serveur rend des codes (chapitre « 6 »,
 * module « RISK_REGISTER », état « PARTIAL ») ; ils deviennent ici des mots
 * dans la langue de l'écran. Un code inconnu s'affiche tel quel.
 */

/** Les sept chapitres communs de la structure-cadre (Annexe SL). */
const CHAPTERS: Record<string, string> = {
  '4': $localize`:@@smi.chapter.4:Contexte de l'organisme`,
  '5': $localize`:@@smi.chapter.5:Leadership et politique`,
  '6': $localize`:@@smi.chapter.6:Risques, objectifs, exigences légales`,
  '7': $localize`:@@smi.chapter.7:Ressources, compétences, documents`,
  '8': $localize`:@@smi.chapter.8:Réalisation opérationnelle`,
  '9': $localize`:@@smi.chapter.9:Évaluation des performances`,
  '10': $localize`:@@smi.chapter.10:Amélioration`
};

const MODULES: Record<string, string> = {
  PROCESS_MAP: $localize`:@@smi.module.process-map:Cartographie`,
  RISK_REGISTER: $localize`:@@smi.module.risk-register:Risques`,
  DOCUMENTS: $localize`:@@smi.module.documents:GED`,
  POLICY: $localize`:@@smi.module.policy:Politique`,
  OBJECTIVES: $localize`:@@smi.module.objectives:Objectifs`,
  TRAINING: $localize`:@@smi.module.training:Compétences`,
  CALIBRATION: $localize`:@@smi.module.calibration:Métrologie`,
  APQP: $localize`:@@smi.module.apqp:APQP`,
  SPC: $localize`:@@smi.module.spc:SPC`,
  CHANGES: $localize`:@@smi.module.changes:MOC`,
  AUDITS: $localize`:@@smi.module.audits:Audits`,
  KPI: $localize`:@@smi.module.kpi:Indicateurs`,
  MANAGEMENT_REVIEW: $localize`:@@smi.module.management-review:Revue`,
  NC: $localize`:@@smi.module.nc:NC`,
  CAPA: $localize`:@@smi.module.capa:CAPA`
};

const COVERAGE: Record<CoverageStatus, string> = {
  COVERED: $localize`:@@smi.coverage.covered:Couvert`,
  PARTIAL: $localize`:@@smi.coverage.partial:Partiel`,
  GAP: $localize`:@@smi.coverage.gap:Écart`,
  NOT_APPLICABLE: $localize`:@@smi.coverage.not-applicable:Sans objet`
};

const UPCOMING: Record<UpcomingKind, string> = {
  CAPA_ACTION: $localize`:@@smi.upcoming.capa-action:Action CAPA`,
  CALIBRATION: $localize`:@@smi.upcoming.calibration:Étalonnage`,
  CHANGE: $localize`:@@smi.upcoming.change:Validation de changement`,
  RISK_REVIEW: $localize`:@@smi.upcoming.risk-review:Revue de risque`,
  AUDIT: $localize`:@@smi.upcoming.audit:Audit`
};

export const chapterLabel = (code: string): string => CHAPTERS[code] ?? code;
export const moduleLabel = (code: string): string => MODULES[code] ?? code;
export const coverageLabel = (s: CoverageStatus): string => COVERAGE[s] ?? s;
export const upcomingLabel = (k: UpcomingKind): string => UPCOMING[k] ?? k;

/** Où mène une échéance de la semaine. */
export function upcomingLink(kind: UpcomingKind, id: string): string[] {
  switch (kind) {
    case 'CAPA_ACTION': return ['/capa', id];
    case 'CALIBRATION': return ['/calibration', id];
    case 'CHANGE': return ['/changes', id];
    case 'RISK_REVIEW': return ['/risques', id];
    case 'AUDIT': return ['/audits', id];
  }
}

/** « Échu », « Aujourd'hui », « J-3 » : l'urgence d'une échéance en un mot. */
export function dueText(daysLeft: number): string {
  if (daysLeft < 0) return $localize`:@@smi.due.overdue:Échu`;
  if (daysLeft === 0) return $localize`:@@smi.due.today:Aujourd'hui`;
  return $localize`:@@smi.due.in-days:J-${daysLeft}:days:`;
}
