import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpoAppointmentsService } from '../../dpo-appointments.service';
import {
  DpoAppointmentStatus,
  DpoAppointmentView,
  DpoType
} from '../../dpo-appointments.types';
import { DpoActivateDialogComponent } from '../dpo-activate-dialog/dpo-activate-dialog.component';
import { DpoEditDialogComponent } from '../dpo-edit-dialog/dpo-edit-dialog.component';
import { DpoEndDialogComponent } from '../dpo-end-dialog/dpo-end-dialog.component';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-dpo-detail',
  templateUrl: './dpo-detail.component.html',
  styleUrls: ['./dpo-detail.component.scss'],
  standalone: false
})
export class DpoDetailComponent implements OnInit {

  appointment$!: Observable<DpoAppointmentView | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  readonly editTooltip         = $localize`:@@common.edit:Modifier`;
  readonly editTooltipLocked   = $localize`:@@dpo-appointments.detail.edit-tooltip-locked:Non éditable dans cet état`;
  readonly removeTooltip       = $localize`:@@dpo-appointments.detail.delete-tooltip-proposed:Supprimer la proposition`;
  readonly removeTooltipLocked = $localize`:@@dpo-appointments.detail.delete-tooltip-proposed-only:Seule une PROPOSED peut être supprimée`;
  readonly cancelReasonTitle   = $localize`:@@dpo-appointments.detail.cancel-reason-title:Motif d'annulation`;
  readonly endReasonTitle      = $localize`:@@dpo-appointments.detail.end-reason-title:Motif de fin de mandat`;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly svc: DpoAppointmentsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.appointment$ = this.route.paramMap.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(p => {
        const id = p.get('id') ?? '';
        // OWASP A03 — UUID v4 regex on path id (mock-id fallback).
        if (!UUID_REGEX.test(id) && !id.startsWith('dpo-')) {
          this.errorState$.next($localize`:@@common.invalid-id:Identifiant invalide.`);
          this.loadingState$.next(false);
          return of(null);
        }
        return this.refresh$.pipe(
          switchMap(() => this.svc.get(id).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[dpo-detail] failed', err?.status, err?.error?.title);
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return of(null);
            }),
            tap(() => this.loadingState$.next(false))
          ))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openEdit(a: DpoAppointmentView): void {
    // OWASP A04 — édition limitée à PROPOSED/ACTIVE (mirror backend).
    if (a.status !== 'PROPOSED' && a.status !== 'ACTIVE') {
      this.snack.open($localize`:@@dpo-appointments.detail.only-proposed-active-editable:La fiche n'est éditable qu'en PROPOSED ou ACTIVE.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(DpoEditDialogComponent, {
      data: { appointment: a }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  activate(a: DpoAppointmentView): void {
    const ref = this.dialog.open(DpoActivateDialogComponent, {
      data: { appointment: a }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  end(a: DpoAppointmentView): void {
    const ref = this.dialog.open(DpoEndDialogComponent, {
      data: { appointment: a, mode: 'END' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  cancelAppointment(a: DpoAppointmentView): void {
    const ref = this.dialog.open(DpoEndDialogComponent, {
      data: { appointment: a, mode: 'CANCEL' }, panelClass: 'qos-dialog-panel',
      autoFocus: 'first-tabbable', restoreFocus: true
    });
    ref.afterClosed().subscribe(updated => { if (updated) this.refresh$.next(); });
  }

  remove(a: DpoAppointmentView): void {
    // OWASP A04 — only PROPOSED can be deleted (mirror backend).
    if (a.status !== 'PROPOSED') {
      this.snack.open($localize`:@@dpo-appointments.detail.only-proposed-deletable:Seule une désignation PROPOSED peut être supprimée.`, $localize`:@@common.ok:OK`, { duration: 4000 });
      return;
    }
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@dpo-appointments.detail.delete-confirm-title:Supprimer la proposition ?`,
        message: 'Suppression définitive de « ' + a.reference + ' » (statut PROPOSED).',
        confirmLabel: $localize`:@@common.delete:Supprimer`, cancelLabel: $localize`:@@common.cancel:Annuler`, danger: true
      }
    });
    ref.afterClosed().subscribe(ok => {
      if (!ok) return;
      this.svc.delete(a.id).subscribe({
        next: () => { this.snack.open($localize`:@@dpo-appointments.detail.proposal-deleted:Proposition supprimée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.router.navigate(['/dpo-appointments']); },
        error: err => this.fail(err, $localize`:@@common.delete-failed:Suppression impossible.`)
      });
    });
  }

  typeLabel(t: DpoType): string {
    return t === 'INTERNAL'
      ? $localize`:@@dpo-appointments.type-internal:Interne (salarié)`
      : $localize`:@@dpo-appointments.type-external:Externe (prestataire)`;
  }
  typeBadge(t: DpoType): string { return 'type-badge type-' + t.toLowerCase(); }
  statusBadge(s: DpoAppointmentStatus): string { return 'badge badge-' + s.toLowerCase(); }

  private fail(err: unknown, fallback: string): void {
    // eslint-disable-next-line no-console
    console.warn('[dpo-detail] action failed', (err as any)?.status, (err as any)?.error?.title);
    this.snack.open(safeErrorMessage(err, fallback), $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
