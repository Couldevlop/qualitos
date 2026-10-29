import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { RESTRICTED_ACCESS_PATH, roleGuard } from './role.guard';

describe('roleGuard', () => {
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }]
    });
  });

  const run = (roles: string[] | undefined, url = '/admin/ia') =>
    TestBed.runInInjectionContext(() => roleGuard(
      { data: roles ? { roles } : {} } as unknown as ActivatedRouteSnapshot,
      { url } as RouterStateSnapshot));

  it('laisse passer une route qui ne déclare aucun rôle', () => {
    expect(run(undefined)).toBeTrue();
    expect(auth.hasAnyRole).not.toHaveBeenCalled();
  });

  it('laisse passer un utilisateur qui détient un des rôles', () => {
    auth.hasAnyRole.and.returnValue(true);
    expect(run(['ADMIN_TENANT'])).toBeTrue();
    expect(auth.hasAnyRole).toHaveBeenCalledWith(['ADMIN_TENANT']);
  });

  it('mène à « accès réservé » en gardant l’adresse demandée', () => {
    auth.hasAnyRole.and.returnValue(false);
    const result = run(['ADMIN_TENANT']) as UrlTree;
    const router = TestBed.inject(Router);
    expect(router.serializeUrl(result))
      .toBe(`/${RESTRICTED_ACCESS_PATH}?from=%2Fadmin%2Fia`);
  });
});
