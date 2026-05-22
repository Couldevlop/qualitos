import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FriaService } from '../../fria.service';
import { FriaStatus, FriaView } from '../../fria.types';
import { FriaApproveDialogComponent } from '../fria-approve-dialog/fria-approve-dialog.component';
import { FriaEditDialogComponent } from '../fria-edit-dialog/fria-edit-dialog.component';
import { FriaReasonDialogComponent } from '../fria-reason-dialog/fria-reason-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-fria-detail',
  templateUrl: './fria-detail.component.html',
  styleUrls: ['./fria-detail.component.scss'],
  standalone: false
})
export class FriaDetailComponent implements OnInit {

  row$!: Observable<FriaView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: FriaService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.row$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('fria-')) {
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

  edit(f: FriaView): void {
    this.dialog.open(FriaEditDialogComponent, {
      data: { row: f }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  submit(f: FriaView): void {
    const userId = this.auth.snapshot()?.userId;
    if (!userId) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Soumettre la FRIA pour validation ?',
              message: 'Une fois soumise, la FRIA n\'est plus modifiable tant qu\'un responsable conformité ne l\'a pas renvoyée en brouillon.',
              confirmLabel: 'Soumettre', cancelLabel: 'Annuler', danger: false }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.submit(f.id, { submittedByUserId: userId }).subscribe({
        next: () => { this.snack.open('FRIA soumise.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Soumission impossible.')
      });
    });
  }

  approve(f: FriaView): void {
    this.dialog.open(FriaApproveDialogComponent, {
      data: { id: f.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  returnToDraft(f: FriaView): void {
    this.dialog.open(FriaReasonDialogComponent, {
      data: { id: f.id, mode: 'RETURN' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  archive(f: FriaView): void {
    this.dialog.open(FriaReasonDialogComponent, {
      data: { id: f.id, mode: 'ARCHIVE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  remove(f: FriaView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Supprimer la FRIA ?', message: 'Suppression définitive.',
              confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(f.id).subscribe({
        next: () => { this.snack.open('FRIA supprimée.', 'OK', { duration: 2200 }); this.router.navigate(['/fria']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  statusBadge(s: FriaStatus): string { return 'badge badge-' + s.toLowerCase(); }

  canEdit(s: FriaStatus): boolean    { return s === 'DRAFT'; }
  canSubmit(s: FriaStatus): boolean  { return s === 'DRAFT'; }
  canApprove(s: FriaStatus): boolean { return s === 'SUBMITTED'; }
  canReturn(s: FriaStatus): boolean  { return s === 'SUBMITTED'; }
  canArchive(s: FriaStatus): boolean { return s === 'APPROVED'; }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[fria-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
