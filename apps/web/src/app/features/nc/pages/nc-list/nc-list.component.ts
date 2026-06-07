import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../nc.service';
import { NcCategory, NcResponse, NcSeverity, NcStatus } from '../../nc.types';
import { NcCreateDialogComponent } from '../nc-create-dialog/nc-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-nc-list',
  templateUrl: './nc-list.component.html',
  styleUrls: ['./nc-list.component.scss'],
  standalone: false
})
export class NcListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'title', 'category', 'severity', 'status', 'detectedAt'];
  readonly statusFilter = new FormControl<NcStatus | ''>('');
  readonly severityFilter = new FormControl<NcSeverity | ''>('');
  readonly categoryFilter = new FormControl<NcCategory | ''>('');

  readonly statuses: NcStatus[] = ['OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'RESOLVED', 'CLOSED', 'CANCELLED'];
  readonly severities: NcSeverity[] = ['MINOR', 'MAJOR', 'CRITICAL'];
  readonly categories: NcCategory[] = ['PRODUCT', 'PROCESS', 'DOCUMENTATION', 'SUPPLIER', 'SAFETY', 'ENVIRONMENT', 'OTHER'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  ncs$!: Observable<NcResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: NcService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.ncs$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.severityFilter.valueChanges.pipe(startWith(this.severityFilter.value)),
      this.categoryFilter.valueChanges.pipe(startWith(this.categoryFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, severity, category, p]) =>
        this.svc.listNcs(p.index, p.size, {
          status: status || undefined,
          severity: severity || undefined,
          category: category || undefined
        }).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[nc-list] listNcs failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return [];
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      map(page => {
        if (Array.isArray(page)) return [];
        this.totalElements = page.totalElements;
        return page.content;
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(NcCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
        this.refresh$.next();
      }
    });
  }

  openNc(n: NcResponse): void {
    if (n.pendingSync) return;
    this.router.navigate(['/nc', n.id]);
  }

  statusBadgeClass(status: NcStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  severityBadgeClass(severity: NcSeverity): string {
    return 'sev sev-' + severity.toLowerCase();
  }
}
