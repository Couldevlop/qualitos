import { byPartOption, componentsOption, meanChartOption, rangeChartOption } from './gage-rr.charts';
import { GOLDEN_GRID, goldenCalculation } from './gage-rr.fixtures';

type Series = { name: string; type: string; data: unknown[]; markLine?: { data: { yAxis: number }[] } };
type Option = { series: Series[]; xAxis: { data: string[] } };

describe('gage-rr.charts', () => {
  const noms = ['Alice', 'Bruno', 'Chloé'];

  it('trace les composantes en pourcentage, avec les seuils de 10 % et 30 %', () => {
    const o = componentsOption(goldenCalculation()) as unknown as Option;
    expect(o.xAxis.data.length).toBe(4);
    expect(o.series.length).toBe(3);
    expect(o.series[1].data[0]).toBe(21);
    expect(o.series[1].markLine!.data.map(d => d.yAxis)).toEqual([10, 30]);
    expect(o.series[2].data[0]).toBe(24.7);
    expect(o.series[0].data[0]).toBe(4.4);
  });

  it('omet la série de tolérance quand la tolérance manque', () => {
    const c = goldenCalculation();
    const o = componentsOption({ ...c, ev: { ...c.ev, percentTolerance: null } }) as unknown as Option;
    expect(o.series.length).toBe(2);
  });

  it('place les étendues de chaque opérateur sur ses pièces, sous UCLR', () => {
    const c = goldenCalculation();
    const o = rangeChartOption(c, noms) as unknown as Option;
    expect(o.xAxis.data.length).toBe(30);
    expect(o.xAxis.data[0]).toBe('A1');
    expect(o.xAxis.data[29]).toBe('C10');
    expect(o.series.map(s => s.name)).toEqual(noms);
    expect(o.series[0].data[0]).toBeCloseTo(c.cellRanges[0][0], 5);
    expect(o.series[0].data[10]).toBe('-');
    expect(o.series[1].data[10]).toBeCloseTo(c.cellRanges[1][0], 5);
    expect(o.series[0].markLine!.data[0].yAxis).toBeCloseTo(c.rangeUcl, 5);
    expect(o.series[0].markLine!.data[1].yAxis).toBeCloseTo(c.meanRange, 5);
  });

  it('borne la carte des moyennes à X̿ ± A2 × R̄̄', () => {
    const c = goldenCalculation();
    const o = meanChartOption(c, noms) as unknown as Option;
    const [ucl, centre, lcl] = o.series[0].markLine!.data.map(d => d.yAxis);
    expect(ucl).toBeCloseTo(c.grandMean + 1.02 * c.meanRange, 5);
    expect(centre).toBeCloseTo(c.grandMean, 5);
    expect(lcl).toBeCloseTo(c.grandMean - 1.02 * c.meanRange, 5);
  });

  it('prend A2 de deux essais', () => {
    const grille = GOLDEN_GRID.map(b => b.map(e => e.slice(0, 2)));
    const c = { ...goldenCalculation(), trialMeans: grille.map(() => [0, 0]) };
    const o = meanChartOption(c, noms) as unknown as Option;
    expect(o.series[0].markLine!.data[0].yAxis).toBeCloseTo(c.grandMean + 1.88 * c.meanRange, 5);
  });

  it('montre chaque essai, la moyenne de chaque opérateur et celle de la pièce', () => {
    const c = goldenCalculation();
    const o = byPartOption(GOLDEN_GRID, c, noms) as unknown as Option;
    expect(o.series.length).toBe(7);
    expect(o.series[0].type).toBe('scatter');
    expect(o.series[0].data.length).toBe(30);
    expect(o.series[0].data[0]).toEqual(['1', 9.955]);
    expect(o.series[1].type).toBe('line');
    expect(o.series[6].data.length).toBe(10);
    expect(o.xAxis.data).toEqual(['1', '2', '3', '4', '5', '6', '7', '8', '9', '10']);
  });
});
