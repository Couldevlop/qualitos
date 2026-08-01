import {
  AiRiskClassification,
  AiSystemStatus,
  AiSystemView
} from './ai-systems.types';

/**
 * Règles de cycle de vie du registre AI Act, recopiées de l'agrégat serveur
 * (`AiSystem` : table ALLOWED, validateNotProhibited, validateInUseInvariants).
 *
 * POURQUOI dupliquer une règle serveur côté écran : le serveur reste seul juge —
 * ces prédicats ne servent qu'à ne pas proposer une action qui serait refusée.
 * Un bouton affiché doit agir ; un bouton qui provoquerait un 409 n'a rien à faire
 * dans l'interface.
 */

/** Prérequis de mise en service imposés par le domaine (Art. 13, 14, 43, 50). */
export type InUseRequirementKey =
  | 'conformity-evidence'
  | 'human-oversight'
  | 'transparency';

export interface InUseRequirement {
  key: InUseRequirementKey;
  satisfied: boolean;
}

/** Champs consultés par les invariants de mise en service. */
export interface InUseEvidence {
  conformityAssessmentEvidenceUrl: string | null;
  humanOversightDescription: string | null;
  transparencyMeasures: string | null;
}

function filled(v: string | null | undefined): boolean {
  return !!v && v.trim().length > 0;
}

/**
 * Obligations à satisfaire AVANT la mise en service, selon la classification.
 *
 * Miroir de `AiRiskClassification` : HIGH exige l'évaluation de conformité et la
 * supervision humaine ; HIGH et LIMITED exigent les mesures de transparence.
 * Retourne la liste complète (satisfaites ou non) pour que l'écran affiche une
 * check-list, pas seulement un refus.
 */
export function inUseRequirements(
  risk: AiRiskClassification,
  evidence: InUseEvidence
): InUseRequirement[] {
  const requirements: InUseRequirement[] = [];
  if (risk === 'HIGH') {
    requirements.push(
      { key: 'conformity-evidence', satisfied: filled(evidence.conformityAssessmentEvidenceUrl) },
      { key: 'human-oversight', satisfied: filled(evidence.humanOversightDescription) }
    );
  }
  if (risk === 'HIGH' || risk === 'LIMITED') {
    requirements.push({ key: 'transparency', satisfied: filled(evidence.transparencyMeasures) });
  }
  return requirements;
}

export function missingInUseRequirements(system: AiSystemView): InUseRequirement[] {
  return inUseRequirements(system.riskClassification, system).filter(r => !r.satisfied);
}

/**
 * Pratique interdite (Art. 5). On croise le drapeau serveur et la classification :
 * un payload plus ancien qui n'exposerait pas `prohibited` ne doit pas rouvrir
 * l'enregistrement d'un système inacceptable.
 */
export function isProhibited(system: AiSystemView): boolean {
  return system.prohibited || system.riskClassification === 'UNACCEPTABLE';
}

/** Seul un brouillon est modifiable : après enregistrement la fiche est figée. */
export function canEdit(system: AiSystemView): boolean {
  return system.status === 'DRAFT';
}

/** Idem pour la suppression : les autres états sont conservés pour l'audit. */
export function canDelete(system: AiSystemView): boolean {
  return system.status === 'DRAFT';
}

export function canRegister(system: AiSystemView): boolean {
  return system.status === 'DRAFT' && !isProhibited(system);
}

export function canPutInUse(system: AiSystemView): boolean {
  return system.status === 'REGISTERED'
    && !isProhibited(system)
    && missingInUseRequirements(system).length === 0;
}

export function canDecommission(system: AiSystemView): boolean {
  return system.status === 'IN_USE';
}

export function canWithdraw(system: AiSystemView): boolean {
  return system.status === 'DRAFT' || system.status === 'REGISTERED';
}

/**
 * Impasse : le système est enregistré mais il lui manque des obligations, et la
 * fiche n'est plus modifiable. Le seul chemin restant est le retrait puis une
 * nouvelle fiche — l'écran doit le dire, pas laisser l'utilisateur chercher.
 */
export function isStuckBeforeUse(system: AiSystemView): boolean {
  return system.status === 'REGISTERED'
    && !isProhibited(system)
    && missingInUseRequirements(system).length > 0;
}

/** Aucune transition possible : la fiche n'est plus qu'une archive. */
export function isTerminal(status: AiSystemStatus): boolean {
  return status === 'DECOMMISSIONED' || status === 'WITHDRAWN';
}
