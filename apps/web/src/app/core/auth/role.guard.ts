import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';

import { AuthService } from './auth.service';

/** Adresse de l'écran « accès réservé », hors de toute section protégée. */
export const RESTRICTED_ACCESS_PATH = 'acces-reserve';

/**
 * Garde de rôle : la route déclare dans `data.roles` les rôles qui l'ouvrent.
 *
 * <p>Sans elle, une page d'administration atteinte par son adresse s'affichait
 * à un compte sans rôle d'administration, puis échouait sur une erreur
 * générique (« Le fournisseur d'IA n'a pas pu être chargé »). La garde mène
 * plutôt à un écran qui dit la vraie raison, en gardant l'adresse demandée.
 *
 * <p>C'est un confort d'interface : le serveur reste seul juge et refuse en 403
 * ce que l'écran aurait laissé passer.
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const roles = (route.data?.['roles'] ?? []) as readonly string[];
  if (!roles.length || inject(AuthService).hasAnyRole(roles)) {
    return true;
  }
  return inject(Router).createUrlTree(['/' + RESTRICTED_ACCESS_PATH], { queryParams: { from: state.url } });
};
