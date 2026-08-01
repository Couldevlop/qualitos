import { CommProvider, ConnectorStatus, EhrAuthMode, EhrProvider, ErpProvider } from './connectors.types';

/** Tonalité de rendu partagée par les badges et les tuiles de l'écran. */
export type ConnectorTone = 'neutral' | 'success' | 'warn' | 'danger';

/**
 * Libellés partagés par l'écran et les trois formulaires.
 *
 * Centralisés ici pour qu'un statut soit nommé de la même façon dans le tableau,
 * dans le sélecteur du formulaire et dans les messages : trois formulations
 * différentes pour le même état serveur rendraient l'écran illisible.
 */
export function connectorStatusLabel(status: ConnectorStatus): string {
  switch (status) {
    case 'ACTIVE': return $localize`:@@connectors.status-active:Active`;
    case 'DISABLED': return $localize`:@@connectors.status-disabled:Désactivée`;
    default: return $localize`:@@connectors.status-disabled-on-errors:Désactivée sur erreurs`;
  }
}

export function connectorStatusTone(status: ConnectorStatus): ConnectorTone {
  switch (status) {
    case 'ACTIVE': return 'success';
    case 'DISABLED': return 'neutral';
    default: return 'danger';
  }
}

/**
 * Noms commerciaux des fournisseurs : ce sont des marques, elles ne se traduisent
 * pas. Une valeur inconnue (référentiel serveur enrichi avant le front) est rendue
 * telle quelle plutôt que masquée.
 */
export function erpProviderLabel(provider: ErpProvider): string {
  switch (provider) {
    case 'SAP': return 'SAP S/4HANA';
    case 'ORACLE_FUSION': return 'Oracle Fusion Cloud';
    case 'DYNAMICS': return 'Microsoft Dynamics 365';
    default: return provider;
  }
}

export function ehrProviderLabel(provider: EhrProvider): string {
  switch (provider) {
    case 'FHIR_R4': return 'HL7 FHIR R4';
    case 'FHIR_R5': return 'HL7 FHIR R5';
    default: return provider;
  }
}

export function commProviderLabel(provider: CommProvider): string {
  switch (provider) {
    case 'TEAMS': return 'Microsoft Teams';
    case 'SLACK': return 'Slack';
    case 'MATTERMOST': return 'Mattermost';
    default: return provider;
  }
}

/** Mode d'authentification FHIR : l'en-tête réellement envoyé par le serveur. */
export function ehrAuthModeLabel(mode: EhrAuthMode): string {
  return mode === 'BEARER'
    ? $localize`:@@connectors.ehr.auth-bearer:Bearer (OAuth2 / SMART-on-FHIR)`
    : $localize`:@@connectors.ehr.auth-basic:Basic (utilisateur + secret)`;
}
