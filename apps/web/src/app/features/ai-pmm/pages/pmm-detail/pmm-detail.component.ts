import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PmmService } from '../../pmm.service';
import { FREQUENCY_LABEL, PmmPlanStatus, PmmPlanView, PmmReviewFrequency } from '../../pmm.types';
import { PmmEditDialogComponent } from '../pmm-edit-dialog/pmm-edit-dialog.component';
import { PmmReasonDialogComponent } from '../pmm-reason-dialog/pmm-reason-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-pmm-detail',
  templateUrl: './pmm-detail.component.html',
  styleUrls: ['./pmm-detail.component.scss'],
  standalone: false
})
export class PmmDetailComponent implements OnInit {

  row$!: Observable<PmmPlanView | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  readonly freqLabel = FREQUENCY_LABEL;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: PmmService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.row$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('pmm-')) {
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

  edit(p: PmmPlanView): void {
    this.dialog.open(PmmEditDialogComponent, {
      data: { row: p }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  activate(p: PmmPlanView): void {
    this.svc.activate(p.id).subscribe({
      next: () => { this.snack.open($localize`:@@ai-pmm.detail.activated:Plan activé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, $localize`:@@ai-pmm.detail.activate-failed:Activation impossible.`)
    });
  }

  recordReview(p: PmmPlanView): void {
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open($localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@ai-pmm.detail.record-review-title:Enregistrer une revue ?`,
              message: $localize`:@@ai-pmm.detail.record-review-message:La date de revue sera horodatée maintenant. La prochaine échéance est recalculée.`,
              confirmLabel: $localize`:@@common.save:Enregistrer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.recordReview(p.id, { reviewedByUserId: userId }).subscribe({
        next: () => { this.snack.open($localize`:@@ai-pmm.detail.review-recorded:Revue enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@ai-pmm.detail.review-failed:Enregistrement impossible.`)
      });
    });
  }

  suspend(p: PmmPlanView): void {
    this.dialog.open(PmmReasonDialogComponent, {
      data: { id: p.id, mode: 'SUSPEND' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  close(p: PmmPlanView): void {
    this.dialog.open(PmmReasonDialogComponent, {
      data: { id: p.id, mode: 'CLOSE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  remove(p: PmmPlanView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: $localize`:@@ai-pmm.detail.delete-title:Supprimer le plan PMM ?`, message: $localize`:@@ai-pmm.detail.delete-message:Suppression définitive.`,
              confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(p.id).subscribe({
        next: () => { this.snack.open($localize`:@@ai-pmm.detail.deleted:Plan supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/ai-pmm']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  statusBadge(s: PmmPlanStatus): string  { return 'badge badge-' + s.toLowerCase(); }
  freqOf(f: PmmReviewFrequency): string  { return this.freqLabel[f]; }

  isOverdue(p: PmmPlanView): boolean {
    return p.status === 'ACTIVE' && !!p.nextReviewDueAt && new Date(p.nextReviewDueAt).getTime() < Date.now();
  }

  canEdit(p: PmmPlanView): boolean     { return p.status !== 'CLOSED'; }
  canActivate(p: PmmPlanView): boolean { return p.status === 'DRAFT' || p.status === 'SUSPENDED'; }
  canReview(p: PmmPlanView): boolean   { return p.status === 'ACTIVE'; }
  canSuspend(p: PmmPlanView): boolean  { return p.status === 'ACTIVE'; }
  canClose(p: PmmPlanView): boolean    { return p.status !== 'CLOSED'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[pmm-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
