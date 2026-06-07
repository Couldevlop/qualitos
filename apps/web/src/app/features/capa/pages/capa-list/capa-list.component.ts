import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity, CapaStatus } from '../../capa.types';
import { CapaCreateDialogComponent } from '../capa-create-dialog/capa-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-capa-list',
  templateUrl: './capa-list.component.html',
  styleUrls: ['./capa-list.component.scss'],
  standalone: false
})
export class CapaListComponent implements OnInit {

  readonly displayedColumns = ['title', 'type', 'criticity', 'status', 'dueDate'];
  readonly statusFilter = new FormControl<CapaStatus | ''>('');
  readonly statuses: CapaStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  cases$!: Observable<CapaCaseResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: CapaService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.cases$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, p]) =>
        this.svc.listCases(p.index, p.size, status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-list] listCases failed', err?.status, err?.error?.title);
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
    const ref = this.dialog.open(CapaCreateDialogComponent, {
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

  openCase(c: CapaCaseResponse): void {
    this.router.navigate(['/capa', c.id]);
  }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
}
