import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DistributionChartComponent } from './distribution-chart.component';

describe('DistributionChartComponent', () => {
  let fixture: ComponentFixture<DistributionChartComponent>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  function rendre(values: number[], mean: number | null, sigma: number | null): void {
    fixture.componentRef.setInput('values', values);
    fixture.componentRef.setInput('mean', mean);
    fixture.componentRef.setInput('sigma', sigma);
    fixture.componentRef.setInput('lsl', 475);
    fixture.componentRef.setInput('usl', 525);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ declarations: [DistributionChartComponent] }).compileComponents();
    fixture = TestBed.createComponent(DistributionChartComponent);
  });

  it('dessine histogramme, zones hors tolérance, limites et moyenne', () => {
    rendre([470, 500, 501, 530], 500, 10);
    expect(hote().querySelector('svg')?.getAttribute('aria-label'))
      .toBe('Histogramme des mesures et limites de tolérance');
    expect(hote().querySelectorAll('[data-test="barre"]').length).toBeGreaterThan(1);
    expect(hote().querySelectorAll('.barre--hors').length).toBe(2);
    expect(hote().querySelector('[data-test="zone-basse"]')).not.toBeNull();
    expect(hote().textContent).toContain('LSL 475');
    expect(hote().textContent).toContain('USL 525');
    expect(hote().querySelector('[data-test="barre"] title')?.textContent).toContain('Mesures dans cette classe');
  });

  it('demande des données valides', () => {
    rendre([], null, null);
    expect(hote().querySelector('svg')).toBeNull();
    expect(hote().textContent).toContain('Saisissez au moins un sous-groupe complet');
  });
});
