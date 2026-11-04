import {
  ChangeDetectionStrategy, ChangeDetectorRef, Component,
  HostListener, NgZone, OnDestroy, OnInit
} from '@angular/core';
import { Subscription } from 'rxjs';

import { AiPrediction, TopRisk } from '../dashboard/dashboard.types';
import { TvModeService, TvSlideLabels } from './tv-mode.service';
import { TvSlide, TV_INTERVALS_SEC } from './tv-mode.types';
import { clampIndex, nextIndex, prevIndex } from './tv-rotation.util';

/**
 * Mode TV / Salle qualité (§7.3) — affichage plein écran rotatif des KPIs
 * pour un écran mural (hall d'usine, pôle qualité).
 *
 * Conçu pour la lisibilité à distance : gros chiffres, contraste fort,
 * rotation automatique configurable, pause/reprise, navigation manuelle au
 * clavier (flèches) et bouton plein écran (Fullscreen API).
 *
 * Sécurité : aucune donnée n'est saisie ici ; toutes les valeurs viennent du
 * DashboardService (filtré `tenant_id` côté serveur) et sont interpolées par
 * Angular (échappement automatique — OWASP A03/LLM02).
 */
@Component({
  selector: 'qos-tv-mode',
  templateUrl: './tv-mode.component.html',
  styleUrls: ['./tv-mode.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class TvModeComponent implements OnInit, OnDestroy {

  slides: TvSlide[] = [];
  index = 0;
  loading = true;
  error: string | null = null;

  /** Rotation auto active (pause/reprise). */
  playing = true;
  /** Intervalle courant en secondes. */
  intervalSec = 10;
  readonly intervals = TV_INTERVALS_SEC;

  /** Plein écran courant (Fullscreen API). */
  fullscreen = false;

  /** Avancement de la slide courante (0..100), pour le rail de rotation. */
  progress = 0;

  /**
   * L'heure, affichée au bandeau.
   *
   * <p>Un tableau mural sans horloge laisse planer un doute que personne ne
   * lève : l'écran est-il figé depuis ce matin ? Elle tourne sur son propre
   * minuteur, indépendant de la rotation, pour ne pas se figer en pause.
   */
  clock = new Date();

  /**
   * Quand les indicateurs affichés ont été reçus.
   *
   * <p>C'est la fraîcheur de *ce qui est à l'écran*, pas celle de l'agrégat
   * serveur : l'API de construction des slides ne renvoie pas son horodatage,
   * et inventer une fraîcheur sur un écran qualité serait un enregistrement
   * faux.
   */
  loadedAt: Date | null = null;

  /** Libellés a11y/UI (binding dynamique → $localize côté TS). */
  readonly playLabel = $localize`:@@tv.a11y.play:Reprendre la rotation`;
  readonly pauseLabel = $localize`:@@tv.a11y.pause:Mettre en pause`;
  readonly prevLabel = $localize`:@@tv.a11y.prev:Slide précédente`;
  readonly nextLabel = $localize`:@@tv.a11y.next:Slide suivante`;
  readonly fullscreenLabel = $localize`:@@tv.a11y.fullscreen:Basculer en plein écran`;

  private dataSub?: Subscription;
  private timerId: ReturnType<typeof setInterval> | null = null;
  private clockId: ReturnType<typeof setInterval> | null = null;
  /** Pas du timer de progression (ms). */
  private static readonly TICK_MS = 100;
  /** Pas de l'horloge : la minute affichée n'a pas besoin de mieux. */
  private static readonly CLOCK_MS = 1000;

  constructor(
    private readonly svc: TvModeService,
    private readonly zone: NgZone,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
    this.startClock();
  }

  ngOnDestroy(): void {
    this.dataSub?.unsubscribe();
    this.stopTimer();
    this.stopClock();
  }

  /** (Re)charge les slides depuis le dashboard exécutif. */
  load(): void {
    this.loading = true;
    this.error = null;
    this.dataSub?.unsubscribe();
    this.dataSub = this.svc.buildSlides(this.labels()).subscribe({
      next: slides => {
        this.slides = slides;
        this.index = clampIndex(this.index, slides.length);
        this.loadedAt = new Date();
        this.loading = false;
        this.restartRotation();
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = $localize`:@@tv.error:Impossible de charger les indicateurs. Réessayez.`;
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get current(): TvSlide | null {
    return this.slides[this.index] ?? null;
  }

  /* ============================================================
   * Navigation.
   * ============================================================ */

  next(): void {
    this.index = nextIndex(this.index, this.slides.length);
    this.restartRotation();
    this.cdr.markForCheck();
  }

  prev(): void {
    this.index = prevIndex(this.index, this.slides.length);
    this.restartRotation();
    this.cdr.markForCheck();
  }

  goTo(i: number): void {
    if (i < 0 || i >= this.slides.length) return;
    this.index = i;
    this.restartRotation();
    this.cdr.markForCheck();
  }

  togglePlay(): void {
    this.playing = !this.playing;
    this.restartRotation();
    this.cdr.markForCheck();
  }

  setInterval(sec: number): void {
    this.intervalSec = sec;
    this.restartRotation();
    this.cdr.markForCheck();
  }

  /* ============================================================
   * Plein écran (Fullscreen API).
   * ============================================================ */

  toggleFullscreen(): void {
    const doc = document as Document;
    if (!doc.fullscreenElement) {
      void doc.documentElement.requestFullscreen?.().catch(() => undefined);
    } else {
      void doc.exitFullscreen?.().catch(() => undefined);
    }
  }

  @HostListener('document:fullscreenchange')
  onFullscreenChange(): void {
    this.fullscreen = !!document.fullscreenElement;
    this.cdr.markForCheck();
  }

  /* ============================================================
   * Raccourcis clavier (écran mural piloté à distance).
   * ============================================================ */

  @HostListener('document:keydown', ['$event'])
  onKey(ev: KeyboardEvent): void {
    switch (ev.key) {
      case 'ArrowRight': this.next(); ev.preventDefault(); break;
      case 'ArrowLeft':  this.prev(); ev.preventDefault(); break;
      case ' ':          this.togglePlay(); ev.preventDefault(); break;
      case 'f': case 'F': this.toggleFullscreen(); ev.preventDefault(); break;
      default: break;
    }
  }

  /* ============================================================
   * Rotation automatique + barre de progression.
   * ============================================================ */

  private restartRotation(): void {
    this.stopTimer();
    this.progress = 0;
    if (!this.playing || this.slides.length <= 1) return;

    const totalMs = this.intervalSec * 1000;
    let elapsed = 0;
    // Hors zone Angular pour éviter une détection de changements à chaque tick ;
    // on ne re-rentre dans la zone qu'aux transitions visibles.
    this.zone.runOutsideAngular(() => {
      this.timerId = setInterval(() => {
        elapsed += TvModeComponent.TICK_MS;
        const pct = Math.min(100, (elapsed / totalMs) * 100);
        this.zone.run(() => {
          this.progress = pct;
          if (elapsed >= totalMs) {
            elapsed = 0;
            this.index = nextIndex(this.index, this.slides.length);
            this.progress = 0;
          }
          this.cdr.markForCheck();
        });
      }, TvModeComponent.TICK_MS);
    });
  }

  private stopTimer(): void {
    if (this.timerId !== null) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  /**
   * L'horloge tourne en propre, et pas sur le minuteur de rotation.
   *
   * <p>Accrochée à la rotation, elle se figerait à la mise en pause : une
   * horloge arrêtée sur un mur est pire que pas d'horloge, parce qu'on la croit.
   */
  private startClock(): void {
    this.zone.runOutsideAngular(() => {
      this.clockId = setInterval(() => {
        const now = new Date();
        if (now.getMinutes() === this.clock.getMinutes()
            && now.getHours() === this.clock.getHours()) {
          return;
        }
        this.zone.run(() => {
          this.clock = now;
          this.cdr.markForCheck();
        });
      }, TvModeComponent.CLOCK_MS);
    });
  }

  private stopClock(): void {
    if (this.clockId !== null) {
      clearInterval(this.clockId);
      this.clockId = null;
    }
  }

  /**
   * Le remplissage de la case d'une slide sur le rail, en pourcentage.
   *
   * <p>Les slides déjà passées sont pleines, celle en cours se remplit, les
   * suivantes sont vides : la position dans le cycle et le temps restant se
   * lisent d'un seul coup d'œil.
   */
  segmentFill(i: number): number {
    if (i < this.index) return 100;
    if (i > this.index) return 0;
    return this.progress;
  }

  /* ============================================================
   * Helpers de présentation.
   * ============================================================ */

  severityTone(s: TopRisk['severity']): 'danger' | 'warn' | 'neutral' {
    if (s === 'critical') return 'danger';
    if (s === 'high')     return 'warn';
    return 'neutral';
  }

  predictionIcon(kind: AiPrediction['kind']): string {
    switch (kind) {
      case 'drift':     return 'monitoring';
      case 'objective': return 'flag';
      case 'supplier':  return 'local_shipping';
      case 'complaint': return 'forum';
    }
  }

  confidencePct(c: number): number {
    return Math.round(c * 100);
  }

  trackBySlide(_i: number, s: TvSlide): string { return s.id; }
  trackById(_i: number, item: { id: string }): string { return item.id; }

  /** Libellés localisés des slides (résolus ici, passés au service pur). */
  private labels(): TvSlideLabels {
    return {
      kpisTitle:        $localize`:@@tv.slide.kpis.title:Indicateurs stratégiques`,
      kpisSubtitle:     $localize`:@@tv.slide.kpis.subtitle:Performance qualité — cible vs réalisé`,
      risksTitle:       $localize`:@@tv.slide.risks.title:Risques critiques`,
      risksSubtitle:    $localize`:@@tv.slide.risks.subtitle:Actions prioritaires à traiter`,
      predictionsTitle: $localize`:@@tv.slide.predictions.title:Prévisions IA`,
      predictionsSubtitle: $localize`:@@tv.slide.predictions.subtitle:Anticipations explicables sur les prochaines semaines`,
      trendTitle:       $localize`:@@tv.slide.trend.title:Tendance qualité — 12 mois`,
      trendSubtitle:    $localize`:@@tv.slide.trend.subtitle:Évolution de la performance globale`,
      emptyTitle:       $localize`:@@tv.slide.empty.title:Aucune donnée à afficher`,
      emptySubtitle:    $localize`:@@tv.slide.empty.subtitle:Les indicateurs apparaîtront ici dès qu'ils seront disponibles.`,
      trendSeriesName:  $localize`:@@tv.trend.series:Qualité globale`,
      trendTargetName:  $localize`:@@tv.trend.target:Cible`
    };
  }
}
