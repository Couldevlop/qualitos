import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuthzService } from '../../../../core/authz/authz.service';
import { SpcService } from '../../spc.service';
import {
  KpiSpcForecastResponse,
  KpiSpcResponse,
  SpcAnalyzeResponse,
  SpcForecastResponse
} from '../../spc.types';
import { SpcAnalyzeComponent } from './spc-analyze.component';

/**
 * Détection d'anomalies SPC : saisie d'une série ou d'un KPI, verdict, carte et
 * règles de Nelson, et des messages d'erreur qui disent quoi faire.
 */
/** Le droit d'ouvrir un dossier, que seul le dernier banc retire. */
let peutOuvrirUneCapa = true;

describe('SpcAnalyzeComponent', () => {
  let fixture: ComponentFixture<SpcAnalyzeComponent>;
  let component: SpcAnalyzeComponent;
  let spc: jasmine.SpyObj<SpcService>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const texte0 = (): string => hote().textContent ?? '';

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
    spc = jasmine.createSpyObj<SpcService>('SpcService',
      ['listKpis', 'analyze', 'analyzeKpi', 'forecast', 'forecastKpi']);
    spc.listKpis.and.returnValue(kpis === 'erreur' ? throwError(() => ({ status: 500 })) : of(kpis));
    await TestBed.configureTestingModule({
      declarations: [SpcAnalyzeComponent],
      imports: [SharedModule, UiModule, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: SpcService, useValue: spc },
        // Faire OUVRIR une CAPA par la carte demande capa.create : la case a
        // cocher ne se propose qu'a qui l'obtiendra. Ces bancs testent la carte,
        // pas la matrice -- le refus a son propre banc, en bas de fichier.
        { provide: AuthzService, useValue: { can: () => of(peutOuvrirUneCapa) } }
      ]
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
    expect((component.chartOption as { grid: { right: number } }).grid.right).toBeGreaterThanOrEqual(90);
    expect(texte0()).toContain('± 3 sigmas');
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

  // --- prévision de sortie des limites (§6.5) ---

  const PREVISION = (o: Partial<SpcForecastResponse> = {}): SpcForecastResponse => ({
    n: 4, horizon: 6, threshold: 0.5, willBreach: true, firstBreachStep: 3, side: 'upper',
    trend: 0.42, residualSigma: 0.2, r2: 0.95, confidence: 'high', model: 'holt_linear',
    limits: { centerLine: 10, sigma: 0.15, ucl: 10.45, lcl: 9.55, estimated: true },
    points: [
      { step: 1, value: 10.2, low: 10.0, high: 10.4, pAboveUcl: 0.1, pBelowLcl: 0, pBreach: 0.1 },
      { step: 2, value: 10.4, low: 10.1, high: 10.7, pAboveUcl: 0.35, pBelowLcl: 0, pBreach: 0.35 },
      { step: 3, value: 10.6, low: 10.2, high: 11.0, pAboveUcl: 0.72, pBelowLcl: 0, pBreach: 0.72 }
    ],
    ...o
  });

  /** Amène l'écran à l'état « une série est analysée », prêt pour la prévision. */
  async function serieAnalysee(): Promise<void> {
    await setup();
    spc.analyze.and.returnValue(of(RESULTAT()));
    component.loadExample();
    component.analyze();
    fixture.detectChanges();
  }

  it('sans série analysée, le panneau de prévision reste absent', async () => {
    await setup();
    expect(component.canForecast).toBeFalse();
    expect(hote().querySelector('[data-test="forecast-run"]')).toBeNull();
  });

  it('une série trop courte ne propose pas de prévision : 3 points ne datent pas une dérive', async () => {
    await setup();
    spc.analyze.and.returnValue(of(RESULTAT({ n: 3 })));
    component.form.patchValue({ valuesText: '10 10.5 11' });
    component.analyze();
    fixture.detectChanges();
    expect(component.canForecast).toBeFalse();
    expect(hote().querySelector('[data-test="forecast-run"]')).toBeNull();
  });

  it('annonce la période de sortie, le côté franchi et la confiance, et trace la projection', async () => {
    await serieAnalysee();
    expect(component.canForecast).toBeTrue();
    spc.forecast.and.returnValue(of(PREVISION()));

    component.runForecast();
    fixture.detectChanges();

    // La prévision repart de la série affichée, avec la baseline saisie (ici aucune).
    expect(spc.forecast).toHaveBeenCalledWith(
      jasmine.any(Array), 6, undefined, undefined);
    expect(spc.forecast.calls.mostRecent().args[0].length).toBe(11);

    const texte = texte0();
    expect(texte).toContain('Le procédé franchira sa limite supérieure dans 3 période(s).');
    expect(texte).toContain('Confiance élevée');
    expect(texte).toContain('+0.420 par période');
    expect(texte).toContain('holt_linear');
    // La projection se lit DANS la carte : l'axe est prolongé des périodes à venir.
    const option = component.chartOption as {
      xAxis: { data: string[] };
      series: Array<{ name: string; data: Array<number | null> }>;
    };
    expect(option.xAxis.data.length).toBe(11 + 3);
    expect(option.xAxis.data[13]).toBe('+3');
    const projection = option.series[2];
    expect(projection.name).toBe('Projection');
    // Elle démarre au dernier point mesuré, sinon la courbe serait en l'air.
    expect(projection.data[10]).toBe(13.6);
    expect(projection.data[0]).toBeNull();
    expect(projection.data[13]).toBe(10.6);
    // La période au-delà du seuil se repère sans lire les chiffres.
    expect(hote().querySelectorAll('tr.row-breach').length).toBe(1);
    expect(texte).toContain('72 %');
  });

  it('une absence de sortie annoncée se dit sans inventer d’étape', async () => {
    await serieAnalysee();
    spc.forecast.and.returnValue(of(PREVISION({
      willBreach: false, firstBreachStep: null, side: null, confidence: 'low'
    })));

    component.runForecast();
    fixture.detectChanges();

    const texte = texte0();
    expect(texte).toContain('Aucune sortie des limites annoncée sur les 6 prochaines périodes.');
    expect(texte).toContain('Confiance faible');
    expect(texte).not.toContain('franchira');
  });

  it('annonce une sortie par le bas quand c’est la limite inférieure qui est franchie', async () => {
    await serieAnalysee();
    spc.forecast.and.returnValue(of(PREVISION({ side: 'lower', firstBreachStep: 2 })));
    component.runForecast();
    fixture.detectChanges();
    expect(texte0()).toContain('Le procédé franchira sa limite inférieure dans 2 période(s).');
  });

  it('transmet la baseline saisie à la prévision : mêmes limites que la carte', async () => {
    await setup();
    spc.analyze.and.returnValue(of(RESULTAT()));
    component.form.patchValue({ valuesText: '10 10.1 9.9 10.2 10.3', center: 10, sigma: 0.15 });
    component.analyze();
    spc.forecast.and.returnValue(of(PREVISION()));
    component.forecastForm.patchValue({ horizon: 12 });

    component.runForecast();

    expect(spc.forecast).toHaveBeenCalledWith([10, 10.1, 9.9, 10.2, 10.3], 12, 10, 0.15);
  });

  it('depuis un KPI, la prévision passe par le KPI et peut ouvrir une CAPA préventive', async () => {
    await setup([{ id: 'k1', code: 'K1', name: 'Taux de rebut' }]);
    const reponseKpi: KpiSpcResponse = {
      kpiId: 'k1', kpiCode: 'K1', kpiName: 'Taux de rebut',
      periods: ['2026-10-01T00:00:00Z', '2026-10-02T00:00:00Z',
                '2026-10-03T00:00:00Z', '2026-10-04T00:00:00Z'],
      values: [10, 10.2, 10.3, 10.4], analysis: RESULTAT({ n: 4 }), capaId: null
    };
    spc.analyzeKpi.and.returnValue(of(reponseKpi));
    component.kpiForm.patchValue({ kpiId: 'k1', limit: 20 });
    component.analyzeKpi();
    fixture.detectChanges();

    const prevue: KpiSpcForecastResponse = {
      kpiId: 'k1', kpiCode: 'K1', kpiName: 'Taux de rebut',
      periods: reponseKpi.periods, values: reponseKpi.values,
      forecast: PREVISION(), capaId: 'capa-prev-1'
    };
    spc.forecastKpi.and.returnValue(of(prevue));
    component.forecastForm.patchValue({ horizon: 4, openCapa: true });
    component.runForecast();
    fixture.detectChanges();

    expect(spc.forecastKpi).toHaveBeenCalledWith('k1', 20, 4, true);
    expect(spc.forecast).not.toHaveBeenCalled();
    expect(component.forecastCapaId).toBe('capa-prev-1');
    const lien = hote().querySelector('[data-test="forecast-capa"]');
    expect(lien).not.toBeNull();
    expect(lien!.textContent).toContain('CAPA préventive ouverte');
  });

  it('une prévision KPI sans CAPA n’affiche aucun lien de dossier', async () => {
    await setup([{ id: 'k1', code: 'K1', name: 'Taux de rebut' }]);
    spc.analyzeKpi.and.returnValue(of({
      kpiId: 'k1', kpiCode: 'K1', kpiName: 'Taux de rebut',
      periods: ['a', 'b', 'c', 'd'], values: [1, 2, 3, 4],
      analysis: RESULTAT({ n: 4 }), capaId: null
    } as KpiSpcResponse));
    component.kpiForm.patchValue({ kpiId: 'k1' });
    component.analyzeKpi();
    spc.forecastKpi.and.returnValue(of({
      kpiId: 'k1', kpiCode: 'K1', kpiName: 'Taux de rebut',
      periods: [], values: [], forecast: PREVISION(), capaId: null
    } as KpiSpcForecastResponse));

    component.runForecast();
    fixture.detectChanges();

    expect(component.forecastCapaId).toBeNull();
    expect(hote().querySelector('[data-test="forecast-capa"]')).toBeNull();
  });

  it('un refus du serveur sur la prévision dit quoi faire, sans toucher à la carte', async () => {
    await serieAnalysee();
    const carteAvant = component.chartOption;

    spc.forecast.and.returnValue(throwError(() => new HttpErrorResponse({ status: 422 })));
    component.runForecast();
    fixture.detectChanges();
    expect(component.forecastError).toContain('Au moins 4 mesures');
    expect(component.forecast).toBeNull();
    expect(component.chartOption).toBe(carteAvant);
    expect(hote().querySelector('[data-test="forecast-error"]')).not.toBeNull();

    spc.forecast.and.returnValue(throwError(() => new HttpErrorResponse({ status: 429 })));
    component.runForecast();
    expect(component.forecastError).toContain('réessayez dans une minute');
    expect(component.forecastLoading).toBeFalse();
  });

  it('ignore un second lancement de prévision, et un horizon hors bornes', async () => {
    await serieAnalysee();
    spc.forecast.and.returnValue(of(PREVISION()));

    component.forecastLoading = true;
    component.runForecast();
    expect(spc.forecast).not.toHaveBeenCalled();

    component.forecastLoading = false;
    component.forecastForm.patchValue({ horizon: 61 });
    component.runForecast();
    expect(spc.forecast).not.toHaveBeenCalled();
  });

  /** Une nouvelle analyse remet le compteur à zéro : une prévision périmée serait un mensonge. */
  it('relancer l’analyse efface la prévision précédente', async () => {
    await serieAnalysee();
    spc.forecast.and.returnValue(of(PREVISION()));
    component.runForecast();
    expect(component.forecast).not.toBeNull();

    component.analyze();
    fixture.detectChanges();
    expect(component.forecast).toBeNull();
    expect(component.forecastCapaId).toBeNull();
    expect(texte0()).not.toContain('franchira');
  });

  // ---- la case qui fait ouvrir un dossier CAPA ----

  /**
   * Lire la carte ne demande rien : savoir si un procede est sous controle n'est
   * le secret de personne. Lui faire OUVRIR un dossier CAPA, si -- c'est le meme
   * droit que sur la fiche CAPA, et le serveur le verifie desormais. L'ecran doit
   * s'aligner : une case cochee par qui n'a pas le droit ferait refuser jusqu'a
   * l'affichage de la carte.
   */
  it('ne propose pas de faire ouvrir une CAPA sans le droit de l en ouvrir une', async () => {
    peutOuvrirUneCapa = false;
    try {
      await setup();
      const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';

      expect(component.canOpenCapa).toBeFalse();
      expect(texte).not.toContain('Ouvrir une CAPA');
      // Et la case ne reste pas cochee en coulisse : le formulaire enverrait
      // openCapa=true depuis un ecran qui ne la montre plus.
      expect(component.kpiForm.value.openCapa).toBeFalse();
      expect(component.forecastForm.value.openCapa).toBeFalse();
    } finally {
      peutOuvrirUneCapa = true;
    }
  });

  it('la propose a qui peut ouvrir un dossier', async () => {
    await setup();
    expect(component.canOpenCapa).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Ouvrir une CAPA');
  });
});
