import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { CapaService } from './capa.service';

describe('CapaService (mock mode)', () => {
  let service: CapaService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CapaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded cases', (done) => {
    service.listCases().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      expect(page.totalElements).toBe(page.content.length);
      done();
    });
  });

  it('filters cases by status', (done) => {
    service.listCases(0, 50, 'OPEN').subscribe(page => {
      expect(page.content.every(c => c.status === 'OPEN')).toBeTrue();
      done();
    });
  });

  it('returns a case by id', (done) => {
    service.getCase('capa-1').subscribe(c => {
      expect(c.id).toBe('capa-1');
      done();
    });
  });

  it('creates a case in OPEN status', (done) => {
    service.createCase({
      title: 'Nouvelle action', type: 'CORRECTIVE', criticity: 'LOW', sourceType: 'INTERNAL', ownerId: 'u'
    }).subscribe(c => {
      expect(c.status).toBe('OPEN');
      expect(c.title).toBe('Nouvelle action');
      expect(c.actions).toEqual([]);
      done();
    });
  });

  it('adds an action to a case', (done) => {
    service.addAction('capa-1', { title: 'Recalibrer' }).subscribe(a => {
      expect(a.capaId).toBe('capa-1');
      expect(a.status).toBe('PENDING');
      done();
    });
  });

  it('updateAction advances an action to DONE and stamps completedAt (ANO-011)', (done) => {
    service.addAction('capa-1', { title: 'Recalibrer' }).subscribe(a => {
      service.updateAction('capa-1', a.id, { title: a.title, status: 'DONE' }).subscribe(updated => {
        expect(updated.status).toBe('DONE');
        expect(updated.completedAt).toBeTruthy();
        done();
      });
    });
  });

  it('suggests AI actions', (done) => {
    service.suggestActions('capa-1').subscribe(actions => {
      expect(actions.length).toBeGreaterThan(0);
      expect(actions[0].title).toBeTruthy();
      done();
    });
  });

  it('verifyEffectiveness closes the case when effective', (done) => {
    service.verifyEffectiveness('capa-1', true).subscribe(c => {
      expect(c.effectivenessVerified).toBeTrue();
      expect(c.status).toBe('CLOSED');
      done();
    });
  });

  it('startCase transitions to IN_PROGRESS', (done) => {
    service.startCase('capa-2').subscribe(c => {
      expect(c.status).toBe('IN_PROGRESS');
      done();
    });
  });
});
