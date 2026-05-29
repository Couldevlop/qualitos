import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { KpisService } from './kpis.service';

describe('KpisService (mock mode)', () => {
  let service: KpisService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(KpisService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded KPIs', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a DRAFT KPI', (done) => {
    service.create({
      code: 'C', name: 'New KPI', category: 'quality',
      direction: 'HIGHER_IS_BETTER', frequency: 'MONTHLY', createdBy: 'u'
    }).subscribe(k => {
      expect(k.status).toBe('DRAFT');
      done();
    });
  });

  it('activate transitions a KPI to ACTIVE', (done) => {
    service.create({
      code: 'A', name: 'Activatable', category: 'quality',
      direction: 'HIGHER_IS_BETTER', frequency: 'MONTHLY', createdBy: 'u'
    }).subscribe(k => {
      service.activate(k.id).subscribe(a => {
        expect(a.status).toBe('ACTIVE');
        done();
      });
    });
  });

  it('currentStatus returns the latest health snapshot', (done) => {
    service.list().subscribe(page => {
      const kpi = page.content[0];
      service.currentStatus(kpi.id).subscribe(status => {
        expect(status.kpiId).toBe(kpi.id);
        done();
      });
    });
  });

  it('trend exposes a sample count', (done) => {
    service.list().subscribe(page => {
      const kpi = page.content[0];
      service.trend(kpi.id).subscribe(trend => {
        expect(trend.kpiId).toBe(kpi.id);
        expect(trend.sampleCount).toBe(trend.points.length);
        done();
      });
    });
  });
});
