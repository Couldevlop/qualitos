import { inject } from '@angular/core';
import { CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';

import { AuthService } from './auth.service';
import { RESTRICTED_ACCESS_PATH } from './role.guard';

/** Le rôle de l'éditeur de la plateforme. Un seul, et il n'appartient à aucun client. */
export const PLATFORM_CONSOLE_ROLES: readonly string[] = ['SUPER_ADMIN'];

/** Adresse de la console de l'éditeur. */
export const PLATFORM_PATH = 'platform';

/**
 * Le rôle d'un consultant de cabinet. Comme l'éditeur, il n'appartient à aucun
 * client — et pour la même raison, il n'a rien à faire dans les écrans métier.
 */
export const PARTNER_CONSOLE_ROLES: readonly string[] = ['CONSULTANT'];

/** Adresse de la console d'un cabinet. */
export const PARTNER_PATH = 'partner';

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
 * La frontière de la console d'un cabinet : seul un consultant y entre.
 *
 * <p>Même raisonnement que pour l'éditeur, et même garde-fou : le serveur
 * refuse de toute façon, mais arriver sur une console vide dont chaque appel
 * échoue ne dit à personne ce qui ne va pas.
 */
export const partnerGuard: CanActivateFn = (_route, state: RouterStateSnapshot) => {
  if (inject(AuthService).hasAnyRole(PARTNER_CONSOLE_ROLES)) {
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
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.hasAnyRole(PLATFORM_CONSOLE_ROLES)) {
    return router.createUrlTree(['/' + PLATFORM_PATH]);
  }
  // Un consultant est dans le même cas que l'éditeur, mot pour mot : son jeton
  // ne porte pas de client. Ouvrir « les non-conformités » sous son compte ne
  // montrerait les non-conformités de personne — et surtout pas celles du
  // client chez qui il est censé intervenir, qui ne s'atteignent que par un
  // accord enregistré.
  if (auth.hasAnyRole(PARTNER_CONSOLE_ROLES)) {
    return router.createUrlTree(['/' + PARTNER_PATH]);
  }
  return true;
};
