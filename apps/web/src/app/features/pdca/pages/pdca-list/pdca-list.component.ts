import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse, PdcaStatus } from '../../pdca.types';
import { PdcaCreateDialogComponent } from '../pdca-create-dialog/pdca-create-dialog.component';

// OWASP A03 — clamp paging params so a tampered URL / browser state can't
// request 10 000 000 rows from the API.
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-pdca-list',
  templateUrl: './pdca-list.component.html',
  styleUrls: ['./pdca-list.component.scss'],
  standalone: false
})
export class PdcaListComponent implements OnInit {

  readonly displayedColumns = ['title', 'status', 'steps', 'updatedAt'];
  readonly statusFilter = new FormControl<PdcaStatus | ''>('');
  readonly statuses: PdcaStatus[] = ['PLAN', 'DO', 'CHECK', 'ACT', 'COMPLETED', 'CANCELLED'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  cycles$!: Observable<PdcaCycleResponse[]>;
  // Init à true : on charge dès l'init, et cela évite le NG0100
  // (ExpressionChangedAfterItHasBeenChecked) quand le tap() passe false→true
  // dans le même cycle de détection (même correctif que kpis-list).
  private readonly loadingState$ = new BehaviorSubject<boolean>(true);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly pdca: PdcaService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  openCycle(cycle: PdcaCycleResponse): void {
    this.router.navigate(['/pdca', cycle.id]);
  }

  ngOnInit(): void {
    this.cycles$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, p]) =>
        this.pdca.listCycles(p.index, p.size, status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[pdca-list] listCycles failed', err?.status, err?.error?.title);
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
    // OWASP A03 — clamp client-side too. The backend re-validates via @PageableDefault.
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(PdcaCreateDialogComponent, {
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

  statusBadgeClass(status: PdcaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }
}
