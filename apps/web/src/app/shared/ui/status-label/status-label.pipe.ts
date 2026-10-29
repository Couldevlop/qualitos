import { Pipe, PipeTransform } from '@angular/core';

import { StatusGender, statusLabel } from './status-labels';

/**
 * `{{ s | statusLabel }}` : le libellé traduit d'un code de statut, à la place
 * du code brut (IN_PROGRESS) dans les listes déroulantes, cellules et pastilles.
 * `{{ s | statusLabel:'f' }}` accorde au féminin (« NC ouverte »).
 */
@Pipe({ name: 'statusLabel', standalone: false })
export class StatusLabelPipe implements PipeTransform {
  transform(code: string | null | undefined, gender: StatusGender = 'm'): string {
    return statusLabel(code, gender);
  }
}
