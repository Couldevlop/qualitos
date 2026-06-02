import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiSeverity, CyiStatus, CyiType, CyiView, TYPE_LABEL } from '../../cyi.types';
import { CyiLinkBreachDialogComponent } from '../cyi-link-breach-dialog/cyi-link-breach-dialog.component';
import { CyiMitigateDialogComponent } from '../cyi-mitigate-dialog/cyi-mitigate-dialog.component';
import { CyiNotificationDialogComponent, CyiNotificationMode } from '../cyi-notification-dialog/cyi-notification-dialog.component';
import { CyiSeverityDialogComponent } from '../cyi-severity-dialog/cyi-severity-dialog.component';
import { CyiTextDialogComponent } from '../cyi-text-dialog/cyi-text-dialog.component';
import { AuthService } from '../../../../core/auth/auth.service';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-cyi-detail',
  templateUrl: './cyi-detail.component.html',
  styleUrls: ['./cyi-detail.component.scss'],
  standalone: false
})
export class CyiDetailComponent implements OnInit {

  row$!: Observable<CyiView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);
  readonly typeLabel = TYPE_LABEL;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly svc: CyberIncidentsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.row$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('cyi-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  startAssessment(i: CyiView): void {
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.svc.startAssessment(i.id, { handledByUserId: userId }).subscribe({
      next: () => { this.snack.open('Évaluation démarrée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Démarrage impossible.')
    });
  }

  mitigate(i: CyiView): void {
    this.dialog.open(CyiMitigateDialogComponent, {
      data: { id: i.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  notify(i: CyiView, mode: CyiNotificationMode): void {
    this.dialog.open(CyiNotificationDialogComponent, {
      data: { id: i.id, mode }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  close(i: CyiView): void {
    this.dialog.open(CyiTextDialogComponent, {
      data: { id: i.id, mode: 'CLOSE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  reject(i: CyiView): void {
    this.dialog.open(CyiTextDialogComponent, {
      data: { id: i.id, mode: 'REJECT' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  changeSeverity(i: CyiView): void {
    this.dialog.open(CyiSeverityDialogComponent, {
      data: { id: i.id, current: i.severity }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  linkBreach(i: CyiView): void {
    this.dialog.open(CyiLinkBreachDialogComponent, {
      data: { id: i.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  statusBadge(s: CyiStatus): string   { return 'badge badge-' + s.toLowerCase(); }
  severityBadge(s: CyiSeverity): string { return 'sev sev-' + s.toLowerCase(); }
  typeOf(t: CyiType): string { return this.typeLabel[t]; }

  canStart(s: CyiStatus): boolean        { return s === 'DETECTED'; }
  canMitigate(s: CyiStatus): boolean     { return s === 'ASSESSING'; }
  canClose(s: CyiStatus): boolean        { return s === 'MITIGATED'; }
  canReject(s: CyiStatus): boolean       { return s === 'DETECTED' || s === 'ASSESSING'; }
  canChangeSev(s: CyiStatus): boolean    { return s !== 'CLOSED' && s !== 'REJECTED'; }
  canNotify(i: CyiView, m: CyiNotificationMode): boolean {
    if (!i.significant || i.status === 'CLOSED' || i.status === 'REJECTED') return false;
    if (m === 'EARLY_WARNING')      return !i.earlyWarningSentAt;
    if (m === 'INITIAL_ASSESSMENT') return !i.initialAssessmentSentAt;
    return !i.finalReportSentAt;
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[cyi-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
