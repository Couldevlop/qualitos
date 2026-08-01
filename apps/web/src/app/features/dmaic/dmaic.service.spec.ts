import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DmaicService } from './dmaic.service';
import { AssignmentResponse, DeviceDetail, DmaicProjectResponse, MeasureResponse } from './dmaic.types';

/**
 * Le service a deux implémentations derrière la même signature : le mode démo
 * hors-ligne (`useMockApi`) et l'API réelle. Les deux sont livrées au navigateur,
 * les deux doivent donc être vérifiées — un écart de contrat entre elles se paie
 * au moment où un tenant bascule sur le vrai backend.
 */
describe('DmaicService (mode démo hors-ligne)', () => {
  let service: DmaicService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DmaicService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded projects', (done) => {
    service.listProjects().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters projects by phase', (done) => {
    service.listProjects(0, 20, undefined, 'MEASURE').subscribe(page => {
      expect(page.content.every(p => p.phase === 'MEASURE')).toBeTrue();
      done();
    });
  });

  it('ne retient que les projets du statut demandé', (done) => {
    service.listProjects(0, 20, 'CANCELLED').subscribe(page => {
      expect(page.content.length).toBe(0);
      expect(page.totalElements).toBe(0);
      done();
    });
  });

  it('creates a project in DEFINE/ACTIVE', (done) => {
    service.createProject({ title: 'Projet test', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        expect(p.phase).toBe('DEFINE');
        expect(p.status).toBe('ACTIVE');
        done();
      });
  });

  it('advance moves DEFINE -> MEASURE', (done) => {
    service.createProject({ title: 'Adv', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        service.advance(p.id).subscribe(adv => {
          expect(adv.phase).toBe('MEASURE');
          done();
        });
      });
  });

  it('hold then resume toggles status', (done) => {
    service.createProject({ title: 'Hold', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        service.hold(p.id).subscribe(h => {
          expect(h.status).toBe('ON_HOLD');
          service.resume(p.id).subscribe(r => {
            expect(r.status).toBe('ACTIVE');
            done();
          });
        });
      });
  });

  it('clôt le projet quand on avance au-delà de CONTROL', (done) => {
    // « dmaic-3 » est semé en phase CONTROL : un pas de plus termine le projet.
    service.advance('dmaic-3').subscribe(p => {
      expect(p.phase).toBe('CONTROL');
      expect(p.status).toBe('COMPLETED');
      expect(p.completedAt).toBeTruthy();
      done();
    });
  });

  it('annule un projet sans toucher à sa phase', (done) => {
    service.cancel('dmaic-2').subscribe(p => {
      expect(p.status).toBe('CANCELLED');
      expect(p.phase).toBe('ANALYZE');
      done();
    });
  });

  it('applique la mise à jour partielle sur le projet visé', (done) => {
    service.updateProject('dmaic-2', { title: 'Titre revu', specUnit: 'ms' }).subscribe(p => {
      expect(p.title).toBe('Titre revu');
      expect(p.specUnit).toBe('ms');
      // Les champs non fournis restent en place.
      expect(p.specUpperLimit).toBe(35);
      done();
    });
  });

  it('retire le projet supprimé de la liste', (done) => {
    service.deleteProject('dmaic-2').subscribe(() => {
      service.listProjects().subscribe(page => {
        expect(page.content.some(p => p.id === 'dmaic-2')).toBeFalse();
        done();
      });
    });
  });

  it('capability warns when fewer than 2 measures', (done) => {
    service.createProject({ title: 'Cap', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        service.capability(p.id).subscribe(cap => {
          expect(cap.sampleSize).toBe(0);
          expect(cap.warnings?.length).toBeGreaterThan(0);
          done();
        });
      });
  });

  it('addMeasure feeds capability computation (mean/stdDev)', (done) => {
    service.createProject({
      title: 'Cap2', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb',
      specLowerLimit: 9, specUpperLimit: 11
    }).subscribe(p => {
      service.addMeasure(p.id, { value: 10 }).subscribe(() => {
        service.addMeasure(p.id, { value: 10.2 }).subscribe(() => {
          service.capability(p.id).subscribe(cap => {
            expect(cap.sampleSize).toBe(2);
            expect(cap.mean).toBeCloseTo(10.1, 5);
            expect(cap.cpk).toBeDefined();
            done();
          });
        });
      });
    });
  });

  it('qualifie un processus capable, marginal ou à améliorer selon le Cpk', (done) => {
    const capabilityOf = (
      lsl: number, usl: number, values: number[]
    ): Promise<{ cpk?: number; interpretation?: string }> =>
      new Promise(resolve => {
        service.createProject({ title: 'C', blackBeltId: 'bb', specLowerLimit: lsl, specUpperLimit: usl })
          .subscribe(p => {
            const push = (i: number): void => {
              if (i >= values.length) {
                service.capability(p.id).subscribe(c => resolve(c));
                return;
              }
              service.addMeasure(p.id, { value: values[i] }).subscribe(() => push(i + 1));
            };
            push(0);
          });
      });

    Promise.all([
      capabilityOf(9, 11, [10, 10.2]),      // centré, dispersion faible → capable
      capabilityOf(8.06, 13.5, [10, 11]),   // Cpk ≈ 1.15 → marginal
      capabilityOf(9, 11, [12, 12.2])       // hors tolérance → à améliorer
    ]).then(([capable, marginal, bad]) => {
      expect(capable.cpk!).toBeGreaterThanOrEqual(1.33);
      expect(capable.interpretation).toBe('Processus capable.');
      expect(marginal.cpk!).toBeGreaterThanOrEqual(1);
      expect(marginal.cpk!).toBeLessThan(1.33);
      expect(marginal.interpretation).toBe('Processus marginalement capable.');
      expect(bad.cpk!).toBeLessThan(1);
      expect(bad.interpretation).toBe('Processus à améliorer.');
      done();
    });
  });

  it('calcule un Cpk unilatéral quand une seule limite est spécifiée', (done) => {
    service.createProject({ title: 'Unilat', blackBeltId: 'bb', specUpperLimit: 11 }).subscribe(p => {
      service.addMeasure(p.id, { value: 10 }).subscribe(() => {
        service.addMeasure(p.id, { value: 10.2 }).subscribe(() => {
          service.capability(p.id).subscribe(cap => {
            expect(cap.cp).toBeUndefined();          // Cp exige les deux limites
            expect(cap.cpl).toBeUndefined();
            expect(cap.cpk).toBe(cap.cpu);
            expect(cap.sigmaLevel).toBeCloseTo(cap.cpk! * 3, 5);
            done();
          });
        });
      });
    });
  });

  it('retire la mesure supprimée du compteur du projet', (done) => {
    service.addMeasure('dmaic-1', { value: 5 }).subscribe(m => {
      expect(service.listMeasures('dmaic-1').length).toBe(1);
      service.deleteMeasure('dmaic-1', m.id).subscribe(() => {
        expect(service.listMeasures('dmaic-1').length).toBe(0);
        service.getProject('dmaic-1').subscribe(p => {
          expect(p.measureCount).toBe(0);
          done();
        });
      });
    });
  });

  it('lists the Poka-Yoke catalog and a device detail', (done) => {
    service.listDevices().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      service.getDevice('pk-1').subscribe(d => {
        expect(d.id).toBe('pk-1');
        expect(d.description).toBeTruthy();
        done();
      });
    });
  });

  it('filtre le catalogue par type et par mécanisme', (done) => {
    service.listDevices(0, 50, 'DETECTION').subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      expect(page.content.every(d => d.type === 'DETECTION')).toBeTrue();
      service.listDevices(0, 50, undefined, 'VISION').subscribe(vision => {
        expect(vision.content.map(d => d.id)).toEqual(['pk-2']);
        done();
      });
    });
  });

  it('assignDevice creates a PROPOSED assignment', (done) => {
    service.assignDevice('dmaic-1', { deviceId: 'pk-1' }).subscribe(a => {
      expect(a.status).toBe('PROPOSED');
      expect(a.deviceId).toBe('pk-1');
      done();
    });
  });

  it('horodate l\'assignation quand elle passe implémentée puis vérifiée', (done) => {
    service.assignDevice('dmaic-1', { deviceId: 'pk-3', note: 'pilote' }).subscribe(a => {
      service.updateAssignment('dmaic-1', a.id, { status: 'IMPLEMENTED', defectReductionPct: 42 })
        .subscribe(impl => {
          expect(impl.implementedAt).toBeTruthy();
          expect(impl.verifiedAt).toBeUndefined();
          expect(impl.defectReductionPct).toBe(42);
          const stamped = impl.implementedAt;
          service.updateAssignment('dmaic-1', a.id, { status: 'VERIFIED', note: 'ok' }).subscribe(ver => {
            expect(ver.verifiedAt).toBeTruthy();
            // L'horodatage d'implémentation ne doit pas être réécrit.
            expect(ver.implementedAt).toBe(stamped);
            expect(ver.note).toBe('ok');
            done();
          });
        });
    });
  });

  it('décrémente le compteur Poka-Yoke à la suppression d\'une assignation', (done) => {
    service.assignDevice('dmaic-1', { deviceId: 'pk-5' }).subscribe(a => {
      expect(service.listAssignments('dmaic-1').length).toBe(1);
      service.deleteAssignment('dmaic-1', a.id).subscribe(() => {
        expect(service.listAssignments('dmaic-1').length).toBe(0);
        service.getProject('dmaic-1').subscribe(p => {
          expect(p.pokaYokeCount).toBe(0);
          done();
        });
      });
    });
  });

  it('renvoie des listes vides pour un projet sans mesure ni assignation', () => {
    expect(service.listMeasures('inconnu')).toEqual([]);
    expect(service.listAssignments('inconnu')).toEqual([]);
  });
});

