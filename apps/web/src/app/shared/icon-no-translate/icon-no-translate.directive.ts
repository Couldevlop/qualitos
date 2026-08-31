import { Directive, HostBinding } from '@angular/core';

/**
 * Soustrait les icônes Material à la traduction automatique du navigateur.
 *
 * <p>Une icône Material s'écrit en LIGATURE : le nom de l'icône est le contenu
 * textuel de la balise (`<mat-icon>home</mat-icon>`), et la police le rend sous
 * forme de dessin. Chrome — qui propose de traduire toute page dont la langue
 * diffère de la sienne — voit ce contenu comme du texte à traduire. Il remplace
 * alors `home` par « MAISON », `chevron_left` par « CHEVRON_GAUCHE » : la
 * ligature n'existe plus dans la police, et la barre de navigation se retrouve
 * criblée de mots en capitales à la place de ses pictogrammes.
 *
 * <p>La correction porte sur les ICÔNES SEULES, jamais sur la page. Poser
 * `notranslate` au niveau du document interdirait la traduction de tout le
 * reste : QualitOS sert six langues, mais un lecteur qui en veut une septième a
 * le droit de la demander à son navigateur. Ce qui ne doit pas être traduit,
 * c'est le nom technique d'un glyphe — pas le contenu qualité qui l'entoure.
 *
 * <p>Sélecteur d'élément et non attribut à poser : cent quarante-trois gabarits
 * emploient `<mat-icon>`, et une règle qu'il faut penser à répéter cent
 * quarante-trois fois est une règle qui sera oubliée à la cent quarante-quatrième.
 */
@Directive({
  // Le projet est en NgModules (pas de composant autonome) ; Angular 20 rendant
  // `standalone` vrai par défaut, il faut le dire.
  standalone: false,
  selector: 'mat-icon'
})
export class IconNoTranslateDirective {

  /**
   * `translate="no"` est l'attribut HTML standard (WHATWG), respecté par Chrome,
   * Edge et Safari — et non une extension propriétaire comme la classe
   * `notranslate` de Google Traduction, qui ne couvre pas les autres moteurs.
   */
  @HostBinding('attr.translate') readonly translate = 'no';
}
