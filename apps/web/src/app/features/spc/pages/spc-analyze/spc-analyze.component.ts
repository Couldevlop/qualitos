import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';

import { SpcService } from '../../spc.service';
import { KpiOption, KpiSpcResponse, SpcAnalyzeResponse } from '../../spc.types';

/**
 * Carte de contrôle SPC (§3.4, §12.1) : l'utilisateur saisit une série de mesures
 * (et, en option, une baseline center/σ connue) ; l'IA calcule les limites de contrôle
 * (carte des valeurs individuelles, σ = MR̄/d2) et applique les 8 règles de Nelson.
 * La carte met en évidence UCL/LCL/ligne centrale et les points hors-contrôle.
 */
@Component({
  selector: 'qos-spc-analyze',
  templateUrl: './spc-analyze.component.html',
  styleUrls: ['./spc-analyze.component.scss'],
  standalone: false
})
export class SpcAnalyzeComponent implements OnInit {

  readonly form = this.fb.group({
    valuesText: ['', [Validators.required]],
    center: [null as number | null],
    sigma: [null as number | null]
  });

  /** Mode « depuis un KPI » : sélection du KPI, fenêtre, et ouverture CAPA. */
  readonly kpiForm = this.fb.group({
    kpiId: ['', [Validators.required]],
    limit: [30, [Validators.min(2), Validators.max(10000)]],
    openCapa: [false]
  });

  /** Série de démonstration : process stable avec un point aberrant en fin de série. */
  readonly example = '10.0, 10.2, 9.8, 10.1, 9.9, 10.05, 9.95, 10.0, 10.1, 9.9, 13.6';

  kpis: KpiOption[] = [];

  loading = false;
  result: SpcAnalyzeResponse | null = null;
  error: string | null = null;
  chartOption: EChartsCoreOption | null = null;

  /** Contexte KPI courant (libellé + CAPA éventuelle) quand l'analyse vient d'un KPI. */
  kpiContext: { code: string; name: string; capaId?: string | null } | null = null;

  constructor(private readonly fb: FormBuilder, private readonly spc: SpcService) {}

  ngOnInit(): void {
    this.spc.listKpis().subscribe({
      next: list => this.kpis = list,
      error: () => { /* le mode saisie manuelle reste utilisable */ }
    });
  }

  loadExample(): void {
    this.form.patchValue({ valuesText: this.example, center: null, sigma: null });
  }

  /** Découpe le texte libre (virgules, points-virgules, espaces, retours) en nombres. */
  private parseValues(): number[] {
    const raw = this.form.value.valuesText ?? '';
    return raw
      .split(/[\s,;]+/)
      .map(t => t.trim())
      .filter(t => t.length > 0)
      .map(Number)
      .filter(n => Number.isFinite(n));
  }

  analyze(): void {
    if (this.loading) {
      return;
    }
    const values = this.parseValues();
    if (values.length === 0) {
      this.error = $localize`:@@spc.analyze.err-no-measure:Saisissez au moins une mesure numérique.`;
      return;
    }
    const center = this.form.value.center;
    const sigma = this.form.value.sigma;
    // Baseline : center et sigma doivent être fournis ensemble (sinon limites estimées).
    if ((center == null) !== (sigma == null)) {
      this.error = $localize`:@@spc.analyze.err-baseline-both:Renseignez la ligne centrale et σ ensemble, ou laissez les deux vides.`;
      return;
    }
    if (sigma != null && sigma <= 0) {
      this.error = $localize`:@@spc.analyze.err-sigma-positive:σ doit être strictement positif.`;
      return;
    }

    this.beginRun();
    this.spc.analyze({
      values,
      center: center ?? undefined,
      sigma: sigma ?? undefined
    }).subscribe({
      next: res => {
        this.result = res;
        this.chartOption = this.buildChart(res, values);
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.messageFor(err);
      }
    });
  }

  /** Analyse SPC du KPI sélectionné (série tirée de kpi_measurements). */
  analyzeKpi(): void {
    if (this.loading) {
      return;
    }
    const kpiId = this.kpiForm.value.kpiId;
    if (!kpiId) {
      this.error = $localize`:@@spc.analyze.err-select-kpi:Sélectionnez un KPI.`;
      return;
    }
    const limit = this.kpiForm.value.limit ?? 30;
    const openCapa = !!this.kpiForm.value.openCapa;

    this.beginRun();
    this.spc.analyzeKpi(kpiId, limit, openCapa).subscribe({
      next: res => {
        this.result = res.analysis;
        this.kpiContext = { code: res.kpiCode, name: res.kpiName, capaId: res.capaId };
        this.chartOption = this.buildChart(res.analysis, res.values, res.periods.map(p => this.shortDate(p)));
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = err.status === 422
          ? $localize`:@@spc.analyze.err-min-two:Au moins 2 mesures sont nécessaires pour ce KPI.`
          : this.messageFor(err);
      }
    });
  }

  /** Réinitialise l'état avant un nouveau calcul (manuel ou KPI). */
  private beginRun(): void {
    this.loading = true;
    this.error = null;
    this.result = null;
    this.chartOption = null;
    this.kpiContext = null;
  }

  /** ISO → libellé d'axe court (jj/mm). */
  private shortDate(iso: string): string {
    const d = new Date(iso);
    return Number.isNaN(d.getTime())
      ? iso
      : d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' });
  }

