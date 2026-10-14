import { safeErrorMessage } from '../../core/http/error-message';
import {
  ImpactDomain,
  ImpactLevel,
  MocClassification,
  MocHistoryEvent,
  MocStatus,
  MocType,
  ValidationDecision
} from './moc.types';

/**
 * Les mots de la gestion des changements. Le serveur rend des codes ; ils
 * deviennent ici des mots dans la langue de l'écran.
 */

const STATUS: Record<MocStatus, string> = {
  DRAFT: $localize`:@@moc.status.draft:Brouillon`,
  IN_ANALYSIS: $localize`:@@moc.status.in-analysis:En analyse`,
  IN_VALIDATION: $localize`:@@moc.status.in-validation:En validation`,
  APPROVED: $localize`:@@moc.status.approved:Approuvé`,
  REJECTED: $localize`:@@moc.status.rejected:Rejeté`
};

const CLASSIFICATION: Record<MocClassification, string> = {
  NOT_ASSESSED: $localize`:@@moc.class.not-assessed:Non évalué`,
  MINOR: $localize`:@@moc.class.minor:Mineur`,
  MAJOR: $localize`:@@moc.class.major:Majeur`,
  CRITICAL: $localize`:@@moc.class.critical:Critique`
};

const LEVEL: Record<ImpactLevel, string> = {
  NONE: $localize`:@@moc.level.none:Aucun`,
  LOW: $localize`:@@moc.level.low:Faible`,
  MEDIUM: $localize`:@@moc.level.medium:Moyen`,
  HIGH: $localize`:@@moc.level.high:Élevé`
};

const DOMAIN: Record<ImpactDomain, string> = {
  SAFETY: $localize`:@@moc.domain.safety:Sécurité`,
  HEALTH_ENVIRONMENT: $localize`:@@moc.domain.health-environment:Santé et environnement`,
  QUALITY: $localize`:@@moc.domain.quality:Qualité`,
  PRODUCTION: $localize`:@@moc.domain.production:Production`,
  MAINTENANCE: $localize`:@@moc.domain.maintenance:Maintenance`,
  COSTS: $localize`:@@moc.domain.costs:Coûts`,
  REGULATORY: $localize`:@@moc.domain.regulatory:Réglementaire`,
  TRAINING: $localize`:@@moc.domain.training:Formation`
};

const TYPE: Record<MocType, string> = {
  PERMANENT: $localize`:@@moc.type.permanent:Permanent`,
  TEMPORARY: $localize`:@@moc.type.temporary:Temporaire`
};

const DECISION: Record<ValidationDecision, string> = {
  PENDING: $localize`:@@moc.decision.pending:En attente`,
  APPROVED: $localize`:@@moc.decision.approved:Approuvé`,
  REJECTED: $localize`:@@moc.decision.rejected:Rejeté`
};

const HISTORY: Record<MocHistoryEvent, string> = {
  CREATED: $localize`:@@moc.history.created:Demande créée`,
  UPDATED: $localize`:@@moc.history.updated:Demande modifiée`,
  SENT_TO_ANALYSIS: $localize`:@@moc.history.sent-to-analysis:Envoyée en analyse`,
  ANALYSIS_COMPLETED: $localize`:@@moc.history.analysis-completed:Analyse d'impact terminée`,
  SUBMITTED: $localize`:@@moc.history.submitted:Soumise à validation`,
  VALIDATION_APPROVED: $localize`:@@moc.history.validation-approved:Validation accordée`,
  VALIDATION_REJECTED: $localize`:@@moc.history.validation-rejected:Validation refusée`,
  APPROVED: $localize`:@@moc.history.approved:Demande approuvée`,
  REJECTED: $localize`:@@moc.history.rejected:Demande rejetée`,
  REOPENED: $localize`:@@moc.history.reopened:Analyse rouverte`,
  REMINDER_SENT: $localize`:@@moc.history.reminder-sent:Rappel envoyé aux validateurs`,
  LEGACY_STATUS: $localize`:@@moc.history.legacy-status:Statut antérieur au MOC`,
  LEGACY_DECISION: $localize`:@@moc.history.legacy-decision:Décision antérieure au MOC`,
  LEGACY_IMPACT: $localize`:@@moc.history.legacy-impact:Objet lié avant le MOC`
};

