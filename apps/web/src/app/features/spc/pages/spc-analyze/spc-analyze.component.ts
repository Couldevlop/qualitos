import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import type { EChartsCoreOption } from 'echarts/core';

import { SpcService } from '../../spc.service';
import { SpcAnalyzeResponse } from '../../spc.types';

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
export class SpcAnalyzeComponent {

  readonly form = this.fb.group({
    valuesText: ['', [Validators.required]],
    center: [null as number | null],
    sigma: [null as number | null]
  });

  /** Série de démonstration : process stable avec un point aberrant en fin de série. */
  readonly example = '10.0, 10.2, 9.8, 10.1, 9.9, 10.05, 9.95, 10.0, 10.1, 9.9, 13.6';

  loading = false;
  result: SpcAnalyzeResponse | null = null;
  error: string | null = null;
  chartOption: EChartsCoreOption | null = null;

  constructor(private readonly fb: FormBuilder, private readonly spc: SpcService) {}

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
      this.error = 'Saisissez au moins une mesure numérique.';
      return;
    }
    const center = this.form.value.center;
    const sigma = this.form.value.sigma;
    // Baseline : center et sigma doivent être fournis ensemble (sinon limites estimées).
    if ((center == null) !== (sigma == null)) {
      this.error = 'La baseline exige center ET σ (ou aucun des deux).';
      return;
    }
    if (sigma != null && sigma <= 0) {
      this.error = 'σ doit être strictement positif.';
      return;
    }

    this.loading = true;
    this.error = null;
    this.result = null;
    this.chartOption = null;
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

  /** Indices (0-based) impliqués dans au moins une violation → points hors-contrôle. */
  private outOfControlIndices(res: SpcAnalyzeResponse): Set<number> {
    const s = new Set<number>();
    res.violations.forEach(v => v.pointIndices.forEach(i => s.add(i)));
    return s;
  }

  private buildChart(res: SpcAnalyzeResponse, values: number[]): EChartsCoreOption {
    const ooc = this.outOfControlIndices(res);
    const categories = values.map((_, i) => `${i + 1}`);
    const oocPoints = values
      .map((v, i) => [i, v] as [number, number])
      .filter(([i]) => ooc.has(i));
    const L = res.limits;
    return {
      tooltip: { trigger: 'axis' },
      legend: { data: ['Mesures', 'Hors-contrôle'], top: 0 },
      xAxis: { type: 'category', data: categories, name: 'Point', boundaryGap: false },
      yAxis: { type: 'value', name: 'Valeur', scale: true },
      series: [
        {
          name: 'Mesures',
          type: 'line',
          smooth: false,
          symbol: 'circle',
          symbolSize: 6,
          data: values,
          lineStyle: { width: 2 },
          markLine: {
            symbol: 'none',
            data: [
              { yAxis: L.ucl, name: 'UCL', lineStyle: { color: '#DC2626', type: 'dashed' },
                label: { formatter: `UCL ${L.ucl.toFixed(2)}`, color: '#DC2626', position: 'end' } },
              { yAxis: L.centerLine, name: 'CL', lineStyle: { color: '#059669' },
                label: { formatter: `CL ${L.centerLine.toFixed(2)}`, color: '#059669', position: 'end' } },
              { yAxis: L.lcl, name: 'LCL', lineStyle: { color: '#DC2626', type: 'dashed' },
                label: { formatter: `LCL ${L.lcl.toFixed(2)}`, color: '#DC2626', position: 'end' } }
            ]
          }
        },
        {
          name: 'Hors-contrôle',
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
      case 0: return 'Backend injoignable (engine sur 8082 ?).';
      case 400: return 'Série invalide (1 à 10000 mesures attendues).';
      case 413: return 'Série trop volumineuse (garde-fou IA).';
      case 429: return 'Débit/quota IA dépassé pour ce tenant — réessayez plus tard.';
      case 502: return 'Passerelle IA indisponible (ai-service injoignable).';
      case 503: return 'Service IA momentanément indisponible (disjoncteur ouvert).';
      default:  return `Échec de l'analyse SPC (HTTP ${err.status}).`;
    }
  }

  severityClass(sev: string): string {
    return sev === 'CRITICAL' || sev === 'high' ? 'sev-crit'
      : sev === 'MAJOR' || sev === 'medium' ? 'sev-maj' : 'sev-min';
  }
}
