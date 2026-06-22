import {
  ChangeDetectionStrategy, Component, Input, OnChanges, OnDestroy, SimpleChanges
} from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { finalize, takeUntil } from 'rxjs/operators';

import { DashboardAnnotationService } from '../dashboard-annotation.service';
import { DashboardAnnotation } from '../dashboard-annotation.types';

/**
 * Panneau d'annotations collaboratives d'un graphique (§7.3).
 *
 * Liste les commentaires persistés en base (horodatés, attribués à leur auteur
 * via le JWT côté API), permet d'en ajouter et d'en supprimer (l'auteur, ou un
 * admin tenant — l'API renvoie {@code deletable}). Le texte est affiché par
 * interpolation Angular (échappé) — jamais d'innerHTML.
 */
@Component({
  selector: 'qos-annotations-panel',
  templateUrl: './annotations-panel.component.html',
  styleUrls: ['./annotations-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AnnotationsPanelComponent implements OnChanges, OnDestroy {

  /** Clé stable du graphique (ex. 'exec.trend'). */
  @Input({ required: true }) chartKey!: string;

  /** Libellé optionnel du point ancré (préselectionné depuis le cross-filter). */
  @Input() anchorLabel: string | null = null;

  readonly annotations$ = new BehaviorSubject<DashboardAnnotation[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(false);
  readonly submitting$ = new BehaviorSubject<boolean>(false);
  readonly error$ = new BehaviorSubject<string | null>(null);

  draft = '';

  readonly maxLength = 2000;

  private readonly destroy$ = new Subject<void>();

  constructor(private readonly svc: DashboardAnnotationService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['chartKey'] && this.chartKey) {
      this.reload();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  reload(): void {
    this.loading$.next(true);
    this.error$.next(null);
    this.svc.list(this.chartKey)
      .pipe(finalize(() => this.loading$.next(false)), takeUntil(this.destroy$))
      .subscribe({
        next: rows => this.annotations$.next(rows),
        error: () => this.error$.next($localize`:@@dashboard.annot.load-error:Impossible de charger les annotations.`)
      });
  }

  add(): void {
    const body = this.draft.trim();
    if (!body || this.submitting$.value) return;
    this.submitting$.next(true);
    this.error$.next(null);
    this.svc.create({ chartKey: this.chartKey, anchorLabel: this.anchorLabel, body })
      .pipe(finalize(() => this.submitting$.next(false)), takeUntil(this.destroy$))
      .subscribe({
        next: created => {
          this.annotations$.next([created, ...this.annotations$.value]);
          this.draft = '';
        },
        error: () => this.error$.next($localize`:@@dashboard.annot.save-error:Échec de l'enregistrement.`)
      });
  }

  remove(a: DashboardAnnotation): void {
    if (!a.deletable) return;
    this.svc.delete(a.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.annotations$.next(
          this.annotations$.value.filter(x => x.id !== a.id)),
        error: () => this.error$.next($localize`:@@dashboard.annot.delete-error:Suppression refusée.`)
      });
  }

  trackById(_: number, a: DashboardAnnotation): string {
    return a.id;
  }
}
