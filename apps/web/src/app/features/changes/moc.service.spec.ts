import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { MocService } from './moc.service';
import { MocSettings } from './moc.types';
import { demande, ligne } from './testing/moc.fixtures';

/** Chaque appel va au bon point d'entrée, avec le bon verbe et sans donnée d'identité. */
describe('MocService', () => {
  const base = `${environment.apiBaseUrl}/api/v1/changes`;
  let service: MocService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(MocService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste, lit, crée, modifie et supprime', () => {
    service.list().subscribe(l => expect(l.length).toBe(1));
    http.expectOne({ method: 'GET', url: base }).flush([ligne()]);

    service.get('c1').subscribe(d => expect(d.code).toBe('MOC-2026-014'));
    http.expectOne({ method: 'GET', url: `${base}/c1` }).flush(demande());

    const form = { title: 'Four', type: 'PERMANENT' as const, site: null, description: null, justification: null, plannedFor: null };
    service.create(form).subscribe();
    const post = http.expectOne({ method: 'POST', url: base });
    expect(post.request.body).toEqual(form);
    expect(post.request.body.requesterUserId).toBeUndefined();
    post.flush(demande());

    service.update('c1', form).subscribe();
    http.expectOne({ method: 'PUT', url: `${base}/c1` }).flush(demande());

    service.delete('c1').subscribe();
    http.expectOne({ method: 'DELETE', url: `${base}/c1` }).flush(null);
  });

  it('fait avancer le cycle de la demande', () => {
    service.sendToAnalysis('c1').subscribe();
    http.expectOne({ method: 'POST', url: `${base}/c1/analysis` }).flush(demande());
    service.assess('c1', 'SAFETY', { level: 'HIGH', notes: 'EPI' }).subscribe();
    const put = http.expectOne({ method: 'PUT', url: `${base}/c1/impacts/SAFETY` });
    expect(put.request.body).toEqual({ level: 'HIGH', notes: 'EPI' });
    put.flush(demande());
    service.submit('c1').subscribe();
    http.expectOne({ method: 'POST', url: `${base}/c1/submit` }).flush(demande());
    service.decide('c1', 'v1', { decision: 'REJECTED', comment: 'Non' }).subscribe();
    const dec = http.expectOne({ method: 'POST', url: `${base}/c1/validations/v1/decision` });
    expect(dec.request.body).toEqual({ decision: 'REJECTED', comment: 'Non' });
    dec.flush(demande());
    service.reopen('c1').subscribe();
    http.expectOne({ method: 'POST', url: `${base}/c1/reopen` }).flush(demande());
  });

  it('télécharge le classeur en blob avec ses en-têtes', () => {
    service.exportXlsx().subscribe(r => expect(r.body instanceof Blob).toBeTrue());
    const req = http.expectOne(`${base}/export.xlsx`);
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
  });

  it('lit et enregistre les paramètres MOC', () => {
    const s = { majorThreshold: 25, criticalThreshold: 50, reminderDays: 3, weights: {}, roles: [] } as unknown as MocSettings;
    service.settings().subscribe(v => expect(v.criticalThreshold).toBe(50));
    http.expectOne({ method: 'GET', url: `${base}/settings` }).flush(s);
    service.saveSettings(s).subscribe();
    http.expectOne({ method: 'PUT', url: `${base}/settings` }).flush(s);
  });
});
