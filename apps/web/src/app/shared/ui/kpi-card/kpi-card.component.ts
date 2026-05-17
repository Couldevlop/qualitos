import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

export type KpiState = 'good' | 'warn' | 'bad' | 'neutral';
export type KpiSize = 'sm' | 'md' | 'lg';

/**
 * Carte KPI premium — gros chiffre, libellé, tendance, cible, mini-sparkline.
 *
 * Usage:
 * ```html
 * <qos-kpi-card
 *   label="Coût d'obtention qualité"
 *   [value]="2.8"
 *   unit="% CA"
 *   [trend]="-0.4"
 *   [target]="3.2"
 *   state="good"
 *   icon="paid">
 * </qos-kpi-card>
 * ```
 */
@Component({
  selector: 'qos-kpi-card',
  templateUrl: './kpi-card.component.html',
  styleUrls: ['./kpi-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class KpiCardComponent {

  @Input() label = '';
  @Input() value: number | string | null = null;
  @Input() unit = '';
  @Input() trend?: number;
  @Input() target?: number;
  @Input() description?: string;
  @Input() icon?: string;
  @Input() state: KpiState = 'neutral';
  @Input() size: KpiSize = 'md';
  @Input() loading = false;
  @Input() trendInvertedIsGood = false;

  /** Classe CSS pour la couleur de la tendance (inversée si trendInvertedIsGood). */
  trendClass(): string {
    if (this.trend == null || this.trend === 0) return 'qos-kpi__trend--flat';
    const isPositive = this.trend > 0;
    const isGood = this.trendInvertedIsGood ? !isPositive : isPositive;
    return isGood ? 'qos-kpi__trend--up' : 'qos-kpi__trend--down';
  }

  trendIcon(): string {
    if (this.trend == null) return 'remove';
    if (this.trend === 0) return 'remove';
    return this.trend > 0 ? 'trending_up' : 'trending_down';
  }

  formattedTrend(): string {
    if (this.trend == null) return '';
    const sign = this.trend > 0 ? '+' : '';
    return `${sign}${this.trend.toFixed(1)}%`;
  }
}
