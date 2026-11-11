import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';

import { ControlChartGeometry, controlChart } from '../../spc-capability.charts';

/**
 * Une carte de contrôle (X̄ ou R) en SVG : les valeurs des sous-groupes, la
 * ligne centrale, les limites UCL / LCL en pointillé, et en rouge les points
 * qui en sortent — le dessin de la maquette, dans les jetons du thème.
 *
 * <p>Quand une analyse des règles de Nelson accompagne la carte (`rules`), les
 * points visés portent leur abrégé (« N2 », « N5 »…) et les frontières ±1σ /
 * ±2σ se dessinent (`showZones`) : ce sont les zones que ces règles mesurent,
 * et sans elles un point marqué « 2 sur 3 au-delà de 2σ » n'est pas lisible.
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
  /** Les règles de Nelson déclenchées, par indice de sous-groupe (analyse IA). */
  @Input() rules: ReadonlyMap<number, string[]> = new Map();
  /** Trace les frontières ±1σ / ±2σ : seule la carte X̄ les porte. */
  @Input() showZones = false;

  geometry: ControlChartGeometry | null = null;

  ngOnChanges(): void {
    this.geometry = controlChart(this.values, this.ucl, this.cl, this.lcl, { usl: this.usl, lsl: this.lsl },
                                 this.digits, this.rules);
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

  /** Les points marqués par une règle de Nelson SANS sortir des limites. */
  get flaggedCount(): number {
    return this.geometry?.points.filter(p => !p.out && p.rules.length > 0).length ?? 0;
  }

  get flaggedLabel(): string {
    const count = this.flaggedCount;
    return $localize`:@@spc.chart.nelson-flagged:Sous-groupes signalés par une règle de Nelson sans sortir des limites : ${count}:count:`;
  }

  /** Les abrégés posés au-dessus d'un point, et son infobulle. */
  rulesLabel(rules: string[]): string {
    return rules.join(' ');
  }

  pointTitle(index: number, value: number, rules: string[]): string {
    const base = `SG ${index + 1} : ${this.fixed(value)}`;
    return rules.length ? `${base} — ${rules.join(', ')}` : base;
  }
}
