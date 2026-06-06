import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { BreachService } from '../../breach.service';
import { BreachSeverity, BreachStatus, BreachView } from '../../breach.types';
import { BreachDetectDialogComponent } from '../breach-detect-dialog/breach-detect-dialog.component';

type ViewMode = 'ALL' | 'DPA_OVERDUE';

@Component({
  selector: 'qos-breach-list',
  templateUrl: './breach-list.component.html',
  styleUrls: ['./breach-list.component.scss'],
  standalone: false
})
export class BreachListComponent implements OnInit {

  readonly modeCtrl   = new FormControl<ViewMode>('ALL', { nonNullable: true });
  readonly statusCtrl = new FormControl<BreachStatus | ''>('', { nonNullable: true });
  readonly statuses: BreachStatus[] = ['DETECTED', 'ASSESSING', 'CONTAINED', 'CLOSED', 'REJECTED'];

  rows$!: Observable<BreachView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly columns = ['reference', 'title', 'severity', 'subjects', 'status', 'dpa', 'subjectsNotif'];
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: BreachService,
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
        const src$ = mode === 'DPA_OVERDUE'
          ? this.svc.dpaOverdue()
          : this.svc.list(status || undefined);
        return src$.pipe(
          catchError(err => {
            this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of([] as BreachView[]);
          }),
          tap(() => this.loading$.next(false)),
          switchMap(rows => {
            if (mode === 'DPA_OVERDUE' && status) rows = rows.filter(r => r.status === status);
            return of(rows);
          })
        );
      })
    );
  }

  open(r: BreachView): void { this.router.navigate(['/breaches', r.id]); }

  detect(): void {
    this.dialog.open(BreachDetectDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((b?: BreachView) => {
      if (b) { this.snack.open($localize`:@@breach.list.saved-toast:Violation enregistrée.`, $localize`:@@common.ok:OK`, { duration: 2200 }); this.refresh$.next(); }
    });
  }

  statusBadge(s: BreachStatus): string     { return 'badge badge-' + s.toLowerCase(); }
  severityBadge(s: BreachSeverity): string { return 'sev sev-' + s.toLowerCase(); }
}
