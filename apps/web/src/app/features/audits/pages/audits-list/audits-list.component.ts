import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditStatus } from '../../audits.types';

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

  constructor(private readonly svc: AuditsService) {}

  ngOnInit(): void {
    this.plans$ = this.statusFilter.valueChanges.pipe(
      startWith(this.statusFilter.value),
      tap(() => this.loading$.next(true)),
      switchMap(s => this.svc.listPlans(0, 50, s || undefined).pipe(
        catchError(() => []),
        finalize(() => this.loading$.next(false))
      )),
      map(p => Array.isArray(p) ? [] : p.content)
    );
  }

  statusBadge(s: AuditStatus): string { return 'badge badge-' + s.toLowerCase(); }
  scoreClass(s?: number): string {
    if (s == null) return 'score';
    if (s >= 85) return 'score score-high';
    if (s >= 70) return 'score score-mid';
    return 'score score-low';
  }
}
