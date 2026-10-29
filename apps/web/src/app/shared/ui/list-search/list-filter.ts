import { FormControl } from '@angular/forms';
import { combineLatest, Observable } from 'rxjs';
import { distinctUntilChanged, map, startWith } from 'rxjs/operators';

/** Valeur d'un champ cherchable : texte, nombre, ou absente. */
export type SearchableValue = string | number | null | undefined;

/** Texte comparable : minuscules, sans accents ni espaces de bord. */
export function normalizeSearch(text: string | null | undefined): string {
  return (text ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim();
}

/**
 * Garde les lignes dont les champs contiennent TOUS les mots de la requête
 * (« lot ligne 1 » trouve « Étiquetage lot manquant — ligne 1 »).
 * Une requête vide rend la liste telle quelle.
 */
export function filterRows<T>(
  rows: readonly T[],
  query: string,
  fields: (row: T) => readonly SearchableValue[]
): T[] {
  const words = normalizeSearch(query).split(/\s+/).filter(Boolean);
  if (words.length === 0) {
    return [...rows];
  }
  return rows.filter(row => {
    const haystack = normalizeSearch(fields(row).filter(v => v != null).join(' '));
    return words.every(word => haystack.includes(word));
  });
}

/**
 * Lignes affichées : la page chargée, filtrée par le champ de recherche.
 *
 * <p>Ces listes n'ont pas de recherche côté serveur : le filtre porte sur la
 * page affichée, et le champ le dit dans son aide.
 */
export function searchRows<T>(
  rows$: Observable<T[]>,
  control: FormControl<string>,
  fields: (row: T) => readonly SearchableValue[]
): Observable<T[]> {
  const query$ = control.valueChanges.pipe(startWith(control.value), distinctUntilChanged());
  return combineLatest([rows$, query$]).pipe(map(([rows, query]) => filterRows(rows, query, fields)));
}
