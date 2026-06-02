import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { AuditsService } from './audits.service';

describe('AuditsService (mock mode)', () => {
  let service: AuditsService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuditsService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded plans', (done) => {
    service.listPlans().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters plans by status', (done) => {
    service.listPlans(0, 50, 'PLANNED').subscribe(page => {
      expect(page.content.every(p => p.status === 'PLANNED')).toBeTrue();
      done();
    });
  });

  it('creates a plan in PLANNED status', (done) => {
    service.createPlan({ title: 'Audit X', type: 'INTERNAL', leadAuditorId: 'u' }).subscribe(p => {
      expect(p.status).toBe('PLANNED');
      expect(p.checklist).toEqual([]);
      done();
    });
  });

  it('adds a checklist item then responds, computing conformity score', (done) => {
    service.addChecklistItem('a2', { question: 'Procédure documentée ?' }).subscribe(item => {
      service.respondChecklistItem('a2', item.id, { conformant: true }).subscribe(updated => {
        expect(updated.conformant).toBeTrue();
        service.getPlan('a2').subscribe(plan => {
          expect(plan.conformityScore).toBe(100);
          done();
        });
      });
    });
  });

  it('startPlan transitions to IN_PROGRESS and sets startedAt', (done) => {
    service.startPlan('a3').subscribe(p => {
      expect(p.status).toBe('IN_PROGRESS');
      expect(p.startedAt).toBeTruthy();
      done();
    });
  });

  it('completePlan stores the report summary', (done) => {
    service.completePlan('a3', 'RAS').subscribe(p => {
      expect(p.status).toBe('COMPLETED');
      expect(p.reportSummary).toBe('RAS');
      done();
    });
  });

  it('addFinding attaches a finding to the plan', (done) => {
    service.addFinding('a1', { type: 'MINOR_NC', description: 'Étiquette manquante', raisedBy: 'u' }).subscribe(f => {
      expect(f.planId).toBe('a1');
      expect(f.type).toBe('MINOR_NC');
      done();
    });
  });
});
