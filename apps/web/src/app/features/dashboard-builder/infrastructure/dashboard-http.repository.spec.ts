import { HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DashboardHttpRepository } from './dashboard-http.repository';
import { DashboardLayout } from '../domain/dashboard.model';
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

  it('get normalizes widgets with a missing/partial position and drops invalid entries', () => {
    // Garde-fou contre des layout_json legacy/corrompus (cf. ANO-013) : sans
    // normalisation, un widget sans `position` faisait planter l'éditeur
    // (TypeError: Cannot read properties of undefined (reading 'x')).
    let loaded: DashboardLayout | undefined;
    repo.get('legacy').subscribe(l => (loaded = l));
    const req = http.expectOne(`${base}/legacy`);
    expect(req.request.method).toBe('GET');
    req.flush({
      id: 'legacy', tenantId: 't', userId: 'u', name: 'L',
      layoutJson: JSON.stringify({ widgets: [
        { id: 'ok', type: 'kpi', title: 'k', position: { x: 1, y: 2, cols: 4, rows: 3 }, config: {} },
        { id: 'noPos', type: 'bar', title: 'b', config: {} },
        { id: 'partial', type: 'pie', title: 'p', position: { x: 5 }, config: {} },
        null, 42, { foo: 'bar' }
      ] }),
      shared: false, version: 1
    });
    // Chaque widget rendu a une position numérique complète…
    expect(loaded!.widgets.every(w =>
      !!w.position &&
      [w.position.x, w.position.y, w.position.cols, w.position.rows].every(n => typeof n === 'number')
    )).toBeTrue();
    // …les champs manquants prennent des défauts sûrs…
    expect(loaded!.widgets.find(w => w.id === 'noPos')!.position).toEqual({ x: 0, y: 0, cols: 3, rows: 2 });
    expect(loaded!.widgets.find(w => w.id === 'partial')!.position).toEqual({ x: 5, y: 0, cols: 3, rows: 2 });
    // …et les entrées non conformes (sans id/type) sont écartées.
    expect(loaded!.widgets.map(w => w.id).sort()).toEqual(['noPos', 'ok', 'partial']);
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

  it('exportPdf POSTs widgets and parses blob + integrity headers', () => {
    repo.exportPdf('d1', [{ title: 'K', type: 'kpi', dataLines: ['x'] }]).subscribe(res => {
      expect(res.fileName).toBe('dashboard-exec.pdf');
      expect(res.verificationCode).toBe('abcDEF012345_-xy');
      expect(res.sha256).toBe('a'.repeat(64));
      expect(res.anchorRef).toBe('tx-1');
      expect(res.blob.type).toBe('application/pdf');
    });
    const req = http.expectOne(`${base}/d1/export/pdf`);
    expect(req.request.method).toBe('POST');
    expect(req.request.responseType).toBe('blob');
    expect(req.request.body.widgets[0].title).toBe('K');
    req.flush(new Blob(['%PDF'], { type: 'application/pdf' }), {
      headers: {
        'Content-Disposition': 'attachment; filename="dashboard-exec.pdf"',
        'X-Export-Verification-Code': 'abcDEF012345_-xy',
        'X-Export-Sha256': 'a'.repeat(64),
        'X-Export-Anchor-Ref': 'tx-1'
      }
    });
  });

  it('exportPdf falls back to a default file name when no disposition header', () => {
    repo.exportPdf('d2', []).subscribe(res => {
      expect(res.fileName).toBe('dashboard-d2.pdf');
      expect(res.verificationCode).toBe('');
    });
    const req = http.expectOne(`${base}/d2/export/pdf`);
    req.flush(new Blob(['%PDF'], { type: 'application/pdf' }));
  });
});
