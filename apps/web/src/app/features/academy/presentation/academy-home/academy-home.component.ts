import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize } from 'rxjs/operators';

import { AcademyService } from '../../infrastructure/academy.service';
import {
  CourseResponse,
  EnrollmentResponse,
  Leaderboard
} from '../../domain/academy.types';

/**
 * Page d'accueil de l'Academy (§19.3) : catalogue des cours publiés, mes
 * formations en cours, et le classement (leaderboard) de gamification du tenant.
 */
@Component({
  selector: 'qos-academy-home',
  templateUrl: './academy-home.component.html',
  styleUrls: ['./academy-home.component.scss'],
  standalone: false
})
export class AcademyHomeComponent implements OnInit {

  courses: CourseResponse[] = [];
  enrollments: EnrollmentResponse[] = [];
  leaderboard?: Leaderboard;

  loadingCourses = false;
  loadingEnrollments = false;
  loadingLeaderboard = false;
  enrollingId: string | null = null;

  constructor(
    private readonly academy: AcademyService,
    readonly router: Router,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadCourses();
    this.loadEnrollments();
    this.loadLeaderboard();
  }

  loadCourses(): void {
    this.loadingCourses = true;
    this.academy.listCourses('PUBLISHED')
      .pipe(finalize(() => (this.loadingCourses = false)))
      .subscribe({
        next: page => (this.courses = page.content),
        error: () => this.snack.open($localize`:@@academy.error.courses:Impossible de charger le catalogue.`, 'OK', { duration: 4000 })
      });
  }

  loadEnrollments(): void {
    this.loadingEnrollments = true;
    this.academy.myEnrollments()
      .pipe(finalize(() => (this.loadingEnrollments = false)))
      .subscribe({
        next: page => (this.enrollments = page.content),
        error: () => { /* silencieux : section secondaire */ }
      });
  }

  loadLeaderboard(): void {
    this.loadingLeaderboard = true;
    this.academy.leaderboard(10)
      .pipe(finalize(() => (this.loadingLeaderboard = false)))
      .subscribe({
        next: board => (this.leaderboard = board),
        error: () => { /* silencieux */ }
      });
  }

  enroll(course: CourseResponse): void {
    this.enrollingId = course.id;
    this.academy.enroll(course.id)
      .pipe(finalize(() => (this.enrollingId = null)))
      .subscribe({
        next: enr => this.router.navigate(['/academy/learn', enr.id]),
        error: err => {
          const msg = err?.status === 409
            ? $localize`:@@academy.error.alreadyEnrolled:Vous êtes déjà inscrit à ce cours.`
            : $localize`:@@academy.error.enroll:L'inscription a échoué.`;
          this.snack.open(msg, 'OK', { duration: 4000 });
          this.loadEnrollments();
        }
      });
  }

  openEnrollment(enr: EnrollmentResponse): void {
    this.router.navigate(['/academy/learn', enr.id]);
  }

  isEnrolled(courseId: string): boolean {
    return this.enrollments.some(
      e => e.courseId === courseId && e.status !== 'CANCELLED' && e.status !== 'FAILED');
  }

  beltClass(belt: string): string {
    return 'belt belt-' + belt.toLowerCase();
  }

  trackById(_: number, item: { id: string }): string {
    return item.id;
  }
}
