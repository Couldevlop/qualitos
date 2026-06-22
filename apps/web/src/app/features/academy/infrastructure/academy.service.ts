import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  CertificateResponse,
  CertificateVerification,
  CourseOutline,
  CourseResponse,
  EnrollmentResponse,
  Leaderboard,
  Page,
  QuizResult
} from '../domain/academy.types';

/**
 * Service HTTP de l'Academy (LMS-light + gamification, §19.3).
 *
 * <p>Cible les endpoints apprenant {@code /api/v1/academy/me/*}, le catalogue
 * {@code /api/v1/academy/courses} et la vérification publique de certificat. Le
 * jeton OAuth2 est attaché par l'intercepteur global ; le {@code tenant_id} est
 * résolu côté backend depuis le JWT (jamais envoyé par le client).</p>
 */
@Injectable({ providedIn: 'root' })
export class AcademyService {

  private readonly base = `${environment.apiBaseUrl}/api/v1/academy`;

  constructor(private readonly http: HttpClient) {}

  // ===== Catalogue (lecture) =====

  listCourses(status = 'PUBLISHED', page = 0, size = 50): Observable<Page<CourseResponse>> {
    const params = new HttpParams().set('status', status).set('page', page).set('size', size);
    return this.http.get<Page<CourseResponse>>(`${this.base}/courses`, { params });
  }

  getCourse(id: string): Observable<CourseResponse> {
    return this.http.get<CourseResponse>(`${this.base}/courses/${id}`);
  }

  // ===== Parcours apprenant =====

  outline(courseId: string): Observable<CourseOutline> {
    return this.http.get<CourseOutline>(`${this.base}/me/courses/${courseId}/outline`);
  }

  enroll(courseId: string): Observable<EnrollmentResponse> {
    return this.http.post<EnrollmentResponse>(`${this.base}/me/enrollments`, { courseId });
  }

  myEnrollments(page = 0, size = 50): Observable<Page<EnrollmentResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<EnrollmentResponse>>(`${this.base}/me/enrollments`, { params });
  }

  getEnrollment(id: string): Observable<EnrollmentResponse> {
    return this.http.get<EnrollmentResponse>(`${this.base}/me/enrollments/${id}`);
  }

  cancelEnrollment(id: string): Observable<EnrollmentResponse> {
    return this.http.post<EnrollmentResponse>(`${this.base}/me/enrollments/${id}/cancel`, {});
  }

  completeLesson(enrollmentId: string, lessonId: string): Observable<EnrollmentResponse> {
    return this.http.post<EnrollmentResponse>(
      `${this.base}/me/enrollments/${enrollmentId}/complete-lesson`, { lessonId });
  }

  submitQuiz(enrollmentId: string, quizId: string, answers: number[]): Observable<QuizResult> {
    return this.http.post<QuizResult>(
      `${this.base}/me/enrollments/${enrollmentId}/submit-quiz`, { quizId, answers });
  }

  certificate(enrollmentId: string): Observable<CertificateResponse> {
    return this.http.get<CertificateResponse>(
      `${this.base}/me/enrollments/${enrollmentId}/certificate`);
  }

  // ===== Gamification =====

  leaderboard(size = 20): Observable<Leaderboard> {
    const params = new HttpParams().set('size', size);
    return this.http.get<Leaderboard>(`${this.base}/me/leaderboard`, { params });
  }

  // ===== Vérification publique de certificat (QR) =====

  verifyCertificate(code: string): Observable<CertificateVerification> {
    return this.http.get<CertificateVerification>(
      `${this.base}/public/certificates/${encodeURIComponent(code)}/verify`);
  }
}
