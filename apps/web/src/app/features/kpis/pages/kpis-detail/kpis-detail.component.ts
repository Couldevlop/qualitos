import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, forkJoin, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { KpisService } from '../../kpis.service';
import {
  KpiCurrentStatus,
  KpiDirection,
  KpiHealth,
  KpiResponse,
  KpiStatus,
  KpiTrend,
  MeasurementResponse
} from '../../kpis.types';
import { KpisDialogComponent } from '../kpis-dialog/kpis-dialog.component';
import { KpisMeasurementDialogComponent } from '../kpis-measurement-dialog/kpis-measurement-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-kpis-detail',
  templateUrl: './kpis-detail.component.html',
  styleUrls: ['./kpis-detail.component.scss'],
  standalone: false
})
export class KpisDetailComponent implements OnInit {

  readonly measurementColumns = ['periodEnd', 'value', 'unit', 'source', 'health', 'actions'];

  kpi$!: Observable<KpiResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  status: KpiCurrentStatus | null = null;
  trend: KpiTrend | null = null;
  measurements: MeasurementResponse[] = [];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private kpiId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: KpisService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.kpi$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID/mock-id regex before backend call
        if (!UUID_REGEX.test(id) && !id.startsWith('kpi-')) {
          this.error$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loading$.next(false);
          return of(null);
        }
        this.kpiId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            kpi:     this.svc.get(id).pipe(catchError(err => {
              this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            })),
            status:  this.svc.currentStatus(id).pipe(catchError(() => of(null))),
            trend:   this.svc.trend(id).pipe(catchError(() => of(null))),
            mesPage: this.svc.listMeasurements(id).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            })))
          })),
          tap(({ status, trend, mesPage }) => {
            this.loading$.next(false);
            this.status = status;
            this.trend = trend;
            this.measurements = mesPage.content;
          }),
          switchMap(({ kpi }) => of(kpi))
        );
      })
    );
  }

  openEdit(k: KpiResponse): void {
    const ref = this.dialog.open(KpisDialogComponent, {
      data: { kpi: k }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  openRecord(k: KpiResponse): void {
    const ref = this.dialog.open(KpisMeasurementDialogComponent, {
      data: { kpiId: k.id, defaultUnit: k.unit },
      panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(k: KpiResponse): void {
    this.svc.activate(k.id).subscribe({
      next: () => { this.snack.open($localize`:@@kpis.detail.activated:KPI activé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@kpis.detail.activate-failed:Activation impossible.`)
    });
  }
  reopen(k: KpiResponse): void {
    this.svc.reopen(k.id).subscribe({
      next: () => { this.snack.open($localize`:@@kpis.detail.reopened:KPI rouvert (DRAFT).`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@kpis.detail.reopen-failed:Réouverture impossible.`)
    });
  }
  archive(k: KpiResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@kpis.detail.archive-title:Archiver le KPI ?`,
        message: '« ' + k.name + ' » sera marqué ARCHIVED. Les mesures restent consultables.',
        confirmLabel: $localize`:@@kpis.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(k.id).subscribe({
        next: () => { this.snack.open($localize`:@@kpis.detail.archived:KPI archivé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@kpis.detail.archive-failed:Archivage impossible.`)
      });
    });
  }
  remove(k: KpiResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@kpis.detail.delete-title:Supprimer le KPI ?`,
        message: 'Suppression définitive de « ' + k.name + ' » et de ses ' + this.measurements.length + ' mesure(s).',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(k.id).subscribe({
        next: () => { this.snack.open($localize`:@@kpis.detail.deleted:KPI supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/kpis']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  removeMeasurement(k: KpiResponse, m: MeasurementResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@kpis.detail.delete-measurement-title:Supprimer cette mesure ?`,
        message: 'La mesure de ' + m.value + ' ' + (m.unit ?? '') + ' sera retirée de la tendance.',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteMeasurement(k.id, m.id).subscribe({
        next: () => { this.measurements = this.measurements.filter(x => x.id !== m.id); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  // ---- Sparkline geometry ----

  sparklinePath(): string {
    const pts = this.trend?.points ?? [];
    if (pts.length < 2) return '';
    const values = pts.map(p => p.value);
    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;
    const w = 100, h = 30;
    return pts.map((p, i) => {
      const x = (i / (pts.length - 1)) * w;
      const y = h - ((p.value - min) / range) * h;
      return (i === 0 ? 'M' : 'L') + x.toFixed(2) + ',' + y.toFixed(2);
    }).join(' ');
  }

  sparklineFill(): string {
    const base = this.sparklinePath();
    if (!base) return '';
    return base + ' L100,30 L0,30 Z';
  }

  directionLabel(d: KpiDirection): string {
    return d === 'HIGHER_IS_BETTER'
      ? $localize`:@@kpis.detail.dir-higher:↑ Plus haut = mieux`
      : $localize`:@@kpis.detail.dir-lower:↓ Plus bas = mieux`;
  }
  statusBadge(s: KpiStatus): string { return 'badge badge-' + s.toLowerCase(); }
  healthBadge(h: KpiHealth): string { return 'health health-' + h.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[kpis-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
