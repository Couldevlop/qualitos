import { EMPTY } from 'rxjs';

import { Router, UrlTree } from '@angular/router';

/**
 * Un espion de {@link Router} qui supporte {@code routerLink}.
 *
 * <p>Un espion à la seule méthode {@code navigate} suffit tant qu'un écran se
 * contente de naviguer par code. Dès qu'un lien {@code routerLink} apparaît
 * dans le gabarit, la directive calcule son {@code href} à l'affichage et
 * appelle {@code createUrlTree} : sans elle, le rendu tombe sur un
 * {@code TypeError} qui n'a rien à voir avec ce que le test vérifie.
 *
 * <p>L'arbre rendu est volontairement vide : les tests vérifient la cible du
 * lien via {@code routerLink}, pas l'URL sérialisée.
 */
export function routerEspion(): jasmine.SpyObj<Router> {
  const espion = jasmine.createSpyObj<Router>(
    'Router', ['navigate', 'navigateByUrl', 'createUrlTree', 'serializeUrl'],
    // La directive s'abonne aussi au flux de navigation, pour reteindre un lien
    // devenu actif : sans flux, elle tombe des son premier affichage.
    { events: EMPTY });
  espion.createUrlTree.and.returnValue({} as UrlTree);
  espion.serializeUrl.and.returnValue('/');
  return espion;
}
