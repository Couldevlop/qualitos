import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditStatus } from '../../audits.types';
import { AuditsCreateDialogComponent } from '../audits-create-dialog/audits-create-dialog.component';

@Component({
  selector: 'qos-audits-list',
  templateUrl: './audits-list.component.html',
  styleUrls: ['./audits-list.component.scss'],
  standalone: false
})
export class AuditsListComponent implements OnInit {

  readonly displayedColumns = ['title', 'type', 'standard', 'status', 'score', 'scheduledDate'];
  readonly statusFilter = new FormControl<AuditStatus | ''>('');
  readonly statuses: AuditStatus[] = ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  plans$!: Observable<AuditPlanResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: AuditsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  openPlan(p: AuditPlanResponse): void {
    this.router.navigate(['/audits', p.id]);
  }

  ngOnInit(): void {
    this.plans$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => this.loading$.next(true)),
      switchMap(([s]) => this.svc.listPlans(0, 50, s || undefined).pipe(
        catchError(() => []),
        finalize(() => this.loading$.next(false))
      )),
      map(p => Array.isArray(p) ? [] : p.content)
    );
  }

  openCreate(): void {
    const ref = this.dialog.open(AuditsCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) this.refresh$.next();
    });
  }

  statusBadge(s: AuditStatus): string { return 'badge badge-' + s.toLowerCase(); }
  scoreClass(s?: number): string {
    if (s == null) return 'score';
    if (s >= 85) return 'score score-high';
    if (s >= 70) return 'score score-mid';
    return 'score score-low';
  }
}
