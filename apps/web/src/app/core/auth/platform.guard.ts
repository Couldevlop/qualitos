import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';

import { AuthService } from './auth.service';
import { RESTRICTED_ACCESS_PATH } from './role.guard';

/** Le rôle de l'éditeur de la plateforme. Un seul, et il n'appartient à aucun client. */
export const PLATFORM_CONSOLE_ROLES: readonly string[] = ['SUPER_ADMIN'];

/** Adresse de la console de l'éditeur. */
export const PLATFORM_PATH = 'platform';

/**
 * La frontière, côté console : seul l'éditeur entre dans `/platform`.
 *
 * <p>Elle ne remplace pas le serveur, qui refuse en 403 ce que l'écran aurait
 * laissé passer. Elle évite qu'un administrateur de client arrive sur une
 * console vide dont chaque appel échoue, sans savoir pourquoi.
 */
export const platformGuard: CanActivateFn = (_route, state: RouterStateSnapshot) => {
  if (inject(AuthService).hasAnyRole(PLATFORM_CONSOLE_ROLES)) {
    return true;
  }
  return inject(Router).createUrlTree(['/' + RESTRICTED_ACCESS_PATH], {
    queryParams: { from: state.url }
  });
};

/**
 * La même frontière, dans l'autre sens : l'éditeur n'entre pas dans les écrans
 * métier d'un client.
 *
 * <p><b>Pourquoi une garde et non simplement une navigation sans ces entrées.</b>
 * Masquer les entrées de menu ne ferme rien : l'adresse `/nc/interne` reste
 * tapable, et un signet la rouvre. Or un écran métier ouvert sous le compte de
 * l'éditeur n'est pas seulement inutile — il est trompeur. Le jeton de l'éditeur
 * ne porte pas de client ; les appels partent sans portée, le serveur refuse ou,
 * pire, répond sur un client arbitraire. Ce que l'utilisateur lirait alors comme
 * « les non-conformités » ne serait les non-conformités de personne.
 *
 * <p>La console est donc structurellement séparée, et non décorativement.
 *
 * <p>Le renvoi va vers `/platform` plutôt que vers l'écran de refus : l'éditeur
 * n'a rien fait d'interdit, il s'est juste trompé de porte. L'y envoyer en
 * « accès réservé » créerait en outre une boucle, cet écran vivant lui-même
 * dans la coquille métier.
 */
export const businessRoutesGuard: CanActivateFn = () => {
  if (inject(AuthService).hasAnyRole(PLATFORM_CONSOLE_ROLES)) {
    return inject(Router).createUrlTree(['/' + PLATFORM_PATH]);
  }
  return true;
};
