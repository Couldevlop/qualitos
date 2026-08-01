import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PdcaService } from './pdca.service';
import { PdcaCycleResponse, PdcaStepResponse } from './pdca.types';

describe('PdcaService (mock mode)', () => {
  let service: PdcaService;
  let prevMock: boolean;

  beforeEach(() => {
    // Ce spec teste le chemin mock : on force le flag (indépendant du défaut
    // d'environment.ts, qui peut être basculé en oidc/no-mock pour la démo).
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(PdcaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('returns demo cycles', (done) => {
    service.listCycles().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      expect(page.content[0].title).toBeTruthy();
      done();
    });
  });

  it('filters by status', (done) => {
    service.listCycles(0, 20, 'CHECK').subscribe(page => {
      expect(page.content.every(c => c.status === 'CHECK')).toBeTrue();
      done();
    });
  });

  it('returns a cycle by id', (done) => {
    service.getCycle('demo-2').subscribe(c => {
      expect(c.id).toBe('demo-2');
      done();
    });
  });

  it('renvoie une page vide quand aucun cycle ne porte le statut demandé', async () => {
    const page = await firstValueFrom(service.listCycles(0, 20, 'CANCELLED'));
    expect(page.content).toEqual([]);
    expect(page.totalElements).toBe(0);
    expect(page.totalPages).toBe(1);
  });

  it('retombe sur le premier cycle quand l\'identifiant est inconnu (démo sans backend)', async () => {
    const c = await firstValueFrom(service.getCycle('inexistant'));
    expect(c.id).toBe('demo-1');
  });

  it('avance le cycle d\'une phase à la suivante dans l\'ordre PDCA', async () => {
    const plan = await firstValueFrom(service.getCycle('demo-2'));
    expect(plan.status).toBe('PLAN');

    expect((await firstValueFrom(service.advanceCycle('demo-2'))).status).toBe('DO');
    expect((await firstValueFrom(service.advanceCycle('demo-2'))).status).toBe('CHECK');
    expect((await firstValueFrom(service.advanceCycle('demo-2'))).status).toBe('ACT');
  });

  it('horodate la clôture au passage en COMPLETED', async () => {
    // demo-3 est en CHECK : ACT puis COMPLETED.
    await firstValueFrom(service.advanceCycle('demo-3'));
    const completed = await firstValueFrom(service.advanceCycle('demo-3'));
    expect(completed.status).toBe('COMPLETED');
    expect(completed.completedAt).toBeTruthy();
    expect(completed.completedAt).toBe(completed.updatedAt);
  });

  it('n\'avance plus un cycle déjà COMPLETED (dernier état de la roue)', async () => {
    await firstValueFrom(service.advanceCycle('demo-3'));
    const completed = await firstValueFrom(service.advanceCycle('demo-3'));
    const again = await firstValueFrom(service.advanceCycle('demo-3'));
    expect(again.status).toBe('COMPLETED');
    expect(again.completedAt).toBe(completed.completedAt);
  });

  it('n\'avance pas un cycle CANCELLED (statut hors de la roue)', async () => {
    const cancelled = await firstValueFrom(service.cancelCycle('demo-2'));
    expect(cancelled.status).toBe('CANCELLED');
    const after = await firstValueFrom(service.advanceCycle('demo-2'));
    expect(after.status).toBe('CANCELLED');
  });

  it('ajoute une étape au cycle et la rend visible à la relecture', async () => {
    const before = await firstValueFrom(service.getCycle('demo-2'));
    const step = await firstValueFrom(
      service.addStep('demo-2', { title: 'Analyse Pareto', phase: 'PLAN' }));

    expect(step.cycleId).toBe('demo-2');
    expect(step.status).toBe('PENDING');   // statut par défaut quand non fourni

    const after = await firstValueFrom(service.getCycle('demo-2'));
    expect(after.steps.length).toBe(before.steps.length + 1);
    expect(after.steps[after.steps.length - 1].title).toBe('Analyse Pareto');
    expect(after.updatedAt).toBe(step.createdAt);
  });

  it('respecte le statut d\'étape explicitement fourni', async () => {
    const step = await firstValueFrom(
      service.addStep('demo-1', { title: 'Poka-Yoke', phase: 'DO', status: 'IN_PROGRESS' }));
    expect(step.status).toBe('IN_PROGRESS');
  });

  it('crée un cycle en PLAN, sans étape, en tête de liste', async () => {
    const created = await firstValueFrom(
      service.createCycle({ title: 'Nouveau', description: 'desc', ownerId: 'u1' }));
    expect(created.status).toBe('PLAN');
    expect(created.steps).toEqual([]);

    const page = await firstValueFrom(service.listCycles());
    expect(page.content[0].id).toBe(created.id);
  });
});

describe('PdcaService (API réelle)', () => {
  let service: PdcaService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/pdca/cycles`;

  const cycle: PdcaCycleResponse = {
    id: 'c1', tenantId: 't1', title: 'Cycle', status: 'PLAN', ownerId: 'u1',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', steps: []
  };

  const step: PdcaStepResponse = {
    id: 's1', cycleId: 'c1', phase: 'PLAN', title: 'Étape', status: 'PENDING',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z'
  };

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(PdcaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('pagine via les paramètres page/size et omet le statut quand aucun filtre', () => {
    service.listCycles(2, 50).subscribe();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.has('status')).toBeFalse();
    req.flush({ content: [cycle], totalElements: 1, totalPages: 1, number: 2, size: 50 });
  });

  it('transmet le filtre de statut au serveur', () => {
    service.listCycles(0, 20, 'DO').subscribe();
    const req = http.expectOne(r => r.url === base);
    expect(req.request.params.get('status')).toBe('DO');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('lit un cycle par identifiant', (done) => {
    service.getCycle('c1').subscribe(c => {
      expect(c.id).toBe('c1');
      done();
    });
    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('GET');
    req.flush(cycle);
  });

  it('crée un cycle avec le corps fourni', (done) => {
    service.createCycle({ title: 'Cycle', ownerId: 'u1' }).subscribe(c => {
      expect(c.id).toBe('c1');
      done();
    });
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Cycle', ownerId: 'u1' });
    req.flush(cycle);
  });

  it('ajoute une étape sur la sous-ressource du cycle', (done) => {
    service.addStep('c1', { title: 'Étape', phase: 'PLAN' }).subscribe(s => {
      expect(s.id).toBe('s1');
      done();
    });
    const req = http.expectOne(`${base}/c1/steps`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Étape', phase: 'PLAN' });
    req.flush(step);
  });

  it('avance et annule le cycle par PATCH (transitions serveur, corps vide)', (done) => {
    service.advanceCycle('c1').subscribe(c => expect(c.status).toBe('DO'));
    const adv = http.expectOne(`${base}/c1/advance`);
    expect(adv.request.method).toBe('PATCH');
    expect(adv.request.body).toEqual({});
    adv.flush({ ...cycle, status: 'DO' });

    service.cancelCycle('c1').subscribe(c => {
      expect(c.status).toBe('CANCELLED');
      done();
    });
    const cancel = http.expectOne(`${base}/c1/cancel`);
    expect(cancel.request.method).toBe('PATCH');
    cancel.flush({ ...cycle, status: 'CANCELLED' });
  });

  it('propage le 409 quand la transition est refusée par le serveur', (done) => {
    service.advanceCycle('c1').subscribe({
      next: () => done.fail('la transition ne devrait pas aboutir'),
      error: err => {
        expect(err.status).toBe(409);
        done();
      }
    });
    http.expectOne(`${base}/c1/advance`)
      .flush({ title: 'Conflict' }, { status: 409, statusText: 'Conflict' });
  });
});
