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
});
