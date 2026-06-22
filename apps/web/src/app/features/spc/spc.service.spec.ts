import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { SpcService } from './spc.service';
import { KpiSpcResponse, SpcAnalyzeResponse } from './spc.types';

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
});
