import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { AiConformityService } from '../../ai-conformity.service';
import {
  ConformityProcedure,
  ConformityStatus,
  ConformityView
} from '../../ai-conformity.types';
import { CnfPlanDialogComponent } from '../cnf-plan-dialog/cnf-plan-dialog.component';

type Mode = 'all' | 'expiring';

@Component({
  selector: 'qos-cnf-list',
  templateUrl: './cnf-list.component.html',
  styleUrls: ['./cnf-list.component.scss'],
  standalone: false
})
export class CnfListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'aiSystem', 'procedure', 'notifiedBody', 'status', 'validUntil'];

  readonly statuses: ConformityStatus[] = ['PLANNED', 'IN_PROGRESS', 'CERTIFIED', 'EXPIRED', 'REVOKED', 'FAILED'];
  readonly statusFilter = new FormControl<ConformityStatus | ''>('');
  readonly modeFilter   = new FormControl<Mode>('all');

  readonly modes: { value: Mode; label: string }[] = [
    { value: 'all',      label: 'Toutes' },
    { value: 'expiring', label: 'Certificats arrivant à échéance (< 90 j)' }
  ];

  rows$!: Observable<ConformityView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: AiConformityService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.modeFilter.valueChanges.pipe(startWith(this.modeFilter.value)),
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([mode, status]) => {
        const op$ = mode === 'expiring' ? this.svc.listExpiring(200) : this.svc.list(status || undefined);
        return op$.pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[cnf-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of([] as ConformityView[]);
          }),
          finalize(() => this.loading$.next(false))
        );
      })
    );
  }

  openPlan(): void {
    const ref = this.dialog.open(CnfPlanDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(c => { if (c) this.router.navigate(['/ai-conformity', c.id]); });
  }

  open(c: ConformityView): void { this.router.navigate(['/ai-conformity', c.id]); }
  isExpiring(): boolean { return this.modeFilter.value === 'expiring'; }

  procedureLabel(p: ConformityProcedure): string {
    return p === 'NOTIFIED_BODY' ? 'Organisme notifié (Annexe VII)' : 'Contrôle interne (Annexe VI)';
  }
  procedureBadge(p: ConformityProcedure): string { return 'proc proc-' + p.toLowerCase(); }
  statusBadge(s: ConformityStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
