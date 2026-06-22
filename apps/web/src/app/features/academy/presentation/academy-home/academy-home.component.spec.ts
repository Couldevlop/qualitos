import { of, throwError } from 'rxjs';

import { AcademyHomeComponent } from './academy-home.component';
import { AcademyService } from '../../infrastructure/academy.service';
import { CourseResponse, EnrollmentResponse } from '../../domain/academy.types';

describe('AcademyHomeComponent', () => {
  let academy: jasmine.SpyObj<AcademyService>;
  let router: jasmine.SpyObj<any>;
  let snack: jasmine.SpyObj<any>;
  let component: AcademyHomeComponent;

  const course: CourseResponse = {
    id: 'c1', tenantId: 't', code: 'iso', title: 'ISO 9001', passingScore: 70,
    pointsReward: 50, status: 'PUBLISHED', createdBy: 'u', createdAt: '', updatedAt: ''
  };

  beforeEach(() => {
    academy = jasmine.createSpyObj<AcademyService>('AcademyService',
      ['listCourses', 'myEnrollments', 'leaderboard', 'enroll']);
    router = jasmine.createSpyObj('Router', ['navigate']);
    snack = jasmine.createSpyObj('MatSnackBar', ['open']);
    academy.listCourses.and.returnValue(of({ content: [course], totalElements: 1, totalPages: 1, number: 0, size: 50 }));
    academy.myEnrollments.and.returnValue(of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 }));
    academy.leaderboard.and.returnValue(of({ entries: [], totalLearners: 0 }));
    component = new AcademyHomeComponent(academy, router, snack);
  });

  it('loads catalog, enrollments and leaderboard on init', () => {
    component.ngOnInit();
    expect(component.courses).toEqual([course]);
    expect(component.loadingCourses).toBeFalse();
    expect(academy.leaderboard).toHaveBeenCalledWith(10);
  });

  it('shows a snackbar when catalog fails', () => {
    academy.listCourses.and.returnValue(throwError(() => new Error('x')));
    component.loadCourses();
    expect(snack.open).toHaveBeenCalled();
  });

  it('enroll navigates to the player on success', () => {
    const enr = { id: 'e1' } as EnrollmentResponse;
    academy.enroll.and.returnValue(of(enr));
    component.enroll(course);
    expect(academy.enroll).toHaveBeenCalledWith('c1');
    expect(router.navigate).toHaveBeenCalledWith(['/academy/learn', 'e1']);
  });

  it('enroll on 409 shows already-enrolled message and reloads', () => {
    academy.enroll.and.returnValue(throwError(() => ({ status: 409 })));
    component.enroll(course);
    expect(snack.open).toHaveBeenCalled();
    // myEnrollments rappelé (initial + reload).
    expect(academy.myEnrollments).toHaveBeenCalledTimes(1);
  });

  it('isEnrolled ignores cancelled/failed enrollments', () => {
    component.enrollments = [
      { courseId: 'c1', status: 'CANCELLED' } as EnrollmentResponse,
      { courseId: 'c2', status: 'IN_PROGRESS' } as EnrollmentResponse
    ];
    expect(component.isEnrolled('c1')).toBeFalse();
    expect(component.isEnrolled('c2')).toBeTrue();
  });

  it('beltClass derives a css class', () => {
    expect(component.beltClass('BLACK')).toBe('belt belt-black');
  });
});
