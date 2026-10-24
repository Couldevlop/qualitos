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

  it('écrit la moyenne avec la précision de l\'étude, pas arrondie au dixième', () => {
    fixture.componentRef.setInput('digits', 4);
    fixture.componentRef.setInput('values', [12.04, 12.05, 12.06]);
    fixture.componentRef.setInput('mean', 12.0533);
    fixture.componentRef.setInput('sigma', 0.0234);
    fixture.componentRef.setInput('lsl', 11.9);
    fixture.componentRef.setInput('usl', 12.1);
    fixture.detectChanges();
    // Arrondie à « 12.1 », elle se confondait avec l'USL.
    expect(hote().querySelector('.moyenne text')?.textContent).toContain('12.0533');
  });

  it('demande des données valides', () => {
    rendre([], null, null);
    expect(hote().querySelector('svg')).toBeNull();
    expect(hote().textContent).toContain('Saisissez au moins un sous-groupe complet');
  });
});
