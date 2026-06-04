import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ActivityFeedService } from './activity-feed.service';
import { ActivityEntry } from './activity-feed.types';

describe('ActivityFeedService', () => {
  let service: ActivityFeedService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/activity-feed`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ActivityFeedService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    httpMock.verify();
  });

  it('renvoie les données fictives en mode mock, limitées', fakeAsync(() => {
    environment.useMockApi = true;
    let items: ActivityEntry[] | undefined;
    service.recent(2).subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBe(2);
    expect(items![0].sequenceNo).toBe(3);
    httpMock.expectNone(() => true);
  }));

  it('appelle GET /activity-feed avec ?size et mappe page.content (réel)', () => {
    environment.useMockApi = false;
    let items: ActivityEntry[] | undefined;
    service.recent(5).subscribe(r => (items = r));

    const req = httpMock.expectOne(r => r.url === endpoint);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({
      content: [{
        id: 'a', sequenceNo: 1, occurredAt: null, recordedAt: null,
        action: 'capa.created', resourceType: 'capa', resourceId: null,
        actorUserId: null, summary: 's'
      }],
      totalElements: 1
    });
    expect(items!.length).toBe(1);
    expect(items![0].action).toBe('capa.created');
  });

  it('utilise la limite par défaut (10) si non fournie', () => {
    environment.useMockApi = false;
    service.recent().subscribe();
    const req = httpMock.expectOne(r => r.url === endpoint);
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ content: [], totalElements: 0 });
  });

  it('renvoie [] si la page n\'a pas de content', () => {
    environment.useMockApi = false;
    let items: ActivityEntry[] | undefined;
    service.recent().subscribe(r => (items = r));
    const req = httpMock.expectOne(r => r.url === endpoint);
    req.flush({ totalElements: 0 } as any);
    expect(items).toEqual([]);
  });

  it('propage l\'erreur HTTP', () => {
    environment.useMockApi = false;
    let err: any;
    service.recent().subscribe({ error: e => (err = e) });
    const req = httpMock.expectOne(r => r.url === endpoint);
    req.flush('boom', { status: 500, statusText: 'Server Error' });
    expect(err.status).toBe(500);
  });
});
