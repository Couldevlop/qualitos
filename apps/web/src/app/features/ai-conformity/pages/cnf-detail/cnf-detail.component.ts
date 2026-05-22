import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiConformityService } from '../../ai-conformity.service';
import {
  ConformityProcedure,
  ConformityStatus,
  ConformityView
} from '../../ai-conformity.types';
import { CnfCertifyDialogComponent } from '../cnf-certify-dialog/cnf-certify-dialog.component';
import { CnfReasonDialogComponent } from '../cnf-reason-dialog/cnf-reason-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-cnf-detail',
  templateUrl: './cnf-detail.component.html',
  styleUrls: ['./cnf-detail.component.scss'],
  standalone: false
})
export class CnfDetailComponent implements OnInit {

  cnf$!: Observable<ConformityView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: AiConformityService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.cnf$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('ca-')) {
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

  start(c: ConformityView): void {
    this.svc.start(c.id).subscribe({
      next: () => { this.snack.open('Évaluation démarrée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Démarrage impossible.')
    });
  }

  certify(c: ConformityView): void {
    const ref = this.dialog.open(CnfCertifyDialogComponent, {
      data: { conformityId: c.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  markExpired(c: ConformityView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Marquer comme expiré ?', message: 'Le certificat sera marqué EXPIRED. Vérifiez avant de continuer.',
              confirmLabel: 'Marquer EXPIRED', cancelLabel: 'Annuler', danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.markExpired(c.id).subscribe({
        next: () => { this.snack.open('Certificat expiré.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Opération impossible.')
      });
    });
  }

  revoke(c: ConformityView): void {
    this.dialog.open(CnfReasonDialogComponent, {
      data: { conformityId: c.id, mode: 'REVOKE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  failed(c: ConformityView): void {
    this.dialog.open(CnfReasonDialogComponent, {
      data: { conformityId: c.id, mode: 'FAIL' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  remove(c: ConformityView): void {
    if (c.status !== 'PLANNED') {
      this.snack.open('Seul un PLANNED peut être supprimé.', 'OK', { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Supprimer la planification ?', message: 'Suppression définitive.',
              confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(c.id).subscribe({
        next: () => { this.snack.open('Planification supprimée.', 'OK', { duration: 2200 }); this.router.navigate(['/ai-conformity']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  procedureLabel(p: ConformityProcedure): string {
    return p === 'NOTIFIED_BODY' ? 'Organisme notifié (Annexe VII)' : 'Contrôle interne (Annexe VI)';
  }
  procedureBadge(p: ConformityProcedure): string { return 'proc proc-' + p.toLowerCase(); }
  statusBadge(s: ConformityStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[cnf-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
