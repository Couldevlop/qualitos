import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { Nis2MeasuresService } from '../../nis2m.service';
import {
  CATEGORY_LABEL,
  Nis2MeasureCategory,
  Nis2MeasureStatus,
  Nis2MeasureView,
  ResidualRiskRating
} from '../../nis2m.types';
import { Nis2mPlanDialogComponent } from '../nis2m-plan-dialog/nis2m-plan-dialog.component';

type ViewMode = 'ALL' | 'OVERDUE';

@Component({
  selector: 'qos-nis2m-list',
  templateUrl: './nis2m-list.component.html',
  styleUrls: ['./nis2m-list.component.scss'],
  standalone: false
})
export class Nis2mListComponent implements OnInit {

  readonly modeCtrl     = new FormControl<ViewMode>('ALL', { nonNullable: true });
  readonly statusCtrl   = new FormControl<Nis2MeasureStatus | ''>('', { nonNullable: true });
  readonly categoryCtrl = new FormControl<Nis2MeasureCategory | ''>('', { nonNullable: true });

  readonly statuses: Nis2MeasureStatus[] = ['PLANNED', 'IN_PROGRESS', 'IMPLEMENTED', 'VERIFIED', 'DEPRECATED'];
  readonly categories: Nis2MeasureCategory[] = Object.keys(CATEGORY_LABEL) as Nis2MeasureCategory[];
  readonly categoryLabel = CATEGORY_LABEL;

  rows$!: Observable<Nis2MeasureView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly columns = ['reference', 'category', 'title', 'maturity', 'risk', 'status', 'nextReview'];
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: Nis2MeasuresService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rows$ = combineLatest([
      this.modeCtrl.valueChanges.pipe(startWith(this.modeCtrl.value)),
      this.statusCtrl.valueChanges.pipe(startWith(this.statusCtrl.value)),
      this.categoryCtrl.valueChanges.pipe(startWith(this.categoryCtrl.value)),
      this.refresh$
    ]).pipe(
      debounceTime(120),
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([mode, status, category]) => {
        let src$: Observable<Nis2MeasureView[]>;
        if (mode === 'OVERDUE') {
          src$ = this.svc.reviewOverdue();
        } else if (category) {
          src$ = this.svc.listByCategory(category);
        } else {
          src$ = this.svc.list(status || undefined);
        }
        return src$.pipe(
          catchError(err => {
            this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as Nis2MeasureView[]);
          }),
          tap(rows => {
            this.loading$.next(false);
            if (mode === 'OVERDUE') {
              // client-side narrow if backend doesn't filter by status/category in overdue endpoint
              if (status) rows = rows.filter(r => r.status === status);
              if (category) rows = rows.filter(r => r.category === category);
            }
          }),
          switchMap(rows => {
            let filtered = rows;
            if (mode === 'OVERDUE') {
              if (status) filtered = filtered.filter(r => r.status === status);
              if (category) filtered = filtered.filter(r => r.category === category);
            } else if (category && status) {
              filtered = filtered.filter(r => r.status === status);
            }
            return of(filtered);
          })
        );
      })
    );
  }

  open(r: Nis2MeasureView): void { this.router.navigate(['/nis2-measures', r.id]); }

  plan(): void {
    this.dialog.open(Nis2mPlanDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((m?: Nis2MeasureView) => {
      if (m) { this.snack.open($localize`:@@nis2-measures.list.planned:Mesure planifiée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); }
    });
  }

  statusBadge(s: Nis2MeasureStatus): string { return 'badge badge-' + s.toLowerCase(); }
  riskBadge(r: ResidualRiskRating): string  { return 'risk risk-' + r.toLowerCase(); }
  catLabel(c: Nis2MeasureCategory): string  { return this.categoryLabel[c]; }
}
