import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Router } from '@angular/router';

import { environment } from '../../../environments/environment';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AiPrediction, TopRisk } from './dashboard.types';
import { DashboardComponent } from './dashboard.component';
import { DashboardService } from './dashboard.service';
import { CrossFilterService } from './interactivity/cross-filter.service';
import { TimeTravelService } from './interactivity/time-travel.service';
import { DashboardSnapshot } from './interactivity/time-travel.types';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let crossFilter: CrossFilterService;
  let timeTravel: jasmine.SpyObj<TimeTravelService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    timeTravel = jasmine.createSpyObj<TimeTravelService>('TimeTravelService', ['kpisAsOf']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);
    router.navigate.and.returnValue(Promise.resolve(true));

    TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      providers: [
        // DashboardService interroge désormais l'agrégat exécutif du serveur
        // (`/api/v1/dashboards/executive`) au lieu de renvoyer des constantes.
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        DashboardService,
        CrossFilterService,
        { provide: TimeTravelService, useValue: timeTravel },
        { provide: Router, useValue: router }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });

    component = TestBed.createComponent(DashboardComponent).componentInstance;
    crossFilter = TestBed.inject(CrossFilterService);
    component.ngOnInit();
  });

  it('initialises the executive observables', (done) => {
    component.kpis$.subscribe(kpis => {
      expect(Array.isArray(kpis)).toBeTrue();
      done();
    });
    // Le dashboard s'alimente désormais d'une agrégation serveur : sans réponse
    // servie, aucune valeur n'est émise (c'est le comportement attendu).
    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.apiBaseUrl}/api/v1/dashboards/executive`)
      .flush({
        kpis: [], qualityTrend: [], defectsByCategory: [], topRisks: [],
        alignment: [], generatedAt: '2026-05-15T00:00:00Z'
      });
  });

  it('maps alignment scores to a tone', () => {
    expect(component.alignmentTone(85)).toBe('success');
    expect(component.alignmentTone(65)).toBe('warn');
    expect(component.alignmentTone(40)).toBe('danger');
  });

  it('maps risk severity to a tone', () => {
    expect(component.severityTone('critical' as TopRisk['severity'])).toBe('danger');
    expect(component.severityTone('high' as TopRisk['severity'])).toBe('warn');
    expect(component.severityTone('medium' as TopRisk['severity'])).toBe('neutral');
  });

  it('maps each prediction kind to an icon', () => {
    expect(component.predictionIcon('drift' as AiPrediction['kind'])).toBe('monitoring');
    expect(component.predictionIcon('objective' as AiPrediction['kind'])).toBe('flag');
    expect(component.predictionIcon('supplier' as AiPrediction['kind'])).toBe('local_shipping');
    expect(component.predictionIcon('complaint' as AiPrediction['kind'])).toBe('forum');
  });

  it('trackByKpi returns the kpi code', () => {
    expect(component.trackByKpi(0, { code: 'DPMO' })).toBe('DPMO');
  });

  it('onParetoSelect applies a cross-filter; clearFilter removes it', () => {
    component.onParetoSelect({ category: 'Machine' } as never);
    expect(crossFilter.snapshot()?.value).toBe('Machine');
    component.clearFilter();
    expect(crossFilter.snapshot()).toBeNull();
  });

  it('onParetoSelect ignores a selection without category', () => {
    component.onParetoSelect({ category: undefined } as never);
    expect(crossFilter.snapshot()).toBeNull();
  });

  it('paretoAnchor returns the label only for the category dimension', () => {
    expect(component.paretoAnchor(null)).toBeNull();
    expect(component.paretoAnchor({ dimension: 'category', value: 'M', label: 'Machine' })).toBe('Machine');
    expect(component.paretoAnchor({ dimension: 'other', value: 'x', label: 'X' })).toBeNull();
  });

  it('applyTimeTravel does nothing without a selected date', () => {
    component.timeTravelDate = '';
    component.applyTimeTravel();
    expect(timeTravel.kpisAsOf).not.toHaveBeenCalled();
  });

  it('applyTimeTravel fetches the snapshot as-of midnight UTC of the chosen day', fakeAsync(() => {
    const snap = { asOf: '2026-03-15T00:00:00.000Z', kpis: [] } as unknown as DashboardSnapshot;
    timeTravel.kpisAsOf.and.returnValue(of(snap));
    component.timeTravelDate = '2026-03-15';
    component.applyTimeTravel();
    tick();
    expect(timeTravel.kpisAsOf).toHaveBeenCalledWith('2026-03-15T00:00:00.000Z');
    expect(component.snapshot$.value).toBe(snap);
    expect(component.timeTravelLoading$.value).toBeFalse();
  }));

  it('applyTimeTravel surfaces an error message on failure', fakeAsync(() => {
    timeTravel.kpisAsOf.and.returnValue(throwError(() => new Error('boom')));
    component.timeTravelDate = '2026-03-15';
    component.applyTimeTravel();
    tick();
    expect(component.timeTravelError$.value).toBeTruthy();
    expect(component.snapshot$.value).toBeNull();
    expect(component.timeTravelLoading$.value).toBeFalse();
  }));

  it('clearTimeTravel resets the date, snapshot and error', () => {
    component.timeTravelDate = '2026-03-15';
    component.snapshot$.next({ asOf: 'x', kpis: [] } as unknown as DashboardSnapshot);
    component.timeTravelError$.next('oops');
    component.clearTimeTravel();
    expect(component.timeTravelDate).toBe('');
    expect(component.snapshot$.value).toBeNull();
    expect(component.timeTravelError$.value).toBeNull();
  });

  it('ngOnDestroy clears the cross-filter', () => {
    crossFilter.apply({ dimension: 'category', value: 'Machine', label: 'Machine' });
    component.ngOnDestroy();
    expect(crossFilter.snapshot()).toBeNull();
  });

  // --- Actions de l'en-tête : ces trois boutons ne faisaient rien -------------

  it('ouvre le générateur de récit narratif depuis « Nouveau rapport »', () => {
    component.newReport();
    expect(router.navigate).toHaveBeenCalledWith(['/storyboard']);
  });

  it('ouvre l’item FMEA à l’origine d’un risque', () => {
    component.openRisk({
      id: 'r1', title: 'Rupture', source: 'FMEA · RPN 240',
      sourceType: 'FMEA', severity: 'critical'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/fmea', 'r1']);
  });

  it('ouvre le dossier CAPA à l’origine d’un risque', () => {
    component.openRisk({
      id: 'c9', title: 'CAPA en retard', source: 'CAPA',
      sourceType: 'CAPA', severity: 'high'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/capa', 'c9']);
  });

  it('exporte les KPIs affichés en CSV', () => {
    const created = document.createElement('a');
    const clickSpy = spyOn(created, 'click');
    spyOn(document, 'createElement').and.returnValue(created);
    spyOn(URL, 'createObjectURL').and.returnValue('blob:fake');
    const revokeSpy = spyOn(URL, 'revokeObjectURL');

    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.apiBaseUrl}/api/v1/dashboards/executive`)
      .flush({
        kpis: [{
          kpiId: 'k1', code: 'FPY', name: 'First Pass Yield', description: null,
          category: 'quality', unit: '%', direction: 'HIGHER_IS_BETTER',
          value: 94.2, targetValue: 95, trendDelta: 2.1, health: 'WARNING',
          latestPeriodStart: null, latestPeriodEnd: null
        }],
        qualityTrend: [], defectsByCategory: [], topRisks: [], alignment: [],
        generatedAt: '2026-05-15T00:00:00Z'
      });

    component.exportKpisCsv();

    expect(clickSpy).toHaveBeenCalled();
    // Le nom de fichier porte la date du jour, pas un identifiant opaque.
    expect(created.download).toMatch(/^qualitos-kpis-\d{4}-\d{2}-\d{2}\.csv$/);
    // L'URL temporaire est libérée : pas de fuite mémoire à chaque export.
    expect(revokeSpy).toHaveBeenCalledWith('blob:fake');
  });
});
