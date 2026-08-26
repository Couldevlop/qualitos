import { TestBed } from '@angular/core/testing';
import { OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthService, AuthUser } from './auth.service';

/**
 * Service d'authentification — les deux modes sont testés.
 *
 * En 'oidc', l'identité vient EXCLUSIVEMENT des claims du jeton : ni le tenant,
 * ni les rôles ne sont saisis ou déduits côté client (§18.2 #2). Les valeurs de
 * repli comptent donc autant que le cas nominal — un claim absent ne doit ni
 * faire planter l'application, ni produire un utilisateur à demi construit qui
 * passerait pour authentifié.
 */
describe('AuthService', () => {

  let prevMode: 'dev' | 'oidc';

  afterEach(() => { environment.authMode = prevMode; });

  // ------------------------------------------------------------------------
  // Mode développement
  // ------------------------------------------------------------------------
  describe('en mode développement', () => {
    let service: AuthService;

    beforeEach(() => {
      // Le mode est lu AU CONSTRUCTEUR : il doit être forcé avant l'injection.
      prevMode = environment.authMode;
      environment.authMode = 'dev';
      TestBed.configureTestingModule({
        // En mode dev, aucune méthode d'OAuthService n'est appelée : un stub vide
        // suffit à satisfaire l'injection.
        providers: [{ provide: OAuthService, useValue: {} as unknown as OAuthService }]
      });
      service = TestBed.inject(AuthService);
    });

    it('expose un utilisateur de démonstration complet', () => {
      const u = service.snapshot();

      expect(u).not.toBeNull();
      expect(u!.userId).toBeTruthy();
      expect(u!.tenantId).toBeTruthy();
      expect(u!.roles).toEqual(['quality_manager']);
    });

    it('publie l\'utilisateur sur le flux, pas seulement en instantané', () => {
      const seen: (AuthUser | null)[] = [];
      const sub = service.user().subscribe(u => seen.push(u));

      expect(seen[0]?.displayName).toBe('Demo User');
      sub.unsubscribe();
    });

    it('produit un faux jeton qui transporte le tenant', () => {
      const token = service.getAccessToken();

      expect(token).toMatch(/^[A-Za-z0-9+/=]+\.[A-Za-z0-9+/=]+\.dev-no-signature$/);
      const [, payload] = token!.split('.');
      const decoded = JSON.parse(atob(payload));
      expect(decoded.tenant_id).toBe(service.snapshot()!.tenantId);
      expect(decoded.sub).toBe(service.snapshot()!.userId);
    });

    it('assume que le faux jeton n\'est PAS signé', () => {
      const [header] = service.getAccessToken()!.split('.');

      // `alg: none` est explicite : ce jeton n'a de valeur qu'en profil de
      // développement, où le serveur ne vérifie pas la signature. Le rendre
      // vraisemblable serait pire — il pourrait être pris pour un vrai.
      expect(JSON.parse(atob(header)).alg).toBe('none');
    });

    it('borne la validité du faux jeton à une heure', () => {
      const [, payload] = service.getAccessToken()!.split('.');
      const { iat, exp } = JSON.parse(atob(payload));

      expect(exp - iat).toBe(3600);
    });

    it('considère authentifié tant qu\'un utilisateur est présent', () => {
      expect(service.isAuthenticated()).toBeTrue();
    });

    it('la déconnexion vide l\'utilisateur, sans appeler le fournisseur', () => {
      service.logout();

      expect(service.snapshot()).toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
    });
  });

  // ------------------------------------------------------------------------
  // Mode OIDC
  // ------------------------------------------------------------------------
  describe('en mode OIDC', () => {
    let events: Subject<unknown>;
    let oauth: {
      events: Subject<unknown>;
      getIdentityClaims: jasmine.Spy;
      getAccessToken: jasmine.Spy;
      hasValidAccessToken: jasmine.Spy;
      logOut: jasmine.Spy;
    };

    function build(): AuthService {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [{ provide: OAuthService, useValue: oauth as unknown as OAuthService }]
      });
      return TestBed.inject(AuthService);
    }

    beforeEach(() => {
      prevMode = environment.authMode;
      environment.authMode = 'oidc';
      events = new Subject<unknown>();
      oauth = {
        events,
        getIdentityClaims: jasmine.createSpy('getIdentityClaims').and.returnValue(null),
        getAccessToken: jasmine.createSpy('getAccessToken').and.returnValue(''),
        hasValidAccessToken: jasmine.createSpy('hasValidAccessToken').and.returnValue(false),
        logOut: jasmine.createSpy('logOut')
      };
    });

    // ---- Extraction de l'identité -------------------------------------------

    it('construit l\'utilisateur à partir des claims du jeton', () => {
      oauth.getIdentityClaims.and.returnValue({
        sub: 'u-42',
        tenant_id: 't-7',
        preferred_username: 'marie.dubois',
        realm_access: { roles: ['quality_manager', 'auditor'] }
      });

      const u = build().snapshot()!;

      expect(u.userId).toBe('u-42');
      expect(u.tenantId).toBe('t-7');
      expect(u.displayName).toBe('marie.dubois');
      expect(u.roles).toEqual(['quality_manager', 'auditor']);
    });

    it('n\'expose aucun utilisateur tant qu\'aucun jeton n\'est présent', () => {
      expect(build().snapshot()).toBeNull();
    });

    it('retombe sur le nom complet quand l\'identifiant de connexion manque', () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1', name: 'Marie Dubois' });

      expect(build().snapshot()!.displayName).toBe('Marie Dubois');
    });

    it('affiche un libellé neutre quand aucun nom n\'est fourni', () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1' });

      // Mieux vaut « User » qu'un « undefined » affiché dans l'interface.
      expect(build().snapshot()!.displayName).toBe('User');
    });

    it('rend des chaînes vides plutôt qu\'indéfinies pour les claims absents', () => {
      oauth.getIdentityClaims.and.returnValue({ preferred_username: 'anon' });

      const u = build().snapshot()!;
      expect(u.userId).toBe('');
      expect(u.tenantId).toBe('');
    });

    it('n\'accorde aucun rôle en l\'absence de realm_access', () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1' });

      // Un tableau vide, jamais indéfini : tout contrôle de rôle doit pouvoir
      // itérer sans garde, et l'absence de rôle n'accorde rien.
      expect(build().snapshot()!.roles).toEqual([]);
    });

    // ---- Origine des rôles ---------------------------------------------------
    //
    // Ces bancs ont laissé passer une panne réelle : ils injectaient `realm_access`
    // par `getIdentityClaims()`, forme que Keycloak NE PRODUIT PAS — son mapper
    // « realm roles » n'écrit pas dans l'ID token. Tout utilisateur arrivait donc
    // sans rôle en production, `superadmin` compris, pendant que le banc restait
    // vert. On teste désormais la forme réelle : les rôles vivent dans le jeton
    // d'accès.

    /** Fabrique un JWT non signé — seule la charge utile compte ici. */
    const jwt = (payload: unknown): string => {
      const base = btoa(JSON.stringify(payload));
      return "entete." + base.replace(/[+]/g, "-").replace(/[/]/g, "_") + ".signature";
    };

    it("lit les rôles dans le jeton d'accès, où Keycloak les met vraiment", () => {
      // Forme mesurée sur la préproduction : l'ID token porte l'identité, pas les rôles.
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-9', preferred_username: 'superadmin' });
      oauth.getAccessToken.and.returnValue(jwt({ realm_access: { roles: ['super_admin'] } }));

      const u = build().snapshot()!;

      expect(u.displayName).toBe('superadmin');
      expect(u.roles).toEqual(['super_admin']);
    });

    it("accorde les droits d'administration quand le jeton d'accès porte super_admin", () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-9' });
      oauth.getAccessToken.and.returnValue(jwt({ realm_access: { roles: ['super_admin'] } }));

      // C'est ce contrôle qui masquait la section « Administration » et les boutons
      // de la console des modules.
      expect(build().hasAnyRole(['SUPER_ADMIN'])).toBeTrue();
    });

    it("retombe sur l'ID token quand le realm y ajoute le mapper", () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1', realm_access: { roles: ['auditor'] } });
      oauth.getAccessToken.and.returnValue('');

      expect(build().snapshot()!.roles).toEqual(['auditor']);
    });

    it("n'accorde aucun rôle quand le jeton d'accès est illisible", () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1' });
      oauth.getAccessToken.and.returnValue('pas-un-jwt');

      // Un jeton qu'on ne sait pas lire ne vaut aucun droit — et ne casse rien.
      expect(build().snapshot()!.roles).toEqual([]);
    });

    it('n\'accorde aucun rôle quand realm_access est présent mais vide', () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1', realm_access: {} });

      expect(build().snapshot()!.roles).toEqual([]);
    });

    // ---- Réaction aux événements du fournisseur --------------------------------

    it('recalcule l\'utilisateur à chaque événement du fournisseur', () => {
      const service = build();
      expect(service.snapshot()).toBeNull();

      oauth.getIdentityClaims.and.returnValue({ sub: 'u-9', tenant_id: 't-9' });
      events.next({ type: 'token_received' });

      // Un rafraîchissement silencieux doit se répercuter sans rechargement.
      expect(service.snapshot()!.userId).toBe('u-9');
    });

    it('efface l\'utilisateur quand le jeton disparaît', () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-9' });
      const service = build();
      expect(service.snapshot()).not.toBeNull();

      oauth.getIdentityClaims.and.returnValue(null);
      events.next({ type: 'session_terminated' });

      expect(service.snapshot()).toBeNull();
    });

    // ---- Jeton et état d'authentification ---------------------------------------

    it('rend le jeton du fournisseur, et null s\'il est vide', () => {
      const service = build();

      oauth.getAccessToken.and.returnValue('jeton-reel');
      expect(service.getAccessToken()).toBe('jeton-reel');

      // Une chaîne vide n'est pas un jeton : la rendre telle quelle produirait
      // un en-tête « Bearer » vide, refusé par le serveur sans explication.
      oauth.getAccessToken.and.returnValue('');
      expect(service.getAccessToken()).toBeNull();
    });

    it('délègue au fournisseur la validité du jeton, sans la déduire', () => {
      const service = build();

      oauth.hasValidAccessToken.and.returnValue(true);
      expect(service.isAuthenticated()).toBeTrue();

      // Même avec un utilisateur en mémoire, un jeton expiré n'authentifie plus.
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1' });
      events.next({ type: 'token_expires' });
      oauth.hasValidAccessToken.and.returnValue(false);
      expect(service.snapshot()).not.toBeNull();
      expect(service.isAuthenticated()).toBeFalse();
    });

    it('la déconnexion vide l\'utilisateur ET informe le fournisseur', () => {
      oauth.getIdentityClaims.and.returnValue({ sub: 'u-1' });
      const service = build();

      service.logout();

      expect(service.snapshot()).toBeNull();
      // Sans appel au fournisseur, la session resterait ouverte côté Keycloak :
      // un nouvel onglet se reconnecterait silencieusement.
      expect(oauth.logOut).toHaveBeenCalled();
    });
  });
});
