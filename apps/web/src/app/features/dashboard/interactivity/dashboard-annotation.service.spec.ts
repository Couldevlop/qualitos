import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { DashboardAnnotationService } from './dashboard-annotation.service';
import { DashboardAnnotation } from './dashboard-annotation.types';

describe('DashboardAnnotationService', () => {
  let svc: DashboardAnnotationService;
  let http: HttpTestingController;
  const base = environment.apiBaseUrl + '/api/v1/dashboards/annotations';

  const sample: DashboardAnnotation = {
    id: 'a1', tenantId: 't', authorId: 'u', chartKey: 'exec.trend',
    anchorLabel: 'Mai', body: 'Dérive', createdAt: '2026-06-20T10:00:00Z', deletable: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardAnnotationService]
    });
    svc = TestBed.inject(DashboardAnnotationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list() GETs with chartKey param', () => {
    svc.list('exec.trend').subscribe(rows => expect(rows).toEqual([sample]));
    const req = http.expectOne(r => r.url === base && r.params.get('chartKey') === 'exec.trend');
    expect(req.request.method).toBe('GET');
    req.flush([sample]);
  });

  it('create() POSTs body without tenantId/authorId', () => {
    svc.create({ chartKey: 'exec.trend', anchorLabel: 'Mai', body: 'Dérive' })
      .subscribe(a => expect(a.id).toBe('a1'));
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ chartKey: 'exec.trend', anchorLabel: 'Mai', body: 'Dérive' });
    expect(req.request.body.tenantId).toBeUndefined();
    expect(req.request.body.authorId).toBeUndefined();
    req.flush(sample);
  });

  it('delete() DELETEs by id', () => {
    svc.delete('a1').subscribe(res => expect(res).toBeNull());
    const req = http.expectOne(`${base}/a1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
