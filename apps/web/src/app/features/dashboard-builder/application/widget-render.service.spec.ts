import { Widget } from '../domain/dashboard.model';
import { WidgetRenderService } from './widget-render.service';

function widget(partial: Partial<Widget>): Widget {
  return {
    id: 'w', type: 'bar', title: 'T',
    position: { x: 0, y: 0, cols: 2, rows: 2 }, config: {},
    ...partial
  };
}

describe('WidgetRenderService', () => {
  let svc: WidgetRenderService;

  beforeEach(() => { svc = new WidgetRenderService(); });

  it('returns null for kpi and narrative widgets', () => {
    expect(svc.optionFor(widget({ type: 'kpi' }))).toBeNull();
    expect(svc.optionFor(widget({ type: 'narrative' }))).toBeNull();
  });

  it('builds a line option with a single series', () => {
    const opt = svc.optionFor(widget({ type: 'line' })) as { series: unknown[] };
    expect(opt).not.toBeNull();
    expect(Array.isArray(opt.series)).toBeTrue();
    expect(opt.series.length).toBe(1);
  });

  it('builds bar / table options', () => {
    expect(svc.optionFor(widget({ type: 'bar' }))).not.toBeNull();
    expect(svc.optionFor(widget({ type: 'table' }))).not.toBeNull();
  });

  it('builds a pie option', () => {
    const opt = svc.optionFor(widget({ type: 'pie' })) as { series: { type: string }[] };
    expect(opt.series[0].type).toBe('pie');
  });

  it('builds a gauge option from the kpi value', () => {
    const opt = svc.optionFor(widget({ type: 'gauge' })) as { series: { type: string }[] };
    expect(opt.series[0].type).toBe('gauge');
  });

  it('builds a control-chart with UCL/x̄/LCL mark lines', () => {
    const opt = svc.optionFor(widget({ type: 'control-chart' })) as {
      series: { markLine: { data: unknown[] } }[];
    };
    expect(opt.series[0].markLine.data.length).toBe(3);
  });

  it('builds a heatmap option', () => {
    const opt = svc.optionFor(widget({ type: 'heatmap' })) as { series: { type: string }[] };
    expect(opt.series[0].type).toBe('heatmap');
  });

  it('kpiValue is deterministic for the same kpi id', () => {
    const w = widget({ type: 'kpi', config: { kpiId: 'fpy' } });
    expect(svc.kpiValue(w)).toBe(svc.kpiValue(w));
  });

  it('kpiValue stays within the configured range', () => {
    const v = svc.kpiValue(widget({ type: 'kpi', config: { kpiId: 'dpmo' } }));
    expect(v).toBeGreaterThanOrEqual(12);
    expect(v).toBeLessThanOrEqual(88);
  });

  it('kpiTrend is centred around zero (can be negative)', () => {
    const t = svc.kpiTrend(widget({ type: 'kpi', config: { kpiId: 'coq' } }));
    expect(t).toBeGreaterThanOrEqual(-10);
    expect(t).toBeLessThanOrEqual(10);
  });
});
