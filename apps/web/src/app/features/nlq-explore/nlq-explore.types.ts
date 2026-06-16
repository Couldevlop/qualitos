/**
 * Types de l'exploration NLQ → graphique (CLAUDE.md §7.3).
 *
 * On réutilise le contrat d'API du module NLQ existant
 * (POST /api/v1/ai/nlq/ask, text-to-SQL validé côté engine).
 * Le type de graphe est choisi côté UI à partir de la nature des colonnes.
 */

/** Type de graphe rendu pour une réponse NLQ. */
export type NlqChartType = 'bar' | 'line';

/**
 * Plan de rendu déduit d'un jeu de lignes : colonne de catégories (axe X),
 * colonne(s) numérique(s) (séries), et faisabilité d'un graphe.
 */
export interface NlqChartPlan {
  /** Le jeu de données peut-il être représenté graphiquement ? */
  graphable: boolean;
  /** Nom de la colonne servant d'axe des catégories (1re colonne textuelle). */
  categoryColumn: string | null;
  /** Noms des colonnes numériques traçables (séries). */
  valueColumns: string[];
}
