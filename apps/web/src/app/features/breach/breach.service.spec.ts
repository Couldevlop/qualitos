import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { BreachService } from './breach.service';

describe('BreachService (mock mode)', () => {
  let service: BreachService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(BreachService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded breaches', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by status', (done) => {
    service.list('DETECTED').subscribe(items => {
      expect(items.every(b => b.status === 'DETECTED')).toBeTrue();
      done();
    });
  });

  it('detect creates a breach with a 72h DPA deadline', fakeAsync(() => {
    let id = '';
    service.detect({
      internalReference: 'BR-TEST-001', title: 'Test',
      detectedAt: new Date().toISOString(), severity: 'HIGH',
      affectedSubjectsCount: 10, reportedByUserId: 'u'
    }).subscribe(b => {
      id = b.id;
      expect(b.status).toBe('DETECTED');
      expect(b.dpaDeadlineAt).toBeTruthy();
      expect(b.subjectNotificationRequired).toBeTrue();
    });
    tick(140);
    expect(id).toBeTruthy();
  }));

  it('rejects duplicate internal reference with 409', fakeAsync(() => {
    let errStatus = 0;
    service.detect({
      internalReference: 'BR-2026-001', title: 'dup',
      detectedAt: new Date().toISOString(), severity: 'LOW',
      affectedSubjectsCount: 1, reportedByUserId: 'u'
    }).subscribe({ error: e => (errStatus = e.status) });
    tick(140);
    expect(errStatus).toBe(409);
  }));

  it('full lifecycle: detect -> assess -> contain -> close', fakeAsync(() => {
    let createdId = '';
    service.detect({
      internalReference: 'BR-LIFECYCLE-1', title: 'lc',
      detectedAt: new Date().toISOString(), severity: 'MEDIUM',
      affectedSubjectsCount: 3, reportedByUserId: 'u'
    }).subscribe(b => (createdId = b.id));
    tick(140);

    let status = '';
    service.startAssessment(createdId, { handledByUserId: 'h' }).subscribe(b => (status = b.status));
    tick(120);
    expect(status).toBe('ASSESSING');

    service.contain(createdId, { containmentMeasures: 'isolé' }).subscribe(b => (status = b.status));
    tick(120);
    expect(status).toBe('CONTAINED');

    service.close(createdId, { closureNotes: 'ok' }).subscribe(b => (status = b.status));
    tick(120);
    expect(status).toBe('CLOSED');
  }));

  it('contain from DETECTED fails with 409', fakeAsync(() => {
    let errStatus = 0;
    service.contain('br-seed-003', { containmentMeasures: 'x' })
      .subscribe({ error: e => (errStatus = e.status) });
    tick(120);
    expect(errStatus).toBe(409);
  }));
});
