import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';
import { filter } from 'rxjs/operators';

import { environment } from '../../../environments/environment';

/** Clé sessionStorage : route demandée avant un redirect de (ré)authentification. */
const RETURN_ROUTE_KEY = 'qos.oidc.returnRoute';

/**
 * Initializer OIDC : appele au demarrage de l'app, configure le client
 * angular-oauth2-oidc, tente de finaliser le login (callback) et, si
 * l'utilisateur n'est toujours pas authentifie, redirige vers Keycloak.
 *
 * Robustesse session (corrige le scintillement / "aucun ecran" sur les routes
 * lorsque l'access token de 15 min expirait) :
 *  - **Rafraichissement automatique via refresh_token** (PAS d'iframe silent —
 *    qui avait ete desactive a cause du "popup sombre"). L'evenement
 *    `token_expires` est emis a `timeoutFactor` (0.75) de la duree de vie ;
 *    on appelle alors `refreshToken()`. La session ne meurt plus a 15 min.
 *  - **Preservation de la route demandee** a travers le redirect Keycloak :
 *    `redirectUri` pointe sur `/`, donc sans cela un F5 / deep-link sur une
 *    methode (PDCA, 5S…) retombait sur `/home`. On memorise la route avant le
 *    redirect et on la restaure au retour, avant le bootstrap du router.
 *
 * Skip total si authMode !== 'oidc' (mode demo sans backend ni Keycloak).
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

    // Rafraichissement automatique base sur le refresh_token (sans iframe).
    // `token_expires` arrive avant l'expiration (timeoutFactor) ; on rafraichit.
    // En cas d'echec (refresh token revoque/expire), on relance un login complet
    // en preservant la route courante.
    oauthService.events
      .pipe(filter(e => e.type === 'token_expires'))
      .subscribe(() => {
        oauthService.refreshToken().catch(() => redirectToLogin(oauthService));
      });

    try {
      await oauthService.loadDiscoveryDocumentAndTryLogin();
    } catch (err) {
      console.error('[OIDC] discovery/login failed', err);
    }

    if (!oauthService.hasValidAccessToken()) {
      // Pas de token valide → kick-off interactive login en memorisant la cible.
      redirectToLogin(oauthService);
      return;
    }

    // Retour d'authentification : restaure la route demandee avant le redirect,
    // avant que le router ne bootstrap (sinon on resterait sur `/` → `/home`).
    restoreReturnRoute();
  };
}

/**
 * Memorise la route courante (hors callback OAuth) puis declenche le login.
 * Idempotent : ne lance qu'un seul flux.
 */
function redirectToLogin(oauthService: OAuthService): void {
  try {
    const here = window.location.pathname + window.location.search;
    if (here && here !== '/' && !here.includes('code=')) {
      sessionStorage.setItem(RETURN_ROUTE_KEY, here);
    }
  } catch {
    /* sessionStorage indisponible : on degrade sans bloquer le login */
  }
  oauthService.initLoginFlow();
}

/**
 * Si une route avait ete memorisee avant le redirect, la restaure dans l'URL
 * (replaceState) pour que le router demarre dessus, puis nettoie la cle.
 */
function restoreReturnRoute(): void {
  try {
    const target = sessionStorage.getItem(RETURN_ROUTE_KEY);
    sessionStorage.removeItem(RETURN_ROUTE_KEY);
    const current = window.location.pathname + window.location.search;
    if (target && target !== current && target !== '/') {
      window.history.replaceState({}, '', target);
    }
  } catch {
    /* pas de route memorisee ou storage indisponible : on reste sur l'URL courante */
  }
}
