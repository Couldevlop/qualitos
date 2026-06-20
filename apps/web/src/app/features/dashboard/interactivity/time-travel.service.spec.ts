import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { TimeTravelService } from './time-travel.service';
import { DashboardSnapshot } from './time-travel.types';

describe('TimeTravelService', () => {
  let svc: TimeTravelService;
  let http: HttpTestingController;
  const base = environment.apiBaseUrl + '/api/v1/dashboards/time-travel/kpis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TimeTravelService]
    });
    svc = TestBed.inject(TimeTravelService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('kpisAsOf() GETs with asOf param and returns the snapshot', () => {
    const snap: DashboardSnapshot = {
      asOf: '2026-03-15T00:00:00.000Z',
      empty: false,
      kpis: [{ kpiId: 'k', code: 'fpy', name: 'FPY', unit: '%', value: 94.2,
               measuredPeriodStart: '2026-03-01T00:00:00Z', present: true }]
    };
    svc.kpisAsOf('2026-03-15T00:00:00.000Z').subscribe(r => {
      expect(r.empty).toBe(false);
      expect(r.kpis[0].code).toBe('fpy');
    });
    const req = http.expectOne(r => r.url === base
      && r.params.get('asOf') === '2026-03-15T00:00:00.000Z');
    expect(req.request.method).toBe('GET');
    req.flush(snap);
  });

  it('kpisAsOf() surfaces the empty-state snapshot', () => {
    svc.kpisAsOf('2020-01-01T00:00:00.000Z').subscribe(r => {
      expect(r.empty).toBe(true);
      expect(r.kpis).toEqual([]);
    });
    http.expectOne(() => true).flush({ asOf: '2020-01-01T00:00:00.000Z', empty: true, kpis: [] });
  });
});
