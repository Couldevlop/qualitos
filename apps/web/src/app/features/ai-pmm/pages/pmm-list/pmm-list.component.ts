import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PmmService } from '../../pmm.service';
import { FREQUENCY_LABEL, PmmPlanStatus, PmmPlanView, PmmReviewFrequency } from '../../pmm.types';
import { PmmDraftDialogComponent } from '../pmm-draft-dialog/pmm-draft-dialog.component';

type ViewMode = 'ALL' | 'OVERDUE';

@Component({
  selector: 'qos-pmm-list',
  templateUrl: './pmm-list.component.html',
  styleUrls: ['./pmm-list.component.scss'],
  standalone: false
})
export class PmmListComponent implements OnInit {

  readonly modeCtrl   = new FormControl<ViewMode>('ALL', { nonNullable: true });
  readonly statusCtrl = new FormControl<PmmPlanStatus | ''>('', { nonNullable: true });
  readonly statuses: PmmPlanStatus[] = ['DRAFT', 'ACTIVE', 'SUSPENDED', 'CLOSED'];
  readonly freqLabel = FREQUENCY_LABEL;

  rows$!: Observable<PmmPlanView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly columns = ['reference', 'name', 'frequency', 'status', 'nextReview'];
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: PmmService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.modeCtrl.valueChanges.pipe(startWith(this.modeCtrl.value)),
      this.statusCtrl.valueChanges.pipe(startWith(this.statusCtrl.value)),
      this.refresh$
    ]).pipe(
      debounceTime(120),
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([mode, status]) => {
        const src$ = mode === 'OVERDUE'
          ? this.svc.overdueReviews()
          : this.svc.list(status || undefined);
        return src$.pipe(
          catchError(err => {
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of([] as PmmPlanView[]);
          }),
          tap(() => this.loading$.next(false)),
          switchMap(rows => {
            if (mode === 'OVERDUE' && status) rows = rows.filter(r => r.status === status);
            return of(rows);
          })
        );
      })
    );
  }

  open(p: PmmPlanView): void { this.router.navigate(['/ai-pmm', p.id]); }

  draft(): void {
    this.dialog.open(PmmDraftDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((p?: PmmPlanView) => {
      if (p) { this.snack.open('Plan PMM créé.', 'OK', { duration: 2200 }); this.refresh$.next(); }
    });
  }

  statusBadge(s: PmmPlanStatus): string { return 'badge badge-' + s.toLowerCase(); }
  freqOf(f: PmmReviewFrequency): string { return this.freqLabel[f]; }

  isOverdue(p: PmmPlanView): boolean {
    return p.status === 'ACTIVE' && !!p.nextReviewDueAt && new Date(p.nextReviewDueAt).getTime() < Date.now();
  }
}
