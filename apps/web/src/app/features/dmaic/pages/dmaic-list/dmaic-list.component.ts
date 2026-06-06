import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DmaicService } from '../../dmaic.service';
import {
  DmaicPhase,
  DmaicProjectResponse,
  DmaicStatus
} from '../../dmaic.types';
import { DmaicCreateDialogComponent } from '../dmaic-create-dialog/dmaic-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-dmaic-list',
  templateUrl: './dmaic-list.component.html',
  styleUrls: ['./dmaic-list.component.scss'],
  standalone: false
})
export class DmaicListComponent implements OnInit {

  readonly displayedColumns = ['title', 'phase', 'status', 'measureCount', 'pokaYokeCount', 'targetCompletionDate'];

  readonly statuses: DmaicStatus[] = ['ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];
  readonly phases: DmaicPhase[]   = ['DEFINE', 'MEASURE', 'ANALYZE', 'IMPROVE', 'CONTROL'];

  readonly statusFilter = new FormControl<DmaicStatus | ''>('');
  readonly phaseFilter  = new FormControl<DmaicPhase  | ''>('');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize  = 20;
  totalElements = 0;

  projects$!: Observable<DmaicProjectResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: DmaicService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.projects$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.phaseFilter.valueChanges.pipe(startWith(this.phaseFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, phase, p]) =>
        this.svc.listProjects(p.index, p.size, status || undefined, phase || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[dmaic-list] listProjects failed', err?.status, err?.error?.title);
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
    this.pageSize  = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(DmaicCreateDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
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

  open(p: DmaicProjectResponse): void {
    this.router.navigate(['/dmaic', p.id]);
  }

  phaseBadge(p: DmaicPhase): string  { return 'phase phase-' + p.toLowerCase(); }
  statusBadge(s: DmaicStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
