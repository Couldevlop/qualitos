import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { NlqAskResponse } from '../nlq/nlq.types';
import { NlqExploreService } from './nlq-explore.service';

describe('NlqExploreService', () => {
  let service: NlqExploreService;
  let httpMock: HttpTestingController;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai/nlq/ask`;

  const sample: NlqAskResponse = {
    question: 'Combien de CAPA par statut ?',
    sql: 'SELECT status, COUNT(*) AS count FROM capa_cases GROUP BY status',
    tenantFilterApplied: true,
    tablesUsed: ['capa_cases'],
    functionsUsed: ['count'],
    rows: [
      { status: 'OPEN', count: 4 },
      { status: 'CLOSED', count: 12 }
    ],
    rowCount: 2,
    confidence: 0.85,
    chart: { chart_type: 'bar' },
    narrative: 'Démo.'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(NlqExploreService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('délègue la question au endpoint NLQ existant (POST, sans tenant_id dans le body)', () => {
    let res: NlqAskResponse | undefined;
    service.ask('Combien de CAPA par statut ?').subscribe(r => (res = r));

    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.question).toBe('Combien de CAPA par statut ?');
    // Invariant multi-tenant (règle 18.2) : le tenant est dérivé du JWT côté serveur.
    expect(req.request.body.tenantId).toBeUndefined();
    expect(req.request.body.tenant_id).toBeUndefined();
    req.flush(sample);
    expect(res!.rowCount).toBe(2);
  });

  it('propage l\'erreur HTTP (ex. 422 question intraduisible)', () => {
    let err: any;
    service.ask('???').subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(endpoint);
    req.flush('unprocessable', { status: 422, statusText: 'Unprocessable Entity' });
    expect(err.status).toBe(422);
  });

  it('buildPlan : déduit catégories (1re colonne texte) et série (colonne numérique)', () => {
    const plan = service.buildPlan(sample.rows);
    expect(plan.graphable).toBeTrue();
    expect(plan.categoryColumn).toBe('status');
    expect(plan.valueColumns).toEqual(['count']);
  });

  it('buildPlan : jeu sans colonne numérique → non graphable', () => {
    const plan = service.buildPlan([
      { code: 'A', label: 'Alpha' },
      { code: 'B', label: 'Bravo' }
    ]);
    expect(plan.graphable).toBeFalse();
    expect(plan.valueColumns.length).toBe(0);
  });

  it('buildPlan : jeu vide → non graphable', () => {
    const plan = service.buildPlan([]);
    expect(plan.graphable).toBeFalse();
    expect(plan.categoryColumn).toBeNull();
  });
});
