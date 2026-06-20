import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';

import { ReplayLikeStore } from './replay-like-store';

/**
 * État de cross-filtering partagé d'une page de dashboard (CLAUDE.md §7.3).
 *
 * Un clic sur un point/segment/barre d'un graphique pose un filtre (dimension +
 * valeur). Tous les widgets de la page s'abonnent au même état et se mettent en
 * surbrillance / se filtrent en conséquence. Le filtre est annulable (clear()).
 *
 * Volontairement mono-filtre (un cross-filter actif à la fois) : c'est le
 * comportement Power BI le plus lisible et évite l'empilement d'états opaques.
 */
@Injectable()
export class CrossFilterService {

  private readonly store = new ReplayLikeStore<CrossFilter | null>(null);

  /** Filtre courant (ou null si aucun). */
  readonly filter$: Observable<CrossFilter | null> = this.store.value$;

  /** True si un filtre est actif. */
  readonly active$: Observable<boolean> = this.filter$.pipe(
    map(f => f !== null),
    distinctUntilChanged()
  );

  /**
   * Applique un filtre. Cliquer la même (dimension, valeur) que le filtre actif
   * agit comme un toggle (on retire le filtre) — UX attendue d'un cross-filter.
   */
  apply(filter: CrossFilter): void {
    const current = this.store.snapshot();
    if (current && current.dimension === filter.dimension && current.value === filter.value) {
      this.clear();
      return;
    }
    this.store.set({ ...filter });
  }

  /** Retire le filtre courant. */
  clear(): void {
    if (this.store.snapshot() !== null) {
      this.store.set(null);
    }
  }

  /** Valeur synchrone du filtre (lecture ponctuelle). */
  snapshot(): CrossFilter | null {
    return this.store.snapshot();
  }

  /**
   * @returns true si la catégorie {@code value} doit rester mise en avant
   *          compte tenu du filtre courant. Sans filtre, tout est mis en avant.
   */
  isHighlighted(dimension: string, value: string): boolean {
    const f = this.store.snapshot();
    if (!f) return true;
    if (f.dimension !== dimension) return true;
    return f.value === value;
  }
}

/** Un filtre croisé : une valeur d'une dimension partagée. */
export interface CrossFilter {
  /** Dimension partagée entre widgets, ex. 'category' (catégorie 6M). */
  dimension: string;
  /** Valeur sélectionnée, ex. 'Machine'. */
  value: string;
  /** Libellé d'affichage (peut différer de la valeur). */
  label: string;
}
