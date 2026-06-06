import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpoAppointmentsService } from '../../dpo-appointments.service';
import {
  DpoAppointmentStatus,
  DpoAppointmentView,
  DpoType
} from '../../dpo-appointments.types';
import { DpoProposeDialogComponent } from '../dpo-propose-dialog/dpo-propose-dialog.component';

@Component({
  selector: 'qos-dpo-list',
  templateUrl: './dpo-list.component.html',
  styleUrls: ['./dpo-list.component.scss'],
  standalone: false
})
export class DpoListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'dpo', 'type', 'scope', 'status', 'effectiveFrom'];

  readonly statuses: DpoAppointmentStatus[] = ['PROPOSED', 'ACTIVE', 'ENDED', 'CANCELLED'];
  readonly statusFilter = new FormControl<DpoAppointmentStatus | ''>('ACTIVE');

  rows$!: Observable<DpoAppointmentView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: DpoAppointmentsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      // loading différé hors de la passe de détection de changements courante :
      // évite NG0100 (le banner *ngIf="loading$ | async" est évalué avant que la
      // souscription synchrone de rows$ ne bascule loading$ à true).
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([status]) =>
        this.svc.list(status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[dpo-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as DpoAppointmentView[]);
          }),
          finalize(() => this.loading$.next(false))
        )
      )
    );
  }

  openPropose(): void {
    const ref = this.dialog.open(DpoProposeDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(a => { if (a) this.router.navigate(['/dpo-appointments', a.id]); });
  }

  open(a: DpoAppointmentView): void { this.router.navigate(['/dpo-appointments', a.id]); }

  typeLabel(t: DpoType): string {
    return t === 'INTERNAL'
      ? $localize`:@@dpo-appointments.edit.type-internal:Interne`
      : $localize`:@@dpo-appointments.edit.type-external:Externe`;
  }
  typeBadge(t: DpoType): string { return 'type-badge type-' + t.toLowerCase(); }
  statusBadge(s: DpoAppointmentStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
