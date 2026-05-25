import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachSeverity, BreachStatus, BreachView } from '../../breach.types';
import { BreachContainDialogComponent } from '../breach-contain-dialog/breach-contain-dialog.component';
import { BreachDpaDialogComponent } from '../breach-dpa-dialog/breach-dpa-dialog.component';
import { BreachSeverityDialogComponent } from '../breach-severity-dialog/breach-severity-dialog.component';
import { BreachSubjectsDialogComponent } from '../breach-subjects-dialog/breach-subjects-dialog.component';
import { BreachTextDialogComponent } from '../breach-text-dialog/breach-text-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-breach-detail',
  templateUrl: './breach-detail.component.html',
  styleUrls: ['./breach-detail.component.scss'],
  standalone: false
})
export class BreachDetailComponent implements OnInit {

  row$!: Observable<BreachView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly svc: BreachService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.row$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('br-')) {
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

  startAssessment(b: BreachView): void {
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.svc.startAssessment(b.id, { handledByUserId: userId }).subscribe({
      next: () => { this.snack.open('Évaluation démarrée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Démarrage impossible.')
    });
  }

  contain(b: BreachView): void {
    this.dialog.open(BreachContainDialogComponent, {
      data: { id: b.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  notifyDpa(b: BreachView): void {
    this.dialog.open(BreachDpaDialogComponent, {
      data: { id: b.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  notifySubjects(b: BreachView): void {
    this.dialog.open(BreachSubjectsDialogComponent, {
      data: { id: b.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  close(b: BreachView): void {
    this.dialog.open(BreachTextDialogComponent, {
      data: { id: b.id, mode: 'CLOSE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  reject(b: BreachView): void {
    this.dialog.open(BreachTextDialogComponent, {
      data: { id: b.id, mode: 'REJECT' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  changeSeverity(b: BreachView): void {
    this.dialog.open(BreachSeverityDialogComponent, {
      data: { id: b.id, current: b.severity }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  statusBadge(s: BreachStatus): string     { return 'badge badge-' + s.toLowerCase(); }
  severityBadge(s: BreachSeverity): string { return 'sev sev-' + s.toLowerCase(); }

  canStart(s: BreachStatus): boolean   { return s === 'DETECTED'; }
  canContain(s: BreachStatus): boolean { return s === 'ASSESSING'; }
  canClose(s: BreachStatus): boolean   { return s === 'CONTAINED'; }
  canReject(s: BreachStatus): boolean  { return s === 'DETECTED' || s === 'ASSESSING'; }
  canSev(s: BreachStatus): boolean     { return s !== 'CLOSED' && s !== 'REJECTED'; }
  canNotifyDpa(b: BreachView): boolean { return !b.dpaNotifiedAt && b.status !== 'CLOSED' && b.status !== 'REJECTED'; }
  canNotifySubjects(b: BreachView): boolean {
    return b.subjectNotificationRequired && !b.subjectsNotifiedAt
        && b.status !== 'CLOSED' && b.status !== 'REJECTED';
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[breach-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
