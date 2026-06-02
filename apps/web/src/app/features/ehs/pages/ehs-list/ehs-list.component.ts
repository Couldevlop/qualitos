import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { EhsService } from '../../ehs.service';
import {
  IncidentSeverity,
  IncidentStatus,
  IncidentType,
  IncidentView,
  Statistics
} from '../../ehs.types';
import { EhsReportDialogComponent } from '../ehs-report-dialog/ehs-report-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-ehs-list',
  templateUrl: './ehs-list.component.html',
  styleUrls: ['./ehs-list.component.scss'],
  standalone: false
})
export class EhsListComponent implements OnInit {

  readonly displayedColumns = ['code', 'title', 'type', 'severity', 'status', 'occurredAt'];

  readonly statuses: IncidentStatus[]   = ['REPORTED', 'INVESTIGATING', 'MITIGATED', 'CLOSED', 'CANCELLED'];
  readonly types: IncidentType[]        = ['INJURY', 'NEAR_MISS', 'ENVIRONMENTAL', 'SECURITY', 'PROPERTY_DAMAGE', 'OTHER'];
  readonly severities: IncidentSeverity[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly statusFilter   = new FormControl<IncidentStatus   | ''>('');
  readonly typeFilter     = new FormControl<IncidentType     | ''>('');
  readonly severityFilter = new FormControl<IncidentSeverity | ''>('');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize  = 20;
  totalElements = 0;

  incidents$!: Observable<IncidentView[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  stats: Statistics | null = null;

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: EhsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.incidents$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.typeFilter.valueChanges.pipe(startWith(this.typeFilter.value)),
      this.severityFilter.valueChanges.pipe(startWith(this.severityFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([status, type, severity, p]) =>
        this.svc.list(p.index, p.size, status || undefined, type || undefined, severity || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[ehs-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
            return of(null);
          }),
          finalize(() => this.loading$.next(false))
        )
      ),
      map(page => {
        if (!page) return [];
        this.totalElements = page.totalElements;
        return page.content;
      })
    );
  }

  loadStats(): void {
    this.svc.statistics().subscribe({
      next: s => (this.stats = s),
      error: () => (this.stats = null)
    });
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize  = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openReport(): void {
    const ref = this.dialog.open(EhsReportDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(c => {
      if (c) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
        this.refresh$.next();
        this.loadStats();
      }
    });
  }

  open(i: IncidentView): void { this.router.navigate(['/ehs', i.id]); }

  statusBadge(s: IncidentStatus): string   { return 'badge badge-' + s.toLowerCase(); }
  severityBadge(s: IncidentSeverity): string { return 'sev sev-' + s.toLowerCase(); }
  typeBadge(t: IncidentType): string       { return 'tbadge tbadge-' + t.toLowerCase(); }
  typeLabel(t: IncidentType): string {
    return ({
      INJURY: 'Accident', NEAR_MISS: 'Presque-accident',
      ENVIRONMENTAL: 'Environnement', SECURITY: 'Sécurité',
      PROPERTY_DAMAGE: 'Dommage matériel', OTHER: 'Autre'
    })[t];
  }
}
