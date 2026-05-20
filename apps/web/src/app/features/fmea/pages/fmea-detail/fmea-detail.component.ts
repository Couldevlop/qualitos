import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, forkJoin } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FmeaService } from '../../fmea.service';
import {
  FmeaItemResponse,
  FmeaProjectResponse,
  FmeaProjectStatistics,
  FmeaStatus,
  FmeaType
} from '../../fmea.types';
import { FmeaEditDialogComponent } from '../fmea-edit-dialog/fmea-edit-dialog.component';
import { FmeaItemDialogComponent } from '../fmea-item-dialog/fmea-item-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-fmea-detail',
  templateUrl: './fmea-detail.component.html',
  styleUrls: ['./fmea-detail.component.scss'],
  standalone: false
})
export class FmeaDetailComponent implements OnInit {

  readonly itemColumns = [
    'sequenceNo', 'function', 'failureMode', 'failureEffect',
    'sod', 'rpn', 'sodAfter', 'rpnAfter', 'actions'
  ];

  project$!: Observable<FmeaProjectResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  stats: FmeaProjectStatistics | null = null;
  items: FmeaItemResponse[] = [];

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private projectId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: FmeaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.project$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('fmea-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        this.projectId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            project: this.svc.get(id).pipe(catchError(err => {
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            })),
            items: this.svc.listItems(id).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            }))),
            stats: this.svc.statistics(id).pipe(catchError(() => of(null)))
          })),
          tap(({ project, items, stats }) => {
            this.loading$.next(false);
            this.items = items.content ?? [];
            this.stats = stats;
            if (project && stats) {
              // keep critical flags in sync if items came from a different cache
              this.items = this.items.map(i => ({
                ...i, critical: i.rpn >= stats.criticalRpnThreshold
              }));
            }
          }),
          switchMap(({ project }) => of(project))
        );
      })
    );
  }

  openEdit(p: FmeaProjectResponse): void {
    const ref = this.dialog.open(FmeaEditDialogComponent, {
      data: { project: p }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(p: FmeaProjectResponse): void {
    this.svc.activate(p.id).subscribe({
      next: () => { this.snack.open('Projet activé.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Activation impossible.')
    });
  }

  reopen(p: FmeaProjectResponse): void {
    this.svc.reopen(p.id).subscribe({
      next: () => { this.snack.open('Projet repassé en DRAFT.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Réouverture impossible.')
    });
  }

  archive(p: FmeaProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Archiver le projet ?',
        message: '« ' + p.name + ' » sera marqué ARCHIVED. Les items resteront consultables.',
        confirmLabel: 'Archiver', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(p.id).subscribe({
        next: () => { this.snack.open('Projet archivé.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Archivage impossible.')
      });
    });
  }

  remove(p: FmeaProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer le projet ?',
        message: 'Suppression définitive de « ' + p.name + ' » et de tous ses items.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(p.id).subscribe({
        next: () => {
          this.snack.open('Projet supprimé.', 'OK', { duration: 2200 });
          this.router.navigate(['/fmea']);
        },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  openAddItem(p: FmeaProjectResponse): void {
    const ref = this.dialog.open(FmeaItemDialogComponent, {
      data: { projectId: p.id, criticalRpnThreshold: p.criticalRpnThreshold },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(item => { if (item) this.refresh$.next(); });
  }

  openEditItem(p: FmeaProjectResponse, item: FmeaItemResponse): void {
    const ref = this.dialog.open(FmeaItemDialogComponent, {
      data: { projectId: p.id, item, criticalRpnThreshold: p.criticalRpnThreshold },
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  removeItem(p: FmeaProjectResponse, item: FmeaItemResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer l\'item ?',
        message: 'Item #' + item.sequenceNo + ' (RPN = ' + item.rpn + ') sera supprimé.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteItem(p.id, item.id).subscribe({
        next: () => { this.items = this.items.filter(x => x.id !== item.id); this.refresh$.next(); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  rpnClass(value: number, threshold: number): string {
    if (!value) return 'rpn rpn-na';
    if (value >= threshold) return 'rpn rpn-critical';
    if (value >= threshold * 0.6) return 'rpn rpn-high';
    return 'rpn rpn-ok';
  }

  statusBadge(s: FmeaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  typeBadge(t: FmeaType): string     { return 'tbadge tbadge-' + t.toLowerCase(); }
  typeLabel(t: FmeaType): string {
    return ({
      PROCESS_FMEA: 'Processus', DESIGN_FMEA: 'Conception', SYSTEM_FMEA: 'Système',
      SERVICE_FMEA: 'Service', BOW_TIE: 'Bow-tie'
    })[t];
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[fmea-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
