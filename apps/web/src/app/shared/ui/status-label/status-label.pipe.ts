import { Pipe, PipeTransform } from '@angular/core';

import { statusLabel } from './status-labels';

/**
 * `{{ s | statusLabel }}` : le libellé traduit d'un code de statut, à la place
 * du code brut (IN_PROGRESS) dans les listes déroulantes, cellules et pastilles.
 */
@Pipe({ name: 'statusLabel', standalone: false })
export class StatusLabelPipe implements PipeTransform {
  transform(code: string | null | undefined): string {
    return statusLabel(code);
  }
}
