import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

describe('AuthService (dev mode)', () => {
  let service: AuthService;
  let prevMode: 'dev' | 'oidc';

  beforeEach(() => {
    // Force le mode dev AVANT l'inject (bootstrapUser() lit authMode au
    // constructeur). Indépendant du défaut d'environment.ts (oidc pour la démo).
    prevMode = environment.authMode;
    environment.authMode = 'dev';
    // En mode dev, AuthService n'appelle aucune méthode d'OAuthService : un stub
    // vide suffit à satisfaire l'injection (OAuthService n'est pas providedIn root).
    TestBed.configureTestingModule({
      providers: [{ provide: OAuthService, useValue: {} as unknown as OAuthService }]
    });
    service = TestBed.inject(AuthService);
  });

  afterEach(() => { environment.authMode = prevMode; });

  it('exposes a demo user', () => {
    const u = service.snapshot();
    expect(u).not.toBeNull();
    expect(u!.tenantId).toBeTruthy();
  });

  it('produces a fake JWT carrying tenant_id', () => {
    const token = service.getAccessToken();
    expect(token).toMatch(/^[A-Za-z0-9+/=]+\.[A-Za-z0-9+/=]+\.dev-no-signature$/);
    const [, payload] = token!.split('.');
    const decoded = JSON.parse(atob(payload));
    expect(decoded.tenant_id).toBe(service.snapshot()!.tenantId);
  });

  it('reports authenticated', () => {
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('logout clears the user', () => {
    service.logout();
    expect(service.snapshot()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });
});