describe('DmaicService (API réelle)', () => {
  let service: DmaicService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/dmaic`;
  const PROJECT_ID = 'a1b2c3d4-1111-2222-3333-444455556666';

  const project: DmaicProjectResponse = {
    id: PROJECT_ID, tenantId: 't1', title: 'Rebut ligne A',
    phase: 'MEASURE', status: 'ACTIVE', blackBeltId: 'bb',
    measureCount: 0, pokaYokeCount: 0,
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z'
  };

  const measure: MeasureResponse = {
    id: 'm1', projectId: PROJECT_ID, value: 10.1, createdAt: '2026-07-01T08:00:00Z'
  };

  const assignment: AssignmentResponse = {
    id: 'as1', projectId: PROJECT_ID, deviceId: 'pk-1', deviceCode: 'PK-INT-001',
    deviceName: 'Interlock', deviceType: 'PREVENTION', status: 'PROPOSED',
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z'
  };

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DmaicService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('pagine sans envoyer de filtre vide', (done) => {
    service.listProjects().subscribe(page => {
      expect(page.content.length).toBe(1);
      done();
    });
    const req = http.expectOne(r => r.url === `${base}/projects`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.has('status')).toBeFalse();
    expect(req.request.params.has('phase')).toBeFalse();
    req.flush({ content: [project], totalElements: 1, totalPages: 1, number: 0, size: 20 });
  });

  it('transmet statut et phase quand l\'utilisateur filtre', (done) => {
    service.listProjects(2, 50, 'ON_HOLD', 'IMPROVE').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/projects`);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('50');
    expect(req.request.params.get('status')).toBe('ON_HOLD');
    expect(req.request.params.get('phase')).toBe('IMPROVE');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 50 });
  });

  it('lit un projet par identifiant', (done) => {
    service.getProject(PROJECT_ID).subscribe(p => {
      expect(p.title).toBe('Rebut ligne A');
      done();
    });
    http.expectOne(`${base}/projects/${PROJECT_ID}`).flush(project);
  });

  it('crée un projet en POST sans réécrire la charte saisie', (done) => {
    service.createProject({ title: 'Nouveau', blackBeltId: 'bb', specTarget: 10 }).subscribe(() => done());
    const req = http.expectOne(`${base}/projects`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'Nouveau', blackBeltId: 'bb', specTarget: 10 });
    req.flush(project);
  });

  it('met à jour un projet en PATCH partiel', (done) => {
    service.updateProject(PROJECT_ID, { title: 'Revu' }).subscribe(() => done());
    const req = http.expectOne(`${base}/projects/${PROJECT_ID}`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ title: 'Revu' });
    req.flush(project);
  });

  it('adresse chaque transition à sa propre route', () => {
    const ops: [() => void, string][] = [
      [() => service.advance(PROJECT_ID).subscribe(), 'advance'],
      [() => service.hold(PROJECT_ID).subscribe(),    'hold'],
      [() => service.resume(PROJECT_ID).subscribe(),  'resume'],
      [() => service.cancel(PROJECT_ID).subscribe(),  'cancel']
    ];
    for (const [call, op] of ops) {
      call();
      const req = http.expectOne(`${base}/projects/${PROJECT_ID}/${op}`);
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({});
      req.flush(project);
    }
  });

  it('supprime un projet', (done) => {
    service.deleteProject(PROJECT_ID).subscribe(() => done());
    const req = http.expectOne(`${base}/projects/${PROJECT_ID}`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('poste une mesure puis la supprime sur les routes du projet', (done) => {
    service.addMeasure(PROJECT_ID, { value: 10.1, subgroupId: 'g1' }).subscribe(() => {
      service.deleteMeasure(PROJECT_ID, 'm1').subscribe(() => done());
      const del = http.expectOne(`${base}/projects/${PROJECT_ID}/measures/m1`);
      expect(del.request.method).toBe('DELETE');
      del.flush(null);
    });
    const post = http.expectOne(`${base}/projects/${PROJECT_ID}/measures`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ value: 10.1, subgroupId: 'g1' });
    post.flush(measure);
  });

  it('demande la capabilité au serveur au lieu de la recalculer', (done) => {
    service.capability(PROJECT_ID).subscribe(c => {
      expect(c.cpk).toBe(1.42);
      done();
    });
    const req = http.expectOne(`${base}/projects/${PROJECT_ID}/capability`);
    expect(req.request.method).toBe('GET');
    req.flush({ sampleSize: 40, cpk: 1.42, warnings: [] });
  });

  it('interroge le catalogue Poka-Yoke avec ses filtres', (done) => {
    service.listDevices(0, 100, 'DETECTION', 'VISION').subscribe(() => done());
    const req = http.expectOne(r => r.url === `${base}/pokayoke`);
    expect(req.request.params.get('size')).toBe('100');
    expect(req.request.params.get('type')).toBe('DETECTION');
    expect(req.request.params.get('mechanism')).toBe('VISION');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 });
  });

  it('lit le détail d\'un dispositif', (done) => {
    const detail: DeviceDetail = {
      id: 'pk-1', code: 'PK-INT-001', name: 'Interlock', type: 'PREVENTION',
      mechanism: 'INTERLOCK', description: 'Verrouillage capot',
      createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z'
    };
    service.getDevice('pk-1').subscribe(d => {
      expect(d.description).toBe('Verrouillage capot');
      done();
    });
    http.expectOne(`${base}/pokayoke/pk-1`).flush(detail);
  });

  it('assigne, met à jour puis détache un Poka-Yoke', (done) => {
    service.assignDevice(PROJECT_ID, { deviceId: 'pk-1', note: 'pilote' }).subscribe(a => {
      service.updateAssignment(PROJECT_ID, a.id, { status: 'IMPLEMENTED' }).subscribe(() => {
        service.deleteAssignment(PROJECT_ID, a.id).subscribe(() => done());
        const del = http.expectOne(`${base}/projects/${PROJECT_ID}/pokayoke/as1`);
        expect(del.request.method).toBe('DELETE');
        del.flush(null);
      });
      const patch = http.expectOne(`${base}/projects/${PROJECT_ID}/pokayoke/as1`);
      expect(patch.request.method).toBe('PATCH');
      expect(patch.request.body).toEqual({ status: 'IMPLEMENTED' });
      patch.flush({ ...assignment, status: 'IMPLEMENTED' });
    });
    const post = http.expectOne(`${base}/projects/${PROJECT_ID}/pokayoke`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ deviceId: 'pk-1', note: 'pilote' });
    post.flush(assignment);
  });

  it('propage le refus du serveur au lieu de le masquer', (done) => {
    service.getProject(PROJECT_ID).subscribe({
      next: () => fail('le refus serveur ne doit pas produire de projet'),
      error: err => {
        expect(err.status).toBe(403);
        done();
      }
    });
    http.expectOne(`${base}/projects/${PROJECT_ID}`)
      .flush({ title: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });
  });
});
