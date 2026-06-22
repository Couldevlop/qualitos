import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { AcademyService } from './academy.service';

describe('AcademyService', () => {
  let service: AcademyService;
  let httpMock: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/academy`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AcademyService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('listCourses defaults to PUBLISHED and passes paging', () => {
    let result: any;
    service.listCourses().subscribe(r => (result = r));
    const req = httpMock.expectOne(r => r.url === `${base}/courses`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('status')).toBe('PUBLISHED');
    expect(req.request.params.get('size')).toBe('50');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 });
    expect(result.totalElements).toBe(0);
  });

  it('enroll POSTs the courseId in the body (never tenant)', () => {
    service.enroll('course-1').subscribe();
    const req = httpMock.expectOne(`${base}/me/enrollments`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ courseId: 'course-1' });
    expect(req.request.body.tenantId).toBeUndefined();
    req.flush({});
  });

  it('submitQuiz POSTs quizId and answers', () => {
    service.submitQuiz('enr-1', 'quiz-1', [0, 2, 1]).subscribe();
    const req = httpMock.expectOne(`${base}/me/enrollments/enr-1/submit-quiz`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ quizId: 'quiz-1', answers: [0, 2, 1] });
    req.flush({ attemptId: 'a', quizId: 'quiz-1', score: 80, passed: true,
      earnedPoints: 4, totalPoints: 5, enrollment: {} });
  });

  it('completeLesson POSTs the lessonId', () => {
    service.completeLesson('enr-1', 'lesson-9').subscribe();
    const req = httpMock.expectOne(`${base}/me/enrollments/enr-1/complete-lesson`);
    expect(req.request.body).toEqual({ lessonId: 'lesson-9' });
    req.flush({});
  });

  it('outline calls the learner outline endpoint', () => {
    service.outline('course-1').subscribe();
    const req = httpMock.expectOne(`${base}/me/courses/course-1/outline`);
    expect(req.request.method).toBe('GET');
    req.flush({ course: {}, modules: [] });
  });

  it('leaderboard passes size param', () => {
    service.leaderboard(5).subscribe();
    const req = httpMock.expectOne(r => r.url === `${base}/me/leaderboard`);
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ entries: [], totalLearners: 0 });
  });

  it('verifyCertificate hits the public endpoint and url-encodes the code', () => {
    service.verifyCertificate('CODE/1').subscribe();
    const req = httpMock.expectOne(`${base}/public/certificates/CODE%2F1/verify`);
    expect(req.request.method).toBe('GET');
    req.flush({ code: 'CODE/1', valid: true });
  });

  it('certificate fetches the enrollment certificate', () => {
    service.certificate('enr-1').subscribe();
    const req = httpMock.expectOne(`${base}/me/enrollments/enr-1/certificate`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('cancelEnrollment POSTs to cancel', () => {
    service.cancelEnrollment('enr-1').subscribe();
    const req = httpMock.expectOne(`${base}/me/enrollments/enr-1/cancel`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('propagates HTTP errors', () => {
    let err: any;
    service.listCourses().subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(r => r.url === `${base}/courses`);
    req.flush('boom', { status: 500, statusText: 'Server Error' });
    expect(err.status).toBe(500);
  });
});
