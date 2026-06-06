import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FmeaService } from '../../fmea.service';
import {
  FmeaProjectResponse,
  FmeaStatus,
  FmeaType
} from '../../fmea.types';
import { FmeaCreateDialogComponent } from '../fmea-create-dialog/fmea-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

@Component({
  selector: 'qos-fmea-list',
  templateUrl: './fmea-list.component.html',
  styleUrls: ['./fmea-list.component.scss'],
  standalone: false
})
export class FmeaListComponent implements OnInit {

  readonly displayedColumns = ['code', 'name', 'type', 'status', 'criticalRpnThreshold', 'revision'];

  readonly statuses: FmeaStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];
  readonly types: FmeaType[] = ['PROCESS_FMEA', 'DESIGN_FMEA', 'SYSTEM_FMEA', 'SERVICE_FMEA', 'BOW_TIE'];

  readonly statusFilter = new FormControl<FmeaStatus | ''>('');
  readonly typeFilter   = new FormControl<FmeaType   | ''>('');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  projects$!: Observable<FmeaProjectResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$    = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: FmeaService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.projects$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.typeFilter.valueChanges.pipe(startWith(this.typeFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, type, p]) =>
        this.svc.list(p.index, p.size, status || undefined, type || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[fmea-list] list failed', err?.status, err?.error?.title);
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
    const ref = this.dialog.open(FmeaCreateDialogComponent, {
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

  open(p: FmeaProjectResponse): void { this.router.navigate(['/fmea', p.id]); }

  typeLabel(t: FmeaType): string {
    return ({
      PROCESS_FMEA: $localize`:@@fmea.type.process:Processus`,
      DESIGN_FMEA: $localize`:@@fmea.type.design:Conception`,
      SYSTEM_FMEA: $localize`:@@fmea.type.system:Système`,
      SERVICE_FMEA: $localize`:@@fmea.type.service:Service`,
      BOW_TIE: $localize`:@@fmea.type.bow-tie:Bow-tie`
    })[t];
  }

  typeBadge(t: FmeaType): string   { return 'tbadge tbadge-' + t.toLowerCase(); }
  statusBadge(s: FmeaStatus): string { return 'badge badge-' + s.toLowerCase(); }
}
