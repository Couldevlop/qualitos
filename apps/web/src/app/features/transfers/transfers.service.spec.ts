import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { TransfersService, requiresDerogationJustification } from './transfers.service';

describe('TransfersService (mock mode)', () => {
  let service: TransfersService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(TransfersService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('requiresDerogationJustification only for Art.49 derogation', () => {
    expect(requiresDerogationJustification('DEROGATION_ART49')).toBeTrue();
    expect(requiresDerogationJustification('STANDARD_CONTRACTUAL_CLAUSES')).toBeFalse();
  });

  it('lists seeded transfers', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters transfers by status', (done) => {
    service.list('ACTIVE').subscribe(items => {
      expect(items.every(t => t.status === 'ACTIVE')).toBeTrue();
      done();
    });
  });

  it('creates a DRAFT transfer', (done) => {
    service.create({
      reference: 'NEW', recipientName: 'X', mechanism: 'ADEQUACY_DECISION', createdByUserId: 'u'
    }).subscribe(t => {
      expect(t.status).toBe('DRAFT');
      done();
    });
  });

  it('activate then suspend toggles status', (done) => {
    service.activate('cbt-3').subscribe(t => {
      expect(t.status).toBe('ACTIVE');
      service.suspend('cbt-3', { reason: 'audit' }).subscribe(s => {
        expect(s.status).toBe('SUSPENDED');
        expect(s.suspendReason).toBe('audit');
        done();
      });
    });
  });

  it('terminate sets effectiveTo and reason', (done) => {
    service.terminate('cbt-1', { reason: 'fin contrat' }).subscribe(t => {
      expect(t.status).toBe('TERMINATED');
      expect(t.terminationReason).toBe('fin contrat');
      expect(t.effectiveTo).toBeTruthy();
      done();
    });
  });
});
