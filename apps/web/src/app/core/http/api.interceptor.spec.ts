import { HTTP_INTERCEPTORS, HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../auth/auth.service';
import { ApiInterceptor } from './api.interceptor';

describe('ApiInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  // Stub AuthService : on contrôle le jeton renvoyé pour exercer les deux branches
  // de l'intercepteur, sans dépendre d'OAuthService ni du mode dev/oidc ambiant.
  const authStub: { token: string | null } = { token: null };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: HTTP_INTERCEPTORS, useClass: ApiInterceptor, multi: true },
        { provide: AuthService, useValue: { getAccessToken: () => authStub.token } }
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('attaches Bearer token when authenticated', () => {
    authStub.token = 'fake.jwt.token';
    http.get('/api/v1/pdca/cycles').subscribe();
    const req = httpMock.expectOne('/api/v1/pdca/cycles');
    expect(req.request.headers.get('Authorization')).toBe('Bearer fake.jwt.token');
    req.flush({});
  });

  it('does not attach header when no token', () => {
    authStub.token = null;
    http.get('/api/v1/pdca/cycles').subscribe();
    const req = httpMock.expectOne('/api/v1/pdca/cycles');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
