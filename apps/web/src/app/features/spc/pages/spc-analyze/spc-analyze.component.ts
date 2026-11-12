import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, Validators } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';
import { Observable } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SpcService } from '../../spc.service';
import {
  KpiOption,
  KpiSpcForecastResponse,
  KpiSpcResponse,
  SpcAnalyzeResponse,
  SpcForecastResponse
} from '../../spc.types';

/**
 * Quatre mesures : le minimum pour qu'une tendance soit autre chose qu'un trait
 * entre deux points. En dessous, le serveur refuse — l'écran le dit avant l'appel.
 */
const MIN_FORECAST_POINTS = 4;

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

  /** Horizon de prévision, en périodes (§6.5). */
  readonly forecastForm = this.fb.group({
    horizon: [6, [Validators.required, Validators.min(1), Validators.max(60)]],
    openCapa: [false]
  });

  forecast: SpcForecastResponse | null = null;
  forecastLoading = false;
  forecastError: string | null = null;
  /** CAPA préventive ouverte par la prévision (distincte de celle de la carte). */
  forecastCapaId: string | null = null;

  /**
   * Série du dernier calcul, conservée pour que la prévision reparte EXACTEMENT
   * des mêmes mesures et des mêmes limites que la carte affichée : une prévision
   * qui parlerait d'une autre série n'expliquerait pas la courbe sous les yeux.
   */
  private lastValues: number[] = [];
  private lastLabels?: string[];
  private lastKpi: { id: string; limit: number } | null = null;

  /**
   * Qui peut faire ouvrir un dossier : le serveur tranche (capa.create) ; l'ecran
   * n'affiche que ce qui servira.
   *
   * <p>LIRE la carte ne demande rien -- savoir si un procede est sous controle
   * n'est le secret de personne. Lui faire OUVRIR un dossier CAPA, si : c'est le
   * meme droit que sur la fiche CAPA, et la case a cocher ne se propose donc qu'a
   * qui l'obtiendra.
   */
  canOpenCapa = false;

  constructor(private readonly fb: FormBuilder, private readonly spc: SpcService,
              authz: AuthzService) {
    authz.can('capa.create').pipe(takeUntilDestroyed()).subscribe(v => {
      this.canOpenCapa = !!v;
      if (!v) {
        // Un formulaire qui garde la case cochee enverrait openCapa=true depuis
        // un ecran qui ne la montre plus : le serveur refuserait la lecture de
        // la carte pour une case invisible.
        this.kpiForm.patchValue({ openCapa: false });
        this.forecastForm.patchValue({ openCapa: false });
      }
    });
  }

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
        this.lastValues = values;
        this.lastLabels = undefined;
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
        this.lastValues = res.values;
        this.lastLabels = res.periods.map(p => this.shortDate(p));
        this.lastKpi = { id: kpiId, limit };
        this.chartOption = this.buildChart(res.analysis, res.values, this.lastLabels);
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
    this.lastKpi = null;
    this.lastValues = [];
    this.lastLabels = undefined;
    this.forecast = null;
    this.forecastError = null;
    this.forecastCapaId = null;
  }

  /** Vrai dès qu'une série est chargée et assez longue pour qu'une projection ait un sens. */
  get canForecast(): boolean {
    return this.lastValues.length >= MIN_FORECAST_POINTS;
  }

  /**
   * Prévision de sortie des limites sur la série affichée (§6.5).
   *
   * <p>Deux chemins, une seule lecture : depuis un KPI, le serveur retire la série
   * et peut ouvrir une CAPA <b>préventive</b> ; en saisie manuelle, il n'y a rien à
   * rattacher, donc rien à ouvrir.
   */
  runForecast(): void {
    if (this.forecastLoading || !this.canForecast || this.forecastForm.invalid) {
      return;
    }
    const horizon = this.forecastForm.value.horizon ?? 6;
    this.forecastLoading = true;
    this.forecastError = null;
    this.forecast = null;
    this.forecastCapaId = null;

    const kpi = this.lastKpi;
    const appel: Observable<SpcForecastResponse | KpiSpcForecastResponse> = kpi
      ? this.spc.forecastKpi(kpi.id, kpi.limit, horizon, !!this.forecastForm.value.openCapa)
      : this.spc.forecast(
          this.lastValues, horizon,
          this.form.value.center ?? undefined,
          this.form.value.sigma ?? undefined);

    appel.subscribe({
      next: (res: SpcForecastResponse | KpiSpcForecastResponse) => {
        const prevision = 'forecast' in res ? res.forecast : res;
        this.forecast = prevision;
        this.forecastCapaId = 'forecast' in res ? (res.capaId ?? null) : null;
        this.forecastLoading = false;
        if (this.result) {
          this.chartOption = this.buildChart(
            this.result, this.lastValues, this.lastLabels, prevision);
        }
      },
      error: (err: HttpErrorResponse) => {
        this.forecastLoading = false;
        this.forecastError = err.status === 422
          ? $localize`:@@spc.forecast.err-min-four:Au moins 4 mesures sont nécessaires pour prévoir une dérive.`
          : this.messageFor(err);
      }
    });
  }

  /**
   * Phrase de tête de la prévision : ce que l'écran doit dire d'abord, et rien de
   * plus que ce que le modèle soutient.
   */
  forecastHeadline(f: SpcForecastResponse): string {
    if (!f.willBreach || f.firstBreachStep == null) {
      return $localize`:@@spc.forecast.no-breach:Aucune sortie des limites annoncée sur les ${f.horizon}:horizon: prochaines périodes.`;
    }
    const n = f.firstBreachStep;
    return f.side === 'lower'
      ? $localize`:@@spc.forecast.breach-lower:Le procédé franchira sa limite inférieure dans ${n}:steps: période(s).`
      : $localize`:@@spc.forecast.breach-upper:Le procédé franchira sa limite supérieure dans ${n}:steps: période(s).`;
  }

  /** Confiance de la prévision : une annonce sans sa confiance n'est pas une prévision (§12.3). */
  confidenceLabel(c: string): string {
    return c === 'high' ? $localize`:@@spc.forecast.conf-high:Confiance élevée`
      : c === 'medium' ? $localize`:@@spc.forecast.conf-medium:Confiance moyenne`
      : $localize`:@@spc.forecast.conf-low:Confiance faible`;
  }

  confidenceClass(c: string): string {
    return c === 'high' ? 'sev-min' : c === 'medium' ? 'sev-maj' : 'sev-crit';
  }

  /** Tendance par période, signée, pour que le sens de la dérive soit lisible. */
  trendLabel(trend: number): string {
    const v = (trend >= 0 ? '+' : '') + trend.toFixed(3);
    return $localize`:@@spc.forecast.trend:${v}:trend: par période`;
  }

  /** Risque de sortie d'une période, en pourcentage entier. */
  riskPercent(p: number): number {
    return Math.round(p * 100);
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

  private buildChart(res: SpcAnalyzeResponse, values: number[], labels?: string[],
                     forecast?: SpcForecastResponse): EChartsCoreOption {
    const ooc = this.outOfControlIndices(res);
    const base = labels && labels.length === values.length
      ? labels
      : values.map((_, i) => `${i + 1}`);
    // Les périodes à venir prolongent l'axe : la projection doit se lire DANS la
    // carte, sinon rien ne relie l'annonce aux limites qu'elle franchit.
    const futur = (forecast?.points ?? []).map(pt => `+${pt.step}`);
    const categories = [...base, ...futur];
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
    const projection = $localize`:@@spc.forecast.chart-projection:Projection`;
    const legende = forecast ? [measures, outOfControl, projection] : [measures, outOfControl];
    // La projection démarre au dernier point mesuré pour que la courbe soit continue.
    const projData: Array<number | null> = forecast
      ? [...values.map(() => null), ...forecast.points.map(pt => pt.value)]
      : [];
    if (forecast && values.length > 0) {
      projData[values.length - 1] = values[values.length - 1];
    }
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: legende, top: 0 },
      // Place à droite pour les étiquettes UCL / CL / LCL, sinon coupées au bord du graphique.
      grid: { left: 48, right: 96, top: 48, bottom: 40 },
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
        },
        ...(forecast ? [{
          name: projection,
          type: 'line' as const,
          data: projData,
          symbol: 'emptyCircle' as const,
          symbolSize: 6,
          connectNulls: true,
          lineStyle: { width: 2, type: 'dashed' as const, color: '#7C3AED' },
          itemStyle: { color: '#7C3AED' },
          z: 4
        }] : [])
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
