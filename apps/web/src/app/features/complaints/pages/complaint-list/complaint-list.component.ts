import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, merge } from 'rxjs';
import { catchError, debounceTime, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { ComplaintsService } from '../../complaints.service';
import {
  COMPLAINT_CATEGORIES, COMPLAINT_CATEGORY_LABEL, COMPLAINT_CHANNEL_LABEL,
  COMPLAINT_SEVERITY_LABEL, COMPLAINT_STATUSES, COMPLAINT_STATUS_LABEL,
  Complaint, ComplaintCategory, ComplaintListCriteria, ComplaintSeverity, ComplaintStatistics,
  ComplaintStatus, isUuid
} from '../../complaints.types';
import { ComplaintCreateDialogComponent } from '../complaint-create-dialog/complaint-create-dialog.component';

/**
 * Critère de filtrage retenu.
 *
 * Un seul à la fois : le serveur n'en honore qu'un (précédence status >
 * category > supplierId). Proposer trois filtres cumulables afficherait un
 * résultat qui contredit l'interface.
 */
export type ComplaintFilterMode = 'NONE' | 'STATUS' | 'CATEGORY' | 'SUPPLIER';

/** Compteur de la barre de synthèse. */
export interface ComplaintTile {
  label: string;
  value: number;
  tone: 'neutral' | 'info' | 'success' | 'warn' | 'danger';
  /** Statut ciblé quand la tuile sert de raccourci de filtrage. */
  status: ComplaintStatus | null;
}

/** Part d'une catégorie dans le volume total, pour la barre de répartition. */
export interface ComplaintCategoryShare {
  category: ComplaintCategory;
  label: string;
  count: number;
  percent: number;
}

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

/**
 * Réclamations clients — liste, filtres et synthèse (§4.9).
 *
 * L'API `/api/v1/complaints` (15 routes) n'avait aucun écran : la saisie
 * multi-canal et le suivi jusqu'à la mesure de satisfaction n'étaient pilotables
 * qu'en appelant l'API à la main. L'analyse NLP reste sur son propre écran
 * (/complaints-nlp) ; ici on gère le dossier et son cycle de vie.
 */
@Component({
  selector: 'qos-complaint-list',
  templateUrl: './complaint-list.component.html',
  styleUrls: ['./complaint-list.component.scss'],
  standalone: false
})
export class ComplaintListComponent implements OnInit {

  readonly displayedColumns = ['code', 'subject', 'channel', 'category', 'severity', 'status', 'received'];

  readonly filterModeCtrl = new FormControl<ComplaintFilterMode>('NONE', { nonNullable: true });
  readonly statusCtrl = new FormControl<ComplaintStatus | ''>('', { nonNullable: true });
  readonly categoryCtrl = new FormControl<ComplaintCategory | ''>('', { nonNullable: true });
  readonly supplierCtrl = new FormControl<string>('', { nonNullable: true });

  readonly statuses = COMPLAINT_STATUSES;
  readonly categories = COMPLAINT_CATEGORIES;
  readonly statusLabel = COMPLAINT_STATUS_LABEL;
  readonly categoryLabel = COMPLAINT_CATEGORY_LABEL;
  readonly channelLabel = COMPLAINT_CHANNEL_LABEL;
  readonly severityLabel = COMPLAINT_SEVERITY_LABEL;

  // Accesseurs de libellé plutôt qu'indexation directe dans le template : la table
  // Material n'infère pas le type de ligne (`let row` reste `any`), et indexer un
  // Record avec une clé `any` est refusé par la compilation AOT en strictTemplates.
  // Le typage est donc porté ici, où il est réellement vérifié.
  channelText(row: Complaint): string { return this.channelLabel[row.channel]; }
  categoryText(row: Complaint): string { return this.categoryLabel[row.category]; }
  severityText(row: Complaint): string { return this.severityLabel[row.severity]; }
  statusText(row: Complaint): string { return this.statusLabel[row.status]; }

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;
  pageIndex = 0;
  pageSize = 20;

  rows$!: Observable<Complaint[]>;
  total$!: Observable<number>;
  tiles$!: Observable<ComplaintTile[]>;
  categoryShares$!: Observable<ComplaintCategoryShare[]>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /**
   * Déclencheur unique de chargement.
   *
   * Les contrôles de filtre n'alimentent pas directement le flux : chaque action
   * met l'état à jour puis demande UN rechargement. Brancher quatre `valueChanges`
   * sur le flux ferait partir jusqu'à quatre requêtes pour un simple « tout
   * afficher », qui remet quatre contrôles à zéro.
   */
  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: ComplaintsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const overview$ = merge(
      this.refresh$,
      // Seule exception : la saisie libre du fournisseur recharge d'elle-même,
      // à la fin de la frappe.
      this.supplierCtrl.valueChanges.pipe(debounceTime(300), tap(() => (this.pageIndex = 0)), map(() => undefined))
    ).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc.overview(this.criteria()).pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@complaints.list.load-error:Impossible de charger les réclamations.`));
          return [];
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      // refCount:false : plusieurs sections (tuiles, répartition, tableau,
      // paginateur) s'abonnent, certaines derrière un *ngIf — un abonnement
      // tardif ne doit pas déclencher un second aller-retour serveur.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.rows$ = overview$.pipe(map(o => o.page.content));
    this.total$ = overview$.pipe(map(o => o.page.totalElements));
    this.tiles$ = overview$.pipe(map(o => this.toTiles(o.statistics)));
    this.categoryShares$ = overview$.pipe(map(o => this.toCategoryShares(o.statistics)));
  }

  // ---- Filtres ---------------------------------------------------------------

  /**
   * Critère courant. Le mode décide seul de ce qui part au serveur : les autres
   * contrôles sont ignorés, comme le fait le serveur lui-même.
   */
  private criteria(): ComplaintListCriteria {
    const paging = { page: this.pageIndex, size: this.pageSize };
    const mode = this.filterModeCtrl.value;
    const supplier = this.supplierCtrl.value.trim();
    if (mode === 'STATUS' && this.statusCtrl.value) {
      return { ...paging, status: this.statusCtrl.value };
    }
    if (mode === 'CATEGORY' && this.categoryCtrl.value) {
      return { ...paging, category: this.categoryCtrl.value };
    }
    // Un identifiant partiel partirait en 400 : tant qu'il n'est pas complet on
    // laisse la liste non filtrée plutôt que d'afficher une erreur à chaque frappe.
    if (mode === 'SUPPLIER' && isUuid(supplier)) {
      return { ...paging, supplierId: supplier };
    }
    return paging;
  }

  /** Le champ fournisseur est renseigné mais pas encore exploitable. */
  supplierFilterIncomplete(): boolean {
    const value = this.supplierCtrl.value.trim();
    return this.filterModeCtrl.value === 'SUPPLIER' && value.length > 0 && !isUuid(value);
  }

  /** Changement de critère : la valeur précédente n'a plus de sens, on la vide. */
  onFilterModeChange(): void {
    this.statusCtrl.setValue('', { emitEvent: false });
    this.categoryCtrl.setValue('', { emitEvent: false });
    this.supplierCtrl.setValue('', { emitEvent: false });
    this.reload();
  }

  onFilterValueChange(): void {
    this.reload();
  }

  /** Raccourci depuis une tuile : bascule le filtre sur le statut concerné. */
  filterByStatus(status: ComplaintStatus | null): void {
    if (!status) {
      this.clearFilter();
      return;
    }
    this.statusCtrl.setValue(status, { emitEvent: false });
    this.filterModeCtrl.setValue('STATUS', { emitEvent: false });
    this.reload();
  }

  clearFilter(): void {
    this.statusCtrl.setValue('', { emitEvent: false });
    this.categoryCtrl.setValue('', { emitEvent: false });
    this.supplierCtrl.setValue('', { emitEvent: false });
    this.filterModeCtrl.setValue('NONE', { emitEvent: false });
    this.reload();
  }

  hasActiveFilter(): boolean {
    return this.filterModeCtrl.value !== 'NONE';
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.refresh$.next();
  }

  /** Toute action de filtrage repart de la première page. */
  private reload(): void {
    this.pageIndex = 0;
    this.refresh$.next();
  }

  // ---- Actions ---------------------------------------------------------------

  openCreate(): void {
    this.dialog.open(ComplaintCreateDialogComponent, {
      panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable', restoreFocus: true
    }).afterClosed().subscribe((created?: Complaint) => {
      if (!created) return;
      // Une réclamation naît en « Reçue » : on revient en tête de liste non
      // filtrée, sans quoi la création semblerait sans effet. `clearFilter`
      // déclenche déjà le rechargement.
      this.clearFilter();
    });
  }

  open(row: Complaint): void {
    this.router.navigate(['/complaints', row.id]);
  }

  // ---- Présentation ----------------------------------------------------------

  statusTone(status: ComplaintStatus): 'neutral' | 'info' | 'success' | 'warn' | 'danger' {
    switch (status) {
      case 'RECEIVED': return 'info';
      case 'UNDER_INVESTIGATION': return 'warn';
      case 'REOPENED': return 'warn';
      case 'RESPONDED': return 'info';
      case 'RESOLVED': return 'success';
      case 'CLOSED': return 'neutral';
      case 'REJECTED': return 'danger';
      default: return 'neutral';
    }
  }

  severityTone(severity: ComplaintSeverity): 'neutral' | 'info' | 'success' | 'warn' | 'danger' {
    switch (severity) {
      case 'LOW': return 'success';
      case 'MEDIUM': return 'info';
      case 'HIGH': return 'warn';
      case 'CRITICAL': return 'danger';
      default: return 'neutral';
    }
  }

  trackById(_index: number, row: Complaint): string {
    return row.id;
  }

  trackByCategory(_index: number, share: ComplaintCategoryShare): string {
    return share.category;
  }

  private toTiles(s: ComplaintStatistics): ComplaintTile[] {
    return [
      { label: $localize`:@@complaints.list.tile-total:Total`, value: s.total, tone: 'neutral', status: null },
      { label: $localize`:@@complaints.list.tile-received:À trier`, value: s.received, tone: 'info', status: 'RECEIVED' },
      { label: $localize`:@@complaints.list.tile-investigation:En investigation`, value: s.underInvestigation, tone: 'warn', status: 'UNDER_INVESTIGATION' },
      { label: $localize`:@@complaints.list.tile-responded:Répondues`, value: s.responded, tone: 'info', status: 'RESPONDED' },
      { label: $localize`:@@complaints.list.tile-resolved:Résolues`, value: s.resolved, tone: 'success', status: 'RESOLVED' },
      { label: $localize`:@@complaints.list.tile-closed:Clôturées`, value: s.closed, tone: 'neutral', status: 'CLOSED' },
      { label: $localize`:@@complaints.list.tile-rejected:Rejetées`, value: s.rejected, tone: 'danger', status: 'REJECTED' }
    ];
  }

  /**
   * Répartition par catégorie (§4.9 — Voice of Customer) : le pourcentage est
   * calculé sur le total du tenant, pas sur la page affichée, sinon la lecture
   * changerait à chaque pagination.
   */
  private toCategoryShares(s: ComplaintStatistics): ComplaintCategoryShare[] {
    const counts: Record<ComplaintCategory, number> = {
      PRODUCT: s.product, SERVICE: s.service, DELIVERY: s.delivery, BILLING: s.billing,
      QUALITY: s.quality, SAFETY: s.safety, OTHER: s.other
    };
    const total = COMPLAINT_CATEGORIES.reduce((sum, c) => sum + counts[c], 0);
    return COMPLAINT_CATEGORIES
      .map(category => ({
        category,
        label: COMPLAINT_CATEGORY_LABEL[category],
        count: counts[category],
        percent: total > 0 ? Math.round((counts[category] / total) * 100) : 0
      }))
      .filter(share => share.count > 0)
      .sort((a, b) => b.count - a.count);
  }
}
