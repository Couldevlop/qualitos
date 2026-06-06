import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, forkJoin } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
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
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

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
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('fmea-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        this.projectId = id;
        return this.refresh$.pipe(
          switchMap(() => forkJoin({
            project: this.svc.get(id).pipe(catchError(err => {
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            })),
            items: this.svc.listItems(id).pipe(catchError(() => of({
              content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
            }))),
            stats: this.svc.statistics(id).pipe(catchError(() => of(null)))
          })),
          tap(({ project, items, stats }) => {
            this.loadingState$.next(false);
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
      }),
      shareReplay({ bufferSize: 1, refCount: true })
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
      next: () => { this.snack.open($localize`:@@fmea.detail.activated:Projet activé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@fmea.detail.activate-failed:Activation impossible.`)
    });
  }

  reopen(p: FmeaProjectResponse): void {
    this.svc.reopen(p.id).subscribe({
      next: () => { this.snack.open($localize`:@@fmea.detail.reopened:Projet repassé en DRAFT.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@fmea.detail.reopen-failed:Réouverture impossible.`)
    });
  }

  archive(p: FmeaProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@fmea.detail.archive-title:Archiver le projet ?`,
        message: $localize`:@@fmea.detail.archive-message:« ${p.name}:name: » sera marqué ARCHIVED. Les items resteront consultables.`,
        confirmLabel: $localize`:@@fmea.detail.archive:Archiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.archive(p.id).subscribe({
        next: () => { this.snack.open($localize`:@@fmea.detail.archived:Projet archivé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@fmea.detail.archive-failed:Archivage impossible.`)
      });
    });
  }

  remove(p: FmeaProjectResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@fmea.detail.delete-title:Supprimer le projet ?`,
        message: $localize`:@@fmea.detail.delete-message:Suppression définitive de « ${p.name}:name: » et de tous ses items.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(p.id).subscribe({
        next: () => {
          this.snack.open($localize`:@@fmea.detail.deleted:Projet supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 });
          this.router.navigate(['/fmea']);
        },
        error: err => this.fail(err, $localize`:@@fmea.detail.delete-failed:Suppression impossible.`)
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
        title: $localize`:@@fmea.detail.delete-item-title:Supprimer l'item ?`,
        message: $localize`:@@fmea.detail.delete-item-message:Item #${item.sequenceNo}:seq: (RPN = ${item.rpn}:rpn:) sera supprimé.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deleteItem(p.id, item.id).subscribe({
        next: () => { this.items = this.items.filter(x => x.id !== item.id); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@fmea.detail.delete-item-failed:Suppression impossible.`)
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
      PROCESS_FMEA: $localize`:@@fmea.type.process:Processus`,
      DESIGN_FMEA: $localize`:@@fmea.type.design:Conception`,
      SYSTEM_FMEA: $localize`:@@fmea.type.system:Système`,
      SERVICE_FMEA: $localize`:@@fmea.type.service:Service`,
      BOW_TIE: $localize`:@@fmea.type.bow-tie:Bow-tie`
    })[t];
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[fmea-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
