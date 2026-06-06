import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DpiaService } from '../../dpia.service';
import { DpiaStatus, DpiaView, RiskLevel } from '../../dpia.types';
import { DpiaCreateDialogComponent } from '../dpia-create-dialog/dpia-create-dialog.component';

type Mode = 'all' | 'consultation';

@Component({
  selector: 'qos-dpia-list',
  templateUrl: './dpia-list.component.html',
  styleUrls: ['./dpia-list.component.scss'],
  standalone: false
})
export class DpiaListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'title', 'risk', 'status', 'consultation', 'updatedAt'];

  readonly statuses: DpiaStatus[] = [
    'DRAFT', 'IN_PROGRESS', 'DPO_REVIEW', 'APPROVED', 'REJECTED', 'ARCHIVED'
  ];
  readonly statusFilter = new FormControl<DpiaStatus | ''>('');
  readonly modeFilter   = new FormControl<Mode>('all');

  readonly modes: { value: Mode; label: string }[] = [
    { value: 'all',          label: $localize`:@@dpia.list.mode-all:Toutes les DPIA` },
    { value: 'consultation', label: $localize`:@@dpia.list.mode-consultation:Exigeant consultation Art. 36` }
  ];

  rows$!: Observable<DpiaView[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: DpiaService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.modeFilter.valueChanges.pipe(startWith(this.modeFilter.value)),
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([mode, status]) => {
        const op$ = mode === 'consultation'
          ? this.svc.requiringConsultation()
          : this.svc.list(status || undefined);
        return op$.pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[dpia-list] failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as DpiaView[]);
          }),
          finalize(() => this.loadingState$.next(false))
        );
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(DpiaCreateDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(d => { if (d) this.router.navigate(['/dpia', d.id]); });
  }

  open(d: DpiaView): void { this.router.navigate(['/dpia', d.id]); }

  isConsultationMode(): boolean { return this.modeFilter.value === 'consultation'; }

  riskBadge(r: RiskLevel): string     { return 'risk risk-' + r.toLowerCase(); }
  statusBadge(s: DpiaStatus): string  { return 'badge badge-' + s.toLowerCase(); }
}
