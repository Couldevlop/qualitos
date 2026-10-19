import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, Observable, Subscription, combineLatest, of } from 'rxjs';
import {
  catchError, debounceTime, distinctUntilChanged, finalize, map, shareReplay, startWith, switchMap, tap
} from 'rxjs/operators';

import { AuthzService } from '../../../../core/authz/authz.service';
import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { DocumentsService } from '../../documents.service';
import {
  DISPLAY_STATUS_LABELS, DOCUMENT_TYPE_LABELS, DOCUMENT_TYPES, FILTERABLE_STATUSES, VIEW_LABELS, displayStatusOf
} from '../../documents.labels';
import {
  DisplayStatus, DocumentFolder, DocumentListFilter, DocumentPage, DocumentProcess, DocumentResponse,
  DocumentType, DocumentView, DocumentViewCounts
} from '../../documents.types';
import { FolderRow, buildFolderRows } from '../../documents.folders';
import { DocumentsCreateDialogComponent } from '../documents-create-dialog/documents-create-dialog.component';
import { DocumentsImportDialogComponent } from '../documents-import-dialog/documents-import-dialog.component';

const PAGE_SIZE = 20;

/** Page vide émise quand le chargement échoue : vide la table et remet le compteur à zéro. */
const EMPTY_PAGE: DocumentPage = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

interface ViewEntry {
  view: DocumentView;
  label: string;
  icon: string;
  key: keyof DocumentViewCounts;
}

/**
 * La documentation qualité (GED, ADR 0094) : les vues et l'arborescence à gauche,
 * la liste filtrée au centre, la fiche du document choisi à droite.
 *
 * <p>Déposer des fichiers n'importe où sur la liste ouvre l'import.
 */
@Component({
  selector: 'qos-documents-list',
  templateUrl: './documents-list.component.html',
  styleUrls: ['./documents-list.component.scss'],
  standalone: false
})
export class DocumentsListComponent implements OnInit, OnDestroy {

  readonly views: ViewEntry[] = [
    { view: 'ALL',         label: VIEW_LABELS.ALL,         icon: 'folder_copy',     key: 'all' },
    { view: 'MINE',        label: VIEW_LABELS.MINE,        icon: 'person',          key: 'mine' },
    { view: 'TO_VALIDATE', label: VIEW_LABELS.TO_VALIDATE, icon: 'rule',            key: 'toValidate' },
    { view: 'REVIEW_DUE',  label: VIEW_LABELS.REVIEW_DUE,  icon: 'event_upcoming',  key: 'reviewDue' }
  ];
  readonly archivesLabel = VIEW_LABELS.ARCHIVED;

  readonly statuses = FILTERABLE_STATUSES;
  readonly types = DOCUMENT_TYPES;
  readonly statusLabels = DISPLAY_STATUS_LABELS;
  readonly typeLabels = DOCUMENT_TYPE_LABELS;

  readonly filters = this.fb.group({
    q: [''],
    displayStatus: [null as DisplayStatus | null],
    type: [null as DocumentType | null],
    processId: [null as string | null],
    ownerId: [null as string | null]
  });

  view: DocumentView = 'ALL';
  folderId: string | null = null;
  pageIndex = 0;
  readonly pageSize = PAGE_SIZE;
  totalElements = 0;

  documents: DocumentResponse[] = [];
  selected: DocumentResponse | null = null;

  folderRows: FolderRow[] = [];
  /** Les dossiers repliés ; tout est déplié au premier affichage. */
  readonly collapsed = new Set<string>();
  processes: DocumentProcess[] = [];
  counts$!: Observable<DocumentViewCounts | null>;

  /** Un glisser de fichiers survole la liste. */
  dragging = false;
  /** Seul qui peut créer un document peut en déposer. */
  private canImport = false;
  private dragDepth = 0;

  private readonly loadingState$ = new BehaviorSubject<boolean>(true);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly query$ = new BehaviorSubject<{ view: DocumentView; folderId: string | null; page: number }>(
    { view: 'ALL', folderId: null, page: 0 });
  private readonly refresh$ = new BehaviorSubject<void>(undefined);
  private readonly subs = new Subscription();

  constructor(
    private readonly svc: DocumentsService,
    private readonly dialog: MatDialog,
    private readonly fb: FormBuilder,
    private readonly authz: AuthzService
  ) {}

