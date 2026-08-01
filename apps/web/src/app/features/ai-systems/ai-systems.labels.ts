import { InUseRequirementKey } from './ai-systems.rules';
import { AiRiskClassification, AiSystemRole, AiSystemStatus } from './ai-systems.types';

/**
 * Libellés et tons du registre AI Act.
 *
 * POURQUOI des fonctions plutôt qu'un dictionnaire consommé dans le template :
 * `*matCellDef="let s"` type la ligne en `any`, et indexer un `Record<Union, …>`
 * avec une clé `any` est refusé par `strictTemplates`. Une fonction typée passe.
 */

export type Tone = 'neutral' | 'info' | 'success' | 'warn' | 'danger';

export function riskLabel(risk: AiRiskClassification): string {
  switch (risk) {
    case 'UNACCEPTABLE': return $localize`:@@ai-systems.risk.unacceptable:Risque inacceptable`;
    case 'HIGH': return $localize`:@@ai-systems.risk.high:Haut risque`;
    case 'LIMITED': return $localize`:@@ai-systems.risk.limited:Risque limité`;
    default: return $localize`:@@ai-systems.risk.minimal:Risque minimal`;
  }
}

/** Base légale rappelée sous le libellé : c'est elle qui justifie les obligations. */
export function riskBasis(risk: AiRiskClassification): string {
  switch (risk) {
    case 'UNACCEPTABLE': return $localize`:@@ai-systems.risk-basis.unacceptable:Pratique interdite — AI Act Art. 5`;
    case 'HIGH': return $localize`:@@ai-systems.risk-basis.high:Annexe III — évaluation de conformité, supervision humaine, transparence`;
    case 'LIMITED': return $localize`:@@ai-systems.risk-basis.limited:Art. 50 — obligation d'informer l'utilisateur qu'il interagit avec une IA`;
    default: return $localize`:@@ai-systems.risk-basis.minimal:Usage libre — aucune obligation spécifique`;
  }
}

export function riskTone(risk: AiRiskClassification): Tone {
  switch (risk) {
    case 'UNACCEPTABLE': return 'danger';
    case 'HIGH': return 'warn';
    case 'LIMITED': return 'info';
    default: return 'success';
  }
}

export function statusLabel(status: AiSystemStatus): string {
  switch (status) {
    case 'DRAFT': return $localize`:@@ai-systems.status.draft:Brouillon`;
    case 'REGISTERED': return $localize`:@@ai-systems.status.registered:Enregistré`;
    case 'IN_USE': return $localize`:@@ai-systems.status.in-use:En service`;
    case 'DECOMMISSIONED': return $localize`:@@ai-systems.status.decommissioned:Retiré du service`;
    default: return $localize`:@@ai-systems.status.withdrawn:Abandonné`;
  }
}

export function statusTone(status: AiSystemStatus): Tone {
  switch (status) {
    case 'IN_USE': return 'success';
    case 'REGISTERED': return 'info';
    case 'DRAFT': return 'neutral';
    default: return 'warn';
  }
}

export function roleLabel(role: AiSystemRole): string {
  switch (role) {
    case 'PROVIDER': return $localize`:@@ai-systems.role.provider:Fournisseur`;
    case 'DEPLOYER': return $localize`:@@ai-systems.role.deployer:Déployeur`;
    case 'IMPORTER': return $localize`:@@ai-systems.role.importer:Importateur`;
    default: return $localize`:@@ai-systems.role.distributor:Distributeur`;
  }
}

export function requirementLabel(key: InUseRequirementKey): string {
  switch (key) {
    case 'conformity-evidence':
      return $localize`:@@ai-systems.requirement.conformity-evidence:Preuve d'évaluation de conformité (Art. 43)`;
    case 'human-oversight':
      return $localize`:@@ai-systems.requirement.human-oversight:Description de la supervision humaine (Art. 14)`;
    default:
      return $localize`:@@ai-systems.requirement.transparency:Mesures de transparence (Art. 13 et 50)`;
  }
}
