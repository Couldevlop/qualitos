import { SpcViolation } from '../spc.types';
import { CalculationView } from './spc-capability.types';

/**
 * Le pont entre l'étude de capabilité et la détection d'anomalies IA
 * (`POST /api/v1/ai/spc/analyze`, règles de Nelson, ADR 0018) : des fonctions
 * pures, pour que la carte X̄ montre les règles déclenchées sans que l'écran
 * ait à faire de statistique.
 *
 * <p>Le point important est la BASELINE : l'analyse part de la ligne centrale
 * et du σ <b>de la carte</b> (X̿ et σ = A2-limites / 3), jamais de ceux qu'elle
 * estimerait elle-même sur les moyennes. Sans cela, elle jugerait la série
 * contre des limites invisibles à l'écran, et désignerait des points qui ne
 * sortent pas des limites tracées — un faux signal, pas une détection.
 */

/** En dessous, une série de moyennes ne porte aucune information de tendance. */
export const MIN_NELSON_POINTS = 2;

/**
 * La série envoyée à l'analyse, et de quels sous-groupes elle vient.
 *
 * <p>`columns` fait le chemin du retour : l'IA répond avec des rangs dans la
 * série envoyée, l'écran doit parler en numéros de sous-groupes.
 */
export interface NelsonSeries {
  values: number[];
  /** L'indice de colonne (0-based) de chaque valeur envoyée. */
  columns: number[];
  center: number;
  sigma: number;
}

/**
 * La série de la carte X̄ : les moyennes des sous-groupes complets, dans
 * l'ordre, avec la baseline de la carte. `null` quand la carte n'a pas de
 * limites, pas de dispersion, ou trop peu de points.
 *
 * <p>Les sous-groupes incomplets sont omis — ils n'ont pas de moyenne. Les
 * règles de série (2, 3, 4…) traitent donc comme consécutifs deux relevés
 * séparés par un trou : c'est le comportement voulu, l'ordre des
 * prélèvements restant celui de la grille.
 */
export function nelsonSeries(calc: CalculationView): NelsonSeries | null {
  const center = calc.grandMean;
  const ucl = calc.meanUcl;
  if (center === null || ucl === null || !Number.isFinite(center) || !Number.isFinite(ucl)) {
    return null;
  }
  const sigma = (ucl - center) / 3;
  if (!(sigma > 0)) return null;
  const values: number[] = [];
  const columns: number[] = [];
  calc.subgroupMeans.forEach((v, c) => {
    if (v !== null && Number.isFinite(v)) {
      values.push(v);
      columns.push(c);
    }
  });
  if (values.length < MIN_NELSON_POINTS) return null;
  return { values, columns, center, sigma };
}

/**
 * De quoi reconnaître une série déjà analysée, pour ne pas rappeler le service
 * à chaque frappe qui ne change rien au tracé (OWASP LLM04 : les quotas IA sont
 * comptés par client).
 */
export function seriesKey(s: NelsonSeries): string {
  return `${s.center}|${s.sigma}|${s.values.join(',')}`;
}

/** « NELSON_5 » → « N5 » : l'abrégé posé sur le point, là où la place manque. */
export function ruleCode(rule: string): string {
  const m = /^NELSON_(\d+)$/.exec(rule ?? '');
  return m ? `N${m[1]}` : (rule ?? '').trim() || '?';
}

/**
 * Les règles déclenchées, par indice de colonne : ce que la carte pose sur
 * chaque point. Les rangs hors de la série envoyée sont ignorés — une réponse
 * mal formée ne doit pas marquer un sous-groupe au hasard.
 */
export function rulesByColumn(violations: readonly SpcViolation[] | null | undefined,
                              columns: readonly number[]): Map<number, string[]> {
  const parRang = new Map<number, string[]>();
  (violations ?? []).forEach(v => {
    const code = ruleCode(v.rule);
    (v.pointIndices ?? []).forEach(rang => {
      const colonne = columns[rang];
      if (colonne === undefined) return;
      const deja = parRang.get(colonne);
      if (!deja) {
        parRang.set(colonne, [code]);
      } else if (!deja.includes(code)) {
        deja.push(code);
      }
    });
  });
  return parRang;
}

/** Les numéros de sous-groupes (affichés à partir de 1) visés par une violation. */
export function violationSubgroups(v: SpcViolation, columns: readonly number[]): number[] {
  const vus = new Set<number>();
  (v.pointIndices ?? []).forEach(rang => {
    const colonne = columns[rang];
    if (colonne !== undefined) vus.add(colonne + 1);
  });
  return [...vus].sort((a, b) => a - b);
}

/** La classe de sévérité du bandeau, sur les deux vocabulaires du service. */
export function severityTone(severity: string): string {
  return severity === 'CRITICAL' || severity === 'high' ? 'sev-crit'
    : severity === 'MAJOR' || severity === 'medium' ? 'sev-maj' : 'sev-min';
}
