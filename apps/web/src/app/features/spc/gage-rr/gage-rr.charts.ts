import type { EChartsCoreOption } from 'echarts/core';

import { A2 } from './gage-rr.calc';
import { operatorLetter } from './gage-rr.labels';
import { GageRrCalculationView } from './gage-rr.types';

/**
 * Les options ECharts des graphiques de l'étude, sans DOM : les composants ne
 * font que passer ces objets à `qos-echart`, qui y ajoute les couleurs et les
 * axes du thème.
 */

const round = (v: number, digits = 4): number => Number(v.toFixed(digits));
const percent = (v: number | null): number | null => (v === null ? null : round(v * 100, 1));

/**
 * Les composantes de variation (méthode moyenne et étendue), en pourcentage :
 * contribution à la variance, part de la variation totale, part de la
 * tolérance. Les seuils AIAG de 10 % et 30 % sont tracés.
 */
export function componentsOption(c: GageRrCalculationView): EChartsCoreOption {
  const items = [c.grr, c.ev, c.av, c.pv];
  const categories = [
    'GRR',
    $localize`:@@spc.gage-rr.chart.repeatability:Répétabilité (EV)`,
    $localize`:@@spc.gage-rr.chart.reproducibility:Reproductibilité (AV)`,
    $localize`:@@spc.gage-rr.chart.parts:Pièces (PV)`
  ];
  const series: Record<string, unknown>[] = [
    {
      name: $localize`:@@spc.gage-rr.chart.contribution:% contribution`,
      type: 'bar',
      data: items.map(i => percent(i.percentContribution))
    },
    {
      name: $localize`:@@spc.gage-rr.chart.total:% variation totale`,
      type: 'bar',
      data: items.map(i => percent(i.percentOfTotal)),
      markLine: {
        symbol: 'none',
        lineStyle: { type: 'dashed' },
        label: { formatter: '{c} %' },
        data: [{ yAxis: 10 }, { yAxis: 30 }]
      }
    }
  ];
  if (items.every(i => i.percentTolerance !== null)) {
    series.push({
      name: $localize`:@@spc.gage-rr.chart.tolerance:% tolérance`,
      type: 'bar',
      data: items.map(i => percent(i.percentTolerance))
    });
  }
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v: unknown) => `${v} %` },
    legend: { top: 0 },
    grid: { top: 36 },
    xAxis: { type: 'category', data: categories },
    yAxis: { type: 'value', axisLabel: { formatter: '{value} %' } },
    series
  };
}

/** Les libellés de l'axe des cartes par opérateur : A1 … A10, B1 … */
function cellLabels(k: number, n: number): string[] {
  return Array.from({ length: k * n }, (_, i) => `${operatorLetter(Math.floor(i / n))}${(i % n) + 1}`);
}

/** Une série par opérateur, placée sur ses propres pièces : les traits ne se rejoignent pas. */
function perOperator(values: number[][], names: string[]): Record<string, unknown>[] {
  const n = values[0]?.length ?? 0;
  return values.map((v, o) => {
    const data: (number | string)[] = Array(values.length * n).fill('-');
    v.forEach((x, p) => data[o * n + p] = round(x, 5));
    return { name: names[o], type: 'line', data, symbolSize: 7, connectNulls: false };
  });
}

/**
 * La carte des étendues par opérateur : chaque étendue sous UCL<sub>R</sub>
 * (D4 × R̄̄). Une étendue au-dessus signale une mesure à vérifier.
 */
export function rangeChartOption(c: GageRrCalculationView, names: string[]): EChartsCoreOption {
  const series = perOperator(c.cellRanges, names);
  series[0] = {
    ...series[0],
    markLine: {
      symbol: 'none',
      data: [
        { yAxis: round(c.rangeUcl, 5), name: 'UCL', label: { formatter: `UCL ${c.rangeUcl.toFixed(4)}` },
          lineStyle: { type: 'dashed', color: '#DC2626' } },
        { yAxis: round(c.meanRange, 5), name: 'R̄̄', label: { formatter: `R̄̄ ${c.meanRange.toFixed(4)}` },
          lineStyle: { type: 'solid', color: '#059669' } }
      ]
    }
  };
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { top: 36, right: 72 },
    xAxis: { type: 'category', data: cellLabels(c.cellRanges.length, c.partMeans.length) },
    yAxis: { type: 'value', scale: true },
    series
  };
}

/**
 * La carte des moyennes par opérateur, limites X̿ ± A2 × R̄̄. Un bon système
 * de mesure y voit la plupart des points HORS des limites : la variation des
 * pièces domine le bruit de la mesure.
 */
export function meanChartOption(c: GageRrCalculationView, names: string[]): EChartsCoreOption {
  const r = c.trialMeans[0]?.length ?? 2;
  const a2 = A2[r] ?? A2[2];
  const ucl = c.grandMean + a2 * c.meanRange;
  const lcl = c.grandMean - a2 * c.meanRange;
  const series = perOperator(c.cellMeans, names);
  series[0] = {
    ...series[0],
    markLine: {
      symbol: 'none',
      data: [
        { yAxis: round(ucl, 5), label: { formatter: `UCL ${ucl.toFixed(4)}` }, lineStyle: { type: 'dashed', color: '#DC2626' } },
        { yAxis: round(c.grandMean, 5), label: { formatter: `X̿ ${c.grandMean.toFixed(4)}` }, lineStyle: { type: 'solid', color: '#059669' } },
        { yAxis: round(lcl, 5), label: { formatter: `LCL ${lcl.toFixed(4)}` }, lineStyle: { type: 'dashed', color: '#DC2626' } }
      ]
    }
  };
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0 },
    grid: { top: 36, right: 80 },
    xAxis: { type: 'category', data: cellLabels(c.cellMeans.length, c.partMeans.length) },
    yAxis: { type: 'value', scale: true },
    series
  };
}

/**
 * Les mesures par pièce : chaque essai de chaque opérateur en point, la
 * moyenne de l'opérateur en trait, la moyenne de la pièce en pointillé. Des
 * traits parallèles disent des opérateurs cohérents ; des traits qui se
 * croisent, une interaction pièce × opérateur.
 */
export function byPartOption(grid: number[][][], c: GageRrCalculationView, names: string[]): EChartsCoreOption {
  const parts = c.partMeans.map((_, p) => String(p + 1));
  const series: Record<string, unknown>[] = [];
  grid.forEach((bloc, o) => {
    series.push({
      name: names[o],
      type: 'scatter',
      symbolSize: 6,
      data: bloc.flatMap((essais, p) => essais.map(v => [String(p + 1), v]))
    });
    series.push({
      name: names[o],
      type: 'line',
      symbol: 'none',
      data: c.cellMeans[o].map(v => round(v, 5))
    });
  });
  series.push({
    name: $localize`:@@spc.gage-rr.chart.part-mean:Moyenne pièce`,
    type: 'line',
    symbol: 'diamond',
    symbolSize: 8,
    lineStyle: { type: 'dashed' },
    data: c.partMeans.map(v => round(v, 5))
  });
  return {
    tooltip: { trigger: 'item' },
    legend: { top: 0 },
    grid: { top: 36 },
    xAxis: { type: 'category', data: parts, name: $localize`:@@spc.gage-rr.chart.part-axis:Pièce` },
    yAxis: { type: 'value', scale: true },
    series
  };
}
