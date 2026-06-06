import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ItsmService } from '../../itsm.service';
import { ConnectionResponse, ConnectionStatus, ItsmProvider } from '../../itsm.types';
import { ItsmConnectionDialogComponent } from '../itsm-connection-dialog/itsm-connection-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-itsm-list',
  templateUrl: './itsm-list.component.html',
  styleUrls: ['./itsm-list.component.scss'],
  standalone: false
})
export class ItsmListComponent implements OnInit {

  readonly displayedColumns = ['name', 'provider', 'baseUrl', 'status', 'lastSyncAt'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  connections$!: Observable<ConnectionResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: ItsmService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.connections$ = this.refresh$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.page$.pipe(
        switchMap(p =>
          this.svc.list(p.index, p.size).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[itsm-list] failed', err?.status, err?.error?.title);
              this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
              return [];
            }),
            finalize(() => this.loadingState$.next(false))
          )
        )
      )),
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
    const ref = this.dialog.open(ItsmConnectionDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(c => {
      if (c) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
        this.refresh$.next();
      }
    });
  }

  open(c: ConnectionResponse): void { this.router.navigate(['/itsm', c.id]); }

  providerLabel(p: ItsmProvider): string { return p === 'SERVICENOW' ? 'ServiceNow' : 'Jira SM'; }
  providerBadge(p: ItsmProvider): string { return 'pbadge pbadge-' + p.toLowerCase(); }
  statusBadge(s: ConnectionStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
