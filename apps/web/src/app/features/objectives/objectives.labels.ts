import { StatusTone } from '../../shared/ui/status-pill/status-pill.component';
import { ObjectiveLevel, ObjectiveStatus, ProposalStatus } from './objectives.types';

/**
 * Les libellés des objectifs qualité.
 *
 * <p>Écrits ici une fois : la revue et la fenêtre de création afficheraient
 * sinon la valeur brute de l'énumération (« NOT_EVALUATED »), qui ne se traduit
 * dans aucune langue. Les traductions vivent dans
 * `scripts/i18n/objectifs_qualite.py`.
 */
const STATUTS: Readonly<Record<ObjectiveStatus, string>> = {
  NOT_EVALUATED: $localize`:@@objectives.status.not-evaluated:Non mesuré`,
  ON_TARGET: $localize`:@@objectives.status.on-target:Atteint`,
  WARNING: $localize`:@@objectives.status.warning:Alerte`,
  CRITICAL: $localize`:@@objectives.status.critical:Critique`
};

/**
 * La teinte suit la gravité, et « non mesuré » reste neutre : le peindre en vert
 * ferait passer un objectif qu'on ne mesure pas pour un objectif tenu.
 */
const TEINTES: Readonly<Record<ObjectiveStatus, StatusTone>> = {
  NOT_EVALUATED: 'neutral',
  ON_TARGET: 'success',
  WARNING: 'warn',
  CRITICAL: 'danger'
};

const NIVEAUX: Readonly<Record<ObjectiveLevel, string>> = {
  WARNING: $localize`:@@objectives.level.warning:Alerte`,
  CRITICAL: $localize`:@@objectives.level.critical:Critique`
};

const DECISIONS: Readonly<Record<ProposalStatus, string>> = {
  PENDING: $localize`:@@objectives.proposal.pending:À trancher`,
  ACCEPTED: $localize`:@@objectives.proposal.accepted:Acceptée`,
  REJECTED: $localize`:@@objectives.proposal.rejected:Rejetée`
};

/** Les quatre périodes admises, pour la liste déroulante. */
export const PERIODES: ReadonlyArray<{ months: number; label: string }> = [
  { months: 1, label: $localize`:@@objectives.period.1:1 mois` },
  { months: 3, label: $localize`:@@objectives.period.3:3 mois` },
  { months: 6, label: $localize`:@@objectives.period.6:6 mois` },
  { months: 12, label: $localize`:@@objectives.period.12:12 mois` }
];

export function objectiveStatusLabel(status: ObjectiveStatus | null | undefined): string {
  return status ? STATUTS[status] ?? status : '';
}

export function objectiveStatusTone(status: ObjectiveStatus | null | undefined): StatusTone {
  return status ? TEINTES[status] ?? 'neutral' : 'neutral';
}

export function objectiveLevelLabel(level: ObjectiveLevel | null | undefined): string {
  return level ? NIVEAUX[level] ?? level : '';
}

export function proposalStatusLabel(status: ProposalStatus | null | undefined): string {
  return status ? DECISIONS[status] ?? status : '';
}

export function periodLabel(months: number | null | undefined): string {
  return PERIODES.find(p => p.months === months)?.label ?? String(months ?? '');
}
