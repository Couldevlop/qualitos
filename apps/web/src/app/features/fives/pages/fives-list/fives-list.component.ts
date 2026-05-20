import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { FivesService } from '../../fives.service';
import { FiveSAuditResponse, FiveSAuditStatus } from '../../fives.types';
import { FivesCreateDialogComponent } from '../fives-create-dialog/fives-create-dialog.component';

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

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: FivesService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  openAudit(a: FiveSAuditResponse): void {
    this.router.navigate(['/fives', a.id]);
  }

  ngOnInit(): void {
    this.audits$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.refresh$
    ]).pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([status]) =>
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

  openCreate(): void {
    const ref = this.dialog.open(FivesCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) this.refresh$.next();
    });
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
