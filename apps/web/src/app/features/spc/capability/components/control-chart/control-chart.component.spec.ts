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

  it('trace la USL et la LSL quand on les lui donne', () => {
    rendre([5, 6], 10, 6, 2);
    expect(hote().querySelector('[data-test="ligne-USL"]')).toBeNull();
    fixture.componentRef.setInput('usl', 12);
    fixture.componentRef.setInput('lsl', 1);
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="ligne-USL"]')?.textContent).toContain('USL 12.00');
    expect(hote().querySelector('[data-test="ligne-LSL"]')?.textContent).toContain('LSL 1.00');
  });

  it('ne signale rien quand tout est dans les limites', () => {
    rendre([5, 6], 10, 6, 2);
    expect(hote().querySelector('[data-test="hors-limites"]')).toBeNull();
  });

  it('marque les points visés par une règle de Nelson et pose son abrégé', () => {
    rendre([5, 6, 7], 10, 6, 2);
    expect(hote().querySelector('[data-test="point-regle"]')).toBeNull();
    fixture.componentRef.setInput('rules', new Map([[1, ['N2', 'N5']]]));
    fixture.detectChanges();
    expect(hote().querySelectorAll('[data-test="point-regle"]').length).toBe(1);
    expect(hote().querySelector('[data-test="etiquette-regle"]')?.textContent).toContain('N2 N5');
    // Le point reste dans les limites : c'est précisément ce que la règle ajoute.
    expect(hote().querySelectorAll('[data-test="point-hors"]').length).toBe(0);
    expect(hote().querySelector('[data-test="signales-nelson"]')?.textContent).toContain('1');
    expect(hote().querySelectorAll('.point--regle').length).toBe(1);
    const titre = hote().querySelectorAll('title')[1];
    expect(titre.textContent).toBe('SG 2 : 6.00 — N2, N5');
  });

  it('ne compte pas deux fois un point hors limites que la règle 1 signale', () => {
    rendre([5, 12], 10, 6, 2);
    fixture.componentRef.setInput('rules', new Map([[1, ['N1']]]));
    fixture.detectChanges();
    expect(hote().querySelectorAll('[data-test="point-hors"]').length).toBe(1);
    expect(hote().querySelector('[data-test="hors-limites"]')?.textContent).toContain('1');
    expect(hote().querySelector('[data-test="signales-nelson"]')).toBeNull();
    // Le halo reste : la règle est nommée même quand le point sort déjà.
    expect(hote().querySelectorAll('[data-test="point-regle"]').length).toBe(1);
  });

  it('ne trace les zones ±1σ / ±2σ que lorsqu\'on le demande', () => {
    rendre([5, 6], 9, 6, 3);
    expect(hote().querySelectorAll('[data-test="zone"]').length).toBe(0);
    fixture.componentRef.setInput('showZones', true);
    fixture.detectChanges();
    const zones = hote().querySelectorAll('[data-test="zone"]');
    expect(zones.length).toBe(4);
    expect(zones[0].textContent).toContain('+2σ');
    expect(zones[3].textContent).toContain('−2σ');
  });

  it('dit qu\'il n\'y a rien à tracer', () => {
    rendre([null], null, null, null);
    expect(hote().querySelector('svg')).toBeNull();
    expect(hote().textContent).toContain('Aucune donnée à tracer.');
  });
});
