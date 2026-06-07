import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { describeDuration, RetentionService } from '../../retention.service';
import { RetentionRuleStatus, RetentionRuleView } from '../../retention.types';
import { RetEvaluateDialogComponent } from '../ret-evaluate-dialog/ret-evaluate-dialog.component';
import { RetRuleDialogComponent } from '../ret-rule-dialog/ret-rule-dialog.component';

@Component({
  selector: 'qos-ret-list',
  templateUrl: './ret-list.component.html',
  styleUrls: ['./ret-list.component.scss'],
  standalone: false
})
export class RetListComponent implements OnInit {

  readonly displayedColumns = ['dataCategoryCode', 'label', 'period', 'status', 'effectiveFrom'];

  readonly statuses: RetentionRuleStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];
  readonly statusFilter = new FormControl<RetentionRuleStatus | ''>('ACTIVE');

  rules$!: Observable<RetentionRuleView[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: RetentionService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.rules$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status]) =>
        this.svc.list(status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[ret-list] failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as RetentionRuleView[]);
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(RetRuleDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(r => { if (r) this.router.navigate(['/retention', r.id]); });
  }

  openEvaluate(): void {
    this.dialog.open(RetEvaluateDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
  }

  open(r: RetentionRuleView): void { this.router.navigate(['/retention', r.id]); }

  describe = describeDuration;
  statusBadge(s: RetentionRuleStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
