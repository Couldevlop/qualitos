import { CapaSourceType, CapaType } from './capa.types';

/**
 * Libellés des raisons d'ouverture d'un dossier CAPA.
 *
 * <p>Écrits ici une fois : le dialogue de création, la liste et la fiche
 * affichaient jusqu'ici la valeur brute de l'énumération (« CORRECTIVE »), ce
 * qui ne se traduit dans aucune langue et se lit mal partout.
 */
export const CAPA_TYPES: ReadonlyArray<{ value: CapaType; label: string }> = [
  { value: 'CONTAINMENT', label: $localize`:@@capa.type.containment:Endiguement` },
  { value: 'CORRECTIVE',  label: $localize`:@@capa.type.corrective:Corrective` },
  { value: 'PREVENTIVE',  label: $localize`:@@capa.type.preventive:Préventive` }
];

/** Libellé lisible d'un type de dossier ; la valeur brute si elle est inconnue. */
export function capaTypeLabel(type: CapaType | string | undefined | null): string {
  if (!type) return '—';
  return CAPA_TYPES.find(t => t.value === type)?.label ?? String(type);
}

/**
 * Libellés des sources d'un dossier. La création libre depuis la liste n'en
 * propose qu'une partie (`manual`) : un dossier de risque, de carte de contrôle
 * ou d'anomalie ne s'ouvre que depuis son écran d'origine, qui le rattache.
 */
export const CAPA_SOURCES: ReadonlyArray<{ value: CapaSourceType; label: string; manual: boolean }> = [
  { value: 'NON_CONFORMITY', label: $localize`:@@capa.source.non-conformity:Non-conformité`, manual: true },
  { value: 'AUDIT',          label: $localize`:@@capa.source.audit:Audit`, manual: true },
  { value: 'COMPLAINT',      label: $localize`:@@capa.source.complaint:Réclamation client`, manual: true },
  { value: 'INTERNAL',       label: $localize`:@@capa.source.internal:Détection interne`, manual: true },
  { value: 'IOT_ALERT',      label: $localize`:@@capa.source.iot-alert:Alerte IoT`, manual: true },
  { value: 'SPC_ALERT',      label: $localize`:@@capa.source.spc-alert:Carte de contrôle`, manual: false },
  { value: 'ANOMALY',        label: $localize`:@@capa.source.anomaly:Anomalie détectée`, manual: false },
  { value: 'RISK',           label: $localize`:@@capa.source.risk:Risque`, manual: false },
  { value: 'OTHER',          label: $localize`:@@capa.source.other:Autre`, manual: true }
];

/** Libellé lisible d'une source ; la valeur brute si elle est inconnue. */
export function capaSourceLabel(source: CapaSourceType | string | undefined | null): string {
  if (!source) return '—';
  return CAPA_SOURCES.find(s => s.value === source)?.label ?? String(source);
}

/**
 * Le nom du pilote d'un dossier — jamais un identifiant (ADR 0088).
 *
 * Ordre : le nom recopié à la désignation ; à défaut celui que l'annuaire
 * (ADR 0091) donne à ce compte, via `nameOf` ; sinon « — ».
 */
export function capaOwnerLabel(
  c: { ownerId?: string; ownerName?: string },
  nameOf: (accountId: string) => string | undefined
): string {
  if (c.ownerName && c.ownerName.trim()) return c.ownerName;
  return (c.ownerId ? nameOf(c.ownerId) : undefined) ?? '—';
}

/** Les comptes des pilotes dont le dossier n'a pas recopié le nom. */
export function unnamedOwners(cases: ReadonlyArray<{ ownerId?: string; ownerName?: string }>): string[] {
  return cases.filter(c => !!c.ownerId && !c.ownerName?.trim()).map(c => c.ownerId as string);
}
