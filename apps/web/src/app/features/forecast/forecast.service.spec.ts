import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ForecastService } from './forecast.service';
import { ForecastResponse } from './forecast.types';

describe('ForecastService', () => {
  let service: ForecastService;
  let httpMock: HttpTestingController;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai/forecast/kpi`;

  const sample: ForecastResponse = {
    n: 12, slope: 1.2, intercept: 75.0, residualSigma: 1.5, r2: 0.95,
    horizon: 6, target: 90, direction: 'at_least', probability: 0.82, confidence: 'high',
    model: 'holt_linear', seasonalPeriod: 0,
    points: [
      { step: 1, value: 76.2, low: 73.2, high: 79.2 },
      { step: 6, value: 82.2, low: 75.0, high: 89.4 }
    ]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ForecastService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST la série et la cible vers /ai/forecast/kpi', () => {
    let res: ForecastResponse | undefined;
    service.forecast({
      values: [62, 63, 61, 64, 66, 65], target: 90, horizon: 6, direction: 'at_least'
    }).subscribe(r => (res = r));

    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.values.length).toBe(6);
    expect(req.request.body.target).toBe(90);
    // Invariant multi-tenant : pas de tenant_id dans le corps (dérivé du JWT serveur).
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush(sample);
    expect(res!.probability).toBe(0.82);
    expect(res!.model).toBe('holt_linear');
    expect(res!.points.length).toBe(2);
  });

  it('transmet la période saisonnière quand elle est fournie', () => {
    service.forecast({ values: [1, 2, 3, 4, 5, 6, 7, 8], target: 20, seasonalPeriod: 4 }).subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.body.seasonalPeriod).toBe(4);
    req.flush(sample);
  });

  it('propage l\'erreur HTTP (ex. 502 passerelle)', () => {
    let err: any;
    service.forecast({ values: [1, 2, 3, 4], target: 10 }).subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(endpoint);
    req.flush('bad gateway', { status: 502, statusText: 'Bad Gateway' });
    expect(err.status).toBe(502);
  });
});
