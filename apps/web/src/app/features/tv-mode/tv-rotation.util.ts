/**
 * Helpers purs de rotation de slides (§7.3) — sans dépendance Angular,
 * donc directement testables. Le composant délègue ici la logique de
 * navigation circulaire pour garder son code d'orchestration mince.
 */

/** Index suivant (boucle sur la fin → début). */
export function nextIndex(current: number, count: number): number {
  if (count <= 0) return 0;
  return (current + 1) % count;
}

/** Index précédent (boucle sur le début → fin). */
export function prevIndex(current: number, count: number): number {
  if (count <= 0) return 0;
  return (current - 1 + count) % count;
}

/**
 * Réindexe après un changement du nombre de slides (ex. rechargement) :
 * clampe l'index courant dans [0, count-1], ou 0 si la liste est vide.
 */
export function clampIndex(current: number, count: number): number {
  if (count <= 0) return 0;
  if (current < 0) return 0;
  if (current >= count) return count - 1;
  return current;
}
