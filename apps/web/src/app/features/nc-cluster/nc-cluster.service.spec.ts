import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { NcClusterService } from './nc-cluster.service';
import { NcClusterResponse } from './nc-cluster.types';

describe('NcClusterService', () => {
  let service: NcClusterService;
  let httpMock: HttpTestingController;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai/nc-clusters`;

  const sample: NcClusterResponse = {
    n: 5, clusteredRatio: 0.8, method: 'dbscan',
    clusters: [{ clusterId: 0, indices: [0, 1], size: 2, topTerms: ['fuite', 'huile'] }],
    noiseIndices: [4]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(NcClusterService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST les textes vers /ai/nc-clusters', () => {
    let res: NcClusterResponse | undefined;
    service.cluster({ texts: ['fuite huile presse', 'fuite huile ligne'], threshold: 0.35 })
      .subscribe(r => (res = r));

    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.texts.length).toBe(2);
    // Invariant multi-tenant : pas de tenant_id dans le corps (dérivé du JWT serveur).
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush(sample);
    expect(res!.method).toBe('dbscan');
    expect(res!.clusters[0].topTerms).toEqual(['fuite', 'huile']);
    expect(res!.noiseIndices).toEqual([4]);
  });

  it('transmet min_samples quand fourni', () => {
    service.cluster({ texts: ['a b', 'a c'], minSamples: 3 }).subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.body.minSamples).toBe(3);
    req.flush(sample);
  });

  it('propage l\'erreur HTTP (ex. 502 passerelle)', () => {
    let err: any;
    service.cluster({ texts: ['a b', 'a c'] }).subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(endpoint);
    req.flush('bad gateway', { status: 502, statusText: 'Bad Gateway' });
    expect(err.status).toBe(502);
  });
});
