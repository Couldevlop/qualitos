import { AuthUser } from '../../core/auth/auth.service';
import { AuditPlanResponse } from './audits.types';

/**
 * Le nom à montrer pour l'auditeur responsable d'un audit — jamais un identifiant.
 *
 * Ordre de lecture : le nom recopié à la désignation ; à défaut, si l'auditeur
 * est l'utilisateur connecté, son nom de session ; à défaut, le nom que
 * l'annuaire donne à son compte (`resolvedName`) ; sinon un libellé neutre.
 * Les audits créés avant la recopie du nom n'en portent pas, et afficher leur
 * identifiant brut était précisément ce qui rendait le champ incompréhensible.
 */
export function leadAuditorDisplay(
  plan: Pick<AuditPlanResponse, 'leadAuditorId' | 'leadAuditorName'>,
  me: AuthUser | null,
  resolvedName?: string
): string {
  if (plan.leadAuditorName) return plan.leadAuditorName;
  if (me && plan.leadAuditorId && plan.leadAuditorId === me.userId) return me.displayName;
  if (resolvedName) return resolvedName;
  return $localize`:@@audits.lead-auditor.unknown:Membre de l'organisation (nom non enregistré)`;
}
