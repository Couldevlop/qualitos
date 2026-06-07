import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse, PdcaPhase, PdcaStatus, PdcaStepResponse } from '../../pdca.types';
import { PdcaStepDialogComponent, PdcaStepDialogData } from '../pdca-step-dialog/pdca-step-dialog.component';

// OWASP A03 — defense-in-depth: the backend re-validates the UUID, but we
// also refuse malformed route params client-side to avoid any chance of
// open-redirect / path-traversal-style abuse if the value ever lands in
// URLs we construct from it later.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-pdca-detail',
  templateUrl: './pdca-detail.component.html',
  styleUrls: ['./pdca-detail.component.scss'],
  standalone: false
})
export class PdcaDetailComponent implements OnInit {

  readonly stepColumns = ['phase', 'title', 'status', 'dueDate', 'updatedAt'];

  cycle$!: Observable<PdcaCycleResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);

  private cycleId = '';
  private readonly reload$ = new Subject<void>();

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly pdca: PdcaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/pdca']);
      return;
    }
    this.cycleId = raw;
    this.cycle$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.pdca.getCycle(this.cycleId).pipe(
        catchError(err => {
          // OWASP A09 — do not echo backend error.detail to the UI: it can
          // disclose stack traces, internal class names, or DB hints. Log
          // technical info to console for ops only.
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] getCycle failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@pdca.detail.not-found:Cycle introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.reload$.next();
  }


  goBack(): void {
    this.router.navigate(['/pdca']);
  }

  openAddStep(currentPhase: PdcaStatus): void {
    const defaultPhase: PdcaPhase | undefined =
      currentPhase === 'PLAN' || currentPhase === 'DO'
        || currentPhase === 'CHECK' || currentPhase === 'ACT'
        ? currentPhase
        : undefined;
    const data: PdcaStepDialogData = { cycleId: this.cycleId, defaultPhase };
    this.dialog
      .open(PdcaStepDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(step => {
        if (step) {
          this.reload$.next();
        }
      });
  }

  advance(currentStatus: PdcaStatus): void {
    if (this.acting$.value) return;
    if (currentStatus === 'COMPLETED' || currentStatus === 'CANCELLED') return;
    this.acting$.next(true);
    this.pdca
      .advanceCycle(this.cycleId)
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@pdca.detail.advanced:Cycle avancé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] advance failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@pdca.detail.advance-error:Erreur lors de l'avancement.`),
            'OK', { duration: 4000 });
        }
      });
  }

  cancel(): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    this.pdca
      .cancelCycle(this.cycleId)
      .pipe(finalize(() => this.acting$.next(false)))
      .subscribe({
        next: () => {
          this.snack.open($localize`:@@pdca.detail.cancelled:Cycle annulé.`, $localize`:@@common.ok:OK`, { duration: 2500 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] cancel failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, $localize`:@@pdca.detail.cancel-error:Erreur lors de l'annulation.`),
            'OK', { duration: 4000 });
        }
      });
  }

  isTerminal(status: PdcaStatus): boolean {
    return status === 'COMPLETED' || status === 'CANCELLED';
  }

  statusBadge(status: PdcaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  stepStatusBadge(status: PdcaStepResponse['status']): string {
    return 'badge badge-' + status.toLowerCase();
  }

  phaseColor(phase: PdcaPhase): string {
    return 'phase phase-' + phase.toLowerCase();
  }
}
