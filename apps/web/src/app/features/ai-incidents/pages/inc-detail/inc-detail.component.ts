import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiIncidentsService } from '../../ai-inc.service';
import { AiIncSeverity, AiIncStatus, AiIncView } from '../../ai-inc.types';
import { IncCloseDialogComponent } from '../inc-close-dialog/inc-close-dialog.component';
import { IncNotifyDialogComponent } from '../inc-notify-dialog/inc-notify-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-inc-detail',
  templateUrl: './inc-detail.component.html',
  styleUrls: ['./inc-detail.component.scss'],
  standalone: false
})
export class IncDetailComponent implements OnInit {

  inc$!: Observable<AiIncView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: AiIncidentsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.inc$ = this.route.paramMap.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        if (!UUID_REGEX.test(id) && !id.startsWith('inc-')) {
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

  startInvestigation(i: AiIncView): void {
    const lead = this.auth.snapshot()?.userId;
    if (!lead) {
      this.snack.open('Session expirée — veuillez vous reconnecter.', 'OK', { duration: 4000 });
      return;
    }
    this.svc.startInvestigation(i.id, { investigationLeadUserId: lead }).subscribe({
      next: () => { this.snack.open('Investigation démarrée.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Démarrage impossible.')
    });
  }

  notifyRegulator(i: AiIncView): void {
    this.dialog.open(IncNotifyDialogComponent, {
      data: { incidentId: i.id }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  close(i: AiIncView): void {
    this.dialog.open(IncCloseDialogComponent, {
      data: { incidentId: i.id, mode: 'CLOSE' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  dismiss(i: AiIncView): void {
    this.dialog.open(IncCloseDialogComponent, {
      data: { incidentId: i.id, mode: 'DISMISS' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe(u => { if (u) this.refresh$.next(); });
  }

  remove(i: AiIncView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: { title: 'Supprimer l\'incident ?', message: 'Suppression définitive.',
              confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(i.id).subscribe({
        next: () => { this.snack.open('Incident supprimé.', 'OK', { duration: 2200 }); this.router.navigate(['/ai-incidents']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  severityLabel(s: AiIncSeverity): string {
    return ({
      DEATH_OR_SERIOUS_HARM_TO_HEALTH: 'Décès / atteinte grave santé (2j)',
      SERIOUS_INFRINGEMENT_FUNDAMENTAL_RIGHTS: 'Atteinte droits fondamentaux (10j)',
      CRITICAL_INFRASTRUCTURE_DISRUPTION: 'Infrastructure critique (15j)',
      SERIOUS_PROPERTY_OR_ENVIRONMENTAL_DAMAGE: 'Dommage bien/env. (15j)'
    })[s];
  }
  severityBadge(s: AiIncSeverity): string { return 'sev sev-' + s.toLowerCase(); }
  statusBadge(s: AiIncStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[inc-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
