import { SUBGROUPS } from './spc-capability.calc';
import { Measurement } from './spc-capability.types';

/**
 * La géométrie des graphiques de l'étude, sans DOM : les composants SVG ne font
 * que dessiner ce qui est calculé ici. Les dimensions sont celles du viewBox,
 * le navigateur met à l'échelle.
 */

export interface ChartPoint { index: number; value: number; x: number; y: number; out: boolean }
export interface ChartLine { label: 'UCL' | 'CL' | 'LCL'; value: number; y: number }
export interface SpecLine { label: 'USL' | 'LSL'; value: number; y: number }
export interface ChartTick { value: number; y: number }

export interface ControlChartGeometry {
  width: number;
  height: number;
  left: number;
  right: number;
  points: ChartPoint[];
  polyline: string;
  lines: ChartLine[];
  /** Les limites de tolérance, tracées sur la carte X̄ comme dans le classeur. */
  specs: SpecLine[];
  ticks: ChartTick[];
  xLabels: { label: number; x: number }[];
}

const CW = 560;
const CH = 260;
const CL_ = 44;
const CR = 64;
const CT = 12;
const CB = 26;

/**
 * Une carte de contrôle : les valeurs de chaque sous-groupe, les trois limites,
 * et les points hors limites. `null` quand il n'y a rien à tracer. Les limites
 * de tolérance, quand on les donne (carte X̄), entrent dans l'échelle.
 */
export function controlChart(values: (number | null)[], ucl: number | null, cl: number | null,
                             lcl: number | null,
                             spec: { usl: number | null; lsl: number | null } = { usl: null, lsl: null }
): ControlChartGeometry | null {
  if (ucl === null || cl === null || lcl === null) return null;
  const pts = values
    .map((v, i) => ({ v, i }))
    .filter((p): p is { v: number; i: number } => p.v !== null && Number.isFinite(p.v));
  if (!pts.length) return null;
  const tolerances: { label: 'USL' | 'LSL'; value: number }[] = [];
  if (spec.usl !== null && Number.isFinite(spec.usl)) tolerances.push({ label: 'USL', value: spec.usl });
  if (spec.lsl !== null && Number.isFinite(spec.lsl)) tolerances.push({ label: 'LSL', value: spec.lsl });
  const all = [...pts.map(p => p.v), ucl, lcl, cl, ...tolerances.map(t => t.value)];
  let lo = Math.min(...all);
  let hi = Math.max(...all);
  const pad = (hi - lo || 1) * 0.12;
  lo -= pad;
  hi += pad;
  const x = (i: number) => CL_ + (CW - CL_ - CR) * i / (SUBGROUPS - 1);
  const y = (v: number) => CT + (CH - CT - CB) * (1 - (v - lo) / (hi - lo));
  const points = pts.map(p => ({ index: p.i, value: p.v, x: x(p.i), y: y(p.v), out: p.v > ucl || p.v < lcl }));
  return {
    width: CW, height: CH, left: CL_, right: CW - CR,
    points,
    polyline: points.map(p => `${p.x},${p.y}`).join(' '),
    lines: [
      { label: 'UCL', value: ucl, y: y(ucl) },
      { label: 'CL', value: cl, y: y(cl) },
      { label: 'LCL', value: lcl, y: y(lcl) }
    ],
    specs: tolerances.map(t => ({ ...t, y: y(t.value) })),
    ticks: [0, 1, 2, 3, 4].map(t => {
      const v = lo + (hi - lo) * t / 4;
      return { value: v, y: y(v) };
    }),
    xLabels: Array.from({ length: SUBGROUPS }, (_, i) => i)
      .filter(i => i % 2 === 0 || i === SUBGROUPS - 1)
      .map(i => ({ label: i + 1, x: x(i) }))
  };
}

export interface HistogramBar { x: number; y: number; width: number; height: number; count: number; out: boolean }

export interface DistributionGeometry {
  width: number;
  height: number;
  top: number;
  bottom: number;
  left: number;
  right: number;
  bars: HistogramBar[];
  curve: string;
  lslX: number;
  uslX: number;
  meanX: number;
  /** Les zones hors tolérance, ombrées : à gauche de LSL, à droite de USL. */
  lowZone: { x: number; width: number };
  highZone: { x: number; width: number };
}

const DW = 560;
const DH = 250;
const DL = 14;
const DR = 14;
const DT = 26;
const DB = 26;
const BINS = 30;

/** Les mesures des sous-groupes complets : celles qui entrent dans le calcul. */
export function completeValues(grid: Measurement[][], n: number): number[] {
  const out: number[] = [];
  for (let c = 0; c < SUBGROUPS; c++) {
    const col: number[] = [];
    for (let r = 0; r < n; r++) {
      const v = grid[r]?.[c];
      if (v !== null && v !== undefined && Number.isFinite(v)) col.push(v);
    }
    if (col.length === n) out.push(...col);
  }
  return out;
}

/**
 * L'histogramme des mesures, la loi normale de moyenne X̿ et d'écart-type σ
 * mise à l'échelle des effectifs, et la tolérance. `null` sans mesure, sans
 * dispersion ou sans tolérance valide.
 */
export function distribution(values: number[], mean: number | null, sigma: number | null,
                             lsl: number | null, usl: number | null): DistributionGeometry | null {
  if (!values.length || mean === null || sigma === null || !(sigma > 0)
    || lsl === null || usl === null || !(usl > lsl)) return null;
  const lo0 = Math.min(lsl, ...values, mean - 4 * sigma);
  const hi0 = Math.max(usl, ...values, mean + 4 * sigma);
  const pad = (hi0 - lo0) * 0.04;
  const lo = lo0 - pad;
  const hi = hi0 + pad;
  const bw = (hi - lo) / BINS;
  const counts = Array<number>(BINS).fill(0);
  values.forEach(v => counts[Math.min(BINS - 1, Math.floor((v - lo) / bw))]++);
  const dens = (v: number) =>
    values.length * bw * Math.exp(-0.5 * ((v - mean) / sigma) ** 2) / (sigma * Math.sqrt(2 * Math.PI));
  const ymax = Math.max(...counts, dens(mean)) * 1.15;
  const x = (v: number) => DL + (DW - DL - DR) * (v - lo) / (hi - lo);
  const y = (v: number) => DT + (DH - DT - DB) * (1 - v / ymax);
  const bars: HistogramBar[] = [];
  counts.forEach((count, i) => {
    if (!count) return;
    const v0 = lo + i * bw;
    const mid = v0 + bw / 2;
    bars.push({
      x: x(v0) + 1, y: y(count), width: Math.max(0, x(v0 + bw) - x(v0) - 2), height: y(0) - y(count),
      count, out: mid < lsl || mid > usl
    });
  });
  const curve = Array.from({ length: 121 }, (_, i) => {
    const v = lo + (hi - lo) * i / 120;
    return `${x(v)},${y(dens(v))}`;
  }).join(' ');
  return {
    width: DW, height: DH, top: DT, bottom: DH - DB, left: DL, right: DW - DR,
    bars, curve,
    lslX: x(lsl), uslX: x(usl), meanX: x(mean),
    lowZone: { x: DL, width: Math.max(0, x(lsl) - DL) },
    highZone: { x: x(usl), width: Math.max(0, DW - DR - x(usl)) }
  };
}
