/**
 * Mise en forme des données du tenant pour les pages d'analyse IA (prévision,
 * anomalies, réclamations) : ces pages travaillaient sur des chiffres collés à
 * la main ; elles partent désormais des mesures et des écarts déjà enregistrés.
 */

/** Une mesure datée, telle que la renvoie l'API des indicateurs. */
export interface DatedValue {
  periodStart: string;
  value: number;
}

/** Série chronologique d'un indicateur, de la plus ancienne à la plus récente. */
export interface KpiSeries {
  values: number[];
  from: string | null;
  to: string | null;
}

/** Trie par période et ne garde que les valeurs numériques finies. */
export function toSeries(measurements: readonly DatedValue[]): KpiSeries {
  const sorted = measurements
    .filter(m => Number.isFinite(m.value))
    .slice()
    .sort((a, b) => a.periodStart.localeCompare(b.periodStart));
  return {
    values: sorted.map(m => m.value),
    from: sorted.length ? sorted[0].periodStart : null,
    to: sorted.length ? sorted[sorted.length - 1].periodStart : null
  };
}

/** Matrice multivariée : une ligne par période commune, une colonne par indicateur. */
export interface AlignedMatrix {
  rows: number[][];
  periods: string[];
}

/**
 * Aligne plusieurs indicateurs sur leurs périodes communes.
 *
 * <p>Une période où l'un des indicateurs n'a pas de mesure est écartée : la
 * détection d'anomalies exige des lignes complètes, et inventer une valeur
 * manquante fausserait le score. Deux mesures sur la même période : la
 * dernière lue l'emporte.
 */
export function alignMatrix(columns: ReadonlyArray<readonly DatedValue[]>): AlignedMatrix {
  if (columns.length === 0) {
    return { rows: [], periods: [] };
  }
  const maps = columns.map(col => {
    const byPeriod = new Map<string, number>();
    for (const m of col) {
      if (Number.isFinite(m.value)) {
        byPeriod.set(m.periodStart, m.value);
      }
    }
    return byPeriod;
  });
  const periods = [...maps[0].keys()]
    .filter(p => maps.every(map => map.has(p)))
    .sort((a, b) => a.localeCompare(b));
  return {
    rows: periods.map(p => maps.map(map => map.get(p) as number)),
    periods
  };
}

/** Texte d'une réclamation sur une seule ligne : titre, puis description. */
export function complaintLine(title: string, description?: string | null): string {
  const parts = [title, description ?? '']
    .map(s => s.replace(/\s+/g, ' ').trim())
    .filter(Boolean);
  return parts.join(' — ');
}
