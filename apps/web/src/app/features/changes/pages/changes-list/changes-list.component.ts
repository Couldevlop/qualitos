import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ChangesService } from '../../changes.service';
import {
  ChangeRequestPriority,
  ChangeRequestStatus,
  ChangeRequestType,
  ChangeResponse
} from '../../changes.types';
import { ChangesCreateDialogComponent } from '../changes-create-dialog/changes-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-changes-list',
  templateUrl: './changes-list.component.html',
  styleUrls: ['./changes-list.component.scss'],
  standalone: false
})
export class ChangesListComponent implements OnInit {

  readonly displayedColumns = ['code', 'title', 'type', 'priority', 'status', 'plannedFor'];

  readonly statuses: ChangeRequestStatus[] = [
    'DRAFT', 'SUBMITTED', 'UNDER_REVIEW',
    'APPROVED', 'REJECTED', 'IMPLEMENTED', 'CANCELLED'
  ];
  readonly types: ChangeRequestType[] = [
    'DOCUMENT', 'PROCESS', 'EQUIPMENT', 'SUPPLIER',
    'IT_SYSTEM', 'ORGANIZATIONAL', 'OTHER'
  ];

  readonly statusFilter = new FormControl<ChangeRequestStatus | ''>('');
  readonly typeFilter   = new FormControl<ChangeRequestType   | ''>('');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  changes$!: Observable<ChangeResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: ChangesService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.changes$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.typeFilter.valueChanges.pipe(startWith(this.typeFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.error$.next(null); queueMicrotask(() => this.loading$.next(true)); }),
      switchMap(([status, type, p]) =>
        this.svc.list(p.index, p.size, status || undefined, type || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[changes-list] failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return [];
          }),
          finalize(() => this.loading$.next(false))
        )
      ),
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
    const ref = this.dialog.open(ChangesCreateDialogComponent, {
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

  open(c: ChangeResponse): void { this.router.navigate(['/changes', c.id]); }

  statusBadge(s: ChangeRequestStatus): string   { return 'badge badge-' + s.toLowerCase(); }
  priorityBadge(p: ChangeRequestPriority): string { return 'prio prio-' + p.toLowerCase(); }
  typeBadge(t: ChangeRequestType): string       { return 'tbadge tbadge-' + t.toLowerCase(); }
  typeLabel(t: ChangeRequestType): string {
    return ({
      DOCUMENT: $localize`:@@changes.type.document:Document`,
      PROCESS: $localize`:@@changes.type.process:Processus`,
      EQUIPMENT: $localize`:@@changes.type.equipment:Équipement`,
      SUPPLIER: $localize`:@@changes.type.supplier:Fournisseur`,
      IT_SYSTEM: $localize`:@@changes.type.it-system:Système IT`,
      ORGANIZATIONAL: $localize`:@@changes.type.organizational:Organisationnel`,
      OTHER: $localize`:@@changes.type.other:Autre`
    })[t];
  }
}
