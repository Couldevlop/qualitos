import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';

import { AuthService } from './auth.service';
import { RESTRICTED_ACCESS_PATH } from './role.guard';
import { businessRoutesGuard, partnerGuard, platformGuard } from './platform.guard';

/**
 * La frontière entre la console de l'éditeur et les écrans d'un client, tenue
 * dans les DEUX sens.
 *
 * <p>Le sens qu'on oublie est le second : empêcher l'éditeur d'ouvrir un écran
 * métier. Masquer les entrées de menu ne ferme rien — l'adresse reste tapable,
 * un signet la rouvre — et un écran métier ouvert sous le compte de l'éditeur
 * est trompeur plutôt qu'inutile : son jeton ne porte aucun client, donc ce que
 * l'écran afficherait ne serait les données de personne.
 */
describe('frontière consoles (éditeur, cabinet) / écrans client', () => {
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }]
    });
  });

  const serialize = (result: unknown) => TestBed.inject(Router).serializeUrl(result as UrlTree);

  describe('platformGuard', () => {
    const run = (url = '/platform/clients') =>
      TestBed.runInInjectionContext(() => platformGuard(
        {} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));

    it('ouvre la console au super administrateur', () => {
      auth.hasAnyRole.and.returnValue(true);
      expect(run()).toBeTrue();
      expect(auth.hasAnyRole).toHaveBeenCalledWith(['SUPER_ADMIN']);
    });

    it('refuse la console à un administrateur de client, en gardant l’adresse', () => {
      auth.hasAnyRole.and.returnValue(false);
      expect(serialize(run('/platform/clients')))
        .toBe(`/${RESTRICTED_ACCESS_PATH}?from=%2Fplatform%2Fclients`);
    });

    it('refuse la console à un membre sans rôle', () => {
      auth.hasAnyRole.and.returnValue(false);
      expect(serialize(run('/platform'))).toContain(RESTRICTED_ACCESS_PATH);
    });
  });

  describe('businessRoutesGuard', () => {
    const run = () => TestBed.runInInjectionContext(() => businessRoutesGuard(
      {} as ActivatedRouteSnapshot, { url: '/nc/interne' } as RouterStateSnapshot));

    it('refuse un écran métier au super administrateur, même tapé à l’adresse', () => {
      auth.hasAnyRole.and.returnValue(true);
      // Renvoi vers la console et non vers « accès réservé » : il ne s'est pas
      // trompé de droits, il s'est trompé de porte -- et « accès réservé »
      // vivant dans la coquille métier, l'y envoyer bouclerait.
      expect(serialize(run())).toBe('/platform');
    });

    it('laisse passer un responsable qualité', () => {
      auth.hasAnyRole.and.returnValue(false);
      expect(run()).toBeTrue();
    });

    it('renvoie un consultant vers la console de son cabinet', () => {
      // Un consultant est dans le même cas que l'éditeur : son jeton ne porte
      // aucun client. Ouvrir « les non-conformités » sous son compte ne
      // montrerait les non-conformités de personne.
      auth.hasAnyRole.and.callFake((roles: readonly string[]) => roles.includes('CONSULTANT'));
      expect(serialize(run())).toBe('/partner');
    });

    it('ne consulte que les deux rôles sans client, et aucun autre', () => {
      auth.hasAnyRole.and.returnValue(false);
      run();
      expect(auth.hasAnyRole.calls.allArgs()).toEqual([[['SUPER_ADMIN']], [['CONSULTANT']]]);
    });
  });

  describe('partnerGuard', () => {
    const run = (url = '/partner/portefeuille') =>
      TestBed.runInInjectionContext(() => partnerGuard(
        {} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot));

    it('ouvre la console du cabinet à un consultant', () => {
      auth.hasAnyRole.and.returnValue(true);
      expect(run()).toBeTrue();
      expect(auth.hasAnyRole).toHaveBeenCalledWith(['CONSULTANT']);
    });

    it('la refuse à qui n’est pas consultant, en gardant l’adresse', () => {
      auth.hasAnyRole.and.returnValue(false);
      expect(serialize(run('/partner/portefeuille')))
        .toBe(`/${RESTRICTED_ACCESS_PATH}?from=%2Fpartner%2Fportefeuille`);
    });
  });
});
