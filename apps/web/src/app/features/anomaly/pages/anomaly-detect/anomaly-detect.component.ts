import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';

import { AnomalyService } from '../../anomaly.service';
import {
  AnomalyDetectResponse,
  AnomalyExplainResponse,
  AnomalyMethod,
  AnomalyPoint
} from '../../anomaly.types';

/**
 * Détection d'anomalies non-supervisée multivariée (§3.4, §12.1) : l'utilisateur
 * saisit une matrice (une ligne = un échantillon, features séparées par virgule/espace)
 * et choisit la méthode (Isolation Forest ou reconstruction par ACP) ; l'IA calcule un
 * score d'anomalie par échantillon et marque les plus anormaux (quantile de contamination
 * ou seuil explicite). Le graphe met en évidence les échantillons hors-norme.
 */
@Component({
  selector: 'qos-anomaly-detect',
  templateUrl: './anomaly-detect.component.html',
  styleUrls: ['./anomaly-detect.component.scss'],
  standalone: false
})
export class AnomalyDetectComponent {

  readonly form = this.fb.group({
    matrixText: ['', [Validators.required]],
    method: ['isolation_forest' as AnomalyMethod, [Validators.required]],
    contamination: [0.1, [Validators.min(0.0001), Validators.max(0.5)]],
    threshold: [null as number | null]
  });

  /**
   * Matrice de démonstration : nuage 2D régulier (~y=2x) + un point franchement
   * aberrant en dernière ligne, que les deux méthodes isolent.
   */
  readonly example = [
    '1.0, 2.0',
    '2.0, 4.1',
    '3.0, 5.9',
    '4.0, 8.0',
    '5.0, 10.1',
    '6.0, 11.9',
    '7.0, 14.0',
    '8.0, 16.1',
    '9.0, 17.9',
    '12.0, -8.0'
  ].join('\n');

  loading = false;
  result: AnomalyDetectResponse | null = null;
  error: string | null = null;
  chartOption: EChartsCoreOption | null = null;

  /** Matrice saisie (conservée pour expliquer un échantillon précis). */
  private matrix: number[][] = [];
  /** Explication SHAP en cours / affichée. */
  explanation: AnomalyExplainResponse | null = null;
  explainChartOption: EChartsCoreOption | null = null;
  explainingIndex: number | null = null;

  constructor(private readonly fb: FormBuilder, private readonly anomaly: AnomalyService) {}

  loadExample(): void {
    this.form.patchValue({
      matrixText: this.example,
      method: 'isolation_forest',
      contamination: 0.1,
      threshold: null
    });
  }

  /** Découpe le texte libre en matrice : une ligne = un échantillon, séparateurs , ; espace. */
  private parseMatrix(): number[][] {
    const raw = this.form.value.matrixText ?? '';
    return raw
      .split(/\r?\n/)
      .map(line => line.trim())
      .filter(line => line.length > 0)
      .map(line => line
        .split(/[\s,;]+/)
        .map(t => t.trim())
        .filter(t => t.length > 0)
        .map(Number));
  }

