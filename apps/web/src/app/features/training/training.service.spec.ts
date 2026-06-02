import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { TrainingService } from './training.service';

describe('TrainingService (mock mode)', () => {
  let service: TrainingService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(TrainingService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded skills', (done) => {
    service.listSkills().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a skill', (done) => {
    service.createSkill({ code: 'SK', name: 'New skill', category: 'QUALITY' }).subscribe(s => {
      expect(s.name).toBe('New skill');
      done();
    });
  });

  it('lists seeded training paths', (done) => {
    service.listPaths().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('returns a competency matrix for a user', (done) => {
    service.getMatrix('demo-user').subscribe(matrix => {
      expect(matrix.userId).toBe('demo-user');
      expect(Array.isArray(matrix.competencies)).toBeTrue();
      done();
    });
  });

  it('lists seeded enrollments', (done) => {
    service.listEnrollments().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('updateProgress sets progressPct', (done) => {
    service.updateProgress('enr-1', { progressPct: 75 }).subscribe(e => {
      expect(e.progressPct).toBe(75);
      done();
    });
  });

  it('getGap computes skill gaps for a path', (done) => {
    service.getGap('demo-user', 'path-1').subscribe(gap => {
      expect(gap.pathId).toBe('path-1');
      expect(gap.totalRequirements).toBeGreaterThan(0);
      done();
    });
  });
});
