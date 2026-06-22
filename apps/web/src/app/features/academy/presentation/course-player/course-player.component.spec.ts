import { of, throwError } from 'rxjs';

import { CoursePlayerComponent } from './course-player.component';
import { AcademyService } from '../../infrastructure/academy.service';
import {
  CourseOutline,
  EnrollmentResponse,
  QuizForLearner,
  QuizResult
} from '../../domain/academy.types';

describe('CoursePlayerComponent', () => {
  let academy: jasmine.SpyObj<AcademyService>;
  let router: jasmine.SpyObj<any>;
  let snack: jasmine.SpyObj<any>;
  let component: CoursePlayerComponent;

  const route: any = { snapshot: { paramMap: { get: (_: string) => 'enr-1' } } };

  const enrollment: EnrollmentResponse = {
    id: 'enr-1', tenantId: 't', userId: 'u', courseId: 'c1', status: 'IN_PROGRESS',
    progressPct: 50, enrolledAt: '', createdAt: '', updatedAt: ''
  };

  const quiz: QuizForLearner = {
    id: 'q1', moduleId: 'm1', title: 'Quiz', passScore: 70,
    questions: [
      { id: 'qa', stem: 'Q1', options: ['A', 'B'], points: 1, orderIndex: 0 },
      { id: 'qb', stem: 'Q2', options: ['A', 'B'], points: 1, orderIndex: 1 }
    ]
  };

  const outline: CourseOutline = {
    course: { id: 'c1', tenantId: 't', code: 'iso', title: 'ISO', passingScore: 70,
      pointsReward: 50, status: 'PUBLISHED', createdBy: 'u', createdAt: '', updatedAt: '' },
    modules: [{
      module: { id: 'm1', courseId: 'c1', title: 'M1', orderIndex: 0, createdAt: '', updatedAt: '' },
      lessons: [{ id: 'l1', moduleId: 'm1', title: 'L1', contentType: 'TEXT', durationMinutes: 5,
        orderIndex: 0, createdAt: '', updatedAt: '' }],
      quiz
    }]
  };

  beforeEach(() => {
    academy = jasmine.createSpyObj<AcademyService>('AcademyService',
      ['getEnrollment', 'outline', 'completeLesson', 'submitQuiz']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    snack = jasmine.createSpyObj('MatSnackBar', ['open']);
    academy.getEnrollment.and.returnValue(of(enrollment));
    academy.outline.and.returnValue(of(outline));
    component = new CoursePlayerComponent(academy, route, router, snack);
  });

  it('loads enrollment and outline on init', () => {
    component.ngOnInit();
    expect(component.enrollment).toEqual(enrollment);
    expect(component.outline).toEqual(outline);
    expect(component.loading).toBeFalse();
  });

  it('completeLesson marks lesson done and updates enrollment', () => {
    component.ngOnInit();
    const updated = { ...enrollment, progressPct: 100 };
    academy.completeLesson.and.returnValue(of(updated));
    component.completeLesson('l1');
    expect(academy.completeLesson).toHaveBeenCalledWith('enr-1', 'l1');
    expect(component.doneLessons.has('l1')).toBeTrue();
    expect(component.enrollment!.progressPct).toBe(100);
  });

  it('does not re-complete an already done lesson', () => {
    component.ngOnInit();
    component.doneLessons.add('l1');
    component.completeLesson('l1');
    expect(academy.completeLesson).not.toHaveBeenCalled();
  });

  it('select / isSelected track answers, canSubmit requires all answered', () => {
    component.ngOnInit();
    expect(component.canSubmit(quiz)).toBeFalse();
    component.select('q1', 0, 1);
    expect(component.isSelected('q1', 0, 1)).toBeTrue();
    expect(component.canSubmit(quiz)).toBeFalse();
    component.select('q1', 1, 0);
    expect(component.canSubmit(quiz)).toBeTrue();
  });

  it('submitQuiz sends answers and stores result; passing shows success', () => {
    component.ngOnInit();
    component.select('q1', 0, 0);
    component.select('q1', 1, 1);
    const completed = { ...enrollment, status: 'COMPLETED' as const, finalScore: 100 };
    const result: QuizResult = { attemptId: 'a', quizId: 'q1', score: 100, passed: true,
      earnedPoints: 2, totalPoints: 2, enrollment: completed };
    academy.submitQuiz.and.returnValue(of(result));

    component.submitQuiz(quiz);

    expect(academy.submitQuiz).toHaveBeenCalledWith('enr-1', 'q1', [0, 1]);
    expect(component.results['q1']).toEqual(result);
    expect(component.completed).toBeTrue();
    expect(snack.open).toHaveBeenCalled();
  });

  it('submitQuiz is blocked when not all answered', () => {
    component.ngOnInit();
    component.select('q1', 0, 0);
    component.submitQuiz(quiz);
    expect(academy.submitQuiz).not.toHaveBeenCalled();
  });

  it('viewCertificate navigates', () => {
    component.ngOnInit();
    component.viewCertificate();
    expect(router.navigate).toHaveBeenCalledWith(['/academy/certificate', 'enr-1']);
  });

  it('shows error when enrollment fails to load', () => {
    academy.getEnrollment.and.returnValue(throwError(() => new Error('x')));
    component.ngOnInit();
    expect(snack.open).toHaveBeenCalled();
    expect(component.loading).toBeFalse();
  });
});
