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
import {
  NcCategory,
  NcOrigin,
  NcPage,
  NcResponse,
  NcSeverity,
  NcStatistics,
  NcStatus,
  NcSubcategory
} from '../../nc.types';
import { NcCreateDialogComponent } from '../nc-create-dialog/nc-create-dialog.component';
import { searchRows } from '../../../../shared/ui/list-search/list-filter';

const PAGE_SIZE_OPTIONS = [10, 20, 50, 100];
const MAX_PAGE_SIZE = 100;

/** Page vide emise quand le chargement echoue : vide la table et remet le compteur a zero. */
const EMPTY_PAGE = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

/** Un groupe de natures d'écart, rangé sous sa catégorie, dans le filtre. */
export interface NcSubcategoryGroup {
  category: NcCategory;
  subcategories: NcSubcategory[];
}

/** Une tuile de dénombrement au-dessus de la liste. */
export interface NcTile {
  label: string;
  value: number;
  tone: 'neutral' | 'info' | 'warn' | 'success' | 'danger';
  /** Statut filtré au clic ; `''` remet la liste à plat (tuile « Total »). */
  status: NcStatus | '';
}
@Component({
  selector: 'qos-nc-list',
  templateUrl: './nc-list.component.html',
  styleUrls: ['./nc-list.component.scss'],
  standalone: false
})
export class NcListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'title', 'category', 'severity', 'status',
                               'reporter', 'detectedAt'];
  readonly statusFilter = new FormControl<NcStatus | ''>('');
  readonly severityFilter = new FormControl<NcSeverity | ''>('');
  readonly categoryFilter = new FormControl<NcCategory | ''>('');
  /** La nature fine de l'écart : le filtre qui dit « lesquelles exactement ». */
  readonly subcategoryFilter = new FormControl<string>('', { nonNullable: true });

  /** Le catalogue des natures, pour le filtre et pour traduire la colonne. */
  subcategories: NcSubcategory[] = [];

  /**
   * Statuts proposés au filtre. `RESOLVED` en est volontairement absent : c'est
   * un état de passage, franchi le temps d'enregistrer la résolution puis
   * clôturé dans la foulée, et le proposer renvoyait presque toujours une liste
   * vide. Les NC résolues se retrouvent sous `CLOSED`.
   */
  readonly statuses: NcStatus[] = [
    'OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'CLOSED', 'CANCELLED', 'REJECTED'];
  readonly severities: NcSeverity[] = ['MINOR', 'MAJOR', 'CRITICAL'];
  readonly categories: NcCategory[] = ['PRODUCT', 'PROCESS', 'DOCUMENTATION', 'SUPPLIER', 'SAFETY', 'ENVIRONMENT', 'OTHER'];

  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;

  /** Les natures proposées au filtre : celles de la catégorie choisie, sinon toutes. */
  get subcategoryChoices(): NcSubcategory[] {
    const category = this.categoryFilter.value;
    return category ? this.subcategories.filter(s => s.category === category) : this.subcategories;
  }

  /**
   * Les mêmes natures, RANGÉES SOUS LEUR CATÉGORIE.
   *
   * <p>Sans catégorie choisie, le filtre déroulait la soixantaine de natures
   * d'une seule coulée : « Étalonnage périmé » et « Procédure obsolète » s'y
   * suivaient sans que rien ne dise qu'elles relèvent de deux familles. Les
   * grouper rend la catégorie lisible au moment du choix — c'est elle qui donne
   * son sens à la nature.
   *
   * <p>L'ordre des catégories est celui de {@link categories}, et non l'ordre
   * d'arrivée du catalogue : le client peut retirer ou ajouter des natures, pas
   * réordonner les familles. Une catégorie sans nature n'apparaît pas.
   */
  get subcategoryGroups(): NcSubcategoryGroup[] {
    const catalogue = this.subcategories;
    const category = this.categoryFilter.value ?? '';
    // MEMOISATION OBLIGATOIRE, et non une commodite : `mat-select` instancie
    // ses options des le rendu, et `*ngFor` les suit par IDENTITE d'objet.
    // Un getter qui fabrique un nouveau groupe a chaque appel fait donc
    // detruire puis recreer toutes les options a chaque passe de detection ;
    // chaque creation d'option previent le select, qui demande une nouvelle
    // passe -- l'ecran se figeait, moteur bloque, sur un ecran blanc (les
    // natures, elles, sont les memes objets : les lister a plat ne bouclait
    // pas). On recalcule donc uniquement quand le catalogue ou la categorie
    // choisie change.
    if (this.groupesCache && this.groupesPour.catalogue === catalogue
        && this.groupesPour.category === category) {
      return this.groupesCache;
    }
    const choix = category ? catalogue.filter(s => s.category === category) : catalogue;
    this.groupesCache = this.categories
      .map(cat => ({ category: cat, subcategories: choix.filter(s => s.category === cat) }))
      .filter(g => g.subcategories.length > 0);
    this.groupesPour = { catalogue, category };
    return this.groupesCache;
  }

  private groupesCache: NcSubcategoryGroup[] | null = null;
  private groupesPour: { catalogue: NcSubcategory[]; category: NcCategory | '' } =
    { catalogue: [], category: '' };

  /** Le libellé d'une nature ; son code si le client l'a retirée de son catalogue. */
  subcategoryLabel(code: string | undefined): string {
    if (!code) return '—';
    return this.subcategories.find(s => s.code === code)?.label ?? code;
  }

  pageIndex = 0;
  pageSize = 20;
  totalElements = 0;

  ncs$!: Observable<NcResponse[]>;
  /** Lignes affichées : la page chargée, filtrée par la recherche. */
  visibleNcs$!: Observable<NcResponse[]>;
  readonly search = new FormControl('', { nonNullable: true });
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
      return $localize`:@@nc.list.title-external:Non-conformités externes (Réclamations)`;
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

  /**
   * Dénombrements par statut, dans le périmètre de l'écran.
   *
   * <p>Rechargés à chaque déclaration : une NC créée doit se voir aussitôt
   * dans les tuiles, sans quoi le chiffre dément le tableau juste en dessous.
   */
  tiles$!: Observable<NcTile[]>;
  private readonly statsReload$ = new BehaviorSubject<void>(undefined);

  ngOnInit(): void {
    // Le catalogue sert le filtre et la traduction de la colonne : son absence
    // n'empêche pas de lire la liste, elle laisse seulement voir les codes.
    this.svc.subcategories().pipe(catchError(() => of({ entries: [] as NcSubcategory[], defaults: true })))
      .subscribe(c => (this.subcategories = c.entries));

    // Changer de catégorie efface la nature : une nature d'une autre catégorie
    // ne ramènerait jamais rien.
    this.categoryFilter.valueChanges.subscribe(() => {
      if (this.subcategoryFilter.value) this.subcategoryFilter.setValue('');
    });

    // Les tuiles vivent hors du flux de la liste : les filtres n'ont pas à les
    // recalculer (un dénombrement filtré par lui-même ne dirait plus rien), mais
    // une déclaration, si.
    this.tiles$ = this.statsReload$.pipe(
      switchMap(() => this.svc.statistics(this.origin ?? undefined).pipe(
        catchError(() => of(null))          // les tuiles s'effacent, la liste reste
      )),
      map(s => s ? this.toTiles(s) : []),
      // `refCount: false` : le bloc des tuiles est sous un `*ngIf`, donc le
      // tuyau async s'y désabonne et s'y réabonne au gré des rendus. Avec un
      // comptage de références, chaque réabonnement relançait la requête —
      // le serveur recomptait les NC pour un affichage qui n'avait pas changé.
      // Sans comptage, la dernière valeur est rejouée et l'appel reste unique.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.ncs$ = combineLatest([
      this.statusFilter.valueChanges.pipe(startWith(this.statusFilter.value)),
      this.severityFilter.valueChanges.pipe(startWith(this.severityFilter.value)),
      this.categoryFilter.valueChanges.pipe(startWith(this.categoryFilter.value)),
      this.subcategoryFilter.valueChanges.pipe(startWith(this.subcategoryFilter.value)),
      this.page$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, severity, category, subcategoryCode, p]) =>
        this.svc.listNcs(p.index, p.size, {
          status: status || undefined,
          severity: severity || undefined,
          category: category || undefined,
          subcategoryCode: subcategoryCode || undefined,
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
    this.visibleNcs$ = searchRows(this.ncs$, this.search, n => [n.reference, n.title, n.zone, n.reporterName]);
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
      panelClass: 'qos-dialog-panel',
      // L'origine de la LISTE suit la déclaration. Sans elle, le dialogue
      // n'envoyait aucune origine et le serveur retombait sur son défaut
      // INTERNAL : une non-conformité déclarée depuis l'écran « externes »
      // atterrissait dans le tableau des internes, donc hors de la liste d'où
      // on venait de la créer — elle semblait s'être perdue.
      //
      // `undefined` sur l'entrée historique `/nc`, qui montre les deux : là,
      // aucune origine n'est impliquée par l'écran, et c'est au serveur de
      // trancher.
      data: { origin: this.origin ?? undefined }
    });
    ref.afterClosed().subscribe(created => {
      if (created) {
        this.pageIndex = 0;
        this.page$.next({ index: 0, size: this.pageSize });
        this.statsReload$.next();   // sinon les tuiles démentiraient le tableau
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

  /**
   * Filtre la liste sur le statut d'une tuile, ou remet à plat si on reclique.
   *
   * <p>Un nombre qu'on ne peut pas ouvrir oblige à retrouver à la main les
   * lignes qu'il désigne : la tuile est le chemin le plus court entre le
   * constat et la liste.
   */
  filterByStatus(status: NcStatus | ''): void {
    this.statusFilter.setValue(this.statusFilter.value === status ? '' : status);
  }

  trackByTile(_index: number, tile: NcTile): string {
    return tile.label;
  }

  /**
   * Les tuiles, dans l'ordre du cycle de vie d'un écart.
   *
   * <p>« Annulée » n'apparaît que sur les écarts signalés du dehors : on
   * n'annule pas un constat qu'on a fait soi-même, on le résout ou on le clôt.
   * La tuile serait donc à zéro à perpétuité sur l'écran interne — un chiffre
   * qui n'apprend rien occupe une place qui compte.
   */
  private toTiles(s: NcStatistics): NcTile[] {
    const tuiles: NcTile[] = [
      { label: $localize`:@@nc.list.tile-total:Total`, value: s.total, tone: 'neutral', status: '' },
      { label: $localize`:@@nc.list.tile-open:Ouvertes`, value: s.open, tone: 'info', status: 'OPEN' },
      { label: $localize`:@@nc.list.tile-analysis:En analyse`, value: s.underAnalysis, tone: 'warn', status: 'UNDER_ANALYSIS' },
      { label: $localize`:@@nc.list.tile-action:Action définie`, value: s.actionDefined, tone: 'warn', status: 'ACTION_DEFINED' },
      { label: $localize`:@@nc.list.tile-closed:Clôturées`, value: s.closed, tone: 'success', status: 'CLOSED' }
    ];
    if (this.origin !== 'INTERNAL') {
      tuiles.push({ label: $localize`:@@nc.list.tile-cancelled:Annulées`, value: s.cancelled, tone: 'danger', status: 'CANCELLED' });
      // « Rejetées » n'a de sens que sur l'externe : on ne rejette pas un constat
      // qu'on a fait soi-même, on le résout, on le clôt ou on l'annule.
      tuiles.push({ label: $localize`:@@nc.list.tile-rejected:Rejetées`, value: s.rejected, tone: 'danger', status: 'REJECTED' });
    }
    return tuiles;
  }
}
