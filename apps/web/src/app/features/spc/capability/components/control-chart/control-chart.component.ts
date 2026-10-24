import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';

import { ControlChartGeometry, controlChart } from '../../spc-capability.charts';

/**
 * Une carte de contrôle (X̄ ou R) en SVG : les valeurs des sous-groupes, la
 * ligne centrale, les limites UCL / LCL en pointillé, et en rouge les points
 * qui en sortent — le dessin de la maquette, dans les jetons du thème.
 */
@Component({
  selector: 'qos-spc-control-chart',
  templateUrl: './control-chart.component.html',
  styleUrls: ['./control-chart.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class ControlChartComponent implements OnChanges {

  @Input() values: (number | null)[] = [];
  @Input() ucl: number | null = null;
  @Input() cl: number | null = null;
  @Input() lcl: number | null = null;
  /** Les limites de tolérance à tracer aussi (carte X̄, comme le classeur). */
  @Input() usl: number | null = null;
  @Input() lsl: number | null = null;
  /** Le nom du graphique, pour les lecteurs d'écran. */
  @Input() label = '';
  @Input() digits = 2;

  geometry: ControlChartGeometry | null = null;

  ngOnChanges(): void {
    this.geometry = controlChart(this.values, this.ucl, this.cl, this.lcl, { usl: this.usl, lsl: this.lsl },
                                 this.digits);
  }

  fixed(v: number): string {
    return v.toFixed(this.digits);
  }

  get outCount(): number {
    return this.geometry?.points.filter(p => p.out).length ?? 0;
  }

  get outLabel(): string {
    const count = this.outCount;
    return $localize`:@@spc.chart.out-of-limits:Sous-groupes hors limites de contrôle : ${count}:count:`;
  }
}
