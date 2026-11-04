import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

import { AuthzService } from '../../../../core/authz/authz.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { StakeholdersService } from '../../stakeholders.service';
import {
  StakeholderCategory,
  StakeholderRow,
  StakeholderSheet,
  StakeholderSide,
  StakeholderStatistics
} from '../../stakeholders.types';
import {
  StakeholderCreateDialogComponent,
  StakeholderCreated
} from '../stakeholder-create-dialog/stakeholder-create-dialog.component';

/** Les trois vues du module, dans l'ordre où on les traverse. */
export type StakeholderTab = 'register' | 'matrix' | 'assistant';

/**
 * Le module « Parties prenantes » (ISO 9001 §4.2), en trois vues : le
 * **registre**, la **matrice d'évaluation**, l'**assistant IA**.
 *
 * <p>Un seul écran et trois onglets, et non trois routes : le registre, la
 * matrice et l'assistant parlent des MÊMES fiches. Les séparer en trois pages
 * obligerait à recharger la liste trois fois et laisserait trois états divergents
 * à l'écran.
 *
 * <p>La fiche s'ouvre dans un **tiroir** par-dessus la liste, pas dans une autre
 * page : on ouvre une partie intéressée pour la lire ou la corriger, puis on
 * revient à la liste. Une navigation ferait perdre le filtre et la position.
 */
@Component({
  selector: 'qos-stakeholders-page',
  templateUrl: './stakeholders-page.component.html',
  styleUrls: ['./stakeholders-page.component.scss'],
  standalone: false
})
export class StakeholdersPageComponent implements OnInit {

  tab: StakeholderTab = 'register';

  rows: StakeholderRow[] = [];
  statistics: StakeholderStatistics | null = null;
  /** La fiche ouverte dans le tiroir, ou null quand il est fermé. */
  openSheet: StakeholderSheet | null = null;

  side: StakeholderSide | '' = '';
  search = '';

  loading = false;
  failed = false;
  canManage = false;

  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: StakeholdersService,
    private readonly authz: AuthzService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.authz.can('stakeholder.manage').subscribe(ok => (this.canManage = ok));
    this.load();
  }

  /** Recharge le registre et ses tuiles : les deux parlent du même périmètre. */
  load(): void {
    this.loading = true;
    this.failed = false;
    this.svc.list(this.side || undefined, this.search).pipe(
      catchError(err => {
        this.failed = true;
        this.fail(err, $localize`:@@stakeholders.load-failed:Le registre des parties prenantes n'a pas pu être chargé.`);
        return of([] as StakeholderRow[]);
      }),
      finalize(() => (this.loading = false))
    ).subscribe(rows => {
      this.rows = rows;
      this.loading = false;
    });
    // Les tuiles comptent TOUT le registre, filtres compris ou non : un total
    // filtré par lui-même ne dirait plus rien.
    this.svc.statistics().pipe(catchError(() => of(null)))
      .subscribe(stats => (this.statistics = stats));
  }

  onFilters(filters: { side: StakeholderSide | ''; search: string }): void {
    this.side = filters.side;
    this.search = filters.search;
    this.load();
  }

  /** Inscrire : trois champs, puis la fiche s'ouvre pour l'analyser. */
  create(): void {
    this.dialog.open<StakeholderCreateDialogComponent, void, StakeholderCreated>(
      StakeholderCreateDialogComponent,
      { autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel' }
    ).afterClosed().subscribe(created => {
      if (!created) return;
      this.load();
      // On enchaîne sur la fiche : inscrire sans analyser laisse une ligne vide
      // au registre, et c'est précisément ce que l'auditeur relève.
      this.open(created.sheet.id);
    });
  }

  open(id: string): void {
    this.svc.get(id).pipe(catchError(err => {
      this.fail(err, $localize`:@@stakeholders.sheet-failed:La fiche n'a pas pu être ouverte.`);
      return of(null);
    })).subscribe(sheet => (this.openSheet = sheet));
  }

  closeSheet(reload: boolean): void {
    this.openSheet = null;
    if (reload) this.load();
  }

  /** La fiche vient d'être enregistrée : la liste suit, le tiroir reste ouvert. */
  onSheetSaved(sheet: StakeholderSheet): void {
    this.openSheet = sheet;
    this.load();
  }

  /** La matrice a coté : le registre reprend les lignes qu'elle rend. */
  onRated(rows: StakeholderRow[]): void {
    this.rows = rows;
    this.svc.statistics().pipe(catchError(() => of(null)))
      .subscribe(stats => (this.statistics = stats));
  }

  /** L'assistant a fait inscrire une partie intéressée : le registre la montre. */
  onSuggestionAccepted(): void {
    this.load();
  }

  private fail(err: unknown, fallback: string): void {
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 5000 });
  }
}
