import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ControlChartComponent } from './control-chart.component';

describe('ControlChartComponent', () => {
  let fixture: ComponentFixture<ControlChartComponent>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  function rendre(values: (number | null)[], ucl: number | null, cl: number | null, lcl: number | null): void {
    fixture.componentRef.setInput('values', values);
    fixture.componentRef.setInput('ucl', ucl);
    fixture.componentRef.setInput('cl', cl);
    fixture.componentRef.setInput('lcl', lcl);
    fixture.componentRef.setInput('label', 'Carte X̄');
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ declarations: [ControlChartComponent] }).compileComponents();
    fixture = TestBed.createComponent(ControlChartComponent);
  });

  it('dessine les limites et les points, et signale ceux qui sortent', () => {
    rendre([5, 6, 12, null], 10, 6, 2);
    expect(hote().querySelector('svg')?.getAttribute('aria-label')).toBe('Carte X̄');
    expect(hote().querySelectorAll('[data-test="point"]').length).toBe(2);
    expect(hote().querySelectorAll('[data-test="point-hors"]').length).toBe(1);
    expect(hote().querySelector('[data-test="ligne-UCL"]')?.textContent).toContain('UCL 10.00');
    expect(hote().querySelector('[data-test="hors-limites"]')?.textContent).toContain('1');
  });

  it('ne signale rien quand tout est dans les limites', () => {
    rendre([5, 6], 10, 6, 2);
    expect(hote().querySelector('[data-test="hors-limites"]')).toBeNull();
  });

  it('dit qu\'il n\'y a rien à tracer', () => {
    rendre([null], null, null, null);
    expect(hote().querySelector('svg')).toBeNull();
    expect(hote().textContent).toContain('Aucune donnée à tracer.');
  });
});
