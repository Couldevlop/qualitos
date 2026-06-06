import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { NcService } from '../../nc.service';
import { NcResponse, NcSeverity, NcStatus } from '../../nc.types';
import {
  NcResolveDialogComponent,
  NcResolveDialogData
} from '../nc-resolve-dialog/nc-resolve-dialog.component';

// OWASP A03 — refuse malformed UUIDs client-side. Demo mock ids ("nc-1"…)
// are also accepted so the page stays usable with useMockApi=true.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-nc-detail',
  templateUrl: './nc-detail.component.html',
  styleUrls: ['./nc-detail.component.scss'],
  standalone: false
})
export class NcDetailComponent implements OnInit {

  nc$!: Observable<NcResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);
  acting$ = new BehaviorSubject<boolean>(false);

  private ncId = '';
  private readonly reload$ = new Subject<void>();
  private isMockId(s: string): boolean { return /^nc-/.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: NcService,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/nc']);
      return;
    }
    this.ncId = raw;
    this.nc$ = this.reload$.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(() => this.svc.getNc(this.ncId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[nc-detail] getNc failed', err?.status, err?.error?.title);
          this.error$.next(safeErrorMessage(err, $localize`:@@nc.detail.not-found:Non-conformité introuvable.`));
          return of(null);
        }),
        finalize(() => this.loading$.next(false))
      ))
    );
    this.reload$.next();
  }

  goBack(): void {
    this.router.navigate(['/nc']);
  }

  photoList(photoUrls?: string): string[] {
    if (!photoUrls) return [];
    return photoUrls.split(/\r?\n/).map(s => s.trim()).filter(Boolean);
  }

  startAnalysis(): void { this.transition('start-analysis'); }
  defineAction(): void { this.transition('define-action'); }
  close(): void { this.transition('close'); }

  cancel(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@nc.detail.cancel-confirm-title:Annuler cette non-conformité ?`,
        message: $localize`:@@nc.detail.cancel-confirm-message:La non-conformité sera marquée comme annulée. Cette décision est traçable.`,
        confirmLabel: $localize`:@@common.cancel:Annuler`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (confirmed) this.transition('cancel');
    });
  }

  openResolve(n: NcResponse): void {
    const data: NcResolveDialogData = { ncId: n.id, reference: n.reference };
    this.dialog
      .open(NcResolveDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(resolved => { if (resolved) this.reload$.next(); });
  }

  escalateToCapa(): void {
    if (this.acting$.value) return;
    const ownerId = this.auth.snapshot()?.userId;
    if (!ownerId) {
      this.snack.open(
        $localize`:@@common.session-expired:Session expirée — veuillez vous reconnecter.`,
        $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@nc.detail.escalate-confirm-title:Escalader vers une CAPA ?`,
        message: $localize`:@@nc.detail.escalate-confirm-message:Une action corrective/préventive (CAPA) sera créée et liée à cette non-conformité.`,
        confirmLabel: $localize`:@@nc.detail.escalate:Escalader CAPA`
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.acting$.next(true);
      this.svc.escalateToCapa(this.ncId, { ownerId })
        .pipe(finalize(() => this.acting$.next(false)))
        .subscribe({
          next: () => {
            this.snack.open(
              $localize`:@@nc.detail.escalate-success:CAPA créée et liée à la non-conformité.`,
              $localize`:@@common.ok:OK`, { duration: 2500 });
            this.reload$.next();
          },
          error: err => {
            // eslint-disable-next-line no-console
            console.warn('[nc-detail] escalate failed', err?.status, err?.error?.title);
            this.snack.open(
              safeErrorMessage(err, $localize`:@@nc.detail.escalate-error:Erreur lors de l'escalade.`),
              'OK', { duration: 4000 });
          }
        });
    });
  }

  private transition(action: 'start-analysis' | 'define-action' | 'close' | 'cancel'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start-analysis' ? this.svc.startAnalysis(this.ncId)
      : action === 'define-action' ? this.svc.defineAction(this.ncId)
      : action === 'close' ? this.svc.close(this.ncId)
      : this.svc.cancel(this.ncId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@nc.detail.transition-success:Statut mis à jour.`,
          $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[nc-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@nc.detail.transition-error:Erreur lors de la transition.`),
          'OK', { duration: 4000 });
      }
    });
  }

  isTerminal(status: NcStatus): boolean {
    return status === 'CLOSED' || status === 'CANCELLED';
  }

  canStartAnalysis(status: NcStatus): boolean { return status === 'OPEN'; }
  canDefineAction(status: NcStatus): boolean { return status === 'UNDER_ANALYSIS'; }
  canResolve(status: NcStatus): boolean { return status === 'ACTION_DEFINED'; }
  canClose(status: NcStatus): boolean { return status === 'RESOLVED'; }
  canCancel(status: NcStatus): boolean { return !this.isTerminal(status); }
  canEscalate(status: NcStatus): boolean { return !this.isTerminal(status); }

  statusBadgeClass(status: NcStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  severityBadgeClass(severity: NcSeverity): string {
    return 'sev sev-' + severity.toLowerCase();
  }
}
