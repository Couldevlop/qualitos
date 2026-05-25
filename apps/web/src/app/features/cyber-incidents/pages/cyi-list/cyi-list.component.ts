import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { BehaviorSubject, combineLatest, Observable, of } from 'rxjs';
import { catchError, debounceTime, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CyberIncidentsService } from '../../cyi.service';
import { CyiSeverity, CyiStatus, CyiType, CyiView, TYPE_LABEL } from '../../cyi.types';
import { CyiDetectDialogComponent } from '../cyi-detect-dialog/cyi-detect-dialog.component';

type ViewMode = 'ALL' | 'EW_OVERDUE' | 'IA_OVERDUE' | 'FR_OVERDUE';

@Component({
  selector: 'qos-cyi-list',
  templateUrl: './cyi-list.component.html',
  styleUrls: ['./cyi-list.component.scss'],
  standalone: false
})
export class CyiListComponent implements OnInit {

  readonly modeCtrl   = new FormControl<ViewMode>('ALL', { nonNullable: true });
  readonly statusCtrl = new FormControl<CyiStatus | ''>('', { nonNullable: true });
  readonly statuses: CyiStatus[] = ['DETECTED', 'ASSESSING', 'MITIGATED', 'CLOSED', 'REJECTED'];

  rows$!: Observable<CyiView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  readonly columns = ['reference', 'title', 'type', 'severity', 'affected', 'status', 'deadlines'];
  readonly typeLabel = TYPE_LABEL;
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: CyberIncidentsService,
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
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([mode, status]) => {
        let src$: Observable<CyiView[]>;
        switch (mode) {
          case 'EW_OVERDUE': src$ = this.svc.earlyWarningOverdue(); break;
          case 'IA_OVERDUE': src$ = this.svc.initialAssessmentOverdue(); break;
          case 'FR_OVERDUE': src$ = this.svc.finalReportOverdue(); break;
          default:           src$ = this.svc.list(status || undefined); break;
        }
        return src$.pipe(
          catchError(err => {
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of([] as CyiView[]);
          }),
          tap(() => this.loading$.next(false)),
          switchMap(rows => {
            if (mode !== 'ALL' && status) rows = rows.filter(r => r.status === status);
            return of(rows);
          })
        );
      })
    );
  }

  open(r: CyiView): void { this.router.navigate(['/cyber-incidents', r.id]); }

  detect(): void {
    this.dialog.open(CyiDetectDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((i?: CyiView) => {
      if (i) { this.snack.open('Incident enregistré.', 'OK', { duration: 2200 }); this.refresh$.next(); }
    });
  }

  statusBadge(s: CyiStatus): string   { return 'badge badge-' + s.toLowerCase(); }
  severityBadge(s: CyiSeverity): string { return 'sev sev-' + s.toLowerCase(); }
  typeOf(t: CyiType): string { return this.typeLabel[t]; }
}
