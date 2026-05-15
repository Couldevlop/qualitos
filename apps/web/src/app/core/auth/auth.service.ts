import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

import { environment } from '../../../environments/environment';

export interface AuthUser {
  userId: string;
  tenantId: string;
  displayName: string;
  roles: string[];
}

/**
 * Authentification minimaliste.
 *
 * En mode 'dev' : retourne un utilisateur fictif et un faux JWT
 * (pratique pour développer l'UI sans Keycloak).
 *
 * En mode 'oidc' : à brancher sur angular-oauth2-oidc dans une itération
 * suivante (méthode loginOidc()). On garde l'API stable pour ne pas
 * casser les composants consommateurs.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly user$ = new BehaviorSubject<AuthUser | null>(this.bootstrapUser());

  user(): Observable<AuthUser | null> {
    return this.user$.asObservable();
  }

  snapshot(): AuthUser | null {
    return this.user$.getValue();
  }

  /** Token Bearer à injecter dans les requêtes API. null = pas authentifié. */
  getAccessToken(): string | null {
    if (environment.authMode === 'dev') {
      return this.devFakeJwt();
    }
    // TODO: brancher angular-oauth2-oidc — récupérer this.oauthService.getAccessToken()
    return null;
  }

  isAuthenticated(): boolean {
    return this.snapshot() !== null;
  }

  /** Logout placeholder. Réel en oidc plus tard. */
  logout(): void {
    this.user$.next(null);
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
    return null;
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
