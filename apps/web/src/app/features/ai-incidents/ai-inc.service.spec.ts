import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { AiIncidentsService } from './ai-inc.service';

describe('AiIncidentsService (mock mode)', () => {
  let service: AiIncidentsService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AiIncidentsService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded incidents', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('listOverdue surfaces incidents past their regulator deadline', (done) => {
    service.listOverdue().subscribe(items => {
      expect(items.every(i => i.overdueForRegulator)).toBeTrue();
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('detect computes a regulator deadline from severity', (done) => {
    const detectedAt = new Date().toISOString();
    service.detect({
      reference: 'AIINC-TEST', aiSystemId: 'sys', severity: 'CRITICAL_INFRASTRUCTURE_DISRUPTION',
      description: 'd', occurredAt: detectedAt, detectedAt, createdByUserId: 'u'
    }).subscribe(i => {
      expect(i.status).toBe('DETECTED');
      expect(i.regulatorNotificationDeadline).toBeTruthy();
      done();
    });
  });

  it('startInvestigation moves to INVESTIGATING', (done) => {
    service.startInvestigation('inc-3', { investigationLeadUserId: 'lead' }).subscribe(i => {
      expect(i.status).toBe('INVESTIGATING');
      expect(i.investigationLeadUserId).toBe('lead');
      done();
    });
  });

  it('notifyRegulator clears overdue flag', (done) => {
    service.notifyRegulator('inc-3', { regulatorReference: 'CNIL-1', rootCauseAnalysis: 'RCA' }).subscribe(i => {
      expect(i.status).toBe('NOTIFIED_REGULATOR');
      expect(i.overdueForRegulator).toBeFalse();
      done();
    });
  });
});
