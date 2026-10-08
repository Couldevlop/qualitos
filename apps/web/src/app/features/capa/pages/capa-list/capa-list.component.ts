import { Component, OnInit, Optional } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { capaTypeLabel } from '../../capa.labels';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaCriticity, CapaPage, CapaStatus } from '../../capa.types';
import {
  CapaCreateDialogComponent, CapaCreatePrefill
} from '../capa-create-dialog/capa-create-dialog.component';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

/** Page vide emise quand le chargement echoue : vide la table et remet le compteur a zero. */
const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

@Component({
  selector: 'qos-capa-list',
  templateUrl: './capa-list.component.html',
  styleUrls: ['./capa-list.component.scss'],
  standalone: false
})
export class CapaListComponent implements OnInit {

  readonly displayedColumns = ['title', 'type', 'criticity', 'status', 'dueDate'];
  readonly statusFilter = new FormControl<CapaStatus | ''>('');
  readonly statuses: CapaStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  cases$!: Observable<CapaCaseResponse[]>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly page$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 20 });

  constructor(
    private readonly svc: CapaService,
    private readonly dialog: MatDialog,
    private readonly router: Router,
    // Facultatif : la liste s'ouvre aussi hors routeur (bancs de test).
    @Optional() private readonly route?: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.openCreateFromLink();
    this.cases$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.page$,
      this.refresh$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, p]) =>
        this.svc.listCases(p.index, p.size, status || undefined).pipe(
          catchError(err => {
            // eslint-disable-next-line no-console
            console.warn('[capa-list] listCases failed', err?.status, err?.error?.title);
            this.errorState$.next(safeErrorMessage(err, $localize`:@@common.error-loading:Erreur lors du chargement.`));
            // `return []` renverrait un observable VIDE : RxJS convertit le
            // tableau en source, donc la liste n'emettrait RIEN et la table
            // garderait les lignes precedentes sous la banniere d'erreur.
            return of(EMPTY_PAGE as unknown as CapaPage);
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

  openCreate(prefill?: CapaCreatePrefill): void {
    const ref = this.dialog.open(CapaCreateDialogComponent, {
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      panelClass: 'qos-dialog-panel',
      data: prefill ?? null
    });
    ref.afterClosed().subscribe(created => {
      if (created) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
        this.refresh$.next();
      }
    });
  }

  /**
   * `/capa?nouveau=1&titre=…&ref=…&description=…` ouvre la création, préremplie.
   *
   * <p>C'est ainsi qu'un autre écran — la matrice des exigences du SMI — propose
   * « Créer une action CAPA » sans embarquer le module CAPA. Les paramètres sont
   * retirés de l'adresse aussitôt lus : recharger la page ne rouvre pas la
   * fenêtre. Rien n'est créé sans que l'utilisateur valide.
   */
  private openCreateFromLink(): void {
    const q = this.route?.snapshot.queryParamMap;
    if (q?.get('nouveau') !== '1') return;
    const texte = (cle: string, max: number) => (q.get(cle) ?? '').slice(0, max);
    const prefill: CapaCreatePrefill = {
      title: texte('titre', 255), description: texte('description', 4000), sourceRef: texte('ref', 255)
    };
    // Après la passe de détection en cours : ouvrir une fenêtre pendant
    // l'initialisation modifie la vue que cette passe vérifie (NG0100).
    queueMicrotask(() => {
      this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      this.openCreate(prefill);
    });
  }

  openCase(c: CapaCaseResponse): void {
    this.router.navigate(['/capa', c.id]);
  }

  statusBadge(s: CapaStatus): string { return 'badge badge-' + s.toLowerCase(); }

  /** Libellé lisible du type de dossier ; la colonne affichait l'énumération brute. */
  caseTypeLabel(type: string): string { return capaTypeLabel(type); }
  criticityBadge(c: CapaCriticity): string { return 'crit crit-' + c.toLowerCase(); }
}
