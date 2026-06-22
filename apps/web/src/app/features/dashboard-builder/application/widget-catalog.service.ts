/**
 * Catalogue des widgets de la palette + options KPI pour le panneau de config.
 * Application layer — sans dépendance Angular au routage/HTTP.
 *
 * Les libellés sont marqués pour l'extraction i18n via $localize : ils sont
 * affichés tels quels dans la palette (CLAUDE.md §15.1, §7.3).
 */
import { Injectable } from '@angular/core';

import {
  KpiOption,
  Widget,
  WidgetCatalogEntry,
  WidgetType
} from '../domain/dashboard.model';

@Injectable()
export class WidgetCatalogService {

  /**
   * Définitions de la palette. L'ordre est l'ordre d'affichage.
   * Réutilise les composants de viz existants (ECharts via qos-echart).
   */
  private readonly catalog: ReadonlyArray<WidgetCatalogEntry> = [
    {
      type: 'kpi',
      label: $localize`:@@dbb.widget.kpi:Indicateur KPI`,
      icon: 'speed',
      description: $localize`:@@dbb.widget.kpi.desc:Valeur unique avec tendance et seuil`,
      defaultCols: 3,
      defaultRows: 3,
      defaultConfig: { kpiId: 'capa_closure_time_avg', kpiLabel: 'Délai clôture CAPA', unit: 'j', threshold: 30 }
    },
    {
      type: 'line',
      label: $localize`:@@dbb.widget.line:Courbe`,
      icon: 'show_chart',
      description: $localize`:@@dbb.widget.line.desc:Tendance temporelle (ECharts)`,
      defaultCols: 5,
      defaultRows: 4,
      defaultConfig: { kpiId: 'quality_trend' }
    },
    {
      type: 'bar',
      label: $localize`:@@dbb.widget.bar:Histogramme`,
      icon: 'bar_chart',
      description: $localize`:@@dbb.widget.bar.desc:Comparaison par catégorie`,
      defaultCols: 5,
      defaultRows: 4,
      defaultConfig: { kpiId: 'nc_by_category' }
    },
    {
      type: 'pie',
      label: $localize`:@@dbb.widget.pie:Camembert`,
      icon: 'pie_chart',
      description: $localize`:@@dbb.widget.pie.desc:Répartition proportionnelle`,
      defaultCols: 4,
      defaultRows: 4,
      defaultConfig: { kpiId: 'nc_by_category' }
    },
    {
      type: 'gauge',
      label: $localize`:@@dbb.widget.gauge:Jauge`,
      icon: 'gauge',
      description: $localize`:@@dbb.widget.gauge.desc:Atteinte d'une cible`,
      defaultCols: 3,
      defaultRows: 4,
      defaultConfig: { kpiId: 'alignment_rate', unit: '%' }
    },
    {
      type: 'control-chart',
      label: $localize`:@@dbb.widget.control:Carte de contrôle`,
      icon: 'timeline',
      description: $localize`:@@dbb.widget.control.desc:SPC avec limites UCL/LCL`,
      defaultCols: 6,
      defaultRows: 4,
      defaultConfig: { kpiId: 'spc_process' }
    },
    {
      type: 'table',
      label: $localize`:@@dbb.widget.table:Tableau`,
      icon: 'table_chart',
      description: $localize`:@@dbb.widget.table.desc:Liste dense de valeurs`,
      defaultCols: 5,
      defaultRows: 4,
      defaultConfig: { kpiId: 'nc_by_category' }
    },
    {
      type: 'heatmap',
      label: $localize`:@@dbb.widget.heatmap:Heatmap`,
      icon: 'grid_on',
      description: $localize`:@@dbb.widget.heatmap.desc:Matrice de conformité`,
      defaultCols: 6,
      defaultRows: 4,
      defaultConfig: { kpiId: 'compliance_heatmap' }
    },
    {
      type: 'narrative',
      label: $localize`:@@dbb.widget.narrative:Récit IA`,
      icon: 'auto_awesome',
      description: $localize`:@@dbb.widget.narrative.desc:Texte / storyboard généré`,
      defaultCols: 4,
      defaultRows: 3,
      defaultConfig: { text: '' }
    }
  ];

  /**
   * Catalogue KPI (extrait CLAUDE.md §6). Tout KPI affiché a une définition
   * formelle (id + libellé + unité) — invariant §18.2.12.
   */
  private readonly kpiOptions: ReadonlyArray<KpiOption> = [
    { id: 'capa_closure_time_avg', label: $localize`:@@dbb.kpi.capa:Délai moyen de clôture CAPA`, unit: 'j' },
    { id: 'capa_recurrence_rate', label: $localize`:@@dbb.kpi.recurrence:Taux de récidive CAPA`, unit: '%' },
    { id: 'nc_rate', label: $localize`:@@dbb.kpi.ncrate:Taux de non-conformités`, unit: 'ppm' },
    { id: 'dpmo', label: $localize`:@@dbb.kpi.dpmo:DPMO`, unit: '' },
    { id: 'fpy', label: $localize`:@@dbb.kpi.fpy:First Pass Yield`, unit: '%' },
    { id: 'coq', label: $localize`:@@dbb.kpi.coq:Coût d'obtention de la qualité`, unit: '%' },
    { id: 'pdca_progress', label: $localize`:@@dbb.kpi.pdca:Avancement cycles PDCA`, unit: '%' },
    { id: 'fives_score', label: $localize`:@@dbb.kpi.fives:Score 5S moyen`, unit: '/5' },
    { id: 'cpk', label: $localize`:@@dbb.kpi.cpk:Capabilité Cpk`, unit: '' },
    { id: 'alignment_rate', label: $localize`:@@dbb.kpi.alignment:Taux d'alignement normatif`, unit: '%' },
    { id: 'audit_completion', label: $localize`:@@dbb.kpi.audit:Audits réalisés vs planifiés`, unit: '%' },
    { id: 'supplier_score', label: $localize`:@@dbb.kpi.supplier:Score qualité fournisseurs`, unit: '%' }
  ];

  /** Renvoie la palette complète. */
  entries(): ReadonlyArray<WidgetCatalogEntry> {
    return this.catalog;
  }

  /** Cherche la définition d'un type donné. */
  entryFor(type: WidgetType): WidgetCatalogEntry | undefined {
    return this.catalog.find(e => e.type === type);
  }

  /** Renvoie le catalogue d'options KPI pour le panneau de configuration. */
  kpis(): ReadonlyArray<KpiOption> {
    return this.kpiOptions;
  }

  /** Libellé KPI à partir de son id (fallback : l'id brut). */
  kpiLabel(id: string | undefined): string {
    if (!id) return '';
    return this.kpiOptions.find(k => k.id === id)?.label ?? id;
  }

  /**
   * Crée un nouveau widget à partir d'un type de la palette, positionné en
   * (x, y). La taille et la config initiales viennent du catalogue.
   */
  createWidget(type: WidgetType, id: string, x: number, y: number): Widget {
    const entry = this.entryFor(type);
    const cols = entry?.defaultCols ?? 3;
    const rows = entry?.defaultRows ?? 3;
    const config = entry ? { ...entry.defaultConfig } : {};
    return {
      id,
      type,
      title: entry?.label ?? type,
      position: { x, y, cols, rows },
      config
    };
  }
}
