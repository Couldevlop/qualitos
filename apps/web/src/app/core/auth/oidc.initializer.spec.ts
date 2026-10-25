import { AuthConfig, OAuthEvent, OAuthService } from 'angular-oauth2-oidc';
import { Subject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { initOidc, restoreReturnRoute } from './oidc.initializer';

/**
 * L'amorçage OIDC.
 *
 * <p>Le réglage qui a le plus coûté ici est l'URI de POST-DÉCONNEXION. Laissée
 * implicite, elle était bien envoyée par la bibliothèque, mais rien dans
 * l'application ne la reliait à la liste `post.logout.redirect.uris` du client
 * Keycloak : quand celle-ci manquait côté realm, la déconnexion répondait
 * « Invalid redirect uri » et l'utilisateur restait bloqué sur une page d'erreur,
 * alors que la connexion, elle, fonctionnait. On la déclare donc, et on la teste.
 */
describe('initOidc', () => {

  let oauth: jasmine.SpyObj<OAuthService>;
  let events: Subject<OAuthEvent>;
  let modeInitial: 'dev' | 'oidc';

  beforeEach(() => {
    modeInitial = environment.authMode;
    events = new Subject<OAuthEvent>();
    oauth = jasmine.createSpyObj<OAuthService>('OAuthService',
      ['configure', 'loadDiscoveryDocumentAndTryLogin', 'hasValidAccessToken',
        'initLoginFlow', 'refreshToken'],
      { events: events.asObservable() });
    oauth.loadDiscoveryDocumentAndTryLogin.and.resolveTo(true);
    oauth.hasValidAccessToken.and.returnValue(true);
    oauth.refreshToken.and.resolveTo({} as never);
    sessionStorage.clear();
  });

  afterEach(() => {
    environment.authMode = modeInitial;
    sessionStorage.clear();
  });

  function config(): AuthConfig {
    return oauth.configure.calls.mostRecent().args[0];
  }

  it('ne touche à rien hors du mode oidc', async () => {
    environment.authMode = 'dev';

    await initOidc(oauth)();

    expect(oauth.configure).not.toHaveBeenCalled();
    expect(oauth.loadDiscoveryDocumentAndTryLogin).not.toHaveBeenCalled();
  });

  it('déclare l’URI de post-déconnexion, et la même que celle de redirection', async () => {
    environment.authMode = 'oidc';

    await initOidc(oauth)();

    expect(config().postLogoutRedirectUri).toBe(environment.keycloak.redirectUri);
    expect(config().postLogoutRedirectUri).toBeTruthy();
  });

  it('configure le client sur le code d’autorisation et l’émetteur du realm', async () => {
    environment.authMode = 'oidc';

    await initOidc(oauth)();

    expect(config().issuer).toBe(environment.keycloak.issuer);
    expect(config().clientId).toBe(environment.keycloak.clientId);
    expect(config().responseType).toBe('code');
    expect(config().sessionChecksEnabled).toBeFalse();
  });

  it('renvoie vers la connexion quand aucun jeton valide n’est en main', async () => {
    environment.authMode = 'oidc';
    oauth.hasValidAccessToken.and.returnValue(false);

    await initOidc(oauth)();

    expect(oauth.initLoginFlow).toHaveBeenCalled();
  });

  it('démarre malgré un échec de découverte plutôt que de rester sans écran', async () => {
    environment.authMode = 'oidc';
    oauth.loadDiscoveryDocumentAndTryLogin.and.rejectWith(new Error('injoignable'));
    oauth.hasValidAccessToken.and.returnValue(false);

    await initOidc(oauth)();

    expect(oauth.initLoginFlow).toHaveBeenCalled();
  });

  it('rafraîchit le jeton avant son expiration, sans reconnexion visible', async () => {
    environment.authMode = 'oidc';
    await initOidc(oauth)();

    events.next({ type: 'token_expires' } as OAuthEvent);

    expect(oauth.refreshToken).toHaveBeenCalled();
  });

  it('relance une connexion complète si le rafraîchissement échoue', async () => {
    environment.authMode = 'oidc';
    oauth.refreshToken.and.rejectWith(new Error('refresh révoqué'));
    await initOidc(oauth)();

    events.next({ type: 'token_expires' } as OAuthEvent);
    await Promise.resolve();
    await Promise.resolve();

    expect(oauth.initLoginFlow).toHaveBeenCalled();
  });
});

/**
 * Restauration de la route après le retour de Keycloak. Le retour arrive sur `/`,
 * que nginx renvoie vers la locale du navigateur : la route mémorisée peut
 * appartenir à une autre build. La restaurer sous la mauvaise base donnait
 * `/en/fr/…` et la page « introuvable ».
 */
describe('restoreReturnRoute', () => {

  const KEY = 'qos.oidc.returnRoute';
  let loc: { pathname: string; search: string; replace: jasmine.Spy };
  let history: jasmine.SpyObj<History>;

  beforeEach(() => {
    sessionStorage.clear();
    loc = { pathname: '/en/', search: '', replace: jasmine.createSpy('replace') };
    history = jasmine.createSpyObj<History>('History', ['replaceState']);
  });

  afterEach(() => sessionStorage.clear());

  it('restaure sans rechargement une route de la même locale', () => {
    sessionStorage.setItem(KEY, '/en/suppliers/non-conformites');

    expect(restoreReturnRoute('/en/', loc as never, history)).toBeFalse();

    expect(history.replaceState).toHaveBeenCalledWith({}, '', '/en/suppliers/non-conformites');
    expect(loc.replace).not.toHaveBeenCalled();
  });

  it('recharge la route dans sa propre build quand la locale diffère', () => {
    sessionStorage.setItem(KEY, '/fr/suppliers/non-conformites');

    expect(restoreReturnRoute('/en/', loc as never, history)).toBeTrue();

    expect(loc.replace).toHaveBeenCalledWith('/fr/suppliers/non-conformites');
    expect(history.replaceState).not.toHaveBeenCalled();
  });

  it('ne fait rien sans route mémorisée, et consomme la clé', () => {
    expect(restoreReturnRoute('/en/', loc as never, history)).toBeFalse();
    sessionStorage.setItem(KEY, '/en/');
    loc.pathname = '/en/';

    expect(restoreReturnRoute('/en/', loc as never, history)).toBeFalse();

    expect(history.replaceState).not.toHaveBeenCalled();
    expect(loc.replace).not.toHaveBeenCalled();
    expect(sessionStorage.getItem(KEY)).toBeNull();
  });
});
