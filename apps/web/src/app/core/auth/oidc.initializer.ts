import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

/**
 * Initializer OIDC : appele au demarrage de l'app, configure le client
 * angular-oauth2-oidc, tente de finaliser le login (callback) et, si
 * l'utilisateur n'est toujours pas authentifie, redirige vers Keycloak.
 *
 * Skip total si authMode === 'dev' (mode demo sans backend ni Keycloak).
 */
export function initOidc(oauthService: OAuthService): () => Promise<void> {
  return async () => {
    if (environment.authMode !== 'oidc') {
      return;
    }

    const cfg: AuthConfig = {
      issuer: environment.keycloak.issuer,
      clientId: environment.keycloak.clientId,
      redirectUri: environment.keycloak.redirectUri,
      responseType: 'code',
      scope: environment.keycloak.scope,
      requireHttps: false,                  // dev local (Keycloak en http://)
      showDebugInformation: !environment.production,
      timeoutFactor: 0.75,
      sessionChecksEnabled: false
    };

    oauthService.configure(cfg);
    // NOTE: setupAutomaticSilentRefresh() volontairement non appele ici. Il
    // declenchait un iframe cache qui chargeait la page de login Keycloak
    // (perception "popup sombre par-dessus la SPA"). Le silent refresh sera
    // rebranche en P5.2 avec un iframe correctement dimensionne + bonne
    // gestion de la session check, ou remplace par le refresh-token flow.

    try {
      await oauthService.loadDiscoveryDocumentAndTryLogin();
    } catch (err) {
      console.error('[OIDC] discovery/login failed', err);
    }

    if (!oauthService.hasValidAccessToken()) {
      // Pas de token valide → kick-off interactive login (full redirect).
      oauthService.initLoginFlow();
    }
  };
}
