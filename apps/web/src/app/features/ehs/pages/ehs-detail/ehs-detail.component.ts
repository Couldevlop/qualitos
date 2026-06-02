import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { EhsService } from '../../ehs.service';
import {
  IncidentSeverity,
  IncidentStatus,
  IncidentType,
  IncidentView
} from '../../ehs.types';
import { EhsEditDialogComponent } from '../ehs-edit-dialog/ehs-edit-dialog.component';
import { EhsLinkDialogComponent } from '../ehs-link-dialog/ehs-link-dialog.component';
import { EhsMitigateDialogComponent } from '../ehs-mitigate-dialog/ehs-mitigate-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-ehs-detail',
  templateUrl: './ehs-detail.component.html',
  styleUrls: ['./ehs-detail.component.scss'],
  standalone: false
})
export class EhsDetailComponent implements OnInit {

  incident$!: Observable<IncidentView | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private incidentId = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: EhsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.incident$ = this.route.paramMap.pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — validate path param shape before hitting backend (mock allows demo ids).
        if (!UUID_REGEX.test(id) && !id.startsWith('ehs-')) {
          this.error$.next('Identifiant invalide.');
          this.loading$.next(false);
          return of(null);
        }
        this.incidentId = id;
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[ehs-detail] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            }),
            tap(() => this.loading$.next(false))
          ))
        );
      })
    );
  }

  openEdit(i: IncidentView): void {
    const ref = this.dialog.open(EhsEditDialogComponent, {
      data: { incident: i }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  investigate(i: IncidentView): void {
    const ownerUserId = this.auth.snapshot()?.userId;
    this.svc.investigate(i.id, { ownerUserId }).subscribe({
      next: () => { this.snack.open('Incident en investigation.', 'OK', { duration: 2200 }); this.refresh$.next(); },
      error: err => this.fail(err, 'Passage en investigation impossible.')
    });
  }

  openMitigate(i: IncidentView): void {
    const ref = this.dialog.open(EhsMitigateDialogComponent, {
      data: { incidentId: i.id }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  close(i: IncidentView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Clôturer l\'incident ?',
        message: 'L\'incident « ' + i.title + ' » sera marqué CLOSED. Aucune nouvelle modification possible.',
        confirmLabel: 'Clôturer', cancelLabel: 'Annuler', danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.close(i.id).subscribe({
        next: () => { this.snack.open('Incident clôturé.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Clôture impossible.')
      });
    });
  }

  cancel(i: IncidentView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Annuler l\'incident ?',
        message: 'L\'incident « ' + i.title + ' » sera marqué CANCELLED. Transition terminale.',
        confirmLabel: 'Annuler l\'incident', cancelLabel: 'Conserver', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.cancel(i.id).subscribe({
        next: () => { this.snack.open('Incident annulé.', 'OK', { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, 'Annulation impossible.')
      });
    });
  }

  remove(i: IncidentView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Supprimer l\'incident ?',
        message: 'Suppression définitive de « ' + i.title + ' » et de tout son historique.',
        confirmLabel: 'Supprimer', cancelLabel: 'Annuler', danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(i.id).subscribe({
        next: () => { this.snack.open('Incident supprimé.', 'OK', { duration: 2200 }); this.router.navigate(['/ehs']); },
        error: err => this.fail(err, 'Suppression impossible.')
      });
    });
  }

  openLink(i: IncidentView, kind: 'CAPA' | 'NC'): void {
    const ref = this.dialog.open(EhsLinkDialogComponent, {
      data: { incidentId: i.id, kind }, autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  statusBadge(s: IncidentStatus): string   { return 'badge badge-' + s.toLowerCase(); }
  severityBadge(s: IncidentSeverity): string { return 'sev sev-' + s.toLowerCase(); }
  typeLabel(t: IncidentType): string {
    return ({
      INJURY: 'Accident corporel', NEAR_MISS: 'Presque-accident',
      ENVIRONMENTAL: 'Environnement', SECURITY: 'Sécurité',
      PROPERTY_DAMAGE: 'Dommage matériel', OTHER: 'Autre'
    })[t];
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[ehs-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
