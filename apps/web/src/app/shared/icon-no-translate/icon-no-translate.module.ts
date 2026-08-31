import { NgModule } from '@angular/core';

import { IconNoTranslateDirective } from './icon-no-translate.directive';

/**
 * Module d'accueil de {@link IconNoTranslateDirective}.
 *
 * <p>Une directive ne peut être déclarée que dans UN module. Or `MatIconModule`
 * est fourni par deux endroits — `SharedModule` pour les écrans, `UiModule` pour
 * les composants du design system — et la directive doit s'appliquer aux deux,
 * sans quoi la moitié des icônes resterait traduisible. D'où ce module d'une
 * seule ligne, importé et ré-exporté par l'un comme par l'autre, plutôt qu'une
 * dépendance entre eux qui les rendrait circulaires.
 */
@NgModule({
  declarations: [IconNoTranslateDirective],
  exports: [IconNoTranslateDirective]
})
export class IconNoTranslateModule {}
