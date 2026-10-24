import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';

import { DistributionGeometry, distribution } from '../../spc-capability.charts';

/**
 * La distribution des mesures face à la tolérance : histogramme, loi normale
 * estimée (X̿, σ = R̄ / d2), limites LSL / USL, zones hors tolérance ombrées et
 * moyenne en pointillé.
 */
@Component({
  selector: 'qos-spc-distribution-chart',
  templateUrl: './distribution-chart.component.html',
  styleUrls: ['./distribution-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class DistributionChartComponent implements OnChanges {

  @Input() values: number[] = [];
  @Input() mean: number | null = null;
  @Input() sigma: number | null = null;
  @Input() lsl: number | null = null;
  @Input() usl: number | null = null;
  /** Les décimales de la moyenne : celles de l'étude (arrondie au dixième, 12.0533 devenait l'USL 12.1). */
  @Input() digits = 2;

  geometry: DistributionGeometry | null = null;

  readonly ariaLabel = $localize`:@@spc.distribution.aria:Histogramme des mesures et limites de tolérance`;

  barTitle(count: number): string {
    return $localize`:@@spc.distribution.bar:Mesures dans cette classe : ${count}:count:`;
  }

  ngOnChanges(): void {
    this.geometry = distribution(this.values, this.mean, this.sigma, this.lsl, this.usl);
  }
}
