import type { EChartsCoreOption } from 'echarts/core';

import { ForecastResponse } from './forecast.types';

/**
 * Graphe d'une prévision : historique, projection, intervalle de prédiction à 95 % et cible.
 *
 * <p>Extrait de la page d'essai pour être partagé avec la fiche de l'indicateur : la même
 * série ne doit pas se dessiner de deux façons selon l'écran qui la montre.
 */
export function buildForecastChart(history: readonly number[], res: ForecastResponse): EChartsCoreOption {
  const n = history.length;
  const total = n + res.horizon;
  const categories = Array.from({ length: total }, (_, i) => `${i + 1}`);

  // Historique : valeurs réelles, puis null sur la zone de prévision.
  const hist: (number | null)[] = history.map(v => v).concat(Array(res.horizon).fill(null));
  // Prévision : raccordée au dernier point d'historique pour une ligne continue.
  const fc: (number | null)[] = Array(Math.max(n - 1, 0)).fill(null);
  if (n > 0) {
    fc.push(history[n - 1]);
  }
  // Garde défensive : un contrat partiel (points absents) ne casse pas le rendu.
  const points = res.points ?? [];
  points.forEach(p => fc.push(p.value));
  const low: (number | null)[] = Array(n).fill(null);
  const high: (number | null)[] = Array(n).fill(null);
  points.forEach(p => { low.push(p.low); high.push(p.high); });

  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['Historique', 'Prévision', 'IP 95 %'], top: 0 },
    grid: { left: 48, right: 24, top: 32, bottom: 36 },
    xAxis: { type: 'category', data: categories, name: 'Période', boundaryGap: false },
    yAxis: { type: 'value', name: 'Valeur', scale: true },
    series: [
      { name: 'Historique', type: 'line', data: hist, symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2, color: '#2563EB' }, itemStyle: { color: '#2563EB' } },
      // Borne basse (transparente) + bande empilée = intervalle de prédiction.
      { name: 'IP 95 %', type: 'line', data: low, stack: 'pi', symbol: 'none',
        lineStyle: { opacity: 0 }, areaStyle: { opacity: 0 }, silent: true },
      { name: 'IP 95 %', type: 'line', data: bandDelta(low, high), stack: 'pi',
        symbol: 'none', lineStyle: { opacity: 0 },
        areaStyle: { color: 'rgba(37, 99, 235, 0.12)' }, silent: true },
      { name: 'Prévision', type: 'line', data: fc, symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 2, type: 'dashed', color: '#7C3AED' }, itemStyle: { color: '#7C3AED' },
        ...(res.target != null ? { markLine: {
          symbol: 'none',
          data: [{ yAxis: res.target, name: 'Cible',
            lineStyle: { color: '#059669' },
            label: { formatter: `Cible ${res.target}`, color: '#059669', position: 'end' } }]
        } } : {}) }
    ]
  };
}

/** Épaisseur de bande (high − low) là où l'IP existe, null ailleurs (pour l'empilement). */
function bandDelta(low: (number | null)[], high: (number | null)[]): (number | null)[] {
  return low.map((l, i) => (l == null || high[i] == null) ? null : (high[i] as number) - l);
}
