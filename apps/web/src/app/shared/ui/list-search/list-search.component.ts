import { Component, Input } from '@angular/core';
import { FormControl } from '@angular/forms';

/**
 * Champ de recherche des listes : icône, effacement en un clic, et une aide
 * qui dit honnêtement sur quoi porte le filtre.
 */
@Component({
  selector: 'qos-list-search',
  templateUrl: './list-search.component.html',
  styleUrls: ['./list-search.component.scss'],
  standalone: false
})
export class ListSearchComponent {
  @Input({ required: true }) control!: FormControl<string>;
  @Input() label = $localize`:@@list-search.label:Rechercher`;
  @Input() hint = $localize`:@@list-search.hint:Filtre les lignes de la page affichée.`;

  readonly clearLabel = $localize`:@@list-search.clear:Effacer la recherche`;

  clear(): void {
    this.control.setValue('');
  }
}
