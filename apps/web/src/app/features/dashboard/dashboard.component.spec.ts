import { NO_ERRORS_SCHEMA } from '@angular/core';
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

  beforeEach(() => {
    timeTravel = jasmine.createSpyObj<TimeTravelService>('TimeTravelService', ['kpisAsOf']);

    TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      providers: [
        DashboardService,
        CrossFilterService,
        { provide: TimeTravelService, useValue: timeTravel }
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
});
