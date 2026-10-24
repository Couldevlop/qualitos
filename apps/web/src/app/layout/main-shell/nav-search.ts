import type { NavItem, NavSection } from './main-shell.component';

/** Une entrée de menu trouvée par la recherche de la barre supérieure. */
export interface NavHit {
  item: NavItem;
  /** Le groupe de l'entrée, affiché à côté pour lever les homonymes. */
  section: string;
}

/** Nombre maximal de propositions : au-delà, la liste ne se parcourt plus d'un coup d'œil. */
export const NAV_SEARCH_LIMIT = 8;

/** Minuscules et sans accents : « securite » trouve « Sécurité ». */
export function normalize(text: string): string {
  return text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

/**
 * Cherche parmi les entrées de menu que l'utilisateur voit déjà.
 *
 * <p>Chaque mot saisi doit figurer dans le libellé de l'entrée ou dans celui de
 * son groupe. Les entrées dont le libellé commence par la saisie passent
 * devant, puis celles dont le libellé la contient, puis celles retrouvées par
 * leur seul groupe ; l'ordre du menu départage les ex aequo.</p>
 */
export function searchNav(sections: NavSection[], query: string, limit = NAV_SEARCH_LIMIT): NavHit[] {
  const q = normalize(query);
  if (!q) {
    return [];
  }
  const words = q.split(/\s+/);
  const seen = new Set<string>();
  const ranked: { hit: NavHit; rank: number; order: number }[] = [];
  let order = 0;
  for (const section of sections) {
    const group = normalize(section.label);
    for (const item of section.items) {
      order++;
      if (seen.has(item.route)) {
        continue;
      }
      const label = normalize(item.label);
      if (!words.every(w => label.includes(w) || group.includes(w))) {
        continue;
      }
      seen.add(item.route);
      const rank = label.startsWith(q) ? 0 : label.includes(q) ? 1 : 2;
      ranked.push({ hit: { item, section: section.label }, rank, order });
    }
  }
  return ranked
    .sort((a, b) => a.rank - b.rank || a.order - b.order)
    .slice(0, limit)
    .map(r => r.hit);
}
