import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { BehaviorSubject, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface AuthUser {
  userId: string;
  tenantId: string;
  displayName: string;
  roles: string[];
}

/**
 * Authentification : mode 'dev' (utilisateur fictif + fake JWT) ou 'oidc'
 * (Keycloak via angular-oauth2-oidc, bootstrap dans APP_INITIALIZER).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly user$ = new BehaviorSubject<AuthUser | null>(null);

  constructor(private readonly oauth: OAuthService) {
    this.user$.next(this.bootstrapUser());
    if (environment.authMode === 'oidc') {
      // Reagit aux changements de token (refresh silencieux, logout, etc.).
      this.oauth.events.subscribe(() => this.user$.next(this.bootstrapUser()));
    }
  }

  user(): Observable<AuthUser | null> {
    return this.user$.asObservable();
  }

  snapshot(): AuthUser | null {
    return this.user$.getValue();
  }

  /** Token Bearer a injecter dans les requetes API. null = pas authentifie. */
  getAccessToken(): string | null {
    if (environment.authMode === 'dev') {
      return this.devFakeJwt();
    }
    const t = this.oauth.getAccessToken();
    return t && t.length > 0 ? t : null;
  }

  /**
   * Vrai si l'utilisateur porte l'un des rôles demandés.
   *
   * <p>La comparaison ignore la casse : Keycloak les rend en minuscules, alors
   * que le serveur les écrit en majuscules dans ses `@PreAuthorize`. L'autorité
   * reste le serveur — cette méthode ne sert qu'à ne pas afficher un bouton qui
   * répondra 403.
   */
  hasAnyRole(roles: string[]): boolean {
    const held = (this.snapshot()?.roles ?? []).map(role => role.toUpperCase());
    return roles.some(role => held.includes(role.toUpperCase()));
  }

  /**
   * Redemande une authentification d'un palier supérieur (second facteur).
   *
   * <p>Le serveur repond 403 « step-up-required » sur les actions critiques quand
   * le jeton ne porte pas la trace d'un second facteur. Ce n'est pas une session
   * invalide : c'est une session trop faible pour CE geste. On repart donc dans
   * le flux d'authentification en demandant le palier, plutôt que de déconnecter
   * l'utilisateur.
   *
   * <p>En mode 'dev' il n'y a pas de fournisseur d'identité : la méthode rend
   * false, et l'appelant se contente d'afficher le refus.
   */
  stepUp(returnTo: string = ''): boolean {
    if (environment.authMode !== 'oidc') return false;
    this.oauth.initLoginFlow(returnTo, { acr_values: environment.stepUpAcrValue });
    return true;
  }

  isAuthenticated(): boolean {
    if (environment.authMode === 'oidc') {
      return this.oauth.hasValidAccessToken();
    }
    return this.snapshot() !== null;
  }

  logout(): void {
    this.user$.next(null);
    if (environment.authMode === 'oidc') {
      this.oauth.logOut();
    }
  }

  private bootstrapUser(): AuthUser | null {
    if (environment.authMode === 'dev') {
      return {
        userId: '00000000-0000-0000-0000-000000000001',
        tenantId: '00000000-0000-0000-0000-000000000099',
        displayName: 'Demo User',
        roles: ['quality_manager']
      };
    }
    // Mode oidc : on extrait les claims du JWT courant si dispo.
    const claims = this.oauth.getIdentityClaims() as Record<string, unknown> | null;
    if (!claims) return null;
    return {
      userId: String(claims['sub'] ?? ''),
      tenantId: String(claims['tenant_id'] ?? ''),
      displayName: String(claims['preferred_username'] ?? claims['name'] ?? 'User'),
      roles: this.readRoles(claims)
    };
  }

  /**
   * Les rôles se lisent dans le JETON D'ACCÈS, pas dans le jeton d'identité.
   *
   * <p>Keycloak ne place pas `realm_access` dans l'ID token : son mapper « realm
   * roles » n'y écrit rien par défaut. Mesuré sur la préproduction — le jeton
   * d'accès porte `realm_access.roles`, l'ID token n'a pas la revendication du
   * tout. Comme cette méthode lisait les claims d'identité, TOUT utilisateur
   * arrivait avec `roles: []` : la section « Administration » restait invisible
   * même pour `superadmin`, et la console des modules affichait « Sur demande
   * auprès de l'éditeur » au lieu de ses boutons. Le nom, lui, s'affichait
   * correctement — `preferred_username` est bien dans l'ID token — ce qui rendait
   * la panne trompeuse : le bon compte, et aucun de ses droits.
   *
   * <p>Le repli sur l'ID token est conservé pour les realms qui ajoutent le
   * mapper, et le jeton n'est pas vérifié ici : sa signature est l'affaire du
   * serveur, qui reste seul juge des autorisations. Ce que l'on décide ici, c'est
   * uniquement de ne pas montrer un bouton qui répondrait 403.
   */
  private readRoles(claims: Record<string, unknown>): string[] {
    const fromAccess = this.decodeRealmRoles(this.oauth.getAccessToken());
    if (fromAccess.length > 0) return fromAccess;
    const realmAccess = (claims['realm_access'] as { roles?: string[] } | undefined);
    return realmAccess?.roles ?? [];
  }

  /** Lecture tolérante de la charge utile d'un JWT : un jeton illisible ne vaut aucun rôle. */
  private decodeRealmRoles(token: string | null | undefined): string[] {
    if (!token) return [];
    const parts = token.split('.');
    if (parts.length < 2) return [];
    try {
      const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = payload + '='.repeat((4 - (payload.length % 4)) % 4);
      const decoded = JSON.parse(atob(padded)) as Record<string, unknown>;
      const realmAccess = decoded['realm_access'] as { roles?: string[] } | undefined;
      return Array.isArray(realmAccess?.roles) ? realmAccess!.roles! : [];
    } catch {
      return [];
    }
  }

  /**
   * Faux JWT non signé, lisible par le backend uniquement en profil de dev
   * (le resource server validerait normalement la signature). Sert seulement
   * à transporter tenant_id côté UI quand on bascule useMockApi=false en local.
   */
  private devFakeJwt(): string {
    const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }));
    const payload = btoa(JSON.stringify({
      sub: this.snapshot()?.userId,
      tenant_id: this.snapshot()?.tenantId,
      iat: Math.floor(Date.now() / 1000),
      exp: Math.floor(Date.now() / 1000) + 3600
    }));
    return `${header}.${payload}.dev-no-signature`;
  }
}
