import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { SimpleChange } from '@angular/core';

import { DashboardBuilderModule } from '../../dashboard-builder.module';
import { Widget } from '../../domain/dashboard.model';
import { WidgetConfigPanelComponent } from './widget-config-panel.component';

function widget(partial: Partial<Widget>): Widget {
  return {
    id: 'w1', type: 'kpi', title: 'Titre',
    position: { x: 0, y: 0, cols: 3, rows: 3 }, config: {},
    ...partial
  };
}

describe('WidgetConfigPanelComponent', () => {
  let fixture: ComponentFixture<WidgetConfigPanelComponent>;
  let component: WidgetConfigPanelComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardBuilderModule, HttpClientTestingModule, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(WidgetConfigPanelComponent);
    component = fixture.componentInstance;
  });

  function load(w: Widget): void {
    component.widget = w;
    component.ngOnChanges({ widget: new SimpleChange(null, w, true) });
    fixture.detectChanges();
  }

  it('loads the working model from the widget config', () => {
    load(widget({ type: 'kpi', title: 'CAPA', config: { kpiId: 'fpy', kpiLabel: 'FPY', unit: '%', threshold: 95 } }));
    expect(component.title).toBe('CAPA');
    expect(component.kpiId).toBe('fpy');
    expect(component.kpiLabel).toBe('FPY');
    expect(component.unit).toBe('%');
    expect(component.threshold).toBe(95);
  });

  it('reports the type flags', () => {
    load(widget({ type: 'kpi', config: {} }));
    expect(component.isKpi).toBeTrue();
    expect(component.isDataDriven).toBeTrue();
    expect(component.isNarrative).toBeFalse();

    load(widget({ type: 'narrative', config: {} }));
    expect(component.isNarrative).toBeTrue();
    expect(component.isDataDriven).toBeFalse();
  });

  it('apply emits an immutable updated widget with the new title', () => {
    const w = widget({ type: 'kpi', config: { kpiId: 'fpy' } });
    load(w);
    const emitted: Widget[] = [];
    component.widgetChange.subscribe(x => emitted.push(x));
    component.title = 'Nouveau';
    component.apply();
    expect(emitted.length).toBe(1);
    expect(emitted[0].title).toBe('Nouveau');
    expect(emitted[0]).not.toBe(w);          // nouvelle référence (immuable)
  });

  it('apply keeps the previous title when emptied', () => {
    load(widget({ type: 'kpi', title: 'Garde', config: {} }));
    const emitted: Widget[] = [];
    component.widgetChange.subscribe(x => emitted.push(x));
    component.title = '   ';
    component.apply();
    expect(emitted[0].title).toBe('Garde');
  });

  it('onKpiSelected pre-fills label and unit when empty', () => {
    load(widget({ type: 'kpi', config: {} }));
    component.kpiLabel = '';
    component.unit = '';
    component.onKpiSelected('fpy');
    expect(component.kpiId).toBe('fpy');
    expect(component.kpiLabel.length).toBeGreaterThan(0);
    expect(component.unit).toBe('%');
  });

  it('onKpiSelected does not overwrite an existing label', () => {
    load(widget({ type: 'kpi', config: {} }));
    component.kpiLabel = 'Mon libellé';
    component.onKpiSelected('fpy');
    expect(component.kpiLabel).toBe('Mon libellé');
  });

  it('narrative config persists the text on apply', () => {
    load(widget({ type: 'narrative', config: {} }));
    const emitted: Widget[] = [];
    component.widgetChange.subscribe(x => emitted.push(x));
    component.text = 'Récit personnalisé';
    component.apply();
    expect(emitted[0].config.text).toBe('Récit personnalisé');
  });

  it('data-driven config persists kpiId on apply', () => {
    load(widget({ type: 'bar', config: {} }));
    const emitted: Widget[] = [];
    component.widgetChange.subscribe(x => emitted.push(x));
    component.kpiId = 'coq';
    component.apply();
    expect(emitted[0].config.kpiId).toBe('coq');
  });

  it('emits removed with the widget id', () => {
    load(widget({ id: 'to-del', config: {} }));
    const spy = jasmine.createSpy('removed');
    component.removed.subscribe(spy);
    component.remove();
    expect(spy).toHaveBeenCalledWith('to-del');
  });

  it('emits closed', () => {
    load(widget({ config: {} }));
    const spy = jasmine.createSpy('closed');
    component.closed.subscribe(spy);
    component.close();
    expect(spy).toHaveBeenCalled();
  });

  it('apply is a no-op when no widget loaded', () => {
    component.widget = null;
    const spy = jasmine.createSpy('change');
    component.widgetChange.subscribe(spy);
    component.apply();
    expect(spy).not.toHaveBeenCalled();
  });
});
