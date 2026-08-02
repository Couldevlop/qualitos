import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { KpisService } from './kpis.service';
import { CreateKpiRequest } from './kpis.types';

describe('KpisService (mock mode)', () => {
  let service: KpisService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(KpisService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  /** KPI sans seuils : sert à vérifier la santé « inconnue ». */
  const bare: CreateKpiRequest = {
    code: 'sans-seuil', name: 'Sans seuil', category: 'Qualité',
    direction: 'HIGHER_IS_BETTER', frequency: 'MONTHLY', createdBy: 'u'
  };

  it('lists seeded KPIs', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a DRAFT KPI', (done) => {
    service.create({
      code: 'C', name: 'New KPI', category: 'quality',
      direction: 'HIGHER_IS_BETTER', frequency: 'MONTHLY', createdBy: 'u'
    }).subscribe(k => {
      expect(k.status).toBe('DRAFT');
      done();
    });
  });

  it('activate transitions a KPI to ACTIVE', (done) => {
    service.create({
      code: 'A', name: 'Activatable', category: 'quality',
      direction: 'HIGHER_IS_BETTER', frequency: 'MONTHLY', createdBy: 'u'
    }).subscribe(k => {
      service.activate(k.id).subscribe(a => {
        expect(a.status).toBe('ACTIVE');
        done();
      });
    });
  });

  it('currentStatus returns the latest health snapshot', (done) => {
    service.list().subscribe(page => {
      const kpi = page.content[0];
      service.currentStatus(kpi.id).subscribe(status => {
        expect(status.kpiId).toBe(kpi.id);
        done();
      });
    });
  });

  it('trend exposes a sample count', (done) => {
    service.list().subscribe(page => {
      const kpi = page.content[0];
      service.trend(kpi.id).subscribe(trend => {
        expect(trend.kpiId).toBe(kpi.id);
        expect(trend.sampleCount).toBe(trend.points.length);
        done();
      });
    });
  });

  // --- Filtres du catalogue -------------------------------------------------

  it('filtre le catalogue par statut puis par catégorie', (done) => {
    service.list(0, 50, 'DRAFT').subscribe(drafts => {
      expect(drafts.content.map(k => k.code)).toEqual(['dpmo']);
      service.list(0, 50, undefined, 'CAPA').subscribe(capa => {
        expect(capa.content.map(k => k.code)).toEqual(['capa-closure-time']);
        expect(capa.totalElements).toBe(1);
        done();
      });
    });
  });

  it('renvoie une page vide quand aucun KPI ne porte la catégorie demandée', (done) => {
    service.list(0, 50, undefined, 'Inexistante').subscribe(page => {
      expect(page.content).toEqual([]);
      expect(page.totalElements).toBe(0);
      done();
    });
  });

  // --- Cycle de vie ---------------------------------------------------------

  it('rouvre puis archive un KPI actif', (done) => {
    service.reopen('kpi-1').subscribe(draft => {
      expect(draft.status).toBe('DRAFT');
      service.archive('kpi-1').subscribe(archived => {
        expect(archived.status).toBe('ARCHIVED');
        done();
      });
    });
  });

  it('met à jour la cible et les seuils sans toucher au code', (done) => {
    service.update('kpi-2', { targetValue: 99, thresholdWarning: 97 }).subscribe(k => {
      expect(k.targetValue).toBe(99);
      expect(k.thresholdWarning).toBe(97);
      expect(k.code).toBe('first-pass-yield');
      done();
    });
  });

  it('supprime un KPI et ses mesures', (done) => {
    service.delete('kpi-1').subscribe(() => {
      service.list().subscribe(page => {
        expect(page.content.some(k => k.id === 'kpi-1')).toBeFalse();
        service.listMeasurements('kpi-1').subscribe(mes => {
          expect(mes.content).toEqual([]);
          done();
        });
      });
    });
  });

  // --- Santé calculée à l'enregistrement d'une mesure ------------------------

  const record = (kpiId: string, value: number) => service.record(kpiId, {
    periodStart: '2026-07-01T00:00:00Z', periodEnd: '2026-07-31T00:00:00Z', value
  });

  it('classe une mesure « plus bas = mieux » selon les seuils du KPI', (done) => {
    // kpi-1 : LOWER_IS_BETTER, warning 45, critical 60.
    record('kpi-1', 60).subscribe(critical => {
      expect(critical.health).toBe('CRITICAL');
      record('kpi-1', 45).subscribe(warning => {
        expect(warning.health).toBe('WARNING');
        record('kpi-1', 44.9).subscribe(ok => {
          expect(ok.health).toBe('OK');
          done();
        });
      });
    });
  });

  it('classe une mesure « plus haut = mieux » selon les seuils du KPI', (done) => {
    // kpi-2 : HIGHER_IS_BETTER, warning 95, critical 90.
    record('kpi-2', 90).subscribe(critical => {
      expect(critical.health).toBe('CRITICAL');
      record('kpi-2', 95).subscribe(warning => {
        expect(warning.health).toBe('WARNING');
        record('kpi-2', 95.1).subscribe(ok => {
          expect(ok.health).toBe('OK');
          done();
        });
      });
    });
  });

  it('déclare la santé inconnue quand le KPI n’a aucun seuil', (done) => {
    service.create(bare).subscribe(k => {
      record(k.id, 42).subscribe(m => {
        expect(m.health).toBe('UNKNOWN');
        done();
      });
    });
  });

  it('hérite de l’unité du KPI et de la source MANUAL par défaut', (done) => {
    record('kpi-1', 30).subscribe(m => {
      expect(m.unit).toBe('jours');
      expect(m.source).toBe('MANUAL');
      done();
    });
  });

  it('la dernière mesure enregistrée devient l’état courant du KPI', (done) => {
    record('kpi-2', 99.5).subscribe(() => {
      service.currentStatus('kpi-2').subscribe(status => {
        expect(status.latestValue).toBe(99.5);
        expect(status.health).toBe('OK');
        expect(status.direction).toBe('HIGHER_IS_BETTER');
        expect(status.targetValue).toBe(98);
        done();
      });
    });
  });

  it('renvoie un état courant neutre pour un KPI jamais mesuré', (done) => {
    // kpi-3 est semé sans aucune mesure.
    service.currentStatus('kpi-3').subscribe(status => {
      expect(status.latestValue).toBeUndefined();
      expect(status.health).toBe('UNKNOWN');
      expect(status.definitionStatus).toBe('DRAFT');
      done();
    });
  });

  it('ajoute puis retire un point de la tendance', (done) => {
    service.trend('kpi-1').subscribe(before => {
      record('kpi-1', 25).subscribe(m => {
        service.trend('kpi-1').subscribe(after => {
          expect(after.sampleCount).toBe(before.sampleCount + 1);
          service.deleteMeasurement('kpi-1', m.id).subscribe(() => {
            service.trend('kpi-1').subscribe(restored => {
              expect(restored.sampleCount).toBe(before.sampleCount);
              done();
            });
          });
        });
      });
    });
  });

  // ---- Replis du mode démo sur identifiant inconnu ---------------------------

  it('retombe sur le premier KPI plutôt que de planter sur une clé inconnue', (done) => {
    // Repli assumé du mode démo : les écrans restent utilisables sans backend,
    // quel que soit l'identifiant présent dans l'URL.
    service.get('kpi-inexistant').subscribe(k => {
      expect(k).toBeTruthy();
      expect(k.id).toBeTruthy();
      done();
    });
  });

  it('ne modifie rien quand la mise à jour vise un KPI inconnu', (done) => {
    service.list().subscribe(before => {
      const names = before.content.map(k => k.name);
      service.update('kpi-inexistant', { name: 'usurpé' }).subscribe(() => {
        service.list().subscribe(after => {
          expect(after.content.map(k => k.name)).toEqual(names);
          done();
        });
      });
    });
  });

  it('ne fait transiter aucun statut quand la cible est inconnue', (done) => {
    service.list().subscribe(before => {
      const statuses = before.content.map(k => k.status);
      service.activate('kpi-inexistant').subscribe(() => {
        service.list().subscribe(after => {
          expect(after.content.map(k => k.status)).toEqual(statuses);
          done();
        });
      });
    });
  });

  it('ignore la suppression d\'un KPI inconnu', (done) => {
    service.list().subscribe(before => {
      const count = before.totalElements;
      service.delete('kpi-inexistant').subscribe(() => {
        service.list().subscribe(after => {
          expect(after.totalElements).toBe(count);
          done();
        });
      });
    });
  });

  it('filtre par catégorie, seule ou combinée au statut', (done) => {
    service.list(0, 50, undefined, 'CAPA').subscribe(byCategory => {
      expect(byCategory.content.every(k => k.category === 'CAPA')).toBeTrue();
      service.list(0, 50, 'ACTIVE', 'categorie-absente').subscribe(none => {
        expect(none.content).toEqual([]);
        done();
      });
    });
  });
});

