import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { DmaicService } from './dmaic.service';

describe('DmaicService (mock mode)', () => {
  let service: DmaicService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DmaicService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded projects', (done) => {
    service.listProjects().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters projects by phase', (done) => {
    service.listProjects(0, 20, undefined, 'MEASURE').subscribe(page => {
      expect(page.content.every(p => p.phase === 'MEASURE')).toBeTrue();
      done();
    });
  });

  it('creates a project in DEFINE/ACTIVE', (done) => {
    service.createProject({ title: 'Projet test', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        expect(p.phase).toBe('DEFINE');
        expect(p.status).toBe('ACTIVE');
        done();
      });
  });

  it('advance moves DEFINE -> MEASURE', (done) => {
    service.createProject({ title: 'Adv', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        service.advance(p.id).subscribe(adv => {
          expect(adv.phase).toBe('MEASURE');
          done();
        });
      });
  });

  it('hold then resume toggles status', (done) => {
    service.createProject({ title: 'Hold', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        service.hold(p.id).subscribe(h => {
          expect(h.status).toBe('ON_HOLD');
          service.resume(p.id).subscribe(r => {
            expect(r.status).toBe('ACTIVE');
            done();
          });
        });
      });
  });

  it('capability warns when fewer than 2 measures', (done) => {
    service.createProject({ title: 'Cap', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb' })
      .subscribe(p => {
        service.capability(p.id).subscribe(cap => {
          expect(cap.sampleSize).toBe(0);
          expect(cap.warnings?.length).toBeGreaterThan(0);
          done();
        });
      });
  });

  it('addMeasure feeds capability computation (mean/stdDev)', (done) => {
    service.createProject({
      title: 'Cap2', problemStatement: 'X', goalStatement: 'Y', blackBeltId: 'bb',
      specLowerLimit: 9, specUpperLimit: 11
    }).subscribe(p => {
      service.addMeasure(p.id, { value: 10 }).subscribe(() => {
        service.addMeasure(p.id, { value: 10.2 }).subscribe(() => {
          service.capability(p.id).subscribe(cap => {
            expect(cap.sampleSize).toBe(2);
            expect(cap.mean).toBeCloseTo(10.1, 5);
            expect(cap.cpk).toBeDefined();
            done();
          });
        });
      });
    });
  });

  it('lists the Poka-Yoke catalog and a device detail', (done) => {
    service.listDevices().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      service.getDevice('pk-1').subscribe(d => {
        expect(d.id).toBe('pk-1');
        expect(d.description).toBeTruthy();
        done();
      });
    });
  });

  it('assignDevice creates a PROPOSED assignment', (done) => {
    service.assignDevice('dmaic-1', { deviceId: 'pk-1' }).subscribe(a => {
      expect(a.status).toBe('PROPOSED');
      expect(a.deviceId).toBe('pk-1');
      done();
    });
  });
});