  ngOnInit(): void {
    const filters$ = this.filters.valueChanges.pipe(
      startWith(this.filters.getRawValue()),
      debounceTime(250),
      map(() => this.filters.getRawValue()),
      distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
      // Un filtre qui change ramène à la première page.
      tap(() => { if (this.pageIndex !== 0) this.goToPage(0); })
    );

    this.subs.add(combineLatest([this.query$, filters$, this.refresh$]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([q, f]) => {
        const filter: DocumentListFilter = {
          view: q.view, folderId: q.folderId, q: f.q ?? undefined, displayStatus: f.displayStatus,
          type: f.type, processId: f.processId, ownerId: f.ownerId
        };
        return this.svc.search(filter, q.page, this.pageSize).pipe(
          catchError(err => {
            this.errorState$.next(safeErrorMessage(err,
              $localize`:@@common.error-loading:Erreur lors du chargement.`));
            return of(EMPTY_PAGE);
          }),
          finalize(() => this.loadingState$.next(false))
        );
      })
    ).subscribe(page => {
      this.documents = page.content;
      this.totalElements = page.totalElements;
      this.loadingState$.next(false);
      // La fiche suit la liste : on garde le document choisi s'il y figure encore,
      // sinon on présente le premier.
      // Sur un écran étroit, la fiche passe par-dessus la liste : elle attend qu'on la demande.
      const keep = this.selected && page.content.find(d => d.id === this.selected!.id);
      this.selected = keep ?? (panelBesideList() ? page.content[0] ?? null : null);
    }));

    this.counts$ = this.refresh$.pipe(
      switchMap(() => this.svc.views().pipe(catchError(() => of(null)))),
      shareReplay({ bufferSize: 1, refCount: true })
    );

