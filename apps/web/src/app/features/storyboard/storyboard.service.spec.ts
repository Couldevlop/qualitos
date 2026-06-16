import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { StoryboardService } from './storyboard.service';
import { StoryboardResponse } from './storyboard.types';

describe('StoryboardService', () => {
  let service: StoryboardService;
  let httpMock: HttpTestingController;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai/storyboard`;

  const sample: StoryboardResponse = {
    narrative: 'Sur mai 2026, le taux de NC recule de 12 %.',
    provider: 'ollama',
    period: 'Mai 2026',
    sources: [{ label: 'Taux de NC', value: '1,8', trend: '-12 %', target: '< 2', unit: '%' }]
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(StoryboardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('POST la période et les indicateurs vers /ai/storyboard', () => {
    let res: StoryboardResponse | undefined;
    service.generate({
      period: 'Mai 2026',
      context: 'Site de Lyon',
      points: [{ label: 'Taux de NC', value: '1,8', trend: '-12 %', target: '< 2', unit: '%' }]
    }).subscribe(r => (res = r));

    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.period).toBe('Mai 2026');
    expect(req.request.body.points.length).toBe(1);
    // Invariant multi-tenant : pas de tenant_id dans le corps (dérivé du JWT serveur).
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush(sample);
    expect(res!.narrative).toContain('taux de NC');
    expect(res!.provider).toBe('ollama');
    expect(res!.sources.length).toBe(1);
  });

  it('transmet plusieurs indicateurs avec leurs champs optionnels', () => {
    service.generate({
      period: 'T1 2026',
      points: [
        { label: 'FPY', value: '97', unit: '%' },
        { label: 'Délai CAPA', value: '26', trend: 'stable', target: '< 30', unit: 'j' }
      ]
    }).subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.body.points.length).toBe(2);
    expect(req.request.body.points[1].target).toBe('< 30');
    req.flush(sample);
  });

  it('propage l\'erreur HTTP (ex. 502 passerelle IA)', () => {
    let err: any;
    service.generate({ period: 'Mai 2026', points: [{ label: 'X', value: '1' }] })
      .subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(endpoint);
    req.flush('bad gateway', { status: 502, statusText: 'Bad Gateway' });
    expect(err.status).toBe(502);
  });
});
