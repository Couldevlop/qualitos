import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

export interface PageBreadcrumb {
  label: string;
  route?: string;
}

/**
 * En-tête de page premium — eyebrow + titre + sous-titre + actions à droite.
 * Slots :
 *  - `[qosBreadcrumbs]` (optionnel) : fil d'Ariane.
 *  - `[qosActions]` : boutons / contrôles à droite.
 *  - `[qosMeta]` : ligne de meta-data sous le titre.
 */
@Component({
  selector: 'qos-page-header',
  templateUrl: './page-header.component.html',
  styleUrls: ['./page-header.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class PageHeaderComponent {
  @Input() eyebrow?: string;
  @Input() title = '';
  @Input() subtitle?: string;
  @Input() breadcrumbs: PageBreadcrumb[] = [];
}
