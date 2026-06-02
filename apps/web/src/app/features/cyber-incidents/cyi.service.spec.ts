import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { CyberIncidentsService } from './cyi.service';

describe('CyberIncidentsService (mock mode)', () => {
  let service: CyberIncidentsService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CyberIncidentsService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded incidents', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by status', (done) => {
    service.list('DETECTED').subscribe(items => {
      expect(items.every(i => i.status === 'DETECTED')).toBeTrue();
      done();
    });
  });

  it('resolves a single incident by id', (done) => {
    service.list().subscribe(items => {
      const id = items[0].id;
      service.get(id).subscribe(found => {
        expect(found.id).toBe(id);
        done();
      });
    });
  });

  it('earlyWarningOverdue only returns overdue incidents', (done) => {
    service.earlyWarningOverdue().subscribe(items => {
      expect(items.every(i => i.earlyWarningOverdue)).toBeTrue();
      done();
    });
  });
});
