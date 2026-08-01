import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService, AuthUser } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';
import { ApiKeysService } from './api-keys.service';
import { ApiKeyView, IssuedApiKey, MissingActorError } from './api-keys.types';

/**
 * `/api/v1/api-keys` n'avait aucun consommateur : émettre ou révoquer une clé imposait
 * un appel HTTP manuel. Ces tests figent le contrat consommé par l'écran.
 */
describe('ApiKeysService', () => {
  let service: ApiKeysService;
  let http: HttpTestingController;
  let currentUser: AuthUser | null;

  const base = `${environment.apiBaseUrl}/api/v1/api-keys`;
  const DAY = 24 * 60 * 60 * 1000;

  const key = (id: string, over: Partial<ApiKeyView> = {}): ApiKeyView => ({
    id, tenantId: 't1', name: 'Clé ' + id, prefix: id.slice(0, 8), scopes: ['audits.read'],
    status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', createdBy: 'u1',
    expiresAt: null, lastUsedAt: null, revokedAt: null, revokedBy: null, ...over
  });

  const issued = (view: ApiKeyView): IssuedApiKey => ({ view, plaintext: 'qos_abc_secret' });

  beforeEach(() => {
    currentUser = {
      userId: '11111111-1111-1111-1111-111111111111',
      tenantId: 't1', displayName: 'Admin', roles: ['ADMIN_TENANT']
    };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    });
    service = TestBed.inject(ApiKeysService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste les clés du tenant courant', (done) => {
    service.list().subscribe(keys => {
      expect(keys.length).toBe(1);
      done();
    });
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([key('a')]);
  });

  it('lit le détail d\'une clé en échappant l\'identifiant', (done) => {
    service.get('a b').subscribe(k => {
      expect(k.id).toBe('a b');
      done();
    });
    http.expectOne(`${base}/a%20b`).flush(key('a b'));
  });

  it('ordonne les clés utilisables en tête, puis les plus récentes', (done) => {
    service.overview().subscribe(({ keys }) => {
      expect(keys.map(k => k.id)).toEqual(['act2', 'act1', 'exp', 'rev']);
      done();
    });
    http.expectOne(base).flush([
      key('rev', { status: 'REVOKED' }),
      key('act1', { createdAt: '2026-02-01T00:00:00Z' }),
      key('exp', { status: 'EXPIRED' }),
      key('act2', { createdAt: '2026-03-01T00:00:00Z' })
    ]);
  });

  it('compte les statuts et les échéances proches', (done) => {
    const soon = new Date(Date.now() + 5 * DAY).toISOString();
    const later = new Date(Date.now() + 400 * DAY).toISOString();
    service.overview().subscribe(({ counts }) => {
      expect(counts).toEqual({ total: 5, active: 3, expiringSoon: 1, expired: 1, revoked: 1 });
      done();
    });
    http.expectOne(base).flush([
      key('a1', { expiresAt: soon }),
      key('a2', { expiresAt: later }),
      key('a3'),
      key('e1', { status: 'EXPIRED' }),
      key('r1', { status: 'REVOKED' })
    ]);
  });

  it('ignore une échéance illisible dans le compteur « expire bientôt »', (done) => {
    service.overview().subscribe(({ counts }) => {
      expect(counts.expiringSoon).toBe(0);
      expect(counts.active).toBe(1);
      done();
    });
    http.expectOne(base).flush([key('a1', { expiresAt: 'pas-une-date' })]);
  });

  it('renvoie une vue vide quand le tenant n\'a aucune clé', (done) => {
    service.overview().subscribe(({ keys, counts }) => {
      expect(keys).toEqual([]);
      expect(counts).toEqual({ total: 0, active: 0, expiringSoon: 0, expired: 0, revoked: 0 });
      done();
    });
    http.expectOne(base).flush([]);
  });

  it('émet une clé en joignant l\'acteur issu de la session', (done) => {
    service.create({ name: 'CI', scopes: ['audits.read'], expiresAt: '2026-12-31T23:59:59.999Z' })
      .subscribe(res => {
        expect(res.plaintext).toBe('qos_abc_secret');
        done();
      });
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      name: 'CI', scopes: ['audits.read'], expiresAt: '2026-12-31T23:59:59.999Z',
      actor: '11111111-1111-1111-1111-111111111111'
    });
    req.flush(issued(key('new')));
  });

  it('fait tourner une clé', (done) => {
    service.rotate('a1').subscribe(res => {
      expect(res.view.id).toBe('a2');
      done();
    });
    const req = http.expectOne(`${base}/a1/rotate`);
    expect(req.request.body).toEqual({ actor: '11111111-1111-1111-1111-111111111111' });
    req.flush(issued(key('a2')));
  });

  it('révoque une clé', (done) => {
    service.revoke('a1').subscribe(v => {
      expect(v.status).toBe('REVOKED');
      done();
    });
    const req = http.expectOne(`${base}/a1/revoke`);
    expect(req.request.body).toEqual({ actor: '11111111-1111-1111-1111-111111111111' });
    req.flush(key('a1', { status: 'REVOKED' }));
  });

  it('refuse toute mutation sans session, sans appeler le serveur', (done) => {
    currentUser = null;
    let seen = 0;
    const expectRefusal = (err: unknown) => {
      expect(err instanceof MissingActorError).toBeTrue();
      if (++seen === 3) done();
    };
    service.create({ name: 'x', scopes: [], expiresAt: null })
      .subscribe({ error: expectRefusal });
    service.rotate('a1').subscribe({ error: expectRefusal });
    service.revoke('a1').subscribe({ error: expectRefusal });
    http.expectNone(() => true);
  });

  it('relit l\'acteur à l\'abonnement, pas à la construction du flux', (done) => {
    currentUser = null;
    const pending$ = service.revoke('a1');
    currentUser = {
      userId: '22222222-2222-2222-2222-222222222222',
      tenantId: 't1', displayName: 'Autre', roles: ['ADMIN_TENANT']
    };
    pending$.subscribe(() => done());
    const req = http.expectOne(`${base}/a1/revoke`);
    expect(req.request.body).toEqual({ actor: '22222222-2222-2222-2222-222222222222' });
    req.flush(key('a1', { status: 'REVOKED' }));
  });

  it('borne la limite d\'expiration aux valeurs acceptées par le serveur', () => {
    // Une requête doit être vidée avant la suivante : `expectOne` échouerait sur deux
    // appels ouverts simultanément vers la même URL.
    const expectLimit = (limit: number | undefined, expected: string) => {
      (limit === undefined ? service.expireDue() : service.expireDue(limit)).subscribe();
      const req = http.expectOne(r => r.url === `${base}/expire-due`);
      expect(req.request.method).toBe('POST');
      expect(req.request.params.get('limit')).toBe(expected);
      req.flush({ expired: 0 });
    };

    expectLimit(999, '500');
    expectLimit(-4, '1');
    expectLimit(0, '200');
    expectLimit(undefined, '200');
  });

  it('renvoie le nombre de clés expirées, 0 si le serveur ne le précise pas', () => {
    const results: number[] = [];

    service.expireDue(10).subscribe(n => results.push(n));
    http.expectOne(r => r.url === `${base}/expire-due`).flush({ expired: 7 });

    service.expireDue(10).subscribe(n => results.push(n));
    http.expectOne(r => r.url === `${base}/expire-due`).flush({});

    expect(results).toEqual([7, 0]);
  });

  it('propage une erreur HTTP de la liste', (done) => {
    service.overview().subscribe({
      error: (err: { status: number }) => {
        expect(err.status).toBe(403);
        done();
      }
    });
    http.expectOne(base).flush('forbidden', { status: 403, statusText: 'Forbidden' });
  });
});
