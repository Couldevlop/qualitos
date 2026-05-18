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
    const realmAccess = (claims['realm_access'] as { roles?: string[] } | undefined);
    return {
      userId: String(claims['sub'] ?? ''),
      tenantId: String(claims['tenant_id'] ?? ''),
      displayName: String(claims['preferred_username'] ?? claims['name'] ?? 'User'),
      roles: realmAccess?.roles ?? []
    };
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
