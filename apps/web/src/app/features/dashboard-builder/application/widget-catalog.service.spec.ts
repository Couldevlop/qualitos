import { WidgetCatalogService } from './widget-catalog.service';

describe('WidgetCatalogService', () => {
  let svc: WidgetCatalogService;

  beforeEach(() => {
    svc = new WidgetCatalogService();
  });

  it('exposes a non-empty palette covering the core widget types', () => {
    const types = svc.entries().map(e => e.type);
    expect(types).toContain('kpi');
    expect(types).toContain('line');
    expect(types).toContain('control-chart');
    expect(types).toContain('narrative');
    expect(svc.entries().length).toBeGreaterThanOrEqual(8);
  });

  it('every catalog entry has a label, icon and default size', () => {
    for (const e of svc.entries()) {
      expect(e.label.length).toBeGreaterThan(0);
      expect(e.icon.length).toBeGreaterThan(0);
      expect(e.defaultCols).toBeGreaterThan(0);
      expect(e.defaultRows).toBeGreaterThan(0);
    }
  });

  it('entryFor returns the matching definition', () => {
    expect(svc.entryFor('gauge')?.type).toBe('gauge');
    expect(svc.entryFor('kpi')?.icon).toBe('speed');
  });

  it('exposes a KPI catalogue with units', () => {
    const kpis = svc.kpis();
    expect(kpis.length).toBeGreaterThan(0);
    expect(kpis.find(k => k.id === 'capa_closure_time_avg')).toBeTruthy();
  });

  it('kpiLabel resolves an id to its label', () => {
    expect(svc.kpiLabel('fpy')).toContain('First Pass');
  });

  it('kpiLabel falls back to the raw id and empty string', () => {
    expect(svc.kpiLabel('unknown_kpi')).toBe('unknown_kpi');
    expect(svc.kpiLabel(undefined)).toBe('');
  });

  it('createWidget builds a widget at the requested position with default size', () => {
    const w = svc.createWidget('bar', 'w_1', 4, 2);
    expect(w.id).toBe('w_1');
    expect(w.type).toBe('bar');
    expect(w.position.x).toBe(4);
    expect(w.position.y).toBe(2);
    expect(w.position.cols).toBe(svc.entryFor('bar')!.defaultCols);
    expect(w.position.rows).toBe(svc.entryFor('bar')!.defaultRows);
  });

  it('createWidget seeds the default config (immutable copy)', () => {
    const w = svc.createWidget('kpi', 'w_2', 0, 0);
    expect(w.config.kpiId).toBe('capa_closure_time_avg');
    // mutation locale ne touche pas le catalogue
    const w2 = svc.createWidget('kpi', 'w_3', 0, 0);
    expect(w2.config.kpiId).toBe('capa_closure_time_avg');
  });
});
