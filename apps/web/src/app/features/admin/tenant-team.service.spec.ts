import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { TenantUser } from './admin.types';
import { TenantTeamService } from './tenant-team.service';

/**
 * `/api/v1/users` existait sans aucun consommateur, et sans l'administrateur de
 * tenant dans sa politique d'accès : habiliter un arrivant réclamait donc une
 * intervention de l'éditeur de la plateforme.
 */
describe('TenantTeamService', () => {
  let service: TenantTeamService;
  let http: HttpTestingController;

  const base = `${environment.apiBaseUrl}/api/v1/users`;

  const user = (over: Partial<TenantUser> = {}): TenantUser => ({
    id: 'u1', tenantId: 't1', keycloakId: 'kc-1', email: 'alice@acme.com',
    roles: ['quality_manager'], active: true,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TenantTeamService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(TenantTeamService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste l’équipe du tenant, paginée', () => {
    let received: TenantUser[] = [];
    service.list().subscribe(page => (received = page.content));

    const req = http.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('50');
    // Le tenant n'est JAMAIS passé par le client : il vient du jeton (§18.2 #2).
    expect(req.request.params.has('tenantId')).toBeFalse();
    req.flush({ content: [user()], totalElements: 1, totalPages: 1, number: 0, size: 50 });

    expect(received.length).toBe(1);
    expect(received[0].email).toBe('alice@acme.com');
  });

  it('envoie l’ensemble complet des rôles, pas un delta', () => {
    service.setRoles('u1', ['quality_manager', 'auditor'], true).subscribe();

    const req = http.expectOne(`${base}/u1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ roles: ['quality_manager', 'auditor'], active: true });
    req.flush(user({ roles: ['quality_manager', 'auditor'] }));
  });

  it('retire l’accès sans effacer le membre', () => {
    service.deactivate('u1').subscribe();

    const req = http.expectOne(`${base}/u1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
