import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { FmeaService, SEUIL_RPN_PAR_DEFAUT } from '../../fmea.service';
import { FmeaProjectPage, FmeaProjectResponse, FmeaStatus, FmeaType } from '../../fmea.types';
import { FmeaCreateDialogComponent } from '../fmea-create-dialog/fmea-create-dialog.component';
import {
  FmeaReferenceDialogComponent
} from '../fmea-reference-dialog/fmea-reference-dialog.component';
import { searchRows } from '../../../../shared/ui/list-search/list-filter';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

/** Page vide emise quand le chargement echoue : vide la table et remet le compteur a zero. */
const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

@Component({
  selector: 'qos-fmea-list',
  templateUrl: './fmea-list.component.html',
  styleUrls: ['./fmea-list.component.scss'],
  standalone: false
})
export class FmeaListComponent implements OnInit {

  readonly displayedColumns = ['code', 'name', 'type', 'status', 'criticalRpnThreshold', 'revision'];
  /** Seuil appliqué à un projet qui n'en fixe pas : cité dans le sous-titre. */
  readonly defaultRpnThreshold = SEUIL_RPN_PAR_DEFAUT;

  readonly statuses: FmeaStatus[] = ['DRAFT', 'ACTIVE', 'ARCHIVED'];
  readonly types: FmeaType[] = ['PROCESS_FMEA', 'DESIGN_FMEA', 'SYSTEM_FMEA', 'SERVICE_FMEA', 'BOW_TIE'];

  readonly statusFilter = new FormControl<FmeaStatus | ''>('');
  readonly typeFilter   = new FormControl<FmeaType   | ''>('');

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  projects$!: Observable<FmeaProjectResponse[]>;
  /** Lignes affichées : la page chargée, filtrée par la recherche. */
  visibleProjects$!: Observable<FmeaProjectResponse[]>;
  readonly search = new FormControl('', { nonNullable: true });
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
            // `return []` renverrait un observable VIDE : RxJS convertit le
            // tableau en source, donc la liste n'emettrait RIEN et la table
            // garderait les lignes precedentes sous la banniere d'erreur.
            return of(EMPTY_PAGE as unknown as FmeaProjectPage);
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      map(page => {
        this.totalElements = page.totalElements;
        return page.content;
      }),
      shareReplay({ bufferSize: 1, refCount: true })
    );
    this.visibleProjects$ = searchRows(this.projects$, this.search, p => [p.code, p.name, p.scope]);
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize  = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  /**
   * Ouvre le référentiel de cotation (barèmes S/O/D et exemple de PFMEA).
   * Purement documentaire : il ne modifie rien, d'où l'absence de suite au
   * `afterClosed()`.
   */
  openReference(): void {
    this.dialog.open(FmeaReferenceDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true,
      panelClass: ['qos-dialog-panel', 'qos-dialog-panel--wide']
    });
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

  /** Un filtre serveur est posé : la liste vide l'est peut-être à cause de lui. */
  get hasFilters(): boolean {
    return !!this.statusFilter.value || !!this.typeFilter.value;
  }

  resetFilters(): void {
    this.statusFilter.setValue('');
    this.typeFilter.setValue('');
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
