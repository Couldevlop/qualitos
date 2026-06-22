import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { MarketplaceService } from './marketplace.service';
import { InstallationView, MarketplacePackView, SubmitRequest } from './marketplace.types';

describe('MarketplaceService', () => {
  let svc: MarketplaceService;
  let http: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/marketplace/packs`;

  const pack: MarketplacePackView = {
    id: 'p1', packId: 'iso', version: '1.0', publisher: 'Pub', title: 'T',
    sector: 'healthcare', norms: ['iso-9001'], priceCents: 0, currency: 'EUR',
    status: 'PUBLISHED', manifestUrl: 'https://x/y', ratingAvg: 0, ratingCount: 0
  };
  const inst: InstallationView = {
    id: 'i1', tenantId: 't1', marketplacePackId: 'p1', packId: 'iso',
    packVersion: '1.0', status: 'INSTALLED'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        MarketplaceService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    svc = TestBed.inject(MarketplaceService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('listPublished without sector', () => {
    svc.listPublished().subscribe(r => expect(r.length).toBe(1));
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.has('sector')).toBeFalse();
    req.flush([pack]);
  });

  it('listPublished with sector param', () => {
    svc.listPublished('healthcare').subscribe();
    const req = http.expectOne(r => r.url === base && r.params.get('sector') === 'healthcare');
    expect(req.request.method).toBe('GET');
    req.flush([pack]);
  });

  it('submit posts the request body', () => {
    const body: SubmitRequest = {
      packId: 'iso', version: '1.0', publisher: 'Pub', title: 'T', sector: 's',
      norms: ['iso-9001'], priceCents: 0, currency: 'EUR',
      manifestUrl: 'https://x/y', manifestJson: '{}', signatureHash: 'deadbeefdeadbeef'
    };
    svc.submit(body).subscribe(r => expect(r.id).toBe('p1'));
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.packId).toBe('iso');
    req.flush(pack);
  });

  it('moderationQueue GET', () => {
    svc.moderationQueue().subscribe(r => expect(r.length).toBe(1));
    http.expectOne(`${base}/moderation/queue`).flush([{ ...pack, status: 'SUBMITTED' }]);
  });

  it('takeForReview / publish / deprecate POST empty body', () => {
    svc.takeForReview('p1').subscribe();
    const r1 = http.expectOne(`${base}/p1/take-review`);
    expect(r1.request.method).toBe('POST');
    r1.flush({ ...pack, status: 'IN_REVIEW' });
    svc.publish('p1').subscribe();
    const r2 = http.expectOne(`${base}/p1/publish`);
    expect(r2.request.method).toBe('POST');
    r2.flush(pack);
    svc.deprecate('p1').subscribe();
    const r3 = http.expectOne(`${base}/p1/deprecate`);
    expect(r3.request.method).toBe('POST');
    r3.flush({ ...pack, status: 'DEPRECATED' });
  });

  it('reject POST carries reason', () => {
    svc.reject('p1', 'incomplet').subscribe();
    const req = http.expectOne(`${base}/p1/reject`);
    expect(req.request.body.reason).toBe('incomplet');
    req.flush({ ...pack, status: 'REJECTED' });
  });

  it('install POST', () => {
    svc.install('p1').subscribe(r => expect(r.status).toBe('INSTALLED'));
    const req = http.expectOne(`${base}/p1/install`);
    expect(req.request.method).toBe('POST');
    req.flush(inst);
  });

  it('uninstall DELETE', () => {
    svc.uninstall('i1').subscribe();
    const req = http.expectOne(`${base}/installations/i1`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ ...inst, status: 'UNINSTALLED' });
  });

  it('myInstallations / history GET', () => {
    svc.myInstallations().subscribe(r => expect(r.length).toBe(1));
    const r1 = http.expectOne(`${base}/installations/my`);
    expect(r1.request.method).toBe('GET');
    r1.flush([inst]);
    svc.myInstallationHistory().subscribe(r => expect(r.length).toBe(1));
    const r2 = http.expectOne(`${base}/installations/my/history`);
    expect(r2.request.method).toBe('GET');
    r2.flush([inst]);
  });

  it('rate POST carries stars', () => {
    svc.rate('p1', 4).subscribe();
    const req = http.expectOne(`${base}/p1/rate`);
    expect(req.request.body.stars).toBe(4);
    req.flush(pack);
  });
});