  /** Indices (0-based) impliqués dans au moins une violation → points hors-contrôle. */
  private outOfControlIndices(res: SpcAnalyzeResponse): Set<number> {
    const s = new Set<number>();
    // Garde défensive : un contrat partiel (violations/limits absents) ne doit
    // pas faire planter le rendu ECharts.
    (res.violations ?? []).forEach(v => (v.pointIndices ?? []).forEach(i => s.add(i)));
    return s;
  }

  private buildChart(res: SpcAnalyzeResponse, values: number[], labels?: string[]): EChartsCoreOption {
    const ooc = this.outOfControlIndices(res);
    const categories = labels && labels.length === values.length
      ? labels
      : values.map((_, i) => `${i + 1}`);
    const oocPoints = values
      .map((v, i) => [i, v] as [number, number])
      .filter(([i]) => ooc.has(i));
    const L = res.limits;
    // markLine seulement si les limites sont présentes (contrat complet).
    const markLine = L ? {
      symbol: 'none' as const,
      data: [
        { yAxis: L.ucl, name: 'UCL', lineStyle: { color: '#DC2626', type: 'dashed' as const },
          label: { formatter: `UCL ${L.ucl.toFixed(2)}`, color: '#DC2626', position: 'end' as const } },
        { yAxis: L.centerLine, name: 'CL', lineStyle: { color: '#059669' },
          label: { formatter: `CL ${L.centerLine.toFixed(2)}`, color: '#059669', position: 'end' as const } },
        { yAxis: L.lcl, name: 'LCL', lineStyle: { color: '#DC2626', type: 'dashed' as const },
          label: { formatter: `LCL ${L.lcl.toFixed(2)}`, color: '#DC2626', position: 'end' as const } }
      ]
    } : undefined;
    const measures = $localize`:@@spc.analyze.chart-measures:Mesures`;
    const outOfControl = $localize`:@@spc.analyze.chart-ooc:Hors contrôle`;
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: [measures, outOfControl], top: 0 },
      xAxis: { type: 'category', data: categories, name: $localize`:@@spc.analyze.chart-point:Point`, boundaryGap: false },
      yAxis: { type: 'value', name: $localize`:@@spc.analyze.chart-value:Valeur`, scale: true },
      series: [
        {
          name: measures,
          type: 'line',
          smooth: false,
          symbol: 'circle',
          symbolSize: 6,
          data: values,
          lineStyle: { width: 2 },
          ...(markLine ? { markLine } : {})
        },
        {
          name: outOfControl,
          type: 'scatter',
          data: oocPoints,
          symbolSize: 13,
          itemStyle: { color: '#DC2626' },
          z: 5
        }
      ]
    };
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@spc.analyze.err-backend:Le serveur ne répond pas. Vérifiez votre connexion, puis relancez l'analyse.`;
      case 400: return $localize`:@@spc.analyze.err-invalid-series:La série doit compter de 1 à 10 000 mesures.`;
      case 413: return $localize`:@@spc.analyze.err-too-large:La série est trop longue pour être analysée en une fois : réduisez-la.`;
      case 429: return $localize`:@@spc.analyze.err-quota:Trop d'analyses en peu de temps : réessayez dans une minute.`;
      case 502: return $localize`:@@spc.analyze.err-gateway:L'analyse est momentanément indisponible. Réessayez dans quelques minutes.`;
      case 503: return $localize`:@@spc.analyze.err-unavailable:L'analyse est momentanément indisponible. Réessayez dans quelques minutes.`;
      default:  return $localize`:@@spc.analyze.err-generic:L'analyse a échoué (erreur ${err.status}:status:). Réessayez.`;
    }
  }

  /** Libellé de l'état du procédé (carte verdict). */
  processStateValue(outOfControl: boolean): string {
    return outOfControl
      ? $localize`:@@spc.analyze.state-out:Hors contrôle`
      : $localize`:@@spc.analyze.state-in:Sous contrôle`;
  }

  /** Origine de la ligne centrale (estimée vs baseline fournie). */
  estimatedDesc(estimated: boolean): string {
    return estimated
      ? $localize`:@@spc.analyze.center-estimated:Estimée depuis la série`
      : $localize`:@@spc.analyze.center-baseline:Valeur fournie`;
  }

  /** Sous-texte de l'état du procédé : nombre de règles déclenchées et de points. */
  stateDescription(rules: number, points: number): string {
    return rules === 0
      ? $localize`:@@spc.analyze.state-desc-none:Aucune règle de Nelson déclenchée · ${points}:points: points`
      : $localize`:@@spc.analyze.state-desc:${rules}:rules: règle(s) de Nelson déclenchée(s) · ${points}:points: points`;
  }

  /** Libellé de la sévérité renvoyée par le service, dans la langue de l'écran. */
  severityLabel(sev: string): string {
    const c = this.severityClass(sev);
    return c === 'sev-crit' ? $localize`:@@spc.analyze.sev-critical:Critique`
      : c === 'sev-maj' ? $localize`:@@spc.analyze.sev-major:Majeure`
      : $localize`:@@spc.analyze.sev-minor:Mineure`;
  }

  severityClass(sev: string): string {
    return sev === 'CRITICAL' || sev === 'high' ? 'sev-crit'
      : sev === 'MAJOR' || sev === 'medium' ? 'sev-maj' : 'sev-min';
  }
}
