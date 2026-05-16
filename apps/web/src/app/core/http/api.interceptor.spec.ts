import { HTTP_INTERCEPTORS, HttpClient, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AuthService } from '../auth/auth.service';
import { ApiInterceptor } from './api.interceptor';

describe('ApiInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: HTTP_INTERCEPTORS, useClass: ApiInterceptor, multi: true }
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => httpMock.verify());

  it('attaches Bearer token when authenticated', () => {
    http.get('/api/v1/pdca/cycles').subscribe();
    const req = httpMock.expectOne('/api/v1/pdca/cycles');
    expect(req.request.headers.get('Authorization')).toMatch(/^Bearer /);
    req.flush({});
  });

  it('does not attach header when no token', () => {
    auth.logout();
    http.get('/api/v1/pdca/cycles').subscribe();
    const req = httpMock.expectOne('/api/v1/pdca/cycles');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
