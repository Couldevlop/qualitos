import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SpcService } from '../../spc.service';
import { KpiSpcResponse, SpcAnalyzeResponse } from '../../spc.types';
import { SpcAnalyzeComponent } from './spc-analyze.component';

/**
 * Détection d'anomalies SPC : saisie d'une série ou d'un KPI, verdict, carte et
 * règles de Nelson, et des messages d'erreur qui disent quoi faire.
 */
describe('SpcAnalyzeComponent', () => {
  let fixture: ComponentFixture<SpcAnalyzeComponent>;
  let component: SpcAnalyzeComponent;
  let spc: jasmine.SpyObj<SpcService>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const RESULTAT = (o: Partial<SpcAnalyzeResponse> = {}): SpcAnalyzeResponse => ({
    n: 11,
    outOfControl: true,
    limits: { centerLine: 10, sigma: 0.15, ucl: 10.45, lcl: 9.55, estimated: true },
    violations: [
      { rule: 'R1', title: 'Point hors limites', description: 'Un point au-delà de 3σ', pointIndices: [10], severity: 'CRITICAL' },
      { rule: 'R2', title: 'Série d’un côté', description: '9 points du même côté', pointIndices: [1, 2], severity: 'medium' },
      { rule: 'R5', title: 'Tendance', description: '2 sur 3 au-delà de 2σ', pointIndices: [3], severity: 'low' }
    ],
    ...o
  });

  async function setup(kpis: 'erreur' | { id: string; code: string; name: string }[] = []): Promise<void> {
    spc = jasmine.createSpyObj<SpcService>('SpcService', ['listKpis', 'analyze', 'analyzeKpi']);
    spc.listKpis.and.returnValue(kpis === 'erreur' ? throwError(() => ({ status: 500 })) : of(kpis));
    await TestBed.configureTestingModule({
      declarations: [SpcAnalyzeComponent],
      imports: [SharedModule, UiModule, ReactiveFormsModule, NoopAnimationsModule],
      providers: [provideRouter([]), { provide: SpcService, useValue: spc }]
    }).compileComponents();
    fixture = TestBed.createComponent(SpcAnalyzeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('sans KPI au catalogue, le dit sous le champ ; une liste en échec laisse la saisie utilisable', async () => {
    await setup('erreur');
    expect(component.kpis).toEqual([]);
    expect(hote().textContent).toContain('Aucun KPI au catalogue.');
    expect(hote().textContent).toContain('Ligne centrale');
  });

  it('analyse l’exemple et montre verdict, sévérités traduites et points en cause', async () => {
    await setup([{ id: 'k1', code: 'K1', name: 'Taux de rebut' }]);
    spc.analyze.and.returnValue(of(RESULTAT()));
    component.loadExample();
    component.analyze();
    fixture.detectChanges();

    expect(spc.analyze).toHaveBeenCalledWith(jasmine.objectContaining({ center: undefined, sigma: undefined }));
    expect(component.chartOption).not.toBeNull();
    const texte = hote().textContent!;
    expect(texte).toContain('Hors contrôle');
    expect(texte).toContain('3 règle(s) de Nelson déclenchée(s) · 11 points');
    expect(texte).toContain('Critique');
    expect(texte).toContain('Majeure');
    expect(texte).toContain('Mineure');
    expect(texte).not.toContain('CRITICAL');
    expect(hote().querySelector('.table-scroll table.violations-table')).not.toBeNull();
    expect(hote().querySelectorAll('.pt-chip').length).toBe(4);
  });

  it('une série maîtrisée le dit sans tableau', async () => {
    await setup();
    spc.analyze.and.returnValue(of(RESULTAT({ outOfControl: false, violations: [],
      limits: { centerLine: 10, sigma: 0.1, ucl: 10.3, lcl: 9.7, estimated: false } })));
    component.form.patchValue({ valuesText: '10 10.1 9.9', center: 10, sigma: 0.1 });
    component.analyze();
    fixture.detectChanges();

    const texte = hote().textContent!;
    expect(texte).toContain('Sous contrôle');
    expect(texte).toContain('Aucune règle de Nelson déclenchée · 11 points');
    expect(texte).toContain('Valeur fournie');
    expect(texte).toContain('le procédé est statistiquement maîtrisé.');
    expect(hote().querySelector('.violations-table')).toBeNull();
  });

  it('refuse une série vide, une ligne centrale sans σ et un σ nul, sans appeler le service', async () => {
    await setup();
    component.analyze();
    expect(component.error).toContain('au moins une mesure');

    component.form.patchValue({ valuesText: '1 2 3', center: 10, sigma: null });
    component.analyze();
    expect(component.error).toBe('Renseignez la ligne centrale et σ ensemble, ou laissez les deux vides.');

    component.form.patchValue({ center: 10, sigma: 0 });
    component.analyze();
    expect(component.error).toContain('strictement positif');
    expect(spc.analyze).not.toHaveBeenCalled();
  });

  it('chaque échec du serveur dit quoi faire, sans jargon technique', async () => {
    await setup();
    component.form.patchValue({ valuesText: '1 2 3' });
    const cas: [number, string][] = [
      [0, 'Le serveur ne répond pas'],
      [400, 'de 1 à 10 000 mesures'],
      [413, 'trop longue'],
      [429, 'réessayez dans une minute'],
      [502, 'momentanément indisponible'],
      [503, 'momentanément indisponible'],
      [500, 'erreur 500']
    ];
    for (const [status, attendu] of cas) {
      spc.analyze.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
      component.analyze();
      expect(component.loading).toBeFalse();
      expect(component.error).toContain(attendu);
      expect(component.error).not.toMatch(/8082|engine|tenant|disjoncteur|garde-fou/);
    }
  });

  it('analyse un KPI : contexte, CAPA ouverte et dates en abscisse', async () => {
    await setup([{ id: 'k1', code: 'K1', name: 'Taux de rebut' }]);
    const reponse: KpiSpcResponse = {
      kpiId: 'k1', kpiCode: 'K1', kpiName: 'Taux de rebut',
      periods: ['2026-10-01T00:00:00Z', 'pas une date'], values: [1, 2],
      analysis: RESULTAT({ n: 2 }), capaId: 'capa-7'
    };
    spc.analyzeKpi.and.returnValue(of(reponse));

    component.analyzeKpi();
    expect(component.error).toBe('Sélectionnez un KPI.');

    component.kpiForm.patchValue({ kpiId: 'k1', limit: 20, openCapa: true });
    component.analyzeKpi();
    fixture.detectChanges();

    expect(spc.analyzeKpi).toHaveBeenCalledWith('k1', 20, true);
    expect(component.kpiContext).toEqual({ code: 'K1', name: 'Taux de rebut', capaId: 'capa-7' });
    expect(hote().querySelector('.kpi-banner .capa-link')).not.toBeNull();
    const axe = (component.chartOption as { xAxis: { data: string[] } }).xAxis.data;
    expect(axe[1]).toBe('pas une date');
  });

  it('un KPI sans assez de mesures le dit ; un autre échec reprend le message commun', async () => {
    await setup([{ id: 'k1', code: 'K1', name: 'Taux de rebut' }]);
    component.kpiForm.patchValue({ kpiId: 'k1' });

    spc.analyzeKpi.and.returnValue(throwError(() => new HttpErrorResponse({ status: 422 })));
    component.analyzeKpi();
    expect(component.error).toContain('Au moins 2 mesures');

    spc.analyzeKpi.and.returnValue(throwError(() => new HttpErrorResponse({ status: 429 })));
    component.analyzeKpi();
    expect(component.error).toContain('réessayez dans une minute');
  });

  it('ignore un second lancement pendant une analyse en cours', async () => {
    await setup();
    component.loading = true;
    component.analyze();
    component.analyzeKpi();
    expect(spc.analyze).not.toHaveBeenCalled();
    expect(spc.analyzeKpi).not.toHaveBeenCalled();
  });

  it('une réponse sans limites ni violations ne casse pas la carte', async () => {
    await setup();
    spc.analyze.and.returnValue(of({ n: 2, outOfControl: false } as unknown as SpcAnalyzeResponse));
    component.form.patchValue({ valuesText: '1, 2' });
    component.analyze();
    const serie = (component.chartOption as { series: Record<string, unknown>[] }).series[0];
    expect(serie['markLine']).toBeUndefined();
  });
});
