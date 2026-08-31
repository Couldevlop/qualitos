import { CapaType } from './capa.types';

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
