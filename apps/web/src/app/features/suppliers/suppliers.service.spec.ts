import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { SuppliersService } from './suppliers.service';

describe('SuppliersService (mock mode)', () => {
  let service: SuppliersService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(SuppliersService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded suppliers', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by status and type', (done) => {
    service.list(0, 50, 'APPROVED').subscribe(page => {
      expect(page.content.every(s => s.status === 'APPROVED')).toBeTrue();
      done();
    });
  });

  it('creates a PROSPECT supplier', (done) => {
    service.create({ code: 'C', name: 'New', supplierType: 'COMPONENT', createdBy: 'u' }).subscribe(s => {
      expect(s.status).toBe('PROSPECT');
      expect(s.score).toBe(0);
      done();
    });
  });

  it('changeStatus to APPROVED sets approver', (done) => {
    service.changeStatus('sup-3', 'APPROVED', { actorUserId: 'mgr' }).subscribe(s => {
      expect(s.status).toBe('APPROVED');
      expect(s.approvedBy).toBe('mgr');
      done();
    });
  });

  it('addAudit recomputes the supplier score', (done) => {
    service.addAudit('sup-1', { auditedOn: '2026-05-01', score: 100, auditorUserId: 'u' })
      .subscribe(a => {
        expect(a.supplierId).toBe('sup-1');
        service.get('sup-1').subscribe(s => {
          expect(s.score).toBeGreaterThan(0);
          done();
        });
      });
  });

  it('statistics reports expired certificates', (done) => {
    service.statistics('sup-2').subscribe(stats => {
      expect(stats.supplierId).toBe('sup-2');
      expect(stats.expiredCertificates).toBe(1);
      done();
    });
  });

  it('addNc creates an OPEN non-conformity', (done) => {
    service.addNc('sup-1', { description: 'NC', severity: 'MAJOR', detectedOn: '2026-05-01' })
      .subscribe(nc => {
        expect(nc.status).toBe('OPEN');
        done();
      });
  });
});
