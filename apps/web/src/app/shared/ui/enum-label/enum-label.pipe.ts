import { Pipe, PipeTransform } from '@angular/core';

import { EnumDomain, enumLabel } from './enum-labels';

/** `{{ d.mode | enumLabel:'ishikawaMode' }}` : le libellé traduit d'une énumération, jamais le code brut. */
@Pipe({ name: 'enumLabel', standalone: false })
export class EnumLabelPipe implements PipeTransform {
  transform(code: string | null | undefined, domain: EnumDomain): string {
    return enumLabel(code, domain);
  }
}

/**
 * `[attr.aria-label]="row.title | openLabel"` : nom accessible d'une ligne ou
 * d'une carte cliquable (« Ouvrir NC-2026-0006 »), traduit d'un seul tenant au
 * lieu d'une concaténation `'Ouvrir ' + …` restée en français dans toutes les
 * langues.
 */
@Pipe({ name: 'openLabel', standalone: false })
export class OpenLabelPipe implements PipeTransform {
  transform(subject: string | null | undefined): string {
    const name = (subject ?? '').trim();
    return name
      ? $localize`:@@common.open-item:Ouvrir ${name}:name:`
      : $localize`:@@common.open:Ouvrir`;
  }
}