/**
 * Mode HTTP (useMockApi=false) : chemin réellement exécuté en production.
 * Le `tenant_id` n'est jamais transmis par le client (règle §18.2 #2).
 */
describe('KpisService (HTTP)', () => {
  let service: KpisService;
  let http: HttpTestingController;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/kpis`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(KpisService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    environment.useMockApi = prevMock;
  });

  const emptyPage = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 0 };

  it('n’envoie statut et catégorie que lorsqu’ils sont fournis', () => {
    service.list(3, 10).subscribe();
    const plain = http.expectOne(r => r.url === base);
    expect(plain.request.params.get('page')).toBe('3');
    expect(plain.request.params.get('size')).toBe('10');
    expect(plain.request.params.has('status')).toBeFalse();
    expect(plain.request.params.has('category')).toBeFalse();
    plain.flush(emptyPage);

    service.list(0, 20, 'ACTIVE', 'CAPA').subscribe();
    const filtered = http.expectOne(r => r.url === base);
    expect(filtered.request.params.get('status')).toBe('ACTIVE');
    expect(filtered.request.params.get('category')).toBe('CAPA');
    filtered.flush(emptyPage);
  });

  it('crée, lit, modifie et supprime une définition sur les bonnes routes', () => {
    const input: CreateKpiRequest = {
      code: 'dpmo', name: 'DPMO', direction: 'LOWER_IS_BETTER', createdBy: 'u1'
    };
    service.create(input).subscribe();
    const created = http.expectOne(base);
    expect(created.request.method).toBe('POST');
    expect(created.request.body).toEqual(input);
    created.flush({});

    service.get('k1').subscribe();
    const read = http.expectOne(`${base}/k1`);
    expect(read.request.method).toBe('GET');
    read.flush({});

    service.update('k1', { name: 'DPMO revu' }).subscribe();
    const patched = http.expectOne(`${base}/k1`);
    expect(patched.request.method).toBe('PATCH');
    expect(patched.request.body).toEqual({ name: 'DPMO revu' });
    patched.flush({});

    service.delete('k1').subscribe();
    const removed = http.expectOne(`${base}/k1`);
    expect(removed.request.method).toBe('DELETE');
    removed.flush(null);
  });

  it('poste chaque transition de statut sur son propre verbe métier', () => {
    service.activate('k1').subscribe();
    const activate = http.expectOne(`${base}/k1/activate`);
    expect(activate.request.method).toBe('POST');
    expect(activate.request.body).toEqual({});
    activate.flush({});

    service.reopen('k1').subscribe();
    http.expectOne(`${base}/k1/reopen`).flush({});

    service.archive('k1').subscribe();
    http.expectOne(`${base}/k1/archive`).flush({});
  });

  it('lit l’état courant et la tendance calculés par le serveur', () => {
    service.currentStatus('k1').subscribe(s => expect(s.health).toBe('WARNING'));
    http.expectOne(`${base}/k1/status`).flush({ kpiId: 'k1', health: 'WARNING' });

    service.trend('k1').subscribe(t => expect(t.sampleCount).toBe(2));
    http.expectOne(`${base}/k1/trend`).flush({ kpiId: 'k1', sampleCount: 2, points: [] });
  });

  it('pagine les mesures et poste un enregistrement complet', () => {
    service.listMeasurements('k1', 1, 25).subscribe();
    const listed = http.expectOne(r => r.url === `${base}/k1/measurements`);
    expect(listed.request.params.get('page')).toBe('1');
    expect(listed.request.params.get('size')).toBe('25');
    listed.flush(emptyPage);

    service.record('k1', {
      periodStart: '2026-07-01T00:00:00Z', periodEnd: '2026-07-31T00:00:00Z',
      value: 12.5, unit: '%', source: 'IOT_AGGREGATED'
    }).subscribe();
    const recorded = http.expectOne(`${base}/k1/measurements`);
    expect(recorded.request.method).toBe('POST');
    expect(recorded.request.body.value).toBe(12.5);
    expect(recorded.request.body.source).toBe('IOT_AGGREGATED');
    recorded.flush({});

    service.deleteMeasurement('k1', 'm1').subscribe();
    const removed = http.expectOne(`${base}/k1/measurements/m1`);
    expect(removed.request.method).toBe('DELETE');
    removed.flush(null);
  });

  it('propage l’erreur HTTP au souscripteur au lieu de la masquer', (done) => {
    service.list().subscribe({
      next: () => fail('un 500 ne doit pas produire de valeur'),
      error: err => { expect(err.status).toBe(500); done(); }
    });
    http.expectOne(r => r.url === base).flush({}, { status: 500, statusText: 'Server Error' });
  });
});
