import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

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

  // --- Filtres du catalogue -------------------------------------------------

  it('filtre les compétences sur la catégorie demandée', (done) => {
    service.listSkills(0, 50, 'Audit').subscribe(page => {
      expect(page.content.length).toBe(1);
      expect(page.content[0].code).toBe('iso-9001-internal-auditor');
      expect(page.totalElements).toBe(1);
      done();
    });
  });

  it('filtre les parcours par statut puis par rôle cible', (done) => {
    service.listPaths(0, 50, 'DRAFT').subscribe(byStatus => {
      expect(byStatus.content.map(p => p.code)).toEqual(['iso-9001-lead-auditor']);
      service.listPaths(0, 50, undefined, 'Auditeur').subscribe(byRole => {
        expect(byRole.content.map(p => p.code)).toEqual(['iso-9001-lead-auditor']);
        done();
      });
    });
  });

  it('filtre les inscriptions par statut, parcours et utilisateur', (done) => {
    service.listEnrollments(0, 50, { status: 'ENROLLED' }).subscribe(byStatus => {
      expect(byStatus.content.map(e => e.id)).toEqual(['enr-2']);
      service.listEnrollments(0, 50, { pathId: 'path-1' }).subscribe(byPath => {
        expect(byPath.content.map(e => e.id)).toEqual(['enr-1']);
        service.listEnrollments(0, 50, { userId: 'quelquun-dautre' }).subscribe(byUser => {
          expect(byUser.content).toEqual([]);
          done();
        });
      });
    });
  });

  // --- Cycle de vie des compétences et des parcours -------------------------

  it('supprime une compétence du catalogue', (done) => {
    service.deleteSkill('skill-1').subscribe(() => {
      service.listSkills().subscribe(page => {
        expect(page.content.some(s => s.id === 'skill-1')).toBeFalse();
        done();
      });
    });
  });

  it('renomme une compétence sans changer son code', (done) => {
    service.updateSkill('skill-2', { name: 'SPC avancé' }).subscribe(s => {
      expect(s.name).toBe('SPC avancé');
      expect(s.code).toBe('spc-control-charts');
      done();
    });
  });

  it('crée un parcours à l’état DRAFT puis l’active, le rouvre et l’archive', (done) => {
    service.createPath({
      code: 'lean-basics', name: 'Lean — bases', durationHours: 7, createdBy: 'demo-user'
    }).subscribe(created => {
      expect(created.status).toBe('DRAFT');
      expect(created.passingScore).toBe(70);   // valeur par défaut quand non fournie
      service.activatePath(created.id).subscribe(active => {
        expect(active.status).toBe('ACTIVE');
        service.reopenPath(created.id).subscribe(draft => {
          expect(draft.status).toBe('DRAFT');
          service.archivePath(created.id).subscribe(archived => {
            expect(archived.status).toBe('ARCHIVED');
            done();
          });
        });
      });
    });
  });

  it('supprime un parcours et ses exigences', (done) => {
    service.deletePath('path-1').subscribe(() => {
      service.listPaths().subscribe(page => {
        expect(page.content.some(p => p.id === 'path-1')).toBeFalse();
        service.listRequirements('path-1').subscribe(reqs => {
          expect(reqs).toEqual([]);
          done();
        });
      });
    });
  });

  it('remplace l’exigence existante quand la même compétence est rattachée deux fois', (done) => {
    service.attachRequirement('path-1', { skillId: 'skill-3', targetLevel: 4 }).subscribe(() => {
      service.listRequirements('path-1').subscribe(reqs => {
        expect(reqs.length).toBe(2);   // pas de doublon
        expect(reqs.find(r => r.skillId === 'skill-3')?.targetLevel).toBe(4);
        done();
      });
    });
  });

  it('détache une exigence du parcours', (done) => {
    service.detachRequirement('path-1', 'skill-3').subscribe(() => {
      service.listRequirements('path-1').subscribe(reqs => {
        expect(reqs.map(r => r.skillId)).toEqual(['skill-4']);
        done();
      });
    });
  });

  // --- Matrice de compétences et analyse d'écart ----------------------------

  it('évalue une compétence, nomme le niveau et signale l’expiration', (done) => {
    service.assessCompetency({
      userId: 'demo-user', skillId: 'skill-3', level: 3, source: 'MANAGER',
      expiresOn: '2020-01-01'
    }).subscribe(c => {
      expect(c.levelName).toBe('COMPETENT');
      expect(c.expired).toBeTrue();
      done();
    });
  });

  it('remplace l’évaluation précédente de la même compétence', (done) => {
    service.assessCompetency({ userId: 'u9', skillId: 'skill-3', level: 1, source: 'SELF' }).subscribe(() => {
      service.assessCompetency({ userId: 'u9', skillId: 'skill-3', level: 4, source: 'AUDIT' }).subscribe(() => {
        service.getMatrix('u9').subscribe(m => {
          expect(m.competencies.length).toBe(1);
          expect(m.competencies[0].level).toBe(4);
          expect(m.competencies[0].levelName).toBe('EXPERT');
          done();
        });
      });
    });
  });

  it('réduit l’écart du parcours dès qu’une compétence atteint le niveau cible', (done) => {
    service.assessCompetency({
      userId: 'demo-user', skillId: 'skill-3', level: 2, source: 'TRAINING'
    }).subscribe(() => {
      service.getGap('demo-user', 'path-1').subscribe(gap => {
        expect(gap.pathCode).toBe('yellow-belt-quality');
        expect(gap.totalRequirements).toBe(2);
        expect(gap.satisfied).toBe(1);
        expect(gap.gaps.map(g => g.skillCode)).toEqual(['capa-rca-5whys']);
        expect(gap.gaps[0].gap).toBe(2);   // cible 2 − acquis 0
        done();
      });
    });
  });

  // --- Inscriptions ---------------------------------------------------------

  it('inscrit un apprenant puis démarre son parcours', (done) => {
    service.enroll({ userId: 'demo-user', pathId: 'path-3' }).subscribe(e => {
      expect(e.status).toBe('ENROLLED');
      expect(e.progressPct).toBe(0);
      service.startEnrollment(e.id).subscribe(started => {
        expect(started.status).toBe('IN_PROGRESS');
        expect(started.startedOn).toBeTruthy();
        done();
      });
    });
  });

  it('délivre un certificat et une date d’expiration quand le score atteint le seuil', (done) => {
    // path-1 : passingScore 70, validityMonths 36.
    service.completeEnrollment('enr-1', { finalScore: 85 }).subscribe(e => {
      expect(e.status).toBe('COMPLETED');
      expect(e.progressPct).toBe(100);
      expect(e.certificateCode).toMatch(/^CERT-/);
      const years = new Date(e.expiresOn!).getFullYear() - new Date().getFullYear();
      expect(years).toBe(3);
      done();
    });
  });

  it('marque l’inscription en échec sans certificat sous le seuil de réussite', (done) => {
    service.completeEnrollment('enr-1', { finalScore: 69 }).subscribe(e => {
      expect(e.status).toBe('FAILED');
      expect(e.certificateCode).toBeUndefined();
      expect(e.expiresOn).toBeUndefined();
      done();
    });
  });

  it('annule une inscription', (done) => {
    service.cancelEnrollment('enr-2').subscribe(e => {
      expect(e.status).toBe('CANCELLED');
      done();
    });
  });
});