    this.subs.add(this.authz.can('document.edit').subscribe(ok => this.canImport = ok));
    this.subs.add(this.svc.folders().pipe(catchError(() => of([] as DocumentFolder[])))
      .subscribe(f => this.folderRows = buildFolderRows(f)));
    this.subs.add(this.svc.processes().pipe(catchError(() => of([] as DocumentProcess[])))
      .subscribe(p => this.processes = p));
  }

  ngOnDestroy(): void {
    this.subs.unsubscribe();
  }

  // ---------- navigation ----------

  selectView(view: DocumentView): void {
    this.view = view;
    this.folderId = null;
    this.goToPage(0);
  }

  selectFolder(row: FolderRow): void {
    // L'arborescence se lit parmi les documents actifs.
    this.view = 'ALL';
    this.folderId = this.folderId === row.folder.id ? null : row.folder.id;
    this.goToPage(0);
  }

  toggleFolder(row: FolderRow, event: Event): void {
    event.stopPropagation();
    if (this.collapsed.has(row.folder.id)) {
      this.collapsed.delete(row.folder.id);
    } else {
      this.collapsed.add(row.folder.id);
    }
  }

  /** Un dossier est visible si aucun de ses ancêtres n'est replié. */
  isVisible(row: FolderRow): boolean {
    let parent = row.folder.parentId;
    while (parent) {
      if (this.collapsed.has(parent)) return false;
      parent = this.folderRows.find(r => r.folder.id === parent)?.folder.parentId ?? null;
    }
    return true;
  }

  goToPage(index: number): void {
    this.pageIndex = Math.max(0, index);
    this.query$.next({ view: this.view, folderId: this.folderId, page: this.pageIndex });
  }

  get pageCount(): number {
    return Math.max(1, Math.ceil(this.totalElements / this.pageSize));
  }

  get rangeStart(): number {
    return this.totalElements === 0 ? 0 : this.pageIndex * this.pageSize + 1;
  }

  get rangeEnd(): number {
    return Math.min(this.totalElements, (this.pageIndex + 1) * this.pageSize);
  }

  get hasFilters(): boolean {
    const f = this.filters.getRawValue();
    return !!(f.q || f.displayStatus || f.type || f.processId || f.ownerId || this.folderId);
  }

  clearFilters(): void {
    this.filters.reset({ q: '', displayStatus: null, type: null, processId: null, ownerId: null });
    if (this.folderId) {
      this.folderId = null;
      this.goToPage(0);
    }
  }

  get heading(): string {
    if (this.folderId) {
      return this.folderRows.find(r => r.folder.id === this.folderId)?.folder.name ?? '';
    }
    return this.view === 'ALL'
      ? $localize`:@@documents.list.heading:Documentation qualité`
      : VIEW_LABELS[this.view];
  }

  // ---------- sélection ----------

  select(d: DocumentResponse): void {
    this.selected = d;
  }

  /** Les flèches haut et bas parcourent la liste, la fiche suit. */
  onRowKey(event: KeyboardEvent, index: number): void {
    const next = event.key === 'ArrowDown' ? index + 1 : event.key === 'ArrowUp' ? index - 1 : -1;
    if (next < 0 || next >= this.documents.length) return;
    event.preventDefault();
    this.selected = this.documents[next];
    const rows = (event.currentTarget as HTMLElement).parentElement?.children;
    (rows?.item(next) as HTMLElement | null)?.focus();
  }

  displayStatus(d: DocumentResponse): DisplayStatus {
    return displayStatusOf(d);
  }

  /** Le numéro de la version qui fait foi, à défaut celui de la dernière. */
  versionNumber(d: DocumentResponse): number | null {
    const v = d.versions.find(x => x.id === d.currentVersionId)
      ?? [...d.versions].sort((a, b) => b.versionNumber - a.versionNumber)[0];
    return v?.versionNumber ?? null;
  }

  processName(id: string | null | undefined): string | null {
    return id ? this.processes.find(p => p.id === id)?.name ?? null : null;
  }

  folderName(id: string | null | undefined): string | null {
    return id ? this.folderRows.find(r => r.folder.id === id)?.folder.name ?? null : null;
  }

  trackById(_: number, d: { id: string }): string {
    return d.id;
  }

  trackByFolder(_: number, r: FolderRow): string {
    return r.folder.id;
  }

  // ---------- création et import ----------

  openCreate(): void {
    const ref = this.dialog.open(DocumentsCreateDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel',
      data: { folderId: this.folderId, folders: this.folderRows, processes: this.processes }
    });
    ref.afterClosed().subscribe(created => {
      if (created) this.afterChange(created);
    });
  }

  openImport(files: File[] = []): void {
    const ref = this.dialog.open(DocumentsImportDialogComponent, {
      autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel', width: '820px', maxWidth: '96vw',
      data: { files, folderId: this.folderId, folders: this.folderRows, processes: this.processes }
    });
    ref.afterClosed().subscribe((imported: DocumentResponse[] | undefined) => {
      if (imported?.length) this.afterChange(imported[0]);
    });
  }

  private afterChange(focus: DocumentResponse): void {
    this.selected = focus;
    this.refresh$.next();
  }

  onPanelChanged(): void {
    this.refresh$.next();
  }

  /** Échap referme la fiche quand elle recouvre la liste. */
  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.selected && !panelBesideList() && !this.dialog.openDialogs.length) this.selected = null;
  }

  // ---------- glisser-déposer ----------

  @HostListener('dragenter', ['$event'])
  onDragEnter(e: DragEvent): void {
    if (!this.canImport || !hasFiles(e)) return;
    e.preventDefault();
    this.dragDepth++;
    this.dragging = true;
  }

  @HostListener('dragover', ['$event'])
  onDragOver(e: DragEvent): void {
    if (!this.canImport || !hasFiles(e)) return;
    e.preventDefault();
    if (e.dataTransfer) e.dataTransfer.dropEffect = 'copy';
  }

  @HostListener('dragleave', ['$event'])
  onDragLeave(e: DragEvent): void {
    if (!this.canImport || !hasFiles(e)) return;
    this.dragDepth = Math.max(0, this.dragDepth - 1);
    if (this.dragDepth === 0) this.dragging = false;
  }

  @HostListener('drop', ['$event'])
  onDrop(e: DragEvent): void {
    if (!this.canImport || !hasFiles(e)) return;
    e.preventDefault();
    this.dragDepth = 0;
    this.dragging = false;
    const files = Array.from(e.dataTransfer?.files ?? []);
    if (files.length) this.openImport(files);
  }
}

/** La fiche tient à côté de la liste (au-delà de 1200 px, comme la feuille de style). */
function panelBesideList(): boolean {
  return typeof window === 'undefined' || !window.matchMedia || window.matchMedia('(min-width: 1201px)').matches;
}

function hasFiles(e: DragEvent): boolean {
  return !!e.dataTransfer && Array.from(e.dataTransfer.types ?? []).includes('Files');
}
