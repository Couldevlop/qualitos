import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';

import { ForecastService } from '../../forecast.service';
import { ForecastDirection, ForecastResponse } from '../../forecast.types';

/**
 * Prévision KPI (§6.5, §12.1) : l'utilisateur saisit une série de mesures, une cible et un
 * horizon ; l'IA projette la série par lissage exponentiel Holt-Winters (niveau + tendance
 * + saisonnalité optionnelle) et estime la probabilité d'atteindre la cible. Le graphe
 * superpose l'historique, la projection, son intervalle de prédiction à 95 % et la cible.
 */
@Component({
  selector: 'qos-forecast-kpi',
  templateUrl: './forecast-kpi.component.html',
  styleUrls: ['./forecast-kpi.component.scss'],
  standalone: false
})
export class ForecastKpiComponent {

  readonly form = this.fb.group({
    valuesText: ['', [Validators.required]],
    target: [null as number | null, [Validators.required]],
    horizon: [6, [Validators.min(1), Validators.max(60)]],
    direction: ['at_least' as ForecastDirection, [Validators.required]],
    seasonalPeriod: [null as number | null]
  });

  /** Série de démonstration : tendance montante régulière avec un léger bruit. */
  readonly example = '62, 63, 61, 64, 66, 65, 68, 69, 71, 70, 73, 75';

  loading = false;
  result: ForecastResponse | null = null;
  error: string | null = null;
  chartOption: EChartsCoreOption | null = null;
  private history: number[] = [];

  constructor(private readonly fb: FormBuilder, private readonly forecast: ForecastService) {}