/**
 * Mode HTTP (useMockApi=false) : c'est le chemin réellement exécuté en production.
 * On y vérifie les routes, les verbes et les paramètres — le `tenant_id` n'est jamais
 * transmis par le client (règle §18.2 #2), il est dérivé du JWT côté serveur.
 */
describe('TrainingService (HTTP)', () => {
  let service: TrainingService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/training`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(TrainingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    environment.useMockApi = prevMock;
  });

  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  it('n’envoie le filtre catégorie que lorsqu’il est renseigné', () => {
    service.listSkills(2, 25).subscribe();
    const plain = http.expectOne(r => r.url === `${base}/skills`);
    expect(plain.request.params.get('page')).toBe('2');
    expect(plain.request.params.get('size')).toBe('25');
    expect(plain.request.params.has('category')).toBeFalse();
    plain.flush(page([]));

    service.listSkills(0, 50, 'Audit').subscribe();
    const filtered = http.expectOne(r => r.url === `${base}/skills`);
    expect(filtered.request.params.get('category')).toBe('Audit');
    filtered.flush(page([]));
  });

  it('crée, lit, modifie et supprime une compétence sur les bonnes routes', () => {
    service.createSkill({ code: 'c', name: 'n' }).subscribe();
    const created = http.expectOne(`${base}/skills`);
    expect(created.request.method).toBe('POST');
    expect(created.request.body).toEqual({ code: 'c', name: 'n' });
    created.flush({});

    service.getSkill('s1').subscribe();
    const read = http.expectOne(`${base}/skills/s1`);
    expect(read.request.method).toBe('GET');
    read.flush({});

    service.updateSkill('s1', { name: 'n2' }).subscribe();
    const patched = http.expectOne(`${base}/skills/s1`);
    expect(patched.request.method).toBe('PATCH');
    patched.flush({});

    service.deleteSkill('s1').subscribe();
    const removed = http.expectOne(`${base}/skills/s1`);
    expect(removed.request.method).toBe('DELETE');
    removed.flush(null);
  });

  it('appelle les routes de compétences et d’analyse d’écart', () => {
    service.assessCompetency({ userId: 'u1', skillId: 's1', level: 2, source: 'MANAGER' }).subscribe();
    const assess = http.expectOne(`${base}/competencies/assess`);
    expect(assess.request.method).toBe('POST');
    assess.flush({});

    service.getMatrix('u1').subscribe();
    http.expectOne(`${base}/competencies/users/u1`).flush({ userId: 'u1', competencies: [] });

    service.getGap('u1', 'p1').subscribe();
    const gap = http.expectOne(r => r.url === `${base}/competencies/users/u1/gap`);
    expect(gap.request.params.get('pathId')).toBe('p1');
    gap.flush({});
  });

  it('n’envoie statut et rôle cible que lorsqu’ils sont fournis', () => {
    service.listPaths(0, 20).subscribe();
    const plain = http.expectOne(r => r.url === `${base}/paths`);
    expect(plain.request.params.has('status')).toBeFalse();
    expect(plain.request.params.has('targetRole')).toBeFalse();
    plain.flush(page([]));

    service.listPaths(1, 10, 'ACTIVE', 'Auditeur').subscribe();
    const filtered = http.expectOne(r => r.url === `${base}/paths`);
    expect(filtered.request.params.get('status')).toBe('ACTIVE');
    expect(filtered.request.params.get('targetRole')).toBe('Auditeur');
    expect(filtered.request.params.get('page')).toBe('1');
    filtered.flush(page([]));
  });

  it('poste chaque transition de parcours sur son propre verbe métier', () => {
    service.activatePath('p1').subscribe();
    const activate = http.expectOne(`${base}/paths/p1/activate`);
    expect(activate.request.method).toBe('POST');
    expect(activate.request.body).toEqual({});
    activate.flush({});

    service.reopenPath('p1').subscribe();
    http.expectOne(`${base}/paths/p1/reopen`).flush({});

    service.archivePath('p1').subscribe();
    http.expectOne(`${base}/paths/p1/archive`).flush({});
  });

  it('gère les exigences de compétences d’un parcours', () => {
    service.listRequirements('p1').subscribe();
    http.expectOne(`${base}/paths/p1/requirements`).flush([]);

    service.attachRequirement('p1', { skillId: 's1', targetLevel: 3 }).subscribe();
    const attach = http.expectOne(`${base}/paths/p1/requirements`);
    expect(attach.request.method).toBe('POST');
    expect(attach.request.body).toEqual({ skillId: 's1', targetLevel: 3 });
    attach.flush({});

    service.detachRequirement('p1', 's1').subscribe();
    const detach = http.expectOne(`${base}/paths/p1/requirements/s1`);
    expect(detach.request.method).toBe('DELETE');
    detach.flush(null);
  });

  it('filtre les inscriptions côté serveur et non en mémoire', () => {
    service.listEnrollments(0, 50, { userId: 'u1', pathId: 'p1', status: 'IN_PROGRESS' }).subscribe();
    const req = http.expectOne(r => r.url === `${base}/enrollments`);
    expect(req.request.params.get('userId')).toBe('u1');
    expect(req.request.params.get('pathId')).toBe('p1');
    expect(req.request.params.get('status')).toBe('IN_PROGRESS');
    req.flush(page([]));
  });

  it('poste les actions du cycle de vie d’une inscription', () => {
    service.enroll({ userId: 'u1', pathId: 'p1' }).subscribe();
    const enroll = http.expectOne(`${base}/enrollments`);
    expect(enroll.request.body).toEqual({ userId: 'u1', pathId: 'p1' });
    enroll.flush({});

    service.startEnrollment('e1').subscribe();
    http.expectOne(`${base}/enrollments/e1/start`).flush({});

    service.updateProgress('e1', { progressPct: 60 }).subscribe();
    const progress = http.expectOne(`${base}/enrollments/e1/progress`);
    expect(progress.request.body).toEqual({ progressPct: 60 });
    progress.flush({});

    service.completeEnrollment('e1', { finalScore: 90 }).subscribe();
    const complete = http.expectOne(`${base}/enrollments/e1/complete`);
    expect(complete.request.body).toEqual({ finalScore: 90 });
    complete.flush({});

    service.cancelEnrollment('e1').subscribe();
    http.expectOne(`${base}/enrollments/e1/cancel`).flush({});
  });

  it('lit et met à jour la progression gamifiée de l’utilisateur du JWT', () => {
    service.myProgress().subscribe();
    const me = http.expectOne(`${base}/progress/me`);
    expect(me.request.method).toBe('GET');
    expect(me.request.url).not.toContain('userId');   // l'utilisateur vient du JWT
    me.flush({});

    service.completeLearning({ itemCode: 'yellow-belt-quality', score: 88 }).subscribe();
    const complete = http.expectOne(`${base}/progress/complete`);
    expect(complete.request.body).toEqual({ itemCode: 'yellow-belt-quality', score: 88 });
    complete.flush({});
  });

  it('propage l’erreur HTTP au souscripteur au lieu de la masquer', (done) => {
    service.listPaths().subscribe({
      next: () => fail('un 403 ne doit pas produire de valeur'),
      error: err => { expect(err.status).toBe(403); done(); }
    });
    http.expectOne(r => r.url === `${base}/paths`)
      .flush({ title: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
  });
});
