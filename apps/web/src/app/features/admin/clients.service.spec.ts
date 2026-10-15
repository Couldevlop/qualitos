import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { OnboardRequest } from './admin.types';
import { ClientsService } from './clients.service';

/** Les clients de la plateforme : la liste paginée, récente d'abord, et l'accueil d'un client. */
describe('ClientsService', () => {
  const endpoint = `${environment.apiBaseUrl}/api/v1/tenants`;
  let service: ClientsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ClientsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste les clients, les plus récents d\'abord', () => {
    service.list().subscribe();
    const req = http.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('100');
    expect(req.request.params.get('sort')).toBe('createdAt,desc');
    req.flush({ content: [] });

    service.list(2, 20).subscribe();
    const suivante = http.expectOne(r => r.url === endpoint);
    expect(suivante.request.params.get('page')).toBe('2');
    expect(suivante.request.params.get('size')).toBe('20');
    suivante.flush({ content: [] });
  });

  it('accueille un client en une seule écriture', () => {
    const demande = { name: 'Acme' } as unknown as OnboardRequest;
    service.onboard(demande).subscribe();
    const req = http.expectOne(`${endpoint}/onboard`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(demande);
    req.flush({});
  });
});
