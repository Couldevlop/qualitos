import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { SpcService } from './spc.service';
import { KpiSpcForecastResponse, KpiSpcResponse, SpcAnalyzeResponse, SpcForecastResponse } from './spc.types';

describe('SpcService', () => {
  let service: SpcService;
  let httpMock: HttpTestingController;
  const base = environment.apiBaseUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SpcService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POSTs the series to the analyze endpoint and never leaks tenant in the body', () => {
    const expected: SpcAnalyzeResponse = {
      n: 3, outOfControl: false,
      limits: { centerLine: 5, sigma: 1, ucl: 8, lcl: 2, estimated: true },
      violations: []
    };
    let received: SpcAnalyzeResponse | undefined;
    service.analyze({ values: [4, 5, 6] }).subscribe(r => (received = r));

    const req = httpMock.expectOne(`${base}/api/v1/ai/spc/analyze`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ values: [4, 5, 6] });
    expect(JSON.stringify(req.request.body)).not.toContain('tenant');
    req.flush(expected);
    expect(received).toEqual(expected);
  });

  it('maps the KPI catalogue page into KpiOption list with size=200', () => {
    let options: { id: string; code: string; name: string; unit?: string }[] = [];
    service.listKpis().subscribe(o => (options = o));

    const req = httpMock.expectOne(r => r.url === `${base}/api/v1/kpis`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('size')).toBe('200');
    req.flush({
      content: [
        { id: '1', code: 'DPMO', name: 'Defects per million', unit: 'ppm' },
        { id: '2', code: 'FPY', name: 'First pass yield' }
      ]
    });

    expect(options.length).toBe(2);
    expect(options[0]).toEqual({ id: '1', code: 'DPMO', name: 'Defects per million', unit: 'ppm' });
    expect(options[1].unit).toBeUndefined();
  });

  it('returns an empty list when the KPI page has no content', () => {
    let options: unknown[] = [{ placeholder: true }];
    service.listKpis().subscribe(o => (options = o));
    httpMock.expectOne(r => r.url === `${base}/api/v1/kpis`).flush({});
    expect(options).toEqual([]);
  });

  it('analyzeKpi POSTs to the kpi endpoint with limit and openCapa query params', () => {
    const expected: KpiSpcResponse = {
      kpiId: 'k1', kpiCode: 'DPMO', kpiName: 'Defects', periods: ['2026-01'], values: [3],
      analysis: { n: 1, outOfControl: true,
        limits: { centerLine: 3, sigma: 0, ucl: 3, lcl: 3, estimated: true }, violations: [] },
      capaId: 'capa-9'
    };
    let received: KpiSpcResponse | undefined;
    service.analyzeKpi('k1', 30, true).subscribe(r => (received = r));

    const req = httpMock.expectOne(r => r.url === `${base}/api/v1/ai/spc/kpi/k1/analyze`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('limit')).toBe('30');
    expect(req.request.params.get('openCapa')).toBe('true');
    req.flush(expected);
    expect(received?.capaId).toBe('capa-9');
  });

  // --- prévision de sortie des limites (§6.5) ---

  const PREVISION = (o: Partial<SpcForecastResponse> = {}): SpcForecastResponse => ({
    n: 4, horizon: 6, threshold: 0.5, willBreach: true, firstBreachStep: 3, side: 'upper',
    trend: 0.4, residualSigma: 0.2, r2: 0.95, confidence: 'high', model: 'holt_linear',
    limits: { centerLine: 10, sigma: 1, ucl: 13, lcl: 7, estimated: true },
    points: [{ step: 1, value: 12, low: 10, high: 14, pAboveUcl: 0.2, pBelowLcl: 0, pBreach: 0.2 }],
    ...o
  });

  it('forecast POSTs the series to the forecast endpoint, without the tenant', () => {
    let recu: SpcForecastResponse | undefined;
    service.forecast([10, 10.6, 11.2, 11.8], 12).subscribe(r => (recu = r));

    const req = httpMock.expectOne(`${base}/api/v1/ai/spc/forecast`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ values: [10, 10.6, 11.2, 11.8], horizon: 12 });
    expect(JSON.stringify(req.request.body)).not.toContain('tenant');
    req.flush(PREVISION());
    expect(recu?.firstBreachStep).toBe(3);
  });

  it('forecast omits the optional baseline and threshold when they are not provided', () => {
    service.forecast([1, 2, 3, 4]).subscribe();
    const req = httpMock.expectOne(`${base}/api/v1/ai/spc/forecast`);
    // Envoyer center/sigma à null ferait estimer des limites nulles plutôt que
    // de les estimer depuis la série : l'absence doit rester une absence.
    expect(req.request.body).toEqual({ values: [1, 2, 3, 4] });
    req.flush(PREVISION({ willBreach: false, firstBreachStep: null, side: null }));
  });

  it('forecast carries the known baseline and the risk threshold through', () => {
    service.forecast([1, 2, 3, 4], 6, 10, 1, 0.9).subscribe();
    const req = httpMock.expectOne(`${base}/api/v1/ai/spc/forecast`);
    expect(req.request.body).toEqual(
      { values: [1, 2, 3, 4], horizon: 6, center: 10, sigma: 1, threshold: 0.9 });
    req.flush(PREVISION());
  });

  it('forecastKpi POSTs to the kpi forecast endpoint with limit, horizon and openCapa', () => {
    const attendu: KpiSpcForecastResponse = {
      kpiId: 'k1', kpiCode: 'OEE', kpiName: 'Rendement', unit: '%',
      periods: ['2026-11-01T00:00:00Z'], values: [10],
      forecast: PREVISION({ side: 'lower' }), capaId: 'capa-preventive-1'
    };
    let recu: KpiSpcForecastResponse | undefined;
    service.forecastKpi('k1', 30, 8, true).subscribe(r => (recu = r));

    const req = httpMock.expectOne(r => r.url === `${base}/api/v1/ai/spc/kpi/k1/forecast`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.get('limit')).toBe('30');
    expect(req.request.params.get('horizon')).toBe('8');
    expect(req.request.params.get('openCapa')).toBe('true');
    req.flush(attendu);
    expect(recu?.capaId).toBe('capa-preventive-1');
    expect(recu?.forecast.side).toBe('lower');
  });
});
