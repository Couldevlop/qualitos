import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { CirclesService } from './circles.service';

describe('CirclesService (mock mode)', () => {
  let service: CirclesService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CirclesService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded circles', (done) => {
    service.listCircles().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters circles by status', (done) => {
    service.listCircles(0, 50, 'ACTIVE').subscribe(page => {
      expect(page.content.every(c => c.status === 'ACTIVE')).toBeTrue();
      done();
    });
  });

  it('creates an ACTIVE circle', (done) => {
    service.createCircle({ name: 'Cercle test', topic: 't' }).subscribe(c => {
      expect(c.status).toBe('ACTIVE');
      expect(c.memberCount).toBe(0);
      done();
    });
  });

  it('addMember increments memberCount', (done) => {
    service.createCircle({ name: 'Members', topic: 't' }).subscribe(c => {
      service.addMember(c.id, { userId: 'u9', role: 'MEMBER' }).subscribe(() => {
        service.getCircle(c.id).subscribe(reloaded => {
          expect(reloaded.memberCount).toBe(1);
          done();
        });
      });
    });
  });

  it('addMeeting and addProposal attach to the circle', (done) => {
    service.createCircle({ name: 'Meetings', topic: 't' }).subscribe(c => {
      service.addMeeting(c.id, { title: 'Réunion', scheduledAt: new Date().toISOString() }).subscribe(m => {
        expect(m.status).toBe('SCHEDULED');
        service.addProposal(c.id, { title: 'Idée', proposedBy: 'u' }).subscribe(p => {
          expect(p.status).toBe('PROPOSED');
          done();
        });
      });
    });
  });

  it('pauseCircle then resumeCircle toggles status', (done) => {
    service.createCircle({ name: 'Pause', topic: 't' }).subscribe(c => {
      service.pauseCircle(c.id).subscribe(p => {
        expect(p.status).toBe('PAUSED');
        service.resumeCircle(c.id).subscribe(r => {
          expect(r.status).toBe('ACTIVE');
          done();
        });
      });
    });
  });

  it('generateMinutes returns structured mock minutes', (done) => {
    service.generateMinutes('circle-id', 'meeting-id', { transcript: 'Du texte de réunion.' }).subscribe(m => {
      expect(m.summary).toBeTruthy();
      expect(Array.isArray(m.decisions)).toBeTrue();
      expect(Array.isArray(m.actions)).toBeTrue();
      done();
    });
  });

  it('generateMinutes mock actions have label and suggestedAssignee', (done) => {
    service.generateMinutes('c1', 'mt1', { transcript: 'Texte' }).subscribe(m => {
      expect(m.actions.length).toBeGreaterThan(0);
      expect(m.actions[0].label).toBeTruthy();
      expect(m.actions[0].suggestedAssignee).toBeDefined();
      done();
    });
  });

  it('reviewProposal transitions proposal to UNDER_REVIEW', (done) => {
    service.createCircle({ name: 'Circle-review', topic: 't' }).subscribe(c => {
      service.addProposal(c.id, { title: 'Prop', proposedBy: 'u' }).subscribe(p => {
        service.reviewProposal(c.id, p.id).subscribe(reviewed => {
          expect(reviewed.status).toBe('UNDER_REVIEW');
          done();
        });
      });
    });
  });

  it('approveProposal transitions proposal to APPROVED', (done) => {
    service.createCircle({ name: 'Circle-approve', topic: 't' }).subscribe(c => {
      service.addProposal(c.id, { title: 'Prop', proposedBy: 'u' }).subscribe(p => {
        service.approveProposal(c.id, p.id, { validatedBy: 'user-uuid' }).subscribe(approved => {
          expect(approved.status).toBe('APPROVED');
          done();
        });
      });
    });
  });

  it('rejectProposal transitions proposal to REJECTED with reason', (done) => {
    service.createCircle({ name: 'Circle-reject', topic: 't' }).subscribe(c => {
      service.addProposal(c.id, { title: 'Prop', proposedBy: 'u' }).subscribe(p => {
        service.rejectProposal(c.id, p.id, { validatedBy: 'user-uuid', reason: 'Budget insuffisant' }).subscribe(rejected => {
          expect(rejected.status).toBe('REJECTED');
          expect((rejected as any)['rejectionReason']).toBe('Budget insuffisant');
          done();
        });
      });
    });
  });

  it('implementProposal transitions proposal to IMPLEMENTED', (done) => {
    service.createCircle({ name: 'Circle-implement', topic: 't' }).subscribe(c => {
      service.addProposal(c.id, { title: 'Prop', proposedBy: 'u' }).subscribe(p => {
        service.implementProposal(c.id, p.id).subscribe(implemented => {
          expect(implemented.status).toBe('IMPLEMENTED');
          done();
        });
      });
    });
  });

  it('recordImpact transitions proposal to MEASURED with impactNote', (done) => {
    service.createCircle({ name: 'Circle-impact', topic: 't' }).subscribe(c => {
      service.addProposal(c.id, { title: 'Prop', proposedBy: 'u' }).subscribe(p => {
        service.recordImpact(c.id, p.id, { impactNote: 'Réduction NC 15%' }).subscribe(measured => {
          expect(measured.status).toBe('MEASURED');
          expect((measured as any)['impactNote']).toBe('Réduction NC 15%');
          done();
        });
      });
    });
  });

  it('lifecycle chain PROPOSED→UNDER_REVIEW→APPROVED→IMPLEMENTED→MEASURED', (done) => {
    service.createCircle({ name: 'Chain', topic: 't' }).subscribe(c => {
      service.addProposal(c.id, { title: 'Chain prop', proposedBy: 'u' }).subscribe(p => {
        service.reviewProposal(c.id, p.id).subscribe(() => {
          service.approveProposal(c.id, p.id, { validatedBy: 'uid' }).subscribe(() => {
            service.implementProposal(c.id, p.id).subscribe(() => {
              service.recordImpact(c.id, p.id, { impactNote: 'done' }).subscribe(final => {
                expect(final.status).toBe('MEASURED');
                done();
              });
            });
          });
        });
      });
    });
  });
});
