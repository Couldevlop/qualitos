import { Component, OnInit } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay } from 'rxjs/operators';

import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { TrainingService } from '../../training.service';
import { Badge, BeltLevel, LearnerProgressResponse, PathResponse } from '../../training.types';

/**
 * Page « Mon apprentissage » (CLAUDE.md §19.3) : ceinture qualité courante,
 * points, badges et parcours à compléter. La progression provient de
 * /api/v1/training/progress/me (tenant + utilisateur du JWT côté serveur).
 */
@Component({
  selector: 'qos-learning-home',
  templateUrl: './learning-home.component.html',
  styleUrls: ['./learning-home.component.scss'],
  standalone: false
})
export class LearningHomeComponent implements OnInit {

  readonly allBelts: BeltLevel[] = ['WHITE', 'YELLOW', 'GREEN', 'BLACK'];

  progress$!: Observable<LearnerProgressResponse>;
  activePaths$!: Observable<PathResponse[]>;
  completing = false;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  constructor(
    private readonly svc: TrainingService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.load();
    this.activePaths$ = this.svc.listPaths(0, 50, 'ACTIVE').pipe(
      map(page => page.content),
      catchError(() => of([] as PathResponse[])),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  private load(): void {
    this.loadingState$.next(true);
    this.errorState$.next(null);
    this.progress$ = this.svc.myProgress().pipe(
      catchError(err => {
        this.errorState$.next(safeErrorMessage(
          err, $localize`:@@learning.error-loading:Erreur de chargement de votre progression.`));
        return of(this.emptyProgress());
      }),
      finalize(() => this.loadingState$.next(false)),
      shareReplay({ bufferSize: 1, refCount: true })
    );
  }

  /** Marque un parcours/quiz comme terminé (démo : score 100 pour la self-formation). */
  markComplete(path: PathResponse): void {
    if (this.completing) return;
    this.completing = true;
    this.svc.completeLearning({ itemCode: path.code, score: 100 }).pipe(
      finalize(() => (this.completing = false))
    ).subscribe({
      next: () => {
        this.snack.open(
          $localize`:@@learning.completed-toast:Parcours marqué comme terminé. Progression mise à jour.`,
          'OK', { duration: 3500 });
        this.load();
      },
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@learning.complete-error:Échec de l'enregistrement.`),
        'OK', { duration: 4000 })
    });
  }

  beltClass(b: BeltLevel): string { return 'belt belt-' + b.toLowerCase(); }
  badgeChipClass(b: Badge): string { return 'badge-chip badge-' + b.toLowerCase().replace(/_/g, '-'); }
  beltReached(current: BeltLevel, belt: BeltLevel): boolean {
    return this.allBelts.indexOf(current) >= this.allBelts.indexOf(belt);
  }

  private emptyProgress(): LearnerProgressResponse {
    const now = new Date().toISOString();
    return {
      userId: '', tenantId: '', points: 0, completedCount: 0,
      beltLevel: 'WHITE', pointsToNextBelt: 100, badges: [],
      createdAt: now, updatedAt: now
    };
  }
}
