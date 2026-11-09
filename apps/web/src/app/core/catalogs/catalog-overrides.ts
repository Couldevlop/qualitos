import { CatalogEntry, CatalogName } from './catalog.types';

/**
 * Le réglage des listes déroulantes du client, tenu en mémoire pour être lu
 * SANS attendre (ADR 0111).
 *
 * <p>Pourquoi un registre de module, et non un service injecté : les libellés
 * des listes sont rendus par des fonctions pures — `auditTypeLabel(code)`,
 * `capaNatureLabel(code)` — appelées depuis des gabarits, des tableaux, des
 * exports et des services. Les rendre asynchrones obligerait à réécrire une
 * quarantaine d'écrans pour un besoin qui tient en une table de correspondance.
 *
 * <p>Le registre est rempli une fois au démarrage par
 * {@link CatalogService.preload}, et vidé à la déconnexion : un client ne doit
 * jamais lire les mots d'un autre. Vide, tout retombe sur les libellés de
 * l'écran, traduits en six langues — l'état d'avant ce mécanisme.
 */
const REGISTRE = new Map<CatalogName, CatalogEntry[]>();

/** Remplace le réglage connu. Un objet vide signifie « le client n'a rien réglé ». */
export function setCatalogOverrides(
  reglage: Partial<Record<CatalogName, CatalogEntry[]>>): void {
  REGISTRE.clear();
  (Object.keys(reglage) as CatalogName[]).forEach(name => {
    const entries = reglage[name];
    if (entries && entries.length) {
      REGISTRE.set(name, entries);
    }
  });
}

/** Oublie tout réglage : à la déconnexion, et au changement de client. */
export function clearCatalogOverrides(): void {
  REGISTRE.clear();
}

/** Les entrées réglées par le client pour cette liste, ou `null`. */
export function catalogOverride(name: CatalogName): CatalogEntry[] | null {
  return REGISTRE.get(name) ?? null;
}

/**
 * Le mot du client pour ce code, ou `null` s'il n'a rien réglé — l'appelant
 * garde alors son propre libellé traduit.
 *
 * <p>Un code désactivé garde son libellé : c'est lui qui explique l'historique.
 */
export function catalogLabel(name: CatalogName, code: string | null | undefined): string | null {
  if (!code) {
    return null;
  }
  const entries = REGISTRE.get(name);
  if (!entries) {
    return null;
  }
  const cherche = code.toUpperCase();
  return entries.find(e => e.code === cherche)?.label ?? null;
}

/** Une option de liste déroulante, telle que les écrans les écrivent déjà. */
export interface CatalogOption<T extends string = string> {
  value: T;
  label: string;
}

/**
 * Les options d'une liste déroulante, réglage du client appliqué.
 *
 * <p>Trois comportements, et un seul appel pour les obtenir :
 *
 * <ul>
 *   <li><b>Rien réglé</b> : les options de l'écran, inchangées.</li>
 *   <li><b>Liste adossée au code</b> (mêmes codes de part et d'autre) : l'ordre
 *       et les libellés du client, appliqués aux options de l'écran. Un code que
 *       le client n'a pas — parce que l'écran n'en propose qu'une partie —
 *       reste à sa place, à la fin.</li>
 *   <li><b>Liste libre</b> : les entrées en service du client, telles quelles.
 *       Les options de l'écran n'étaient qu'un point de départ.</li>
 * </ul>
 */
export function catalogOptions<T extends string>(
  name: CatalogName, fallback: readonly CatalogOption<T>[]): CatalogOption<T>[] {
  const entries = REGISTRE.get(name);
  if (!entries) {
    return [...fallback];
  }
  const parCode = new Map(fallback.map(o => [o.value.toUpperCase(), o] as const));
  const ordonnees: CatalogOption<T>[] = [];
  // Tous les codes du client, hors service compris : un code qu'il a retiré de
  // la saisie ne doit pas revenir par le repli de l'écran.
  const vus = new Set(entries.map(e => e.code));
  entries.filter(e => e.active).forEach(e => {
    const connue = parCode.get(e.code);
    // Le code du client fait foi : s'il n'est pas dans les options de l'écran,
    // c'est une valeur à lui (liste libre), et elle s'affiche quand même.
    ordonnees.push({ value: (connue?.value ?? e.code) as T, label: e.label });
  });
  fallback.forEach(o => {
    if (!vus.has(o.value.toUpperCase())) {
      ordonnees.push(o);
    }
  });
  return ordonnees;
}

/**
 * Les codes d'une liste, dans l'ordre du client : pour les écrans qui affichent
 * un tableau de codes plutôt que des options (`readonly types: X[]`).
 */
export function catalogCodes<T extends string>(
  name: CatalogName, fallback: readonly T[]): T[] {
  return catalogOptions(name, fallback.map(c => ({ value: c, label: c })))
    .map(o => o.value);
}
