import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { sheet, summary } from './spc-capability.fixtures';
import { SpcCapabilityService } from './spc-capability.service';
import { StudyRequest } from './spc-capability.types';

describe('SpcCapabilityService', () => {
  let service: SpcCapabilityService;
  let http: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/spc`;

  const request: StudyRequest = {
    partReference: 'Shaft AX', characteristic: null, specification: null, machine: null,
    leaderUserId: null, leaderName: null, studyDate: '2026-10-01', actionPlan: false,
    lsl: 475, usl: 525, subgroupSize: 5, measurements: sheet().measurements
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SpcCapabilityService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste le registre', () => {
    service.list().subscribe(r => expect(r.length).toBe(1));
    const req = http.expectOne(`${base}/studies`);
    expect(req.request.method).toBe('GET');
    req.flush([summary()]);
  });

  it('lit une étude, en encodant son identifiant', () => {
    service.get('a/b').subscribe(s => expect(s.study.id).toBe('s1'));
    const req = http.expectOne(`${base}/studies/a%2Fb`);
    expect(req.request.method).toBe('GET');
    req.flush(sheet());
  });

  it('crée et met à jour une étude sans tenant ni auteur', () => {
    service.create(request).subscribe();
    const post = http.expectOne(`${base}/studies`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(request);
    expect(Object.keys(post.request.body)).not.toContain('tenantId');
    post.flush(sheet());

    service.update('s1', request).subscribe();
    const put = http.expectOne(`${base}/studies/s1`);
    expect(put.request.method).toBe('PUT');
    put.flush(sheet());
  });

  it('change le plan d\'actions et supprime', () => {
    service.setActionPlan('s1', true).subscribe(s => expect(s.actionPlan).toBeTrue());
    const patch = http.expectOne(`${base}/studies/s1/action-plan`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body).toEqual({ actionPlan: true });
    patch.flush(summary({ actionPlan: true }));

    service.delete('s1').subscribe();
    const del = http.expectOne(`${base}/studies/s1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
  });

  it('lit et règle les seuils', () => {
    service.settings().subscribe(s => expect(s.defaults).toBeTrue());
    http.expectOne(`${base}/settings`).flush({ capableThreshold: 1.67, watchThreshold: 1.33, defaults: true });

    service.updateSettings(2, 1.5).subscribe();
    const put = http.expectOne(`${base}/settings`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ capableThreshold: 2, watchThreshold: 1.5 });
    put.flush({ capableThreshold: 2, watchThreshold: 1.5, defaults: false });
  });
});
