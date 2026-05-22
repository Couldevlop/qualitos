import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { catchError, finalize, map, switchMap, tap } from 'rxjs/operators';

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
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: ItsmService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.connections$ = this.refresh$.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(() => this.page$.pipe(
        switchMap(p =>
          this.svc.list(p.index, p.size).pipe(
            catchError(err => {
              // eslint-disable-next-line no-console
              console.warn('[itsm-list] failed', err?.status, err?.error?.title);
              this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
              return [];
            }),
            finalize(() => this.loading$.next(false))
          )
        )
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
