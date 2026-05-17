import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

/**
 * Carte / panneau premium — titre + sous-titre + actions + contenu projeté.
 * Sert de wrapper de section (graph, liste, formulaire).
 *
 * Slots :
 *  - `[qosActions]` : à droite du header.
 *  - `[qosFooter]` : zone footer optionnelle.
 */
@Component({
  selector: 'qos-panel',
  templateUrl: './panel.component.html',
  styleUrls: ['./panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class PanelComponent {
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() icon?: string;
  @Input() padded = true;
  @Input() loading = false;
}
