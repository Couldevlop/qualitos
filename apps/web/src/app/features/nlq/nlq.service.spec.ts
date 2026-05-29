import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { NlqService } from './nlq.service';
import { NlqAskResponse } from './nlq.types';

describe('NlqService', () => {
  let service: NlqService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai/nlq/ask`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(NlqService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('POST la question vers l\'engine (mode non-mock)', () => {
    environment.useMockApi = false;
    let res: NlqAskResponse | undefined;
    service.ask('Combien de CAPA par statut ?', 50).subscribe(r => (res = r));

    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ question: 'Combien de CAPA par statut ?', maxRows: 50 });
    req.flush({ sql: 'SELECT 1', tenantFilterApplied: true } as Partial<NlqAskResponse>);

    expect(res!.sql).toBe('SELECT 1');
    httpMock.verify();
  });

  it('utilise maxRows=100 par défaut', () => {
    environment.useMockApi = false;
    service.ask('q').subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.body).toEqual({ question: 'q', maxRows: 100 });
    req.flush({});
    httpMock.verify();
  });

  it('renvoie un mock sans appel HTTP (mode mock)', fakeAsync(() => {
    environment.useMockApi = true;
    let res: NlqAskResponse | undefined;
    service.ask('Score moyen des audits 5S').subscribe(r => (res = r));
    tick(650);

    expect(res!.question).toBe('Score moyen des audits 5S');
    expect(res!.rows.length).toBeGreaterThan(0);
    httpMock.verify(); // aucun appel HTTP émis en mode mock
  }));
});
