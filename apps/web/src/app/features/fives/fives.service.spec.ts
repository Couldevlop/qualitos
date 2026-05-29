import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { FivesService } from './fives.service';

describe('FivesService (mock mode)', () => {
  let service: FivesService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(FivesService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded audits', (done) => {
    service.listAudits().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters audits by status', (done) => {
    service.listAudits(0, 50, 'COMPLETED').subscribe(page => {
      expect(page.content.every(a => a.status === 'COMPLETED')).toBeTrue();
      done();
    });
  });

  it('creates a DRAFT audit', (done) => {
    service.createAudit({ zone: 'Atelier Z', auditorId: 'u' }).subscribe(a => {
      expect(a.status).toBe('DRAFT');
      expect(a.zone).toBe('Atelier Z');
      done();
    });
  });

  it('scorePillar adds an item and recomputes overall score', (done) => {
    service.createAudit({ zone: 'Zone score', auditorId: 'u' }).subscribe(a => {
      service.scorePillar(a.id, { pillar: 'SEIRI', score: 8 }).subscribe(item => {
        expect(item.pillar).toBe('SEIRI');
        service.getAudit(a.id).subscribe(reloaded => {
          expect(reloaded.overallScore).toBe(80);
          done();
        });
      });
    });
  });

  it('completeAudit transitions to COMPLETED and sets completedAt', (done) => {
    service.completeAudit('5s-2').subscribe(a => {
      expect(a.status).toBe('COMPLETED');
      expect(a.completedAt).toBeTruthy();
      done();
    });
  });
});
