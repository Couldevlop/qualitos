import { StatusTone } from '../../shared/ui/status-pill/status-pill.component';
import {
  CustomerDecision, DerogationStatus, DeviationOrigin, DurationKind, Occurrence, ShipmentStage
} from './derogations.types';

/**
 * Les textes affichés du dossier de dérogation.
 *
 * Les libellés d'état reprennent **mot pour mot** ceux de la maquette : c'est
 * le vocabulaire que les utilisateurs ont validé, et « Approuvée par la
 * qualité » dit autre chose que « Signée » — la première nomme qui engage sa
 * responsabilité, la seconde décrit un geste.
 */

const STATUS: Record<DerogationStatus, string> = {
  DRAFT: $localize`:@@derogation.status.draft:Brouillon`,
  PENDING_INTERNAL: $localize`:@@derogation.status.pending-internal:En attente du manager qualité`,
  TO_AMEND: $localize`:@@derogation.status.to-amend:À modifier`,
  QUALITY_APPROVED: $localize`:@@derogation.status.quality-approved:Approuvée par la qualité`,
  PENDING_CUSTOMER: $localize`:@@derogation.status.pending-customer:En attente du client`,
  ACCEPTED: $localize`:@@derogation.status.accepted:Acceptée`,
  REFUSED: $localize`:@@derogation.status.refused:Refusée`
};

/**
 * La couleur de la pastille d'état.
 *
 * « À modifier » est un AVERTISSEMENT et non une erreur : le dossier n'est pas
 * rejeté, il attend une correction. Le refus du client, lui, est définitif.
 */
const STATUS_TONE: Record<DerogationStatus, StatusTone> = {
  DRAFT: 'neutral',
  PENDING_INTERNAL: 'accent',
  TO_AMEND: 'warn',
  QUALITY_APPROVED: 'success',
  PENDING_CUSTOMER: 'accent',
  ACCEPTED: 'success',
  REFUSED: 'danger'
};

const DECISION: Record<CustomerDecision, string> = {
  PENDING: $localize`:@@derogation.decision.pending:En attente`,
  ACCEPTED: $localize`:@@derogation.decision.accepted:Acceptée`,
  ACCEPTED_WITH_CONDITIONS:
    $localize`:@@derogation.decision.accepted-conditions:Acceptée sous conditions`,
  REFUSED: $localize`:@@derogation.decision.refused:Refusée`
};

const SHIPMENT: Record<ShipmentStage, string> = {
  TO_SHIP: $localize`:@@derogation.shipment.to-ship:Pièces à expédier`,
  ALREADY_SHIPPED: $localize`:@@derogation.shipment.already-shipped:Pièces déjà expédiées`
};

const ORIGIN: Record<DeviationOrigin, string> = {
  PRODUCT: $localize`:@@derogation.origin.product:Lié au produit`,
  PROCESS: $localize`:@@derogation.origin.process:Lié au process`
};

const OCCURRENCE: Record<Occurrence, string> = {
  FIRST: $localize`:@@derogation.occurrence.first:Première occurrence`,
  REPEAT: $localize`:@@derogation.occurrence.repeat:Répétition`
};

const DURATION: Record<DurationKind, string> = {
  TEMPORARY: $localize`:@@derogation.duration.temporary:Temporaire`,
  PERMANENT: $localize`:@@derogation.duration.permanent:Permanente`
};

export function statusLabel(status: DerogationStatus): string {
  return STATUS[status] ?? status;
}

export function statusTone(status: DerogationStatus): StatusTone {
  return STATUS_TONE[status] ?? 'neutral';
}

export function decisionLabel(decision: CustomerDecision): string {
  return DECISION[decision] ?? decision;
}

export function shipmentLabel(value: ShipmentStage): string {
  return SHIPMENT[value] ?? value;
}

export function originLabel(value: DeviationOrigin): string {
  return ORIGIN[value] ?? value;
}

export function occurrenceLabel(value: Occurrence): string {
  return OCCURRENCE[value] ?? value;
}

export function durationLabel(value: DurationKind): string {
  return DURATION[value] ?? value;
}

/** Les états proposés au filtre, dans l'ordre du cycle et non alphabétique. */
export const STATUS_ORDER: DerogationStatus[] = [
  'DRAFT', 'PENDING_INTERNAL', 'TO_AMEND', 'QUALITY_APPROVED',
  'PENDING_CUSTOMER', 'ACCEPTED', 'REFUSED'
];

/** Les réponses qu'on peut porter au dossier : « En attente » n'en est pas une. */
export const DECIDABLE: CustomerDecision[] = [
  'ACCEPTED', 'ACCEPTED_WITH_CONDITIONS', 'REFUSED'
];

export const SHIPMENT_ORDER: ShipmentStage[] = ['TO_SHIP', 'ALREADY_SHIPPED'];
export const ORIGIN_ORDER: DeviationOrigin[] = ['PRODUCT', 'PROCESS'];
export const OCCURRENCE_ORDER: Occurrence[] = ['FIRST', 'REPEAT'];
export const DURATION_ORDER: DurationKind[] = ['TEMPORARY', 'PERMANENT'];
