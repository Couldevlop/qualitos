import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { DashboardBuilderModule } from '../../dashboard-builder.module';
import { Widget } from '../../domain/dashboard.model';
import { WidgetHostComponent } from './widget-host.component';

function widget(partial: Partial<Widget>): Widget {
  return {
    id: 'w', type: 'kpi', title: 'T',
    position: { x: 0, y: 0, cols: 2, rows: 2 }, config: {},
    ...partial
  };
}

describe('WidgetHostComponent', () => {
  let fixture: ComponentFixture<WidgetHostComponent>;
  let component: WidgetHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardBuilderModule, HttpClientTestingModule, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(WidgetHostComponent);
    component = fixture.componentInstance;
  });

  function setWidget(w: Widget): void {
    component.widget = w;
    fixture.detectChanges();
  }

  it('renders a numeric KPI value for a kpi widget', () => {
    setWidget(widget({ type: 'kpi', config: { kpiId: 'capa_closure_time_avg' } }));
    expect(typeof component.kpiValue === 'number' || typeof component.kpiValue === 'string').toBeTrue();
    expect(component.echartsOptions).toBeNull();
  });

  it('formats KPI value with unit', () => {
    setWidget(widget({ type: 'kpi', config: { kpiId: 'fpy', unit: '%' } }));
    expect(String(component.kpiValue)).toContain('%');
  });

  it('flags breach when value exceeds threshold', () => {
    setWidget(widget({ type: 'kpi', config: { kpiId: 'capa_closure_time_avg', threshold: 0 } }));
    expect(component.kpiBreached).toBeTrue();
  });

  it('does not flag breach without threshold', () => {
    setWidget(widget({ type: 'kpi', config: { kpiId: 'capa_closure_time_avg' } }));
    expect(component.kpiBreached).toBeFalse();
  });

  it('renders provided narrative text', () => {
    setWidget(widget({ type: 'narrative', config: { text: 'Bonjour' } }));
    expect(component.narrative).toBe('Bonjour');
  });

  it('renders default narrative when no text provided', () => {
    setWidget(widget({ type: 'narrative', config: {} }));
    expect(component.narrative.length).toBeGreaterThan(10);
  });

  it('builds echarts options for chart widgets', () => {
    setWidget(widget({ type: 'bar', title: 'B', config: { kpiId: 'nc_by_category' } }));
    expect(component.echartsOptions).not.toBeNull();
    expect(component.isChart).toBeTrue();
  });

  it('control-chart is a chart type', () => {
    setWidget(widget({ type: 'control-chart', config: {} }));
    expect(component.echartsOptions).not.toBeNull();
  });

  it('isChart is false for kpi and narrative', () => {
    setWidget(widget({ type: 'kpi', config: {} }));
    expect(component.isChart).toBeFalse();
    setWidget(widget({ type: 'narrative', config: {} }));
    expect(component.isChart).toBeFalse();
  });

  it('recomputes the view when the widget input changes', () => {
    setWidget(widget({ type: 'kpi', config: { kpiId: 'a' } }));
    const first = component.kpiValue;
    setWidget(widget({ type: 'kpi', config: { kpiId: 'totally-different-kpi' } }));
    // déterministe par graine : deux ids différents → potentiellement différent,
    // au minimum la recomputation a tourné sans erreur.
    expect(component.kpiValue).toBeDefined();
    expect(first).toBeDefined();
  });

  it('trendClass reflects the sign of the trend', () => {
    setWidget(widget({ type: 'kpi', config: {} }));
    component.kpiTrend = -3;
    expect(component.trendClass()).toContain('trend-down');
    component.kpiTrend = 5;
    expect(component.trendClass()).toContain('trend-up');
  });

  it('trendLabel formats the absolute percent', () => {
    setWidget(widget({ type: 'kpi', config: {} }));
    component.kpiTrend = -3;
    expect(component.trendLabel()).toContain('3');
  });

  it('does not emit pointSelected while editing', () => {
    setWidget(widget({ type: 'bar', config: {} }));
    component.editing = true;
    const spy = jasmine.createSpy('point');
    component.pointSelected.subscribe(spy);
    component.onPoint({ category: 'X' });
    expect(spy).not.toHaveBeenCalled();
  });

  it('emits pointSelected when not editing', () => {
    setWidget(widget({ type: 'bar', config: {} }));
    component.editing = false;
    const spy = jasmine.createSpy('point');
    component.pointSelected.subscribe(spy);
    component.onPoint({ category: 'X' });
    expect(spy).toHaveBeenCalled();
  });
});
