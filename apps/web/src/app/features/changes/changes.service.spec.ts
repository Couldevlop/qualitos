import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { ChangesService } from './changes.service';

describe('ChangesService (mock mode)', () => {
  let service: ChangesService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ChangesService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded change requests', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a DRAFT change request with default priority', (done) => {
    service.create({
      code: 'C', title: 'Change X', type: 'PROCESS', requesterUserId: 'u'
    }).subscribe(c => {
      expect(c.status).toBe('DRAFT');
      expect(c.priority).toBe('MEDIUM');
      done();
    });
  });

  it('addApprover then lists approvals', (done) => {
    service.create({ code: 'AP', title: 'Approvable', type: 'PROCESS', requesterUserId: 'u' })
      .subscribe(c => {
        service.addApprover(c.id, { approverUserId: 'mgr' }).subscribe(() => {
          service.listApprovals(c.id).subscribe(approvals => {
            expect(approvals.length).toBeGreaterThan(0);
            done();
          });
        });
      });
  });

  it('addImpact then lists impacts', (done) => {
    service.create({ code: 'IM', title: 'Impactful', type: 'PROCESS', requesterUserId: 'u' })
      .subscribe(c => {
        service.addImpact(c.id, { targetType: 'DOCUMENT', targetId: 'doc-1' }).subscribe(() => {
          service.listImpacts(c.id).subscribe(impacts => {
            expect(impacts.length).toBeGreaterThan(0);
            done();
          });
        });
      });
  });
});
