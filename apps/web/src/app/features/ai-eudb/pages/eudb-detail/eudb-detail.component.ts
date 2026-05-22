import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { EudbService } from '../../eudb.service';
import { EudbStatus, EudbView } from '../../eudb.types';
import { EudbEditDialogComponent } from '../eudb-edit-dialog/eudb-edit-dialog.component';
import { EudbReasonDialogComponent } from '../eudb-reason-dialog/eudb-reason-dialog.component';
import { EudbRegisterDialogComponent } from '../eudb-register-dialog/eudb-register-dialog.component';
import { EudbUpdateDialogComponent } from '../eudb-update-dialog/eudb-update-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-eudb-detail',
  templateUrl: './eudb-detail.component.html',
  styleUrls: ['./eudb-detail.component.scss'],
  standalone: false
})
export class EudbDetailComponent implements OnInit {

  row$!: Observable<EudbView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: EudbService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.row$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('eudb-')) {
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

  edit(r: EudbView): void {
    this.dialog.open(EudbEditDialogComponent, {
      data: { row: r }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  submit(r: EudbView): void {
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Soumettre l\'enregistrement à EUDB ?',
        message: 'Une fois soumis, le brouillon ne peut plus être modifié librement. Confirmer ?',
        confirmLabel: 'Soumettre', cancelLabel: 'Annuler', danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.submit(r.id, { submittedByUserId: userId }).subscribe({
        next: () => { this.snack.open('Soumis à EUDB.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Soumission impossible.')
      });
    });
  }

  markRegistered(r: EudbView): void {
    this.dialog.open(EudbRegisterDialogComponent, {
      data: { id: r.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  declareUpdate(r: EudbView): void {
    this.dialog.open(EudbUpdateDialogComponent, {
      data: { id: r.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  reject(r: EudbView): void {
    this.dialog.open(EudbReasonDialogComponent, {
      data: { id: r.id, mode: 'REJECT' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  retire(r: EudbView): void {
    this.dialog.open(EudbReasonDialogComponent, {
      data: { id: r.id, mode: 'RETIRE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  remove(r: EudbView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Supprimer l\'enregistrement ?', message: 'Suppression définitive.',
              confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(r.id).subscribe({
        next: () => { this.snack.open('Enregistrement supprimé.', 'OK', { duration: 2200 }); this.router.navigate(['/ai-eudb']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  statusBadge(s: EudbStatus): string { return 'badge badge-' + s.toLowerCase(); }

  canEdit(s: EudbStatus): boolean       { return s === 'DRAFT'; }
  canSubmit(s: EudbStatus): boolean     { return s === 'DRAFT'; }
  canRegister(s: EudbStatus): boolean   { return s === 'SUBMITTED'; }
  canDeclareUpdate(s: EudbStatus): boolean { return s === 'REGISTERED' || s === 'UPDATED'; }
  canReject(s: EudbStatus): boolean     { return s === 'DRAFT' || s === 'SUBMITTED'; }
  canRetire(s: EudbStatus): boolean     { return s === 'REGISTERED' || s === 'UPDATED'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[eudb-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
