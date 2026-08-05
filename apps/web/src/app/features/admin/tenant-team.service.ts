import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { TenantUser, TenantUserPage } from './admin.types';

/**
 * Habilitations de l'équipe du tenant (§16).
 *
 * L'API `/api/v1/users` existait — lister, créer, changer les rôles, désactiver —
 * mais deux choses manquaient pour que la promesse « le tenant habilite son
 * équipe » soit tenue : l'administrateur du tenant n'y avait pas accès (rôle
 * absent de la politique de sécurité), et aucun écran ne l'appelait. Ajouter un
 * membre à l'équipe qualité exigeait donc une intervention de l'éditeur.
 *
 * Le tenant est dérivé du JWT côté serveur (§18.2 #2) : aucune méthode ne le
 * prend en paramètre, et la liste est nécessairement celle de l'organisation.
 */
@Injectable({ providedIn: 'root' })
export class TenantTeamService {

  // Chemin relatif au même hôte : l'ingress route `/api/v1/users` vers api-core,
  // le reste de `/api` vers le moteur qualité. En développement hors conteneur,
  // pointer `apiBaseUrl` sur api-core pour travailler cet écran.
  private readonly endpoint = `${environment.apiBaseUrl}/api/v1/users`;

  constructor(private readonly http: HttpClient) {}

  /** Membres de l'équipe du tenant courant, paginés. */
  list(page = 0, size = 50): Observable<TenantUserPage> {
    const params = new HttpParams()
      .set('page', String(page))
      .set('size', String(size));
    return this.http.get<TenantUserPage>(this.endpoint, { params });
  }

  /**
   * Fixe les rôles d'un membre — et, accessoirement, son état d'activité.
   * L'API attend l'ensemble complet des rôles, pas un delta.
   */
  setRoles(userId: string, roles: string[], active: boolean): Observable<TenantUser> {
    return this.http.put<TenantUser>(`${this.endpoint}/${userId}`, { roles, active });
  }

  /** Retire l'accès sans supprimer l'historique (désactivation, pas effacement). */
  deactivate(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/${userId}`);
  }
}