  loadExample(): void {
    this.form.patchValue({
      valuesText: this.example, target: 90, horizon: 6,
      direction: 'at_least', seasonalPeriod: null
    });
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

  run(): void {
    if (this.loading) {
      return;
    }
    const values = this.parseValues();
    if (values.length < 4) {
      this.error = $localize`:@@forecast.kpi.err-min-points:Saisissez au moins 4 mesures numériques.`;
      return;
    }
    const target = this.form.value.target;
    if (target == null || !Number.isFinite(target)) {
      this.error = $localize`:@@forecast.kpi.err-target:Indiquez une valeur cible.`;
      return;
    }
    const seasonal = this.form.value.seasonalPeriod;
    if (seasonal != null && (seasonal < 2 || seasonal > 365)) {
      this.error = $localize`:@@forecast.kpi.err-seasonal:La période saisonnière doit être comprise entre 2 et 365.`;
      return;
    }

    this.history = values;
    this.loading = true;
    this.error = null;
    this.result = null;
    this.chartOption = null;

    this.forecast.forecast({
      values,
      target,
      horizon: this.form.value.horizon ?? 6,
      direction: this.form.value.direction ?? 'at_least',
      seasonalPeriod: seasonal ?? undefined
    }).subscribe({
      next: res => {
        this.result = res;
        this.chartOption = this.buildChart(res);
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.error = this.messageFor(err);
      }
    });
  }

  probabilityPct(p: number): string {
    return `${Math.round(p * 100)} %`;
  }

  directionLabel(d: ForecastDirection): string {
    return d === 'at_most'
      ? $localize`:@@forecast.kpi.dir-at-most:atteindre au plus`
      : $localize`:@@forecast.kpi.dir-at-least:atteindre au moins`;
  }

  modelLabel(model: string, seasonalPeriod: number): string {
    if (model === 'holt_winters_additive') {
      return $localize`:@@forecast.kpi.model-hw:Holt-Winters (saisonnier)` + ` · ${seasonalPeriod}`;
    }
    return $localize`:@@forecast.kpi.model-holt:Holt (niveau + tendance)`;
  }

  confidenceState(confidence: string): 'good' | 'warn' | 'neutral' {
    return confidence === 'high' ? 'good' : confidence === 'medium' ? 'warn' : 'neutral';
  }

  probabilityState(p: number): 'good' | 'warn' | 'bad' {
    return p >= 0.7 ? 'good' : p >= 0.4 ? 'warn' : 'bad';
  }

  private buildChart(res: ForecastResponse): EChartsCoreOption {
    const n = this.history.length;
    const total = n + res.horizon;
    const categories = Array.from({ length: total }, (_, i) => `${i + 1}`);

    // Historique : valeurs réelles, puis null sur la zone de prévision.
    const hist: (number | null)[] = this.history.map(v => v).concat(Array(res.horizon).fill(null));
    // Prévision : raccordée au dernier point d'historique pour une ligne continue.
    const fc: (number | null)[] = Array(n - 1).fill(null);
    fc.push(this.history[n - 1]);
    res.points.forEach(p => fc.push(p.value));
    const low: (number | null)[] = Array(n).fill(null);
    const high: (number | null)[] = Array(n).fill(null);
    res.points.forEach(p => { low.push(p.low); high.push(p.high); });

    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['Historique', 'Prévision', 'IP 95 %'], top: 0 },
      grid: { left: 48, right: 24, top: 32, bottom: 36 },
      xAxis: { type: 'category', data: categories, name: 'Période', boundaryGap: false },
      yAxis: { type: 'value', name: 'Valeur', scale: true },
      series: [
        { name: 'Historique', type: 'line', data: hist, symbol: 'circle', symbolSize: 6,
          lineStyle: { width: 2, color: '#2563EB' }, itemStyle: { color: '#2563EB' } },
        // Borne basse (transparente) + bande empilée = intervalle de prédiction.
        { name: 'IP 95 %', type: 'line', data: low, stack: 'pi', symbol: 'none',
          lineStyle: { opacity: 0 }, areaStyle: { opacity: 0 }, silent: true },
        { name: 'IP 95 %', type: 'line', data: this.bandDelta(low, high), stack: 'pi',
          symbol: 'none', lineStyle: { opacity: 0 },
          areaStyle: { color: 'rgba(37, 99, 235, 0.12)' }, silent: true },
        { name: 'Prévision', type: 'line', data: fc, symbol: 'circle', symbolSize: 6,
          lineStyle: { width: 2, type: 'dashed', color: '#7C3AED' }, itemStyle: { color: '#7C3AED' },
          markLine: {
            symbol: 'none',
            data: [{ yAxis: res.target, name: 'Cible',
              lineStyle: { color: '#059669' },
              label: { formatter: `Cible ${res.target}`, color: '#059669', position: 'end' } }]
          } }
      ]
    };
  }

  /** Épaisseur de bande (high − low) là où l'IP existe, null ailleurs (pour l'empilement). */
  private bandDelta(low: (number | null)[], high: (number | null)[]): (number | null)[] {
    return low.map((l, i) => (l == null || high[i] == null) ? null : (high[i] as number) - l);
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@forecast.kpi.err-backend:Backend injoignable (engine sur 8082 ?).`;
      case 400: return $localize`:@@forecast.kpi.err-invalid:Requête invalide (≥ 4 mesures, horizon 1–60, cible requise).`;
      case 413: return $localize`:@@forecast.kpi.err-too-large:Série trop volumineuse (garde-fou IA).`;
      case 422: return $localize`:@@forecast.kpi.err-unprocessable:Série insuffisante pour une prévision fiable.`;
      case 429: return $localize`:@@forecast.kpi.err-quota:Débit/quota IA dépassé pour ce tenant — réessayez plus tard.`;
      case 502: return $localize`:@@forecast.kpi.err-gateway:Passerelle IA indisponible (ai-service injoignable).`;
      case 503: return $localize`:@@forecast.kpi.err-unavailable:Service IA momentanément indisponible (disjoncteur ouvert).`;
      default:  return $localize`:@@forecast.kpi.err-generic:Échec de la prévision (HTTP ${err.status}:status:).`;
    }
  }
}
