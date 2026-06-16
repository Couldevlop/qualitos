import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AnomalyService } from './anomaly.service';
import { AnomalyDetectResponse } from './anomaly.types';

describe('AnomalyService', () => {
  let service: AnomalyService;
  let httpMock: HttpTestingController;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai/anomaly/detect`;

  const sample: AnomalyDetectResponse = {
    n: 3, nFeatures: 2, method: 'isolation_forest', contamination: 0.1,
    threshold: 0.7, anomalyCount: 1, hasAnomalies: true,
    points: [
      { index: 0, score: 0.4, isAnomaly: false, topFeature: null },
      { index: 1, score: 0.45, isAnomaly: false, topFeature: null },
      { index: 2, score: 0.9, isAnomaly: true, topFeature: null }
    ]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AnomalyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST la matrice et les paramètres vers /ai/anomaly/detect', () => {
    let res: AnomalyDetectResponse | undefined;
    service.detect({
      samples: [[1, 2], [2, 4], [12, -8]],
      method: 'isolation_forest',
      contamination: 0.1
    }).subscribe(r => (res = r));

    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.samples.length).toBe(3);
    expect(req.request.body.method).toBe('isolation_forest');
    // Invariant multi-tenant : aucun tenant_id dans le corps (dérivé du JWT serveur).
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush(sample);
    expect(res!.anomalyCount).toBe(1);
    expect(res!.points[2].isAnomaly).toBeTrue();
  });

  it('transmet le seuil explicite quand il est fourni', () => {
    service.detect({ samples: [[1], [2]], method: 'reconstruction', threshold: 0.5 }).subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.body.threshold).toBe(0.5);
    expect(req.request.body.method).toBe('reconstruction');
    req.flush(sample);
  });

  it('propage l\'erreur HTTP (ex. 503 disjoncteur)', () => {
    let err: any;
    service.detect({ samples: [[1, 2]] }).subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(endpoint);
    req.flush('unavailable', { status: 503, statusText: 'Service Unavailable' });
    expect(err.status).toBe(503);
  });

  it('POST l\'explication vers /ai/anomaly/explain avec l\'index', () => {
    const explainUrl = `${environment.apiBaseUrl}/api/v1/ai/anomaly/explain`;
    let res: any;
    service.explain({ samples: [[1, 2], [50, -50]], index: 1 }).subscribe(r => (res = r));
    const req = httpMock.expectOne(explainUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.index).toBe(1);
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush({
      index: 1, method: 'isolation_forest', score: 0.82, baseValue: 0.5,
      contributions: [{ feature: 0, value: 50, contribution: 0.2 }]
    });
    expect(res.method).toBe('isolation_forest');
    expect(res.contributions[0].contribution).toBe(0.2);
  });
});
