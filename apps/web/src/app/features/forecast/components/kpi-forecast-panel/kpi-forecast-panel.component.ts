import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import type { EChartsCoreOption } from 'echarts/core';

import { toSeries } from '../../../../shared/ai-sources/series';
import { KpisService } from '../../../kpis/kpis.service';
import { KpiResponse } from '../../../kpis/kpis.types';
import { buildForecastChart } from '../../forecast-chart';
import { ForecastService } from '../../forecast.service';
import { ForecastDirection, ForecastResponse } from '../../forecast.types';

/** Nombre de mesures sous lequel il n'y a pas de tendance à prolonger. */
export const MIN_POINTS = 4;

/** Mesures lues pour construire la série (les plus récentes suffisent). */
export const MEASUREMENT_PAGE_SIZE = 200;

/**
 * Prévision d'un indicateur, à l'endroit où on le consulte (§6.5).
 *
 * <p>La projection vivait dans une page d'essai à part, sous une entrée de menu propre :
 * il fallait y retourner, choisir l'indicateur, et comparer de tête avec sa fiche. La
 * courbe se lit maintenant dans le contexte de l'indicateur, sur ses propres mesures, sa
 * propre cible et son propre sens d'amélioration — aucune ressaisie, et le chiffre projeté
 * porte sur la définition qui est affichée juste au-dessus.
 *
 * <p>Le panneau dit toujours pourquoi il ne montre rien : série trop courte, cible absente,
 * ou service de prévision indisponible. Un panneau muet laisserait croire que l'indicateur
 * n'a pas de tendance.
 */
@Component({
  selector: 'qos-kpi-forecast-panel',
  templateUrl: './kpi-forecast-panel.component.html',
  styleUrls: ['./kpi-forecast-panel.component.scss'],
  standalone: false
})
export class KpiForecastPanelComponent implements OnChanges {

  /** L'indicateur consulté. Sa cible et son sens pilotent la prévision. */
  @Input() kpi: KpiResponse | null = null;

  /** Horizon de projection, en périodes. */
  @Input() horizon = 6;

  loading = false;
  result: ForecastResponse | null = null;
  chartOption: EChartsCoreOption | null = null;
  /** Ce qui empêche la prévision, dit en clair. Jamais un panneau muet. */
  unavailable: string | null = null;
  /** Nombre de mesures effectivement projetées. */
  points = 0;

  private history: number[] = [];

  constructor(
    private readonly kpis: KpisService,
    private readonly forecast: ForecastService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['kpi'] || changes['horizon']) {
      this.run();
    }
  }

  /** Sens d'amélioration de l'indicateur, traduit pour le moteur de prévision. */
  get direction(): ForecastDirection {
    return this.kpi?.direction === 'LOWER_IS_BETTER' ? 'at_most' : 'at_least';
  }

  probabilityPct(p: number): string {
    return `${Math.round(p * 100)} %`;
  }

  probabilityState(p: number): 'good' | 'warn' | 'bad' {
    return p >= 0.7 ? 'good' : p >= 0.4 ? 'warn' : 'bad';
  }

  confidenceState(confidence: string): 'good' | 'warn' | 'neutral' {
    return confidence === 'high' ? 'good' : confidence === 'medium' ? 'warn' : 'neutral';
  }

  directionLabel(): string {
    return this.direction === 'at_most'
      ? $localize`:@@forecast.panel.dir-at-most:ne pas dépasser`
      : $localize`:@@forecast.panel.dir-at-least:atteindre au moins`;
  }

  modelLabel(model: string, seasonalPeriod: number): string {
    return model === 'holt_winters_additive'
      ? $localize`:@@forecast.panel.model-hw:Holt-Winters (saisonnier)` + ` · ${seasonalPeriod}`
      : $localize`:@@forecast.panel.model-holt:Holt (niveau + tendance)`;
  }

  /** Relit les mesures et relance la projection (après un enregistrement, par exemple). */
  reload(): void {
    this.run();
  }

  private run(): void {
    const kpi = this.kpi;
    this.result = null;
    this.chartOption = null;
    this.unavailable = null;
    this.points = 0;
    if (!kpi) {
      return;
    }
    // Sans cible, « atteindra-t-il son objectif ? » n'a pas de réponse : on le dit,
    // plutôt que de projeter une courbe que rien ne vient juger.
    if (kpi.targetValue == null || !Number.isFinite(kpi.targetValue)) {
      this.unavailable = $localize`:@@forecast.panel.no-target:Cet indicateur n'a pas de valeur cible : renseignez-la pour obtenir une prévision.`;
      return;
    }
    const target = kpi.targetValue;

    this.loading = true;
    this.kpis.listMeasurements(kpi.id, 0, MEASUREMENT_PAGE_SIZE).subscribe({
      next: page => {
        const values = toSeries(page.content).values;
        if (values.length < MIN_POINTS) {
          this.loading = false;
          this.unavailable = $localize`:@@forecast.panel.too-few:${values.length}:count: mesure(s) enregistrée(s) : il en faut au moins ${MIN_POINTS}:min: pour projeter une tendance.`;
          return;
        }
        this.history = values;
        this.points = values.length;
        this.project(target);
      },
      error: () => {
        this.loading = false;
        this.unavailable = $localize`:@@forecast.panel.measures-error:Impossible de lire les mesures de cet indicateur.`;
      }
    });
  }

  private project(target: number): void {
    this.forecast.forecast({
      values: this.history,
      target,
      horizon: this.horizon,
      direction: this.direction
    }).subscribe({
      next: res => {
        this.loading = false;
        this.result = res;
        this.chartOption = buildForecastChart(this.history, res);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        this.unavailable = this.messageFor(err);
      }
    });
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0:   return $localize`:@@forecast.panel.err-backend:Service de prévision injoignable.`;
      case 422: return $localize`:@@forecast.panel.err-unprocessable:Série insuffisante pour une prévision fiable.`;
      case 429: return $localize`:@@forecast.panel.err-quota:Débit IA dépassé pour ce client — réessayez plus tard.`;
      case 502:
      case 503: return $localize`:@@forecast.panel.err-unavailable:Service de prévision momentanément indisponible.`;
      default:  return $localize`:@@forecast.panel.err-generic:Échec de la prévision (HTTP ${err.status}:status:).`;
    }
  }
}
