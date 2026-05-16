import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { FivesService } from '../../fives.service';
import { FiveSAuditResponse, FiveSAuditStatus } from '../../fives.types';

@Component({
  selector: 'qos-fives-list',
  templateUrl: './fives-list.component.html',
  styleUrls: ['./fives-list.component.scss'],
  standalone: false
})
export class FivesListComponent implements OnInit {

  readonly displayedColumns = ['zone', 'status', 'score', 'scheduledAt', 'updatedAt'];
  readonly statusFilter = new FormControl<FiveSAuditStatus | ''>('');
  readonly statuses: FiveSAuditStatus[] = ['DRAFT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

  audits$!: Observable<FiveSAuditResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);

  constructor(private readonly svc: FivesService) {}

  ngOnInit(): void {
    this.audits$ = this.statusFilter.valueChanges.pipe(
      startWith(this.statusFilter.value),
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(status =>
        this.svc.listAudits(0, 50, status || undefined).pipe(
          catchError(err => {
            this.error$.next(err?.message ?? 'Erreur réseau');
            return [];
          }),
          finalize(() => this.loading$.next(false))
        )
      ),
      map(page => (Array.isArray(page) ? [] : page.content))
    );
  }

  badgeClass(status: FiveSAuditStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  scoreClass(score?: number): string {
    if (score == null) return 'score';
    if (score >= 80) return 'score score-high';
    if (score >= 60) return 'score score-mid';
    return 'score score-low';
  }
}
