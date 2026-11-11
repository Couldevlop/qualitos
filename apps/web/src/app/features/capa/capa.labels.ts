import { catalogLabel, catalogOptions } from '../../core/catalogs/catalog-overrides';
import { CapaActionType, CapaCriticity, CapaSourceType, CapaType } from './capa.types';

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
  // Le mot du client passe devant (ADR 0111).
  return catalogLabel('capa-nature', type)
    ?? CAPA_TYPES.find(t => t.value === type)?.label
    ?? String(type);
}

/** Les natures proposées à l'ouverture d'un dossier, dans les mots du client. */
export function capaTypeOptions(): Array<{ value: CapaType; label: string }> {
  return catalogOptions('capa-nature', CAPA_TYPES);
}

/** Libellés des criticités ; la liste et la fiche affichaient « HIGH », « CRITICAL ». */
/**
 * Les natures d'action, dans l'ordre du déroulé réel d'un traitement :
 * on endigue, on corrige, puis on prévient.
 *
 * <p>Ici et non dans un composant : la fiche CAPA et le panneau lu depuis la
 * fiche d'un écart affichent la même nature, et deux listes divergeraient au
 * premier libellé retouché d'un seul côté.
 */
export const CAPA_ACTION_TYPES: ReadonlyArray<{ value: CapaActionType; label: string }> = [
  { value: 'CONTAINMENT', label: $localize`:@@capa.action-type.containment:Endiguement` },
  { value: 'CORRECTIVE',  label: $localize`:@@capa.action-type.corrective:Corrective` },
  { value: 'PREVENTIVE',  label: $localize`:@@capa.action-type.preventive:Préventive` }
];

/** Libellé lisible d'une nature d'action, avec repli sur le code inconnu. */
export function actionTypeLabel(type: CapaActionType | string | undefined | null): string {
  if (!type) {
    return '';
  }
  return CAPA_ACTION_TYPES.find(t => t.value === type)?.label ?? String(type);
}

export const CAPA_CRITICITIES: ReadonlyArray<{ value: CapaCriticity; label: string }> = [
  { value: 'LOW',      label: $localize`:@@capa.open.criticity-low:Faible` },
  { value: 'MEDIUM',   label: $localize`:@@capa.open.criticity-medium:Moyenne` },
  { value: 'HIGH',     label: $localize`:@@capa.open.criticity-high:Élevée` },
  { value: 'CRITICAL', label: $localize`:@@capa.open.criticity-critical:Critique` }
];

/** Libellé lisible d'une criticité ; la valeur brute si elle est inconnue. */
export function capaCriticityLabel(criticity: CapaCriticity | string | undefined | null): string {
  if (!criticity) return '—';
  return CAPA_CRITICITIES.find(c => c.value === criticity)?.label ?? String(criticity);
}

/**
 * De quel registre se tire la référence de la source.
 *
 * <p>Une CAPA qui dit « Audit » sans dire LEQUEL ne se rattache à rien : la
 * référence tapée à la main se perd dès qu'elle est mal recopiée, et le dossier
 * devient orphelin de sa cause. Les sources qui désignent un objet de la
 * plateforme nomment donc ici le registre où la choisir, et la fenêtre de
 * création n'accepte plus qu'une référence existante.
 *
 * <p>`undefined` pour les sources qui ne désignent aucun objet — une détection
 * interne, une alerte IoT, « autre » : là, le texte libre est la seule réponse
 * honnête.
 */
export type CapaSourceRegister = 'nc' | 'audit' | 'complaint' | 'risk';

/**
 * Libellés des sources d'un dossier. La création libre depuis la liste n'en
 * propose qu'une partie (`manual`) : un dossier de carte de contrôle ou
 * d'anomalie ne s'ouvre que depuis son écran d'origine, qui le rattache.
 */
export const CAPA_SOURCES: ReadonlyArray<{
  value: CapaSourceType; label: string; manual: boolean; register?: CapaSourceRegister;
}> = [
  // « Non-conformité » tout court laissait croire que la source couvrait aussi
  // l'écart remonté par un client. Elle ne couvre que l'écart constaté en
  // interne : le dire lève l'ambiguïté sans changer le code de l'énumération,
  // qu'un dossier déjà ouvert porte toujours.
  { value: 'NON_CONFORMITY', label: $localize`:@@capa.source.non-conformity-internal:Non-conformité interne`, manual: true, register: 'nc' },
  { value: 'AUDIT',          label: $localize`:@@capa.source.audit:Audit`, manual: true, register: 'audit' },
  { value: 'COMPLAINT',      label: $localize`:@@capa.source.complaint:Réclamation client`, manual: true, register: 'complaint' },
  { value: 'INTERNAL',       label: $localize`:@@capa.source.internal:Détection interne`, manual: true },
  { value: 'IOT_ALERT',      label: $localize`:@@capa.source.iot-alert:Alerte IoT`, manual: true },
  { value: 'SPC_ALERT',      label: $localize`:@@capa.source.spc-alert:Carte de contrôle`, manual: false },
  { value: 'ANOMALY',        label: $localize`:@@capa.source.anomaly:Anomalie détectée`, manual: false },
  // Ouvrable depuis la liste : §6.1 traite le risque AVANT qu'il se réalise, et
  // obliger à passer par la fiche du registre privait d'un chemin légitime. Le
  // rattachement reste garanti, mais par la référence — choisie au registre.
  { value: 'RISK',           label: $localize`:@@capa.source.risk:Risque`, manual: true, register: 'risk' },
  { value: 'OTHER',          label: $localize`:@@capa.source.other:Autre`, manual: true }
];

/** Libellé lisible d'une source ; la valeur brute si elle est inconnue. */
export function capaSourceLabel(source: CapaSourceType | string | undefined | null): string {
  if (!source) return '—';
  return CAPA_SOURCES.find(s => s.value === source)?.label ?? String(source);
}

/**
 * Le registre où choisir la référence de cette source, s'il y en a un.
 *
 * <p>C'est la seule question que posent la fenêtre de création et ses tests :
 * faut-il une référence choisie, ou un texte libre ?
 */
export function capaSourceRegister(
  source: CapaSourceType | string | undefined | null
): CapaSourceRegister | undefined {
  if (!source) return undefined;
  return CAPA_SOURCES.find(s => s.value === source)?.register;
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
