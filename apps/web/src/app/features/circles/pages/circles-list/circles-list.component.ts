import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { CirclesService } from '../../circles.service';
import { CircleResponse, CircleStatus } from '../../circles.types';
import { CirclesCreateDialogComponent } from '../circles-create-dialog/circles-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-circles-list',
  templateUrl: './circles-list.component.html',
  styleUrls: ['./circles-list.component.scss'],
  standalone: false
})
export class CirclesListComponent implements OnInit {

  readonly displayedColumns = ['name', 'status', 'members', 'meetings', 'proposals', 'updatedAt'];
  readonly statusFilter = new FormControl<CircleStatus | ''>('');
  readonly statuses: CircleStatus[] = ['ACTIVE', 'PAUSED', 'ARCHIVED'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  circles$!: Observable<CircleResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: CirclesService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.circles$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([s, p]) => this.svc.listCircles(p.index, p.size, s || undefined).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[circles-list] listCircles failed', err?.status, err?.error?.title);
          this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
          return [];
        }),
        finalize(() => this.loading$.next(false))
      )),
      map(page => {
        if (Array.isArray(page)) return [];
        this.totalElements = page.totalElements;
        return page.content;
      })
    );
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(CirclesCreateDialogComponent, {
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

  openCircle(c: CircleResponse): void {
    this.router.navigate(['/circles', c.id]);
  }

  badge(s: CircleStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
