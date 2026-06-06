import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { Nis2MeasuresService } from '../../nis2m.service';
import {
  CATEGORY_LABEL,
  Nis2MeasureCategory,
  Nis2MeasureStatus,
  Nis2MeasureView,
  ResidualRiskRating
} from '../../nis2m.types';
import { Nis2mEditDialogComponent } from '../nis2m-edit-dialog/nis2m-edit-dialog.component';
import { Nis2mReviewDialogComponent } from '../nis2m-review-dialog/nis2m-review-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-nis2m-detail',
  templateUrl: './nis2m-detail.component.html',
  styleUrls: ['./nis2m-detail.component.scss'],
  standalone: false
})
export class Nis2mDetailComponent implements OnInit {

  row$!: Observable<Nis2MeasureView | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  readonly categoryLabel = CATEGORY_LABEL;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: Nis2MeasuresService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.row$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('nis2m-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loadingState$.next(false))
          ))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  edit(m: Nis2MeasureView): void {
    this.dialog.open(Nis2mEditDialogComponent, {
      data: { row: m }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  start(m: Nis2MeasureView): void {
    this.svc.startImplementation(m.id).subscribe({
      next: () => { this.snack.open($localize`:@@nis2-measures.detail.started:Implémentation démarrée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@nis2-measures.detail.start-failed:Démarrage impossible.`)
    });
  }

  markImplemented(m: Nis2MeasureView): void {
    this.svc.markImplemented(m.id).subscribe({
      next: () => { this.snack.open($localize`:@@nis2-measures.detail.marked-implemented:Marquée comme implémentée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@nis2-measures.detail.mark-failed:Marquage impossible.`)
    });
  }

  verify(m: Nis2MeasureView): void {
    this.dialog.open(Nis2mReviewDialogComponent, {
      data: { id: m.id, mode: 'VERIFY' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  review(m: Nis2MeasureView): void {
    this.dialog.open(Nis2mReviewDialogComponent, {
      data: { id: m.id, mode: 'REVIEW' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  deprecate(m: Nis2MeasureView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@nis2-measures.detail.deprecate-title:Désactiver la mesure ?`,
              message: $localize`:@@nis2-measures.detail.deprecate-message:La mesure passe en DEPRECATED. Cette action est terminale.`,
              confirmLabel: $localize`:@@nis2-measures.detail.deprecate:Désactiver`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.deprecate(m.id).subscribe({
        next: () => { this.snack.open($localize`:@@nis2-measures.detail.deprecated:Mesure désactivée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@nis2-measures.detail.deprecate-failed:Désactivation impossible.`)
      });
    });
  }

  remove(m: Nis2MeasureView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@nis2-measures.detail.delete-title:Supprimer la mesure ?`, message: $localize`:@@nis2-measures.detail.delete-message:Suppression définitive.`,
              confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(m.id).subscribe({
        next: () => { this.snack.open($localize`:@@nis2-measures.detail.deleted:Mesure supprimée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/nis2-measures']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  catLabel(c: Nis2MeasureCategory): string  { return this.categoryLabel[c]; }
  statusBadge(s: Nis2MeasureStatus): string { return 'badge badge-' + s.toLowerCase(); }
  riskBadge(r: ResidualRiskRating): string  { return 'risk risk-' + r.toLowerCase(); }

  canEdit(s: Nis2MeasureStatus): boolean      { return s !== 'DEPRECATED'; }
  canStart(s: Nis2MeasureStatus): boolean     { return s === 'PLANNED'; }
  canImplement(s: Nis2MeasureStatus): boolean { return s === 'IN_PROGRESS'; }
  canVerify(s: Nis2MeasureStatus): boolean    { return s === 'IMPLEMENTED'; }
  canReview(s: Nis2MeasureStatus): boolean    { return s === 'VERIFIED'; }
  canDeprecate(s: Nis2MeasureStatus): boolean { return s !== 'DEPRECATED'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[nis2m-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
