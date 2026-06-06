import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
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
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

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
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — validate path param shape before hitting backend (mock allows demo ids).
        if (!UUID_REGEX.test(id) && !id.startsWith('ehs-')) {
          this.errorState$.next('Identifiant invalide.');
          this.loadingState$.next(false);
          return of(null);
        }
        this.incidentId = id;
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[ehs-detail] failed', err?.status, err?.error?.title);
              this.errorState$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return of(null);
            }),
            tap(() => this.loadingState$.next(false))
          ))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
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
        title: $localize`:@@ehs.detail.close-title:Clôturer l'incident ?`,
        message: $localize`:@@ehs.detail.close-message:L'incident « ${i.title}:title: » sera marqué CLOSED. Aucune nouvelle modification possible.`,
        confirmLabel: $localize`:@@ehs.detail.close-incident:Clôturer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: false
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.close(i.id).subscribe({
        next: () => { this.snack.open($localize`:@@ehs.detail.closed:Incident clôturé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@ehs.detail.close-failed:Clôture impossible.`)
      });
    });
  }

  cancel(i: IncidentView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ehs.detail.cancel-title:Annuler l'incident ?`,
        message: $localize`:@@ehs.detail.cancel-message:L'incident « ${i.title}:title: » sera marqué CANCELLED. Transition terminale.`,
        confirmLabel: $localize`:@@ehs.detail.cancel-confirm:Annuler l'incident`, cancelLabel: $localize`:@@ehs.detail.keep:Conserver`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.cancel(i.id).subscribe({
        next: () => { this.snack.open($localize`:@@ehs.detail.cancelled:Incident annulé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); },
        error: err => this.fail(err, $localize`:@@ehs.detail.cancel-failed:Annulation impossible.`)
      });
    });
  }

  remove(i: IncidentView): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@ehs.detail.delete-title:Supprimer l'incident ?`,
        message: $localize`:@@ehs.detail.delete-message:Suppression définitive de « ${i.title}:title: » et de tout son historique.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(i.id).subscribe({
        next: () => { this.snack.open($localize`:@@ehs.detail.deleted:Incident supprimé.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/ehs']); },
        error: err => this.fail(err, $localize`:@@ehs.detail.delete-failed:Suppression impossible.`)
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
      INJURY: $localize`:@@ehs.detail.type-injury:Accident corporel`,
      NEAR_MISS: $localize`:@@ehs.type.near-miss:Presque-accident`,
      ENVIRONMENTAL: $localize`:@@ehs.type.environmental:Environnement`,
      SECURITY: $localize`:@@ehs.type.security:Sécurité`,
      PROPERTY_DAMAGE: $localize`:@@ehs.type.property-damage:Dommage matériel`,
      OTHER: $localize`:@@ehs.type.other:Autre`
    })[t];
  }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[ehs-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), 'OK', { duration: 4000 });
  }
}
