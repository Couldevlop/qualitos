import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { KpisService } from '../../kpis.service';
import { KpiDirection, KpiResponse, KpiStatus } from '../../kpis.types';
import { KpisDialogComponent } from '../kpis-dialog/kpis-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-kpis-list',
  templateUrl: './kpis-list.component.html',
  styleUrls: ['./kpis-list.component.scss'],
  standalone: false
})
export class KpisListComponent implements OnInit {

  readonly displayedColumns = ['code', 'name', 'category', 'direction', 'target', 'frequency', 'status'];

  readonly statuses: KpiStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];
  readonly statusFilter   = new FormControl<KpiStatus | ''>('ACTIVE');
  readonly categoryFilter = new FormControl<string>('');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  kpis$!: Observable<KpiResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: KpisService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.kpis$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.categoryFilter.valueChanges.pipe(startWith(this.categoryFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(([status, category, p]) =>
        this.svc.list(p.index, p.size, status || undefined, category?.trim() || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[kpis-list] failed', err?.status, err?.error?.title);
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

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize  = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(KpisDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(k => {
      if (k) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
        this.refresh$.next();
      }
    });
  }

  open(k: KpiResponse): void { this.router.navigate(['/kpis', k.id]); }

  directionLabel(d: KpiDirection): string {
    return d === 'HIGHER_IS_BETTER' ? '↑ mieux' : '↓ mieux';
  }
  directionBadge(d: KpiDirection): string {
    return 'dir dir-' + (d === 'HIGHER_IS_BETTER' ? 'up' : 'down');
  }
  statusBadge(s: KpiStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
