import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { SuppliersService } from '../../suppliers.service';
import { SupplierResponse, SupplierStatus, SupplierType } from '../../suppliers.types';
import { SuppliersCreateDialogComponent } from '../suppliers-create-dialog/suppliers-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-suppliers-list',
  templateUrl: './suppliers-list.component.html',
  styleUrls: ['./suppliers-list.component.scss'],
  standalone: false
})
export class SuppliersListComponent implements OnInit {

  readonly displayedColumns = ['code', 'name', 'country', 'type', 'status', 'score', 'lastAuditAt'];

  readonly statuses: SupplierStatus[] = ['PROSPECT', 'APPROVED', 'CONDITIONAL', 'SUSPENDED', 'BLACKLISTED'];
  readonly types: SupplierType[] = [
    'RAW_MATERIAL', 'COMPONENT', 'SERVICE', 'CONTRACT_MANUFACTURER',
    'SOFTWARE', 'LOGISTICS', 'OTHER'
  ];

  readonly statusFilter = new FormControl<SupplierStatus | ''>('');
  readonly typeFilter   = new FormControl<SupplierType   | ''>('');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  suppliers$!: Observable<SupplierResponse[]>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$   = new BehaviorSubject<string | null>(null);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: SuppliersService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.suppliers$ = combineLatest([
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
            console.warn('[suppliers-list] list failed', err?.status, err?.error?.title);
            this.error$.next(safeErrorMessage(err, 'Erreur lors du chargement.'));
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
    const ref = this.dialog.open(SuppliersCreateDialogComponent, {
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

  open(s: SupplierResponse): void { this.router.navigate(['/suppliers', s.id]); }

  scoreClass(score: number, status: SupplierStatus): string {
    if (status === 'PROSPECT') return 'score score-na';
    if (score >= 85) return 'score score-good';
    if (score >= 65) return 'score score-warn';
    return 'score score-bad';
  }

  statusBadge(s: SupplierStatus): string { return 'badge badge-' + s.toLowerCase(); }
  typeBadge(t: SupplierType): string     { return 'tbadge tbadge-' + t.toLowerCase(); }
  typeLabel(t: SupplierType): string {
    return ({
      RAW_MATERIAL: 'Matière première', COMPONENT: 'Composant', SERVICE: 'Service',
      CONTRACT_MANUFACTURER: 'Sous-traitant', SOFTWARE: 'Logiciel',
      LOGISTICS: 'Logistique', OTHER: 'Autre'
    })[t];
  }
}
