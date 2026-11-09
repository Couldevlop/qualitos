import { PokaYokeMechanism, PokaYokeType } from './dmaic.types';

/**
 * Les mots des détrompeurs. Le serveur rend des codes (`PHYSICAL_SHAPE`,
 * `INTERLOCK`) ; ils deviennent ici des mots dans la langue de l'écran — un
 * code brut sur un écran de qualité n'apprend rien à qui le lit.
 *
 * <p>Un code inconnu s'affiche tel quel : la plateforme peut en apprendre un
 * nouveau avant que l'écran ne soit traduit, et mieux vaut un code lisible
 * qu'une case vide.
 */

const TYPE: Readonly<Record<PokaYokeType, string>> = {
  PREVENTION: $localize`:@@pokayoke.type.prevention:Prévention`,
  DETECTION: $localize`:@@pokayoke.type.detection:Détection`
};

const MECHANISM: Readonly<Record<PokaYokeMechanism, string>> = {
  PHYSICAL_SHAPE: $localize`:@@pokayoke.mech.physical-shape:Forme physique`,
  INTERLOCK: $localize`:@@pokayoke.mech.interlock:Verrouillage`,
  LIMIT_SWITCH: $localize`:@@pokayoke.mech.limit-switch:Fin de course`,
  SENSOR: $localize`:@@pokayoke.mech.sensor:Capteur`,
  VISION: $localize`:@@pokayoke.mech.vision:Vision`,
  CHECKLIST: $localize`:@@pokayoke.mech.checklist:Liste de vérification`,
  COLOR_CODING: $localize`:@@pokayoke.mech.color-coding:Code couleur`,
  POSITION_REFERENCE: $localize`:@@pokayoke.mech.position-reference:Référence de position`,
  COUNTER: $localize`:@@pokayoke.mech.counter:Comptage`,
  SOFTWARE_VALIDATION: $localize`:@@pokayoke.mech.software-validation:Contrôle logiciel`,
  OTHER: $localize`:@@pokayoke.mech.other:Autre`
};

export const POKAYOKE_TYPES: readonly PokaYokeType[] = ['PREVENTION', 'DETECTION'];

export const POKAYOKE_MECHANISMS: readonly PokaYokeMechanism[] = [
  'PHYSICAL_SHAPE', 'INTERLOCK', 'LIMIT_SWITCH', 'SENSOR', 'VISION',
  'CHECKLIST', 'COLOR_CODING', 'POSITION_REFERENCE', 'COUNTER',
  'SOFTWARE_VALIDATION', 'OTHER'
];

export function pokaYokeTypeLabel(code: PokaYokeType | string | null | undefined): string {
  if (!code) return '';
  return TYPE[code as PokaYokeType] ?? code;
}

export function pokaYokeMechanismLabel(code: PokaYokeMechanism | string | null | undefined): string {
  if (!code) return '';
  return MECHANISM[code as PokaYokeMechanism] ?? code;
}
