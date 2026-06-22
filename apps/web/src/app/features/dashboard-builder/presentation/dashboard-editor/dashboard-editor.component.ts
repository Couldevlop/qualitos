/**
 * Éditeur de dashboard drag &amp; drop avancé (CLAUDE.md §7.3).
 *
 * Grille interactive angular-gridster2 :
 *  - déplacement (drag), redimensionnement (resize) et réorganisation des
 *    widgets, responsive (mobile : empilement) ;
 *  - palette : ajout par glisser-déposer (HTML5 drag) ou clic ;
 *  - configuration par widget via panneau latéral.
 *
 * Persistance : positions / tailles / config sérialisées dans layout_json
 * (jsonb) par le repository → api-quality-engine (tenant_id du JWT, jamais body).
 */
import { Component, OnDestroy, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import {
  CompactType,
  DisplayGrid,
  GridsterConfig,
  GridsterItem,
  GridType
} from 'angular-gridster2';
import { Subject, takeUntil } from 'rxjs';

import { DashboardBuilderService } from '../../application/dashboard-builder.service';
import { WidgetCatalogService } from '../../application/widget-catalog.service';
import {
  DashboardLayout,
  Widget,
  WidgetCatalogEntry,
  WidgetType
} from '../../domain/dashboard.model';

/** Item gridster décoré du widget métier (position synchronisée par gridster). */
interface GridItem extends GridsterItem {
  widget: Widget;
}

@Component({
  selector: 'qos-dashboard-editor',
  templateUrl: './dashboard-editor.component.html',
  styleUrls: ['./dashboard-editor.component.scss'],
  standalone: false
})
export class DashboardEditorComponent implements OnInit, OnDestroy {

  layout!: DashboardLayout;
  loading = false;
  saving = false;
  exporting = false;
  isNew = true;

  /** Items affichés dans la grille (vue mutable synchronisée avec gridster). */
  items: GridItem[] = [];

  /** Widget en cours de configuration (panneau ouvert si non null). */
  selected: Widget | null = null;

  readonly palette: ReadonlyArray<WidgetCatalogEntry>;
  options!: GridsterConfig;

  /** Type en cours de glissement depuis la palette (HTML5 DnD). */
  private dragType: WidgetType | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly svc: DashboardBuilderService,
    private readonly catalog: WidgetCatalogService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snack: MatSnackBar
  ) {
    this.palette = this.catalog.entries();
  }

  ngOnInit(): void {
    this.configureGrid();
    this.loading = true;
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        const id = params.get('id');
        if (id) {
          this.isNew = false;
          this.svc.get(id).subscribe({
            next: l => { this.setLayout(l); this.loading = false; },
            error: () => {
              this.loading = false;
              this.snack.open($localize`:@@dbb.editor.notFound:Tableau de bord introuvable`, 'OK');
            }
          });
        } else {
          this.setLayout(this.svc.newDefaultLayout($localize`:@@dbb.editor.newName:Nouveau tableau de bord`));
          this.loading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /* ============================================================
   * Configuration gridster.
   * ============================================================ */

  private configureGrid(): void {
    this.options = {
      gridType: GridType.VerticalFixed,
      fixedRowHeight: 84,
      compactType: CompactType.None,
      displayGrid: DisplayGrid.OnDragAndResize,
      margin: 14,
      minCols: 12,
      maxCols: 12,
      minRows: 6,
      mobileBreakpoint: 640,
      keepFixedHeightInMobile: false,
      draggable: {
        enabled: true,
        ignoreContent: true,
        dragHandleClass: 'drag-handle'
      },
      resizable: { enabled: true },
      pushItems: true,
      swap: false,
      enableEmptyCellDrop: true,
      emptyCellDropCallback: (event: DragEvent, item: GridsterItem) =>
        this.onEmptyCellDrop(event, item),
      itemChangeCallback: () => this.syncPositions()
    };
  }

  /** Force gridster à recharger sa configuration après une mutation externe. */
  private refreshGrid(): void {
    if (this.options.api?.optionsChanged) {
      this.options.api.optionsChanged();
    }
  }

  /* ============================================================
   * Chargement / mapping.
   * ============================================================ */

  private setLayout(layout: DashboardLayout): void {
    this.layout = layout;
    this.items = layout.widgets.map(w => this.toItem(w));
  }

  private toItem(w: Widget): GridItem {
    return {
      x: w.position.x,
      y: w.position.y,
      cols: w.position.cols,
      rows: w.position.rows,
      widget: w
    };
  }

  /** Reconstruit le layout immuable à partir des items (positions à jour). */
  private syncPositions(): void {
    const widgets: Widget[] = this.items.map(it => ({
      ...it.widget,
      position: { x: it.x, y: it.y, cols: it.cols, rows: it.rows }
    }));
    this.layout = { ...this.layout, widgets };
  }

  /* ============================================================
   * Palette — drag &amp; drop + clic.
   * ============================================================ */

  onPaletteDragStart(event: DragEvent, entry: WidgetCatalogEntry): void {
    this.dragType = entry.type;
    if (event.dataTransfer) {
      event.dataTransfer.setData('text/plain', entry.type);
      event.dataTransfer.effectAllowed = 'copy';
    }
  }

  onPaletteDragEnd(): void {
    this.dragType = null;
  }

  /** Drop dans une cellule vide → crée le widget à la position visée. */
  private onEmptyCellDrop(event: DragEvent, item: GridsterItem): void {
    const type = (event.dataTransfer?.getData('text/plain') as WidgetType) || this.dragType;
    if (!type) return;
    const widget = this.catalog.createWidget(type, this.svc.generateId(), item.x, item.y);
    this.addItem(widget);
    this.dragType = null;
  }

  /** Ajout par clic (accessibilité clavier) — placé en bas de grille. */
  addWidget(type: WidgetType): void {
    const y = this.items.reduce((max, it) => Math.max(max, it.y + it.rows), 0);
    const widget = this.catalog.createWidget(type, this.svc.generateId(), 0, y);
    this.addItem(widget);
    this.snack.open(
      $localize`:@@dbb.editor.added:Widget ajouté`, 'OK', { duration: 1500 });
  }

  private addItem(widget: Widget): void {
    this.items = [...this.items, this.toItem(widget)];
    this.syncPositions();
    this.selected = widget;
  }

  /* ============================================================
   * Sélection / configuration / suppression.
   * ============================================================ */

  select(item: GridItem): void {
    this.selected = item.widget;
  }

  onWidgetConfigured(updated: Widget): void {
    this.items = this.items.map(it =>
      it.widget.id === updated.id ? { ...it, widget: updated } : it);
    this.selected = updated;
    this.syncPositions();
  }

  closeConfig(): void {
    this.selected = null;
  }

  removeWidget(id: string): void {
    this.items = this.items.filter(it => it.widget.id !== id);
    if (this.selected?.id === id) {
      this.selected = null;
    }
    this.syncPositions();
  }

  /* ============================================================
   * Métadonnées du dashboard + sauvegarde.
   * ============================================================ */

  onNameChange(value: string): void {
    this.layout = { ...this.layout, name: value };
  }

  onSharedChange(value: boolean): void {
    this.layout = { ...this.layout, shared: value };
  }

  save(): void {
    this.syncPositions();
    this.saving = true;
    const op$ = this.isNew && !this.layout.id
      ? this.svc.save(this.layout)
      : this.svc.update(this.layout.id!, this.layout);
    op$.subscribe({
      next: saved => {
        this.setLayout(saved);
        this.refreshGrid();
        this.isNew = false;
        this.saving = false;
        this.snack.open(
          $localize`:@@dbb.editor.saved:Tableau de bord enregistré`, 'OK', { duration: 2500 });
      },
      error: () => {
        this.saving = false;
        this.snack.open(
          $localize`:@@dbb.editor.saveFailed:Échec de l'enregistrement`, 'OK', { duration: 3500 });
      }
    });
  }

  /**
   * Export the current dashboard as a signed (ML-DSA) + blockchain-anchored PDF
   * with a verification QR code (§7.3/§7.4), then trigger a browser download.
   */
  exportPdf(): void {
    if (!this.layout.id) {
      this.snack.open('Enregistrez le tableau de bord avant de l\'exporter.', 'OK', { duration: 3500 });
      return;
    }
    this.exporting = true;
    this.svc.exportPdf(this.layout).subscribe({
      next: result => {
        this.triggerDownload(result.blob, result.fileName);
        this.exporting = false;
        this.snack.open(
          `PDF signé exporté (code ${result.verificationCode}).`, 'OK', { duration: 4000 });
      },
      error: () => {
        this.exporting = false;
        this.snack.open('Échec de l\'export PDF.', 'OK', { duration: 3500 });
      }
    });
  }

  private triggerDownload(blob: Blob, fileName: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    a.click();
    URL.revokeObjectURL(url);
  }

  back(): void {
    this.router.navigate(['../'], { relativeTo: this.route });
  }

  trackByItem(_: number, it: GridItem): string {
    return it.widget.id;
  }

  trackByEntry(_: number, e: WidgetCatalogEntry): WidgetType {
    return e.type;
  }
}
