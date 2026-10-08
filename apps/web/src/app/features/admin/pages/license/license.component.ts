import { Component, OnInit } from '@angular/core';
import { combineLatest, Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

import { EditionService } from '../../../../core/edition/edition.service';
import { EditionView, LicenseStatus } from '../../../../core/edition/edition.types';
import { ModuleCatalogEntry } from '../../admin.types';
import { TenantModulesService } from '../../tenant-modules.service';

/** Ce que la page montre : la licence, et le nom des modules qu'elle ouvre. */
export interface LicensePage {
  view: EditionView;
  allModules: boolean;
  modules: string[];
}

/**
 * La licence de l'installation on-premise (ADR 0082) : pour qui, jusqu'à quand,
 * quels modules, combien d'utilisateurs — et comment la renouveler.
 */
@Component({
  selector: 'qos-license',
  templateUrl: './license.component.html',
  styleUrls: ['./license.component.scss'],
  standalone: false
})
export class LicenseComponent implements OnInit {

  page$!: Observable<LicensePage>;

  constructor(private readonly edition: EditionService, private readonly modules: TenantModulesService) {}

  ngOnInit(): void {
    const catalogue$ = this.modules.catalog().pipe(catchError(() => of<ModuleCatalogEntry[]>([])));
    this.page$ = combineLatest([this.edition.current(), catalogue$]).pipe(
      map(([view, catalogue]) => {
        const noms = new Map(catalogue.map(e => [e.code, e.name]));
        return {
          view,
          allModules: view.modules.includes('*'),
          modules: view.modules.filter(m => m !== '*').map(m => noms.get(m) ?? m).sort((a, b) => a.localeCompare(b))
        };
      })
    );
  }

  statusText(s: LicenseStatus): string {
    switch (s) {
      case 'VALID': return $localize`:@@license.status.valid:Valide`;
      case 'GRACE': return $localize`:@@license.status.grace:Échue — délai de grâce`;
      case 'EXPIRED': return $localize`:@@license.status.expired:Échue — lecture seule`;
      case 'NOT_YET_VALID': return $localize`:@@license.status.not-yet:Pas encore en vigueur`;
      case 'INVALID': return $localize`:@@license.status.invalid:Non vérifiée`;
      case 'MISSING': return $localize`:@@license.status.missing:Absente`;
      default: return $localize`:@@license.status.not-required:Sans objet`;
    }
  }

  tone(s: LicenseStatus): 'ok' | 'warning' | 'blocked' {
    if (s === 'VALID' || s === 'NOT_REQUIRED') return 'ok';
    return s === 'GRACE' ? 'warning' : 'blocked';
  }

  /** La part des places occupées, de 0 à 100, pour la jauge des utilisateurs. */
  usage(v: EditionView): number {
    if (!v.maxUsers || v.activeUsers === null) return 0;
    return Math.min(100, Math.round(100 * v.activeUsers / v.maxUsers));
  }
}
