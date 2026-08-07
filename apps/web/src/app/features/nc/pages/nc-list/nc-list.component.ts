import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { NcService } from '../../nc.service';
import { NcCategory, NcOrigin, NcPage, NcResponse, NcSeverity, NcStatus } from '../../nc.types';
import { NcCreateDialogComponent } from '../nc-create-dialog/nc-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

/** Page vide emise quand le chargement echoue : vide la table et remet le compteur a zero. */
const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

@Component({
  selector: 'qos-nc-list',
  templateUrl: './nc-list.component.html',
  styleUrls: ['./nc-list.component.scss'],
  standalone: false
})
export class NcListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'title', 'category', 'severity', 'status', 'detectedAt'];
  readonly statusFilter = new FormControl<NcStatus | ''>('');
  readonly severityFilter = new FormControl<NcSeverity | ''>('');
  readonly categoryFilter = new FormControl<NcCategory | ''>('');

  readonly statuses: NcStatus[] = ['OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'RESOLVED', 'CLOSED', 'CANCELLED'];
  readonly severities: NcSeverity[] = ['MINOR', 'MAJOR', 'CRITICAL'];
  readonly categories: NcCategory[] = ['PRODUCT', 'PROCESS', 'DOCUMENTATION', 'SUPPLIER', 'SAFETY', 'ENVIRONMENT', 'OTHER'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  ncs$!: Observable<NcResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  // `page$` amorce à lui seul le combineLatest et rejoue à chaque rechargement
  // voulu : un second sujet « refresh » ferait partir DEUX requêtes pour une
  // déclaration (course entre les deux réponses, la plus ancienne pouvant gagner).
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  /**
   * Origine consultée, portée par la ROUTE (`/nc/interne`, `/nc/externe`) et non
   * par un filtre de l'écran : deux entrées de navigation qui mèneraient au même
   * endroit dès qu'on touche à un menu déroulant n'auraient aucun sens. Absente
   * sur l'entrée historique `/nc`, qui continue de tout montrer.
   */
  readonly origin: NcOrigin | null = null;

  constructor(
    private readonly svc: NcService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    route: ActivatedRoute
  ) {
    const declared = route.snapshot.data['origin'];
    this.origin = declared === 'INTERNAL' || declared === 'EXTERNAL' ? declared : null;
  }

  /** Titre de l'écran : il doit dire laquelle des deux listes on regarde. */
  get pageTitle(): string {
    if (this.origin === 'INTERNAL') {
      return $localize`:@@nc.list.title-internal:Non-conformités internes`;
    }
    if (this.origin === 'EXTERNAL') {
      return $localize`:@@nc.list.title-external:Non-conformités externes`;
    }
    return $localize`:@@nc.list.title:Non-conformités`;
  }

  get pageSubtitle(): string {
    if (this.origin === 'INTERNAL') {
      return $localize`:@@nc.list.subtitle-internal:Écarts détectés par l'organisation elle-même : autocontrôle, audit interne, revue.`;
    }
    if (this.origin === 'EXTERNAL') {
      return $localize`:@@nc.list.subtitle-external:Écarts signalés du dehors : client, fournisseur, autorité, organisme certificateur.`;
    }
    return $localize`:@@nc.list.subtitle:Saisie terrain, analyse de cause racine, résolution et escalade CAPA — traçables de bout en bout.`;
  }

  ngOnInit(): void {
    this.ncs$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.severityFilter.valueChanges.pipe(startWith(this.severityFilter.value)),
      this.categoryFilter.valueChanges.pipe(startWith(this.categoryFilter.value)),
      this.page$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, severity, category, p]) =>
        this.svc.listNcs(p.index, p.size, {
          status: status || undefined,
          severity: severity || undefined,
          category: category || undefined,
          origin: this.origin ?? undefined
        }).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[nc-list] listNcs failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            // `return []` renverrait un observable VIDE : RxJS convertit le
            // tableau en source, donc la liste n'emettrait RIEN et la table
            // garderait les lignes precedentes sous la banniere d'erreur.
            return of(EMPTY_PAGE as unknown as NcPage);
          }),
          finalize(() => this.loadingState$.next(false))
        )
      ),
      map(page => {
        this.totalElements = page.totalElements;
        return page.content;
      }),
      shareReplay({ bufferSize: 1, refCount: false }) // refCount:false : evite la boucle de teardown quand *ngIf loading masque la table
    );
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.page$.next({ index: this.pageIndex, size: this.pageSize });
  }

  openCreate(): void {
    const ref = this.dialog.open(NcCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel'
    });
    ref.afterClosed().subscribe(created => {
      if (created) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
      }
    });
  }

  openNc(n: NcResponse): void {
    if (n.pendingSync) return;
    this.router.navigate(['/nc', n.id]);
  }

  statusBadgeClass(status: NcStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  severityBadgeClass(severity: NcSeverity): string {
    return 'sev sev-' + severity.toLowerCase();
  }
}
