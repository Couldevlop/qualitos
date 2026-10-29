import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';

/**
 * Libellés traduits du paginateur Material, fournis à toute l'application.
 *
 * <p>Sans cette classe, chaque liste affichait le paginateur en anglais
 * (« Items per page: », « 1 – 7 of 7 ») quelle que soit la langue du build.
 * Les traductions vivent dans `scripts/i18n/design_audit.py`.
 */
@Injectable()
export class QosPaginatorIntl extends MatPaginatorIntl {
  override itemsPerPageLabel = $localize`:@@paginator.items-per-page:Éléments par page :`;
  override nextPageLabel = $localize`:@@paginator.next:Page suivante`;
  override previousPageLabel = $localize`:@@paginator.previous:Page précédente`;
  override firstPageLabel = $localize`:@@paginator.first:Première page`;
  override lastPageLabel = $localize`:@@paginator.last:Dernière page`;

  override getRangeLabel = (page: number, pageSize: number, length: number): string => {
    const total = Math.max(length, 0);
    if (total === 0 || pageSize === 0) {
      return $localize`:@@paginator.range-empty:0 sur ${total}:total:`;
    }
    const start = page * pageSize;
    // Une page au-delà de la fin (liste raccourcie entre deux chargements) se
    // borne au total au lieu d'annoncer « 21 – 20 ».
    const end = Math.min(start + pageSize, total);
    const first = Math.min(start + 1, total);
    return $localize`:@@paginator.range:${first}:start: – ${end}:end: sur ${total}:total:`;
  };
}
