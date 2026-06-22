import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AcademyService } from '../../infrastructure/academy.service';
import {
  CourseOutline,
  EnrollmentResponse,
  QuizForLearner,
  QuizResult
} from '../../domain/academy.types';

/**
 * Lecteur de cours (§19.3) : déroule modules → leçons → quiz pour l'inscription
 * courante. Marque les leçons complétées, soumet les quiz (auto-correction) et
 * reflète la progression / complétion (gamification + certificat côté backend).
 */
@Component({
  selector: 'qos-course-player',
  templateUrl: './course-player.component.html',
  styleUrls: ['./course-player.component.scss'],
  standalone: false
})
export class CoursePlayerComponent implements OnInit {

  enrollmentId!: string;
  enrollment?: EnrollmentResponse;
  outline?: CourseOutline;

  loading = false;
  busy = false;

  /** Réponses sélectionnées par quiz : quizId → (questionIndex → optionIndex). */
  readonly selections: Record<string, Record<number, number>> = {};
  /** Dernier résultat de quiz affiché : quizId → résultat. */
  readonly results: Record<string, QuizResult> = {};
  /** Leçons localement marquées complétées (feedback immédiat). */
  readonly doneLessons = new Set<string>();

  constructor(
    private readonly academy: AcademyService,
    private readonly route: ActivatedRoute,
    readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.enrollmentId = this.route.snapshot.paramMap.get('enrollmentId') ?? '';
    this.load();
  }

  load(): void {
    this.loading = true;
    this.academy.getEnrollment(this.enrollmentId).subscribe({
      next: enr => {
        this.enrollment = enr;
        this.academy.outline(enr.courseId)
          .pipe(finalize(() => (this.loading = false)))
          .subscribe({
            next: o => (this.outline = o),
            error: () => this.fail($localize`:@@academy.error.outline:Impossible de charger le cours.`)
          });
      },
      error: () => {
        this.loading = false;
        this.fail($localize`:@@academy.error.enrollment:Inscription introuvable.`);
      }
    });
  }

  completeLesson(lessonId: string): void {
    if (this.busy || this.doneLessons.has(lessonId)) {
      return;
    }
    this.busy = true;
    this.academy.completeLesson(this.enrollmentId, lessonId)
      .pipe(finalize(() => (this.busy = false)))
      .subscribe({
        next: enr => {
          this.enrollment = enr;
          this.doneLessons.add(lessonId);
        },
        error: () => this.snack.open(
          $localize`:@@academy.error.lesson:Échec de la validation de la leçon.`, 'OK', { duration: 4000 })
      });
  }

  select(quizId: string, questionIndex: number, optionIndex: number): void {
    this.selections[quizId] = this.selections[quizId] ?? {};
    this.selections[quizId][questionIndex] = optionIndex;
  }

  isSelected(quizId: string, questionIndex: number, optionIndex: number): boolean {
    return this.selections[quizId]?.[questionIndex] === optionIndex;
  }

  canSubmit(quiz: QuizForLearner): boolean {
    const sel = this.selections[quiz.id] ?? {};
    return quiz.questions.every((_, i) => sel[i] !== undefined);
  }

  submitQuiz(quiz: QuizForLearner): void {
    if (this.busy || !this.canSubmit(quiz)) {
      return;
    }
    const sel = this.selections[quiz.id] ?? {};
    const answers = quiz.questions.map((_, i) => sel[i]);
    this.busy = true;
    this.academy.submitQuiz(this.enrollmentId, quiz.id, answers)
      .pipe(finalize(() => (this.busy = false)))
      .subscribe({
        next: res => {
          this.results[quiz.id] = res;
          this.enrollment = res.enrollment;
          const msg = res.passed
            ? $localize`:@@academy.quiz.passed:Quiz réussi (${res.score}:score:%) !`
            : $localize`:@@academy.quiz.failed:Quiz échoué (${res.score}:score:%). Réessayez.`;
          this.snack.open(msg, 'OK', { duration: 4000 });
        },
        error: () => this.snack.open(
          $localize`:@@academy.error.quiz:Échec de la soumission du quiz.`, 'OK', { duration: 4000 })
      });
  }

  viewCertificate(): void {
    this.router.navigate(['/academy/certificate', this.enrollmentId]);
  }

  get completed(): boolean {
    return this.enrollment?.status === 'COMPLETED';
  }

  private fail(message: string): void {
    this.snack.open(message, 'OK', { duration: 5000 });
  }
}
