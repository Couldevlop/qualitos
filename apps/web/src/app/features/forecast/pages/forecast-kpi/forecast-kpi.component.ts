import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormControl, Validators } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';
import { of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';

import { toSeries } from '../../../../shared/ai-sources/series';
import { KpisService } from '../../../kpis/kpis.service';
import { KpiResponse } from '../../../kpis/kpis.types';
import { buildForecastChart } from '../../forecast-chart';
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
export class ForecastKpiComponent implements OnInit {

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

  /** Valeur du sélecteur de source pour la saisie libre. */
  static readonly MANUAL = 'manual';
  readonly manual = ForecastKpiComponent.MANUAL;

  /** Indicateurs actifs du catalogue, proposés comme source de la série. */
  kpis: KpiResponse[] = [];
  readonly source = new FormControl<string>(ForecastKpiComponent.MANUAL, { nonNullable: true });
  sourceLoading = false;
  /** Ce qui a été chargé, dit en clair (nombre de mesures, période). */
  sourceInfo: string | null = null;

  constructor(
    private readonly fb: FormBuilder,
    private readonly forecast: ForecastService,
    private readonly kpiService: KpisService
  ) {}

  ngOnInit(): void {
    // Le catalogue est une aide : s'il ne répond pas, la saisie libre reste.
    this.kpiService.list(0, 100, 'ACTIVE').pipe(catchError(() => of(null)))
      .subscribe(page => (this.kpis = page?.content ?? []));
  }

  /** Remplit la série, la cible et le sens depuis l'indicateur choisi. */
  onSourceChange(id: string): void {
    this.sourceInfo = null;
    if (id === ForecastKpiComponent.MANUAL) {
      return;
    }
    const kpi = this.kpis.find(k => k.id === id);
    if (!kpi) {
      return;
    }
    this.sourceLoading = true;
    this.error = null;
    this.kpiService.listMeasurements(kpi.id, 0, 200)
      .pipe(finalize(() => (this.sourceLoading = false)))
      .subscribe({
        next: page => {
          const series = toSeries(page.content);
          this.form.patchValue({
            valuesText: series.values.join(', '),
            target: kpi.targetValue ?? this.form.value.target ?? null,
            direction: kpi.direction === 'LOWER_IS_BETTER' ? 'at_most' : 'at_least'
          });
          const count = series.values.length;
          this.sourceInfo = count < 4
            ? $localize`:@@forecast.kpi.source-too-few:${count}:count: mesure(s) seulement pour cet indicateur : il en faut au moins 4.`
            : $localize`:@@forecast.kpi.source-loaded:${count}:count: mesures chargées, du ${this.day(series.from)}:from: au ${this.day(series.to)}:to:.`;
        },
        error: () => {
          this.error = $localize`:@@forecast.kpi.source-error:Impossible de charger les mesures de cet indicateur.`;
        }
      });
  }

  private day(iso: string | null): string {
    return iso ? iso.slice(0, 10) : '—';
  }

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
        this.chartOption = buildForecastChart(this.history, res);
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
