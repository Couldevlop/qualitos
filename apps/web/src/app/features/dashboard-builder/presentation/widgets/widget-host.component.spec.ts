import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { DashboardBuilderModule } from '../../dashboard-builder.module';
import { Widget } from '../../domain/dashboard.model';
import { WidgetHostComponent } from './widget-host.component';
import { HttpClientTestingModule } from '@angular/common/http/testing';

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

  it('renders KPI value for kpi widget', () => {
    setWidget({ id: 'k', type: 'kpi', title: 'KPI', position: { x:0, y:0, cols:2, rows:2 }, config: {} });
    expect(component.kpiValue).toBe(27);
  });

  it('renders narrative text', () => {
    setWidget({ id: 'n', type: 'narrative', title: 'N', position: { x:0, y:0, cols:2, rows:2 }, config: { text: 'hi' } });
    expect(component.narrative).toBe('hi');
  });

  it('renders default narrative when no text provided', () => {
    setWidget({ id: 'n', type: 'narrative', title: 'N', position: { x:0, y:0, cols:2, rows:2 }, config: {} });
    expect(component.narrative.length).toBeGreaterThan(10);
  });

  it('renders echarts options for chart widget', () => {
    setWidget({ id: 'b', type: 'bar', title: 'B', position: { x:0, y:0, cols:2, rows:2 }, config: {} });
    expect(component.echartsOptions).not.toBeNull();
  });

  it('trendClass returns trend-down for negative trend', () => {
    component.widget = { id: 'k', type: 'kpi', title: 'k', position: { x:0, y:0, cols:2, rows:2 }, config: {} };
    fixture.detectChanges();
    component['kpiTrend'] = -3;
    expect(component.trendClass()).toContain('trend-down');
  });

  it('trendClass returns trend-up for positive trend', () => {
    component.widget = { id: 'k', type: 'kpi', title: 'k', position: { x:0, y:0, cols:2, rows:2 }, config: {} };
    fixture.detectChanges();
    component['kpiTrend'] = 5;
    expect(component.trendClass()).toContain('trend-up');
  });

  it('trendLabel formats absolute percent', () => {
    component.widget = { id: 'k', type: 'kpi', title: 'k', position: { x:0, y:0, cols:2, rows:2 }, config: {} };
    fixture.detectChanges();
    component['kpiTrend'] = -3;
    expect(component.trendLabel()).toContain('3');
  });
});
