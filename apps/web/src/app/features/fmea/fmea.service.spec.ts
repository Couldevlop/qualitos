import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { FmeaService } from './fmea.service';

describe('FmeaService (mock mode)', () => {
  let service: FmeaService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(FmeaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded projects', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by type', (done) => {
    service.list(0, 50, undefined, 'PROCESS_FMEA').subscribe(page => {
      expect(page.content.every(p => p.type === 'PROCESS_FMEA')).toBeTrue();
      done();
    });
  });

  it('creates a DRAFT project with default RPN threshold', (done) => {
    service.create({ code: 'X', name: 'New FMEA', type: 'PROCESS_FMEA', createdBy: 'u' }).subscribe(p => {
      expect(p.status).toBe('DRAFT');
      expect(p.criticalRpnThreshold).toBe(100);
      done();
    });
  });

  it('activate transitions to ACTIVE', (done) => {
    service.activate('fmea-2').subscribe(p => {
      expect(p.status).toBe('ACTIVE');
      done();
    });
  });

  it('addItem computes RPN and critical flag', (done) => {
    service.addItem('fmea-2', {
      function: 'F', failureMode: 'M', severity: 9, occurrence: 4, detection: 4
    }).subscribe(item => {
      expect(item.rpn).toBe(9 * 4 * 4);
      expect(item.critical).toBeTrue();
      done();
    });
  });

  it('statistics aggregates items', (done) => {
    service.statistics('fmea-1').subscribe(stats => {
      expect(stats.totalItems).toBe(2);
      expect(stats.maxRpn).toBe(160);
      expect(stats.criticalItems).toBe(1);
      done();
    });
  });

  it('updateItem recomputes RPN', (done) => {
    service.listItems('fmea-1').subscribe(page => {
      const item = page.content[0];
      service.updateItem('fmea-1', item.id, { detection: 1 }).subscribe(updated => {
        expect(updated.rpn).toBe(updated.severity * updated.occurrence * 1);
        done();
      });
    });
  });
});