export const mocStatusLabel = (s: MocStatus): string => STATUS[s] ?? s;
export const mocClassificationLabel = (c: MocClassification): string => CLASSIFICATION[c] ?? c;
export const mocLevelLabel = (l: ImpactLevel): string => LEVEL[l] ?? l;
export const mocDomainLabel = (d: ImpactDomain): string => DOMAIN[d] ?? d;
export const mocTypeLabel = (t: MocType): string => TYPE[t] ?? t;
export const mocDecisionLabel = (d: ValidationDecision): string => DECISION[d] ?? d;
export const mocHistoryLabel = (e: MocHistoryEvent): string => HISTORY[e] ?? e;

/**
 * Ce que l'utilisateur lit quand le serveur refuse : le code stable du refus,
 * traduit, et le rôle qu'il nomme. Jamais le message brut du serveur (OWASP A09).
 */
export function mocErrorMessage(err: unknown, fallback: string): string {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const body = (err as any)?.error;
  const code: unknown = body?.code;
  const role: string = typeof body?.role === 'string' ? body.role : '';
  switch (code) {
    case 'moc.analysis-incomplete':
      return $localize`:@@moc.error.analysis-incomplete:Évaluez les huit domaines d'impact avant de soumettre à validation.`;
    case 'moc.role-without-holder':
      return $localize`:@@moc.error.role-without-holder:Le rôle « ${role}:role: » n'a aucun titulaire : demandez à l'administrateur d'en nommer un.`;
    case 'moc.role-only-requester':
      return $localize`:@@moc.error.role-only-requester:Le rôle « ${role}:role: » n'a pour titulaire que le demandeur, qui ne peut pas valider sa propre demande.`;
    case 'moc.not-holder':
      return $localize`:@@moc.error.not-holder:Seul un titulaire du rôle « ${role}:role: » peut statuer sur cette carte.`;
    case 'moc.not-owner':
      return $localize`:@@moc.error.not-owner:Seul le demandeur ou un gestionnaire des changements peut conduire cette demande.`;
    case 'moc.self-validation':
      return $localize`:@@moc.error.self-validation:Vous êtes le demandeur : vous ne pouvez pas valider votre propre demande.`;
    case 'moc.comment-required':
      return $localize`:@@moc.error.comment-required:Un refus doit être motivé par un commentaire.`;
    case 'moc.already-decided':
      return $localize`:@@moc.error.already-decided:Ce rôle a déjà statué.`;
    case 'moc.delete-not-draft':
      return $localize`:@@moc.error.delete-not-draft:Seule une demande en brouillon peut être supprimée.`;
    case 'moc.settings.role-in-use':
      return $localize`:@@moc.error.role-in-use:Le rôle « ${role}:role: » est attendu par une demande en validation : il ne peut pas être retiré.`;
    case 'moc.settings.role-in-use-holders':
      return $localize`:@@moc.error.role-in-use-holders:Le rôle « ${role}:role: » est attendu par une demande en validation : il doit garder au moins un titulaire.`;
    case 'moc.settings.role-duplicate':
      return $localize`:@@moc.error.role-duplicate:Deux rôles portent le même libellé « ${role}:role: ».`;
    case 'moc.settings.required-empty':
      return $localize`:@@moc.error.required-empty:Chaque classification doit exiger au moins un rôle de validation.`;
    case 'moc.settings.thresholds':
      return $localize`:@@moc.error.thresholds:Les seuils doivent respecter 0 < seuil majeur < seuil critique ≤ 100.`;
    case 'moc.settings.weights-zero':
      return $localize`:@@moc.error.weights-zero:Au moins un domaine doit avoir un poids supérieur à 0.`;
    default:
      return safeErrorMessage(err, fallback);
  }
}
