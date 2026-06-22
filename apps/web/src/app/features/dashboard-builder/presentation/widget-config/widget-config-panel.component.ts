/**
 * Panneau de configuration d'un widget (source KPI, type, options).
 * Émet un widget mis à jour (immuable) — l'éditeur applique le changement.
 */
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

import { WidgetCatalogService } from '../../application/widget-catalog.service';
import { KpiOption, Widget, WidgetType } from '../../domain/dashboard.model';

@Component({
  selector: 'qos-widget-config-panel',
  templateUrl: './widget-config-panel.component.html',
  styleUrls: ['./widget-config-panel.component.scss'],
  standalone: false
})
export class WidgetConfigPanelComponent implements OnChanges {
  @Input() widget: Widget | null = null;

  @Output() widgetChange = new EventEmitter<Widget>();
  @Output() closed = new EventEmitter<void>();
  @Output() removed = new EventEmitter<string>();

  readonly kpiOptions: ReadonlyArray<KpiOption>;

  /** Modèle local éditable (copie de travail). */
  title = '';
  kpiId = '';
  kpiLabel = '';
  unit = '';
  threshold: number | null = null;
  text = '';

  constructor(private readonly catalog: WidgetCatalogService) {
    this.kpiOptions = this.catalog.kpis();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['widget'] && this.widget) {
      this.title = this.widget.title;
      this.kpiId = this.widget.config.kpiId ?? '';
      this.kpiLabel = this.widget.config.kpiLabel ?? '';
      this.unit = this.widget.config.unit ?? '';
      this.threshold = typeof this.widget.config.threshold === 'number' ? this.widget.config.threshold : null;
      this.text = this.widget.config.text ?? '';
    }
  }

  get configLabel(): string {
    return $localize`:@@dbb.config.aria.label:Panneau de configuration du widget`;
  }

  get type(): WidgetType | null {
    return this.widget?.type ?? null;
  }

  get isKpi(): boolean {
    return this.type === 'kpi';
  }

  get isNarrative(): boolean {
    return this.type === 'narrative';
  }

  get isDataDriven(): boolean {
    return this.type != null && this.type !== 'narrative';
  }

  /** Quand l'utilisateur choisit un KPI : pré-remplit le libellé si vide. */
  onKpiSelected(id: string): void {
    this.kpiId = id;
    if (!this.kpiLabel.trim()) {
      this.kpiLabel = this.catalog.kpiLabel(id);
    }
    const unit = this.kpiOptions.find(k => k.id === id)?.unit;
    if (unit && !this.unit.trim()) {
      this.unit = unit;
    }
    this.apply();
  }

  /** Reconstruit un widget immuable et l'émet. */
  apply(): void {
    if (!this.widget) return;
    const config = { ...this.widget.config };
    if (this.isDataDriven) {
      config.kpiId = this.kpiId || undefined;
    }
    if (this.isKpi) {
      config.kpiLabel = this.kpiLabel || undefined;
      config.unit = this.unit || undefined;
      config.threshold = this.threshold == null ? undefined : this.threshold;
    }
    if (this.isNarrative) {
      config.text = this.text;
    }
    this.widgetChange.emit({
      ...this.widget,
      title: this.title.trim() || this.widget.title,
      config
    });
  }

  close(): void {
    this.closed.emit();
  }

  remove(): void {
    if (this.widget) {
      this.removed.emit(this.widget.id);
    }
  }
}
