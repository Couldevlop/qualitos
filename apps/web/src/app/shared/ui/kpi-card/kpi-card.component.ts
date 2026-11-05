import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

export type KpiState = 'good' | 'warn' | 'bad' | 'neutral';
export type KpiSize = 'sm' | 'md' | 'lg';

/**
 * La tuile d'indicateur : une mesure, sa cible, et la position de l'une par
 * rapport à l'autre.
 *
 * <p>Le vocabulaire est celui de la carte de contrôle, parce que c'est celui que
 * les gens de la qualité lisent déjà : une valeur se juge contre une limite, pas
 * dans l'absolu. Une tuile qui annonce « 2,8 % » et relègue « Cible 3,2 % » en
 * gris à côté oblige à faire la comparaison de tête, pour chacun des douze
 * indicateurs de l'écran — personne ne le fait, et le tableau de bord ne dit
 * plus rien. La **règle de mesure** fait cette comparaison à l'œil : le trait
 * court jusqu'à la valeur, un repère marque la cible.
 *
 * <p>L'échelle se déduit des seules données réelles (valeur et cible) :
 * `max(valeur, cible) × 1,35`. Aucun historique n'est dessiné, parce que l'API
 * n'en renvoie pas par indicateur — une courbe inventée sur un écran qualité
 * serait un enregistrement falsifié.
 *
 * <p>Sans cible, pas de règle : la tuile retombe sur un liseré d'état. Ce vide
 * se voit, et c'est voulu — un KPI sans seuil est un écart au §18.2.8.
 *
 * @example
 * ```html
 * <qos-kpi-card label="Coût d'obtention qualité" [value]="2.8" unit=" % CA"
 *               [trend]="-0.4" [target]="3.2" state="good"
 *               [trendInvertedIsGood]="true"></qos-kpi-card>
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

  /** Ce qui est au-delà du trait : la règle a besoin d'un peu d'air à droite. */
  private static readonly HEADROOM = 1.35;

  // ---------- tendance ----------

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

  // ---------- règle de mesure ----------

  /**
   * La valeur, quand c'est un nombre.
   *
   * <p>Certains indicateurs arrivent en texte (« 4,2 σ », « n/a ») : on ne tente
   * pas de les interpréter, on renonce à la règle.
   */
  get numericValue(): number | null {
    return typeof this.value === 'number' && Number.isFinite(this.value) ? this.value : null;
  }

  /** Une règle n'a de sens qu'avec une valeur chiffrée et une cible positive. */
  get hasRail(): boolean {
    return this.numericValue !== null && this.target != null
      && Number.isFinite(this.target) && this.target > 0 && !this.loading;
  }

  /** Le haut de l'échelle, déduit des seules données affichées. */
  private get railMax(): number {
    const value = Math.max(this.numericValue ?? 0, 0);
    const target = this.target ?? 0;
    return Math.max(value, target) * KpiCardComponent.HEADROOM;
  }

  /** La position de la valeur sur la règle, en pourcentage de l'échelle. */
  get valuePercent(): number {
    return this.percentOf(this.numericValue ?? 0);
  }

  /** La position du repère de cible. */
  get targetPercent(): number {
    return this.percentOf(this.target ?? 0);
  }

  private percentOf(amount: number): number {
    const max = this.railMax;
    if (max <= 0) return 0;
    const percent = (Math.max(amount, 0) / max) * 100;
    return Math.min(Math.max(percent, 0), 100);
  }

  // ---------- textes ----------

  /** « Cible 3,2 % », écrit une fois et réutilisé par l'étiquette accessible. */
  get targetText(): string {
    if (this.target == null) return '';
    return `${$localize`:@@kpi.target:Cible`} ${this.target}${this.unit}`;
  }

  /**
   * Ce qu'un lecteur d'écran entend.
   *
   * <p>La règle est muette (`aria-hidden`) : un graphique ne se lit pas à voix
   * haute. Sa seule information — où se situe la valeur par rapport à la cible —
   * est donc énoncée ici en mots.
   */
  get ariaLabel(): string {
    const parts = [`${this.label}: ${this.value ?? ''} ${this.unit}`.trim()];
    if (this.target != null) parts.push(this.targetText);
    return parts.join(', ');
  }
}
