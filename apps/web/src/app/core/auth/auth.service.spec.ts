import { TestBed } from '@angular/core/testing';

import { AuthService } from './auth.service';

describe('AuthService (dev mode)', () => {
  let service: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AuthService);
  });

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
