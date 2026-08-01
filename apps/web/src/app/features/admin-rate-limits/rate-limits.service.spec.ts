import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { RateLimitsService } from './rate-limits.service';
import { RateLimitDecision, RateLimitPolicy, UNLIMITED_LIMIT } from './rate-limits.types';

/**
 * `/api/v1/rate-limits` n'avait aucun consommateur : la consommation réelle d'un quota
 * (lecture sans incrément) n'était visible nulle part avant les premiers 429.
 */
describe('RateLimitsService', () => {
  let service: RateLimitsService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/rate-limits`;

  const policy = (scope: string, over: Partial<RateLimitPolicy> = {}): RateLimitPolicy => ({
    id: 'p-' + scope, tenantId: 't1', scope, windowSeconds: 60, maxRequests: 100,
    enabled: true, createdAt: '2026-07-01T10:00:00Z', updatedAt: '2026-07-01T10:00:00Z', ...over
  });

  const decision = (over: Partial<RateLimitDecision> = {}): RateLimitDecision => ({
    allowed: true, limit: 100, remaining: 100, retryAfterSeconds: 0, resetSeconds: 30, ...over
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(RateLimitsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // ---- Routes brutes ---------------------------------------------------------

  it('liste les politiques du tenant', (done) => {
    service.list().subscribe(list => {
      expect(list.length).toBe(1);
      expect(list[0].scope).toBe('api.documents');
      done();
    });
    const req = http.expectOne(`${base}/policies`);
    expect(req.request.method).toBe('GET');
    req.flush([policy('api.documents')]);
  });

  it('lit une politique par identifiant', (done) => {
    service.get('p-1').subscribe(p => {
      expect(p.id).toBe('p-api.documents');
      done();
    });
    http.expectOne(`${base}/policies/p-1`).flush(policy('api.documents'));
  });

  it('encode le périmètre dans le chemin de la sonde', (done) => {
    service.check('api:documents').subscribe(d => {
      expect(d.limit).toBe(100);
      done();
    });
    // Le périmètre vient d'un champ de saisie : jamais de segment de chemin brut.
    http.expectOne(`${base}/check/api%3Adocuments`).flush(decision());
  });

  it('enregistre une politique via PUT', (done) => {
    service.upsert({ scope: 'api.nc', windowSeconds: 60, maxRequests: 10, enabled: true })
      .subscribe(p => { expect(p.scope).toBe('api.nc'); done(); });
    const req = http.expectOne(`${base}/policies`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({
      scope: 'api.nc', windowSeconds: 60, maxRequests: 10, enabled: true
    });
    req.flush(policy('api.nc', { maxRequests: 10 }));
  });

  it('supprime une politique', (done) => {
    service.delete('p-1').subscribe(() => done());
    const req = http.expectOne(`${base}/policies/p-1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  // ---- savePolicy : nettoyage du doublon serveur ------------------------------

  it('enregistre la politique sans chercher à nettoyer quoi que ce soit', (done) => {
    // Une version antérieure supprimait une prétendue « ligne résiduelle » que le
    // serveur aurait créée en réactivant une politique suspendue. La contrainte
    // d'unicité (tenant_id, scope) rend ce scénario impossible : il n'y a jamais
    // qu'une ligne par périmètre. Aucun DELETE ne doit partir — http.verify() en
    // afterEach échouerait si c'était le cas.
    service.savePolicy({ scope: 'api.nc', windowSeconds: 60, maxRequests: 10, enabled: true })
      .subscribe(p => { expect(p.scope).toBe('api.nc'); done(); });

    http.expectOne(`${base}/policies`).flush(policy('api.nc'));
  });

  it('réactive une politique suspendue par une simple mise à jour', (done) => {
    service.savePolicy({ scope: 'api.nc', windowSeconds: 60, maxRequests: 10, enabled: true })
      .subscribe(p => { expect(p.enabled).toBeTrue(); done(); });

    const req = http.expectOne(`${base}/policies`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.enabled).toBeTrue();
    req.flush(policy('api.nc', { enabled: true }));
  });

  // ---- overview : politiques + consommation ----------------------------------

  it('confronte chaque politique appliquée à sa consommation réelle', (done) => {
    service.overview().subscribe(({ rows, totals }) => {
      expect(rows.length).toBe(1);
      expect(rows[0].used).toBe(40);
      expect(rows[0].ratio).toBeCloseTo(0.4, 5);
      expect(rows[0].level).toBe('normal');
      expect(totals.policies).toBe(1);
      expect(totals.enforced).toBe(1);
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('api.documents')]);
    http.expectOne(`${base}/check/api.documents`).flush(decision({ remaining: 60 }));
  });

  it('classe les niveaux selon la part consommée', (done) => {
    service.overview().subscribe(({ rows, totals }) => {
      const byScope = new Map(rows.map(r => [r.policy.scope, r]));
      expect(byScope.get('a')!.level).toBe('normal');
      expect(byScope.get('b')!.level).toBe('warning');
      expect(byScope.get('c')!.level).toBe('critical');
      expect(byScope.get('d')!.level).toBe('exceeded');
      expect(totals.nearLimit).toBe(1);
      expect(totals.exceeded).toBe(1);
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('a'), policy('b'), policy('c'), policy('d')]);
    http.expectOne(`${base}/check/a`).flush(decision({ remaining: 90 }));
    http.expectOne(`${base}/check/b`).flush(decision({ remaining: 50 }));
    http.expectOne(`${base}/check/c`).flush(decision({ remaining: 20 }));
    http.expectOne(`${base}/check/d`)
      .flush(decision({ allowed: false, remaining: 0, retryAfterSeconds: 12 }));
  });

  it('remonte les périmètres les plus tendus en tête de liste', (done) => {
    service.overview().subscribe(({ rows }) => {
      expect(rows.map(r => r.policy.scope)).toEqual(['c', 'b', 'a']);
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('a'), policy('b'), policy('c')]);
    http.expectOne(`${base}/check/a`).flush(decision({ remaining: 100 }));
    http.expectOne(`${base}/check/b`).flush(decision({ remaining: 40 }));
    http.expectOne(`${base}/check/c`).flush(decision({ allowed: false, remaining: 0 }));
  });

  it('ne sonde pas une politique suspendue : le serveur répondrait « illimité »', (done) => {
    service.overview().subscribe(({ rows, totals }) => {
      expect(rows[0].level).toBe('unmeasured');
      expect(rows[0].usage).toBeNull();
      expect(totals.enforced).toBe(0);
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('api.nc', { enabled: false })]);
    // Aucune sonde attendue : http.verify() en afterEach le garantit.
  });

  it('émet une vue vide plutôt que de rester muette quand rien n\'est sondé', (done) => {
    // forkJoin([]) complète sans émettre : sans court-circuit l'écran resterait en attente.
    service.overview().subscribe(({ rows, totals }) => {
      expect(rows).toEqual([]);
      expect(totals).toEqual({ policies: 0, enforced: 0, nearLimit: 0, exceeded: 0 });
      done();
    });
    http.expectOne(`${base}/policies`).flush([]);
  });

  it('survit à une sonde en échec sans perdre la politique', (done) => {
    service.overview().subscribe(({ rows }) => {
      expect(rows.length).toBe(1);
      expect(rows[0].level).toBe('unmeasured');
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('api.nc')]);
    http.expectOne(`${base}/check/api.nc`).flush({}, { status: 500, statusText: 'Server Error' });
  });

  it('ne lit pas la réponse « illimitée » comme une consommation nulle', (done) => {
    service.overview().subscribe(({ rows }) => {
      expect(rows[0].level).toBe('unmeasured');
      expect(rows[0].used).toBe(0);
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('api.nc')]);
    http.expectOne(`${base}/check/api.nc`)
      .flush(decision({ limit: UNLIMITED_LIMIT, remaining: UNLIMITED_LIMIT }));
  });

  it('borne la part consommée à 100 % même si le compteur a dépassé la limite', (done) => {
    service.overview().subscribe(({ rows }) => {
      expect(rows[0].ratio).toBe(1);
      expect(rows[0].used).toBe(100);
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('api.nc')]);
    http.expectOne(`${base}/check/api.nc`).flush(decision({ allowed: false, remaining: -20 }));
  });

  it('traite une limite nulle comme non mesurable plutôt que de diviser par zéro', (done) => {
    service.overview().subscribe(({ rows }) => {
      expect(rows[0].level).toBe('unmeasured');
      expect(rows[0].ratio).toBe(0);
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('api.nc')]);
    http.expectOne(`${base}/check/api.nc`).flush(decision({ limit: 0, remaining: 0 }));
  });

  it('départage les périmètres à consommation égale par ordre alphabétique', (done) => {
    service.overview().subscribe(({ rows }) => {
      expect(rows.map(r => r.policy.scope)).toEqual(['alpha', 'beta']);
      expect(rows[0].level).toBe('idle');
      done();
    });

    http.expectOne(`${base}/policies`).flush([policy('beta'), policy('alpha')]);
    http.expectOne(`${base}/check/beta`).flush(decision({ remaining: 100 }));
    http.expectOne(`${base}/check/alpha`).flush(decision({ remaining: 100 }));
  });

  it('propage l\'erreur de liste : l\'écran doit afficher un message, pas un tableau vide', (done) => {
    service.overview().subscribe({
      next: () => fail('aucune vue ne doit être émise'),
      error: err => { expect(err.status).toBe(500); done(); }
    });
    http.expectOne(`${base}/policies`).flush({}, { status: 500, statusText: 'Server Error' });
  });
});
