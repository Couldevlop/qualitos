import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';

import { NlqAskResponse } from '../../../nlq/nlq.types';
import { NlqExploreService } from '../../nlq-explore.service';
import { NlqChartPlan, NlqChartType } from '../../nlq-explore.types';

/**
 * Exploration NLQ → graphique (CLAUDE.md §7.3) : « Natural Language Query →
 * réponse + graphique généré ». L'utilisateur pose une question en langage
 * naturel ; l'IA (text-to-SQL, côté engine) renvoie des lignes ; la page déduit
 * un graphique ECharts (catégories = 1re colonne texte, série = colonne numérique)
 * ET affiche la table des lignes. Le type de graphe (barres/lignes) est ajustable.
 */
@Component({
  selector: 'qos-nlq-explore',
  templateUrl: './nlq-explore.component.html',
  styleUrls: ['./nlq-explore.component.scss'],
  standalone: false
})
export class NlqExploreComponent {

  readonly form = this.fb.group({
    question: ['', [Validators.required, Validators.maxLength(500)]]
  });

  readonly examples: string[] = [
    $localize`:@@nlq-explore.example-1:Combien de CAPA par statut ?`,
    $localize`:@@nlq-explore.example-2:Nombre de diagrammes Ishikawa par statut`,
    $localize`:@@nlq-explore.example-3:Combien de CAPA par criticité ?`,
    $localize`:@@nlq-explore.example-4:Nombre de fournisseurs par statut`
  ];

  loading = false;
  result: NlqAskResponse | null = null;
  error: string | null = null;
  showSql = false;

  /** Type de graphe sélectionné par l'utilisateur (défaut : barres). */
  chartType: NlqChartType = 'bar';

  plan: NlqChartPlan = { graphable: false, categoryColumn: null, valueColumns: [] };
  chartOption: EChartsCoreOption | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly explore: NlqExploreService
  ) {}

  ask(preset?: string): void {
    if (preset) {
      this.form.patchValue({ question: preset });
    }
    const question = (this.form.value.question ?? '').trim();
    if (!question || this.loading) {
      return;
    }
    this.loading = true;
    this.error = null;
    this.result = null;
    this.showSql = false;
    this.chartOption = null;
    this.plan = { graphable: false, categoryColumn: null, valueColumns: [] };

    this.explore.ask(question).subscribe({
      next: res => {
        this.result = res;
        this.plan = this.explore.buildPlan(res.rows);
        this.chartOption = this.plan.graphable ? this.buildChart() : null;
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.messageFor(err);
      }
    });
  }

  /** Bascule barres/lignes et recalcule le graphe sans nouvel appel. */
  selectChartType(type: NlqChartType): void {
    if (this.chartType === type) {
      return;
    }
    this.chartType = type;
    if (this.plan.graphable) {
      this.chartOption = this.buildChart();
    }
  }

  /** Colonnes dérivées de la première ligne (varient selon la requête). */
  columns(): string[] {
    const rows = this.result?.rows ?? [];
    return rows.length ? Object.keys(rows[0]) : [];
  }

  cell(value: unknown): string {
    if (value === null || value === undefined) {
      return '—';
    }
    return String(value);
  }

  confidencePct(): number {
    return Math.round((this.result?.confidence ?? 0) * 100);
  }

  confidenceClass(): string {
    const c = this.result?.confidence ?? 0;
    return c >= 0.8 ? 'ok' : c >= 0.5 ? 'warn' : 'bad';
  }

  tenantFilterLabel(): string {
    return this.result?.tenantFilterApplied
      ? $localize`:@@nlq-explore.tenant-filtered:Filtré par tenant`
      : $localize`:@@nlq-explore.tenant-unfiltered:Tenant non filtré`;
  }

  sqlToggleLabel(): string {
    return this.showSql
      ? $localize`:@@nlq-explore.hide-sql:Masquer le SQL`
      : $localize`:@@nlq-explore.show-sql:Voir le SQL généré`;
  }

  /** Construit l'option ECharts à partir du plan déduit et du type choisi. */
  private buildChart(): EChartsCoreOption {
    const rows = this.result?.rows ?? [];
    const cat = this.plan.categoryColumn;
    const categories = cat
      ? rows.map(r => this.cell(r[cat]))
      : rows.map((_r, i) => `${i + 1}`);

    const series = this.plan.valueColumns.map(col => ({
      name: col,
      type: this.chartType,
      smooth: this.chartType === 'line',
      symbol: 'circle',
      symbolSize: 6,
      data: rows.map(r => {
        const v = r[col];
        return typeof v === 'number' && Number.isFinite(v) ? v : null;
      })
    }));

    return {
      tooltip: { trigger: 'axis' },
      legend: { data: this.plan.valueColumns, top: 0 },
      grid: { left: 48, right: 24, top: 36, bottom: 48, containLabel: true },
      xAxis: {
        type: 'category',
        data: categories,
        boundaryGap: this.chartType === 'bar',
        axisLabel: { interval: 0, rotate: categories.length > 8 ? 30 : 0 }
      },
      yAxis: { type: 'value', scale: true },
      series
    };
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0:   return $localize`:@@nlq-explore.err-backend:Backend injoignable (engine sur 8082 ?).`;
      case 400:
      case 422: return $localize`:@@nlq-explore.err-untranslatable:La question n'a pas pu être traduite en requête sûre. Reformule-la plus simplement.`;
      case 413: return $localize`:@@nlq-explore.err-too-large:Question trop volumineuse (garde-fou IA).`;
      case 429: return $localize`:@@nlq-explore.err-quota:Débit/quota IA dépassé pour ce tenant — réessayez plus tard.`;
      case 502:
      case 503: return $localize`:@@nlq-explore.err-unavailable:L'assistant IA est momentanément indisponible (modèle en cours de chargement ?). Réessaie dans un instant.`;
      default:  return $localize`:@@nlq-explore.err-generic:Une erreur est survenue lors du traitement de la question (HTTP ${err.status}:status:).`;
    }
  }
}
