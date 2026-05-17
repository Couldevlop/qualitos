import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DashboardHttpRepository } from './dashboard-http.repository';
import { environment } from '../../../../environments/environment';

describe('DashboardHttpRepository', () => {
  let repo: DashboardHttpRepository;
  let http: HttpTestingController;
  const base = environment.apiBaseUrl + '/api/v1/dashboards/custom';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DashboardHttpRepository]
    });
    repo = TestBed.inject(DashboardHttpRepository);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('list parses widgets from layout_json', () => {
    repo.list().subscribe(rows => {
      expect(rows.length).toBe(1);
      expect(rows[0].widgets.length).toBe(2);
    });
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([{
      id: 'a', tenantId: 't', userId: 'u', name: 'L1',
      layoutJson: JSON.stringify({ widgets: [{ id: 'w1', type: 'kpi', title: 'k', position: {x:0,y:0,cols:2,rows:2}, config: {} }, { id: 'w2', type: 'bar', title: 'b', position: {x:0,y:0,cols:2,rows:2}, config: {} }] }),
      shared: false, version: 1
    }]);
  });

  it('save POSTs SaveRequest body without tenantId', () => {
    repo.save({ name: 'A', widgets: [], shared: false }).subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.tenantId).toBeUndefined();
    expect(req.request.body.name).toBe('A');
    expect(JSON.parse(req.request.body.layoutJson)).toEqual({ widgets: [] });
    req.flush({ id: 'i', tenantId: 't', userId: 'u', name: 'A', layoutJson: '{}', shared: false, version: 1 });
  });

  it('update PUTs to the right URL', () => {
    repo.update('xyz', { name: 'A', widgets: [], shared: true }).subscribe();
    const req = http.expectOne(`${base}/xyz`);
    expect(req.request.method).toBe('PUT');
    req.flush({ id: 'xyz', tenantId: 't', userId: 'u', name: 'A', layoutJson: '{}', shared: true, version: 2 });
  });

  it('delete DELETEs to the right URL', () => {
    repo.delete('xyz').subscribe();
    const req = http.expectOne(`${base}/xyz`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
