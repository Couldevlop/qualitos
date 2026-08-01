import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { CalibrationService } from '../../calibration.service';
import {
  CalibrationOverview, DueState, EquipmentResponse, EquipmentRow, EquipmentStatus
} from '../../calibration.types';
import {
  CalibrationEquipmentDialogComponent
} from '../calibration-equipment-dialog/calibration-equipment-dialog.component';

/** Tuile de synthèse en tête d'écran. */
interface SummaryTile {
  label: string;
  value: number;
  tone: 'neutral' | 'warn' | 'danger';
}

/** Horizon de surveillance des échéances, en jours. */
const HORIZONS = [30, 60, 90];

/**
 * Calibration & équipements — liste (§4.10).
 *
 * L'écran est organisé autour de l'échéance : un équipement dont la calibration est
 * dépassée ou proche remonte en tête de liste et porte un marqueur explicite. Le
 * reste (fabricant, n° de série…) relève du détail, pas de la liste.
 */
@Component({
  selector: 'qos-calibration-list',
  templateUrl: './calibration-list.component.html',
  styleUrls: ['./calibration-list.component.scss'],
  standalone: false
})
export class CalibrationListComponent implements OnInit {

  readonly displayedColumns = ['equipment', 'location', 'status', 'due', 'actions'];
  readonly horizons = HORIZONS;

  /** Chaîne vide = « tous les statuts » (le paramètre n'est alors pas envoyé). */
  status: EquipmentStatus | '' = '';
  horizonDays = 60;
  pageIndex = 0;
  pageSize = 25;

  rows$!: Observable<EquipmentRow[]>;
  tiles$!: Observable<SummaryTile[]>;
  total$!: Observable<number>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: CalibrationService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const overview$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc
        .overview(this.status || null, this.pageIndex, this.pageSize, this.horizonDays)
        .pipe(
          catchError(err => {
            this.errorState$.next(safeErrorMessage(err,
              $localize`:@@calibration.list.load-error:Impossible de charger les équipements.`));
            return [];
          }),
          finalize(() => this.loadingState$.next(false))
        )),
      // refCount:false : trois sections de la page (tuiles, tableau, paginateur)
      // s'abonnent séparément — un abonnement tardif ne doit pas relancer les requêtes.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.rows$ = overview$.pipe(map(o => o.rows));
    this.total$ = overview$.pipe(map(o => o.page.totalElements));
    this.tiles$ = overview$.pipe(map(o => this.toTiles(o)));
  }

  // ---- Filtres et pagination ---------------------------------------------------

  onStatusChange(value: EquipmentStatus | ''): void {
    this.status = value;
    this.pageIndex = 0;
    this.reload$.next();
  }

  onHorizonChange(days: number): void {
    this.horizonDays = days;
    this.reload$.next();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.reload$.next();
  }

  refresh(): void {
    this.reload$.next();
  }

  // ---- Actions ------------------------------------------------------------------

  /**
   * Après création on ouvre directement la fiche : l'étape suivante est toujours la
   * configuration du plan de calibration, qui n'existe pas encore.
   */
  create(): void {
    const ref = this.dialog.open(CalibrationEquipmentDialogComponent, {
      data: { equipment: null },
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe((created?: EquipmentResponse) => {
      if (!created) return;
      this.snack.open(
        $localize`:@@calibration.list.created:Équipement créé.`,
        $localize`:@@common.ok:OK`, { duration: 2500 });
      this.router.navigate(['/calibration', created.id]);
    });
  }

  // ---- Présentation --------------------------------------------------------------

  trackById(_index: number, row: EquipmentRow): string {
    return row.equipment.id;
  }

  dueTone(due: DueState): 'neutral' | 'warn' | 'danger' {
    if (due === 'OVERDUE') return 'danger';
    if (due === 'DUE_SOON') return 'warn';
    return 'neutral';
  }

  dueLabel(due: DueState): string {
    if (due === 'OVERDUE') return $localize`:@@calibration.due.overdue:En retard`;
    if (due === 'DUE_SOON') return $localize`:@@calibration.due.soon:Échéance proche`;
    return $localize`:@@calibration.due.ok:À jour`;
  }

  statusLabel(status: EquipmentStatus): string {
    return ({
      ACTIVE: $localize`:@@calibration.status.active:En service`,
      OUT_OF_SERVICE: $localize`:@@calibration.status.out-of-service:Hors service`,
      RETIRED: $localize`:@@calibration.status.retired:Réformé`
    })[status];
  }

  statusTone(status: EquipmentStatus): 'neutral' | 'success' | 'warn' {
    if (status === 'ACTIVE') return 'success';
    if (status === 'OUT_OF_SERVICE') return 'warn';
    return 'neutral';
  }

  private toTiles(o: CalibrationOverview): SummaryTile[] {
    return [
      {
        label: $localize`:@@calibration.list.tile-total:Équipements`,
        value: o.page.totalElements, tone: 'neutral'
      },
      {
        label: $localize`:@@calibration.list.tile-overdue:Calibrations en retard`,
        value: o.overdueCount, tone: 'danger'
      },
      {
        label: $localize`:@@calibration.list.tile-due-soon:Échéances dans l'horizon`,
        value: o.dueSoonCount, tone: 'warn'
      }
    ];
  }
}
