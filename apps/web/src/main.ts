import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

/**
 * Forme du fichier de configuration runtime (`assets/config.json`).
 * Toutes les clés sont facultatives : une clé absente ou vide conserve la valeur
 * compilée dans `src/environments/`.
 */
interface RuntimeConfig {
  apiBaseUrl?: string;
  keycloak?: {
    issuer?: string;
    clientId?: string;
    redirectUri?: string;
    scope?: string;
  };
}

/**
 * Charge la configuration runtime AVANT le bootstrap Angular.
 *
 * Pourquoi : `environment.production.ts` figeait `http://localhost:8082` et l'issuer
 * Keycloak dans le bundle — l'image n'était donc déployable que sur le poste de
 * développement. En lisant `assets/config.json` au démarrage, la même image sert
 * n'importe quel domaine (exigence souveraineté / on-premise, CLAUDE.md §10.3).
 *
 * L'URL est résolue relativement au `<base href>` (`/fr/`, `/en/`…), donc le
 * fichier est servi par nginx comme n'importe quel asset de la locale.
 *
 * Dégradation : toute erreur (fichier absent, JSON invalide, réseau) est avalée et
 * l'application démarre sur les valeurs compilées. Jamais d'écran blanc à cause de
 * la configuration.
 */
async function loadRuntimeConfig(): Promise<void> {
  try {
    const base = document.querySelector('base')?.getAttribute('href') ?? '/';
    const response = await fetch(`${base}assets/config.json`, { cache: 'no-cache' });
    if (!response.ok) {
      return;
    }
    const config = (await response.json()) as RuntimeConfig;

    if (config.apiBaseUrl) {
      environment.apiBaseUrl = config.apiBaseUrl;
    }
    if (config.keycloak?.issuer) {
      environment.keycloak.issuer = config.keycloak.issuer;
    }
    if (config.keycloak?.clientId) {
      environment.keycloak.clientId = config.keycloak.clientId;
    }
    if (config.keycloak?.redirectUri) {
      environment.keycloak.redirectUri = config.keycloak.redirectUri;
    }
    if (config.keycloak?.scope) {
      environment.keycloak.scope = config.keycloak.scope;
    }
  } catch {
    // Configuration runtime indisponible : on démarre sur les valeurs compilées.
  }
}

loadRuntimeConfig()
  .then(() => platformBrowserDynamic().bootstrapModule(AppModule))
  .catch(err => console.error(err));
