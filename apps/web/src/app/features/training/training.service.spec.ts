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

  // --- Gamification (§19.3) ---

  it('myProgress returns the seeded learner progress', (done) => {
    service.myProgress().subscribe(p => {
      expect(p.beltLevel).toBe('YELLOW');
      expect(p.points).toBe(160);
      expect(p.badges).toContain('FIRST_STEPS');
      done();
    });
  });

  it('completeLearning with a passing score adds points and a completion', (done) => {
    service.myProgress().subscribe(before => {
      service.completeLearning({ itemCode: 'green-belt-six-sigma', score: 80 }).subscribe(after => {
        expect(after.points).toBe(before.points + 50 + 40);   // base + bonus(80/2)
        expect(after.completedCount).toBe(before.completedCount + 1);
        done();
      });
    });
  });

  it('completeLearning crossing 700 points promotes to BLACK belt with belt badges', (done) => {
    // 160 + plusieurs complétions parfaites (100 → 100 pts) franchit GREEN puis BLACK.
    const run = (n: number): void => {
      if (n === 0) {
        service.myProgress().subscribe(p => {
          expect(p.beltLevel).toBe('BLACK');
          expect(p.badges).toContain('BLACK_BELT');
          expect(p.badges).toContain('PERFECTIONIST');
          done();
        });
        return;
      }
      service.completeLearning({ itemCode: 'x' + n, score: 100 }).subscribe(() => run(n - 1));
    };
    run(6);   // 160 + 6*100 = 760 ≥ 700
  });

  it('completeLearning with a failing score grants no points', (done) => {
    service.myProgress().subscribe(before => {
      service.completeLearning({ itemCode: 'hard', score: 40 }).subscribe(after => {
        expect(after.points).toBe(before.points);
        expect(after.completedCount).toBe(before.completedCount);
        done();
      });
    });
  });
});
