import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

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
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);
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
      this.snack.open('Identifiant invalide.', 'OK', { duration: 3000 });
      this.router.navigate(['/pdca']);
      return;
    }
    this.cycleId = raw;
    this.cycle$ = this.reload$.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(() => this.pdca.getCycle(this.cycleId).pipe(
        catchError(err => {
          // OWASP A09 — do not echo backend error.detail to the UI: it can
          // disclose stack traces, internal class names, or DB hints. Log
          // technical info to console for ops only.
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] getCycle failed', err?.status, err?.error?.title);
          this.error$.next(safeErrorMessage(err, 'Cycle introuvable.'));
          return of(null);
        }),
        finalize(() => this.loading$.next(false))
      ))
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
          this.snack.open('Cycle avancé.', 'OK', { duration: 2500 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] advance failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'avancement.'),
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
          this.snack.open('Cycle annulé.', 'OK', { duration: 2500 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[pdca-detail] cancel failed', err?.status, err?.error?.title);
          this.snack.open(safeErrorMessage(err, 'Erreur lors de l\'annulation.'),
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
