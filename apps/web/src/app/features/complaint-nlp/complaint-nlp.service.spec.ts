import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ComplaintNlpService } from './complaint-nlp.service';
import { ComplaintAnalyzeResponse } from './complaint-nlp.types';

describe('ComplaintNlpService', () => {
  let service: ComplaintNlpService;
  let httpMock: HttpTestingController;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai/complaints/analyze`;

  const sample: ComplaintAnalyzeResponse = {
    n: 2, criticalCount: 1,
    insights: [
      { index: 0, sentiment: -0.8, sentimentLabel: 'negative', category: 'securite', critical: true },
      { index: 1, sentiment: 0.7, sentimentLabel: 'positive', category: 'service', critical: false }
    ]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ComplaintNlpService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST le lot vers /ai/complaints/analyze', () => {
    let res: ComplaintAnalyzeResponse | undefined;
    service.analyze({ texts: ['dangereux rappel', 'service parfait'] }).subscribe(r => (res = r));

    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.texts.length).toBe(2);
    // Invariant multi-tenant : pas de tenant_id dans le corps (dérivé du JWT serveur).
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush(sample);
    expect(res!.criticalCount).toBe(1);
    expect(res!.insights[0].category).toBe('securite');
  });

  it('transmet une taxonomie personnalisée', () => {
    service.analyze({ texts: ['chambre sale'], categories: { hygiene: ['sale'] } }).subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.body.categories.hygiene).toEqual(['sale']);
    req.flush(sample);
  });

  it('propage l\'erreur HTTP (ex. 502 passerelle)', () => {
    let err: any;
    service.analyze({ texts: ['x'] }).subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(endpoint);
    req.flush('bad gateway', { status: 502, statusText: 'Bad Gateway' });
    expect(err.status).toBe(502);
  });
});
