import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DocumentsService } from '../../documents.service';
import {
  DocumentResponse,
  DocumentStatus,
  DocumentType,
  VersionStatus
} from '../../documents.types';
import { DocumentsCreateDialogComponent } from '../documents-create-dialog/documents-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-documents-list',
  templateUrl: './documents-list.component.html',
  styleUrls: ['./documents-list.component.scss'],
  standalone: false
})
export class DocumentsListComponent implements OnInit {

  readonly displayedColumns = ['code', 'title', 'type', 'currentVersion', 'status', 'mandatoryRead'];

  readonly statuses: DocumentStatus[] = ['ACTIVE', 'ARCHIVED'];
  readonly statusFilter = new FormControl<DocumentStatus | ''>('ACTIVE');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize  = 20;
  totalElements = 0;

  documents$!: Observable<DocumentResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: DocumentsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.documents$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, p]) =>
        this.svc.list(p.index, p.size, status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[documents-list] list failed', err?.status, err?.error?.title);
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
    const ref = this.dialog.open(DocumentsCreateDialogComponent, {
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

  open(d: DocumentResponse): void {
    this.router.navigate(['/documents', d.id]);
  }

  currentVersion(d: DocumentResponse): { number?: number; status?: VersionStatus } {
    const v = d.versions.find(x => x.id === d.currentVersionId)
           ?? d.versions[d.versions.length - 1];
    return { number: v?.versionNumber, status: v?.status };
  }

  versionBadge(s?: VersionStatus): string { return s ? 'vbadge vbadge-' + s.toLowerCase() : ''; }
  statusBadge(s: DocumentStatus): string { return 'badge badge-' + s.toLowerCase(); }
  typeBadge(t: DocumentType): string     { return 'tbadge tbadge-' + t.toLowerCase(); }
}
