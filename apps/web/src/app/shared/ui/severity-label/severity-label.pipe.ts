import { Pipe, PipeTransform } from '@angular/core';

import { ncCategoryLabel, severityLabel } from './severity-labels';

/** `{{ s | severityLabel }}` : le libellé traduit d'une gravité, à la place du code brut (HIGH). */
@Pipe({ name: 'severityLabel', standalone: false })
export class SeverityLabelPipe implements PipeTransform {
  transform(code: string | null | undefined): string {
    return severityLabel(code);
  }
}

/** `{{ c | ncCategoryLabel }}` : le libellé traduit d'une catégorie de NC (DOCUMENTATION…). */
@Pipe({ name: 'ncCategoryLabel', standalone: false })
export class NcCategoryLabelPipe implements PipeTransform {
  transform(code: string | null | undefined): string {
    return ncCategoryLabel(code);
  }
}
