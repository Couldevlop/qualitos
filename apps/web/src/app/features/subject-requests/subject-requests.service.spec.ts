import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { SubjectRequestsService } from './subject-requests.service';
import { ReceiveSubjectRequest, SubjectRequestView } from './subject-requests.types';

describe('SubjectRequestsService', () => {
  let service: SubjectRequestsService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/subject-requests`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(SubjectRequestsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded requests (mock)', fakeAsync(() => {
    let items: SubjectRequestView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('only stores hashes, never plaintext identifiers (RGPD-safe) (mock)', fakeAsync(() => {
    let items: SubjectRequestView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.every(r => r.subjectIdentifierHash.startsWith('fnv1a:'))).toBeTrue();
    expect(JSON.stringify(items)).not.toContain('alice@example.fr');
  }));

  it('receive hashes the identifier and sets a 1-month deadline (mock)', fakeAsync(() => {
    const req: ReceiveSubjectRequest = {
      type: 'ACCESS', subjectIdentifier: 'eve@example.fr', requestedByUserId: 'u'
    };
    let created: SubjectRequestView | undefined;
    service.receive(req).subscribe(r => (created = r));
    tick(150);
    expect(created!.status).toBe('RECEIVED');
    expect(created!.subjectIdentifierHash).toMatch(/^fnv1a:/);
    expect(JSON.stringify(created)).not.toContain('eve@example.fr');
    const delta = new Date(created!.deadlineAt).getTime() - new Date(created!.receivedAt).getTime();
    expect(Math.abs(delta - 30 * 86400000)).toBeLessThan(60000);
  }));

  it('searchBySubject matches the stored hash of the same identifier (mock)', fakeAsync(() => {
    let items: SubjectRequestView[] | undefined;
    service.searchBySubject('alice@example.fr').subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBe(1);
    expect(items![0].id).toBe('sr-1');
  }));

  it('overdue returns only live past-deadline requests (mock)', fakeAsync(() => {
    let items: SubjectRequestView[] | undefined;
    service.overdue().subscribe(r => (items = r));
    tick(120);
    // sr-4 is IN_PROGRESS with a past deadline
    expect(items!.some(r => r.id === 'sr-4')).toBeTrue();
    expect(items!.every(r => r.status !== 'COMPLETED' && r.status !== 'REJECTED')).toBeTrue();
  }));

  it('start moves RECEIVED to IN_PROGRESS (mock)', fakeAsync(() => {
    let r: SubjectRequestView | undefined;
    service.start('sr-3', { handledByUserId: 'h' }).subscribe(x => (r = x));
    tick(120);
    expect(r!.status).toBe('IN_PROGRESS');
    expect(r!.handledByUserId).toBe('h');
  }));

  it('complete sets COMPLETED with resolution + evidence (mock)', fakeAsync(() => {
    let r: SubjectRequestView | undefined;
    service.complete('sr-1', {
      resolutionNotes: 'fait', evidenceUrl: 'https://x/p.pdf'
    }).subscribe(x => (r = x));
    tick(120);
    expect(r!.status).toBe('COMPLETED');
    expect(r!.resolutionNotes).toBe('fait');
  }));

  it('extend pushes the deadline and flags extended (mock)', fakeAsync(() => {
    const newDeadline = new Date(Date.now() + 60 * 86400000).toISOString();
    let r: SubjectRequestView | undefined;
    service.extend('sr-3', { newDeadline }).subscribe(x => (r = x));
    tick(120);
    expect(r!.extended).toBeTrue();
    expect(r!.deadlineAt).toBe(newDeadline);
  }));

  it('GET list hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    service.list('RECEIVED').subscribe();
    const req = httpMock.expectOne(r => r.url === endpoint && r.params.get('status') === 'RECEIVED');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    httpMock.verify();
  });
});