  detect(): void {
    if (this.loading) {
      return;
    }
    const matrix = this.parseMatrix();
    if (matrix.length === 0) {
      this.error = $localize`:@@anomaly.detect.err-no-data:Saisissez au moins un échantillon (une ligne de nombres).`;
      return;
    }
    const width = matrix[0].length;
    if (width === 0 || matrix.some(row => row.length !== width)) {
      this.error = $localize`:@@anomaly.detect.err-ragged:Toutes les lignes doivent avoir le même nombre de features (≥ 1).`;
      return;
    }
    if (matrix.some(row => row.some(v => !Number.isFinite(v)))) {
      this.error = $localize`:@@anomaly.detect.err-numbers:La matrice ne doit contenir que des nombres.`;
      return;
    }
    const contamination = this.form.value.contamination ?? 0.1;
    if (!(contamination > 0 && contamination <= 0.5)) {
      this.error = $localize`:@@anomaly.detect.err-contamination:La contamination doit être dans l'intervalle ]0 ; 0,5].`;
      return;
    }
    const threshold = this.form.value.threshold;

    this.matrix = matrix;
    this.loading = true;
    this.error = null;
    this.result = null;
    this.chartOption = null;
    this.explanation = null;
    this.explainChartOption = null;
    this.explainingIndex = null;

    this.anomaly.detect({
      samples: matrix,
      method: this.form.value.method ?? 'isolation_forest',
      contamination,
      threshold: threshold ?? undefined
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

  /** Échantillons marqués anormaux (pour la table), triés par score décroissant. */
  anomalies(res: AnomalyDetectResponse): AnomalyPoint[] {
    return res.points.filter(p => p.isAnomaly).sort((a, b) => b.score - a.score);
  }

  methodLabel(method: AnomalyMethod): string {
    return method === 'reconstruction'
      ? $localize`:@@anomaly.detect.method-reconstruction:Reconstruction (ACP)`
      : $localize`:@@anomaly.detect.method-iforest:Isolation Forest`;
  }

  /** Libellé de la feature dominante (mode reconstruction) ; tiret si absent. */
  topFeatureLabel(p: AnomalyPoint): string {
    return p.topFeature == null
      ? '—'
      : $localize`:@@anomaly.detect.feature-prefix:Feature` + ` #${p.topFeature + 1}`;
  }

  /** Demande l'explication SHAP du score d'anomalie de l'échantillon `index`. */
  explain(index: number): void {
    if (this.explainingIndex === index && this.explanation) {
      return;
    }
    this.explainingIndex = index;
    this.explanation = null;
    this.explainChartOption = null;
    this.anomaly.explain({ samples: this.matrix, index }).subscribe({
      next: res => {
        this.explanation = res;
        this.explainChartOption = this.buildExplainChart(res);
      },
      error: (err: HttpErrorResponse) => {
        this.explainingIndex = null;
        this.error = this.messageFor(err);
      }
    });
  }

  private buildExplainChart(res: AnomalyExplainResponse): EChartsCoreOption {
    const pushColor = '#DC2626';   // pousse vers l'anormalité (contribution > 0)
    const pullColor = '#2563EB';   // pousse vers la normalité (contribution < 0)
    // Trié par contribution croissante pour un affichage barres horizontales lisible.
    const sorted = [...res.contributions].sort((a, b) => a.contribution - b.contribution);
    return {
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 90, right: 24, top: 16, bottom: 28 },
      xAxis: { type: 'value', name: 'Contribution' },
      yAxis: {
        type: 'category',
        data: sorted.map(c => `Feature #${c.feature + 1}`)
      },
      series: [{
        type: 'bar',
        data: sorted.map(c => ({
          value: c.contribution,
          itemStyle: { color: c.contribution >= 0 ? pushColor : pullColor }
        })),
        barMaxWidth: 22
      }]
    };
  }

  private buildChart(res: AnomalyDetectResponse): EChartsCoreOption {
    const normalColor = '#2563EB';
    const anomalyColor = '#DC2626';
    const data = res.points.map(p => ({
      value: p.score,
      itemStyle: { color: p.isAnomaly ? anomalyColor : normalColor }
    }));
    const categories = res.points.map(p => `${p.index + 1}`);
    return {
      tooltip: { trigger: 'axis' },
      grid: { left: 48, right: 24, top: 28, bottom: 40 },
      xAxis: {
        type: 'category',
        data: categories,
        name: $localize`:@@anomaly.detect.axis-sample:Échantillon`,
        nameLocation: 'middle',
        nameGap: 26
      },
      yAxis: {
        type: 'value',
        name: $localize`:@@anomaly.detect.axis-score:Score`,
        scale: true
      },
      series: [
        {
          type: 'bar',
          data,
          barMaxWidth: 26,
          markLine: {
            symbol: 'none',
            data: [
              {
                yAxis: res.threshold,
                lineStyle: { color: anomalyColor, type: 'dashed' },
                label: {
                  formatter: $localize`:@@anomaly.detect.threshold-mark:Seuil` + ` ${res.threshold.toFixed(3)}`,
                  color: anomalyColor,
                  position: 'end'
                }
              }
            ]
          }
        }
      ]
    };
  }

  private messageFor(err: HttpErrorResponse): string {
    switch (err.status) {
      case 0: return $localize`:@@anomaly.detect.err-backend:Backend injoignable (engine sur 8082 ?).`;
      case 400: return $localize`:@@anomaly.detect.err-invalid:Matrice invalide (1 à 50000 échantillons, contamination ∈ ]0 ; 0,5]).`;
      case 413: return $localize`:@@anomaly.detect.err-too-large:Matrice trop volumineuse (garde-fou IA).`;
      case 429: return $localize`:@@anomaly.detect.err-quota:Débit/quota IA dépassé pour ce tenant — réessayez plus tard.`;
      case 502: return $localize`:@@anomaly.detect.err-gateway:Passerelle IA indisponible (ai-service injoignable).`;
      case 503: return $localize`:@@anomaly.detect.err-unavailable:Service IA momentanément indisponible (disjoncteur ouvert).`;
      default:  return $localize`:@@anomaly.detect.err-generic:Échec de la détection (HTTP ${err.status}:status:).`;
    }
  }
}
