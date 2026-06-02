import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AiQmsService } from './ai-qms.service';
import { AiQmsView, DraftAiQmsRequest } from './ai-qms.types';

describe('AiQmsService', () => {
  let service: AiQmsService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/qms`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(AiQmsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded QMS (mock)', fakeAsync(() => {
    let items: AiQmsView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('resolves by reference (mock)', fakeAsync(() => {
    let q: AiQmsView | undefined;
    service.getByReference('QMS-AI-MEDPHARM').subscribe(r => (q = r));
    tick(100);
    expect(q!.reference).toBe('QMS-AI-MEDPHARM');
  }));

  it('draft creates a DRAFT QMS (mock)', fakeAsync(() => {
    const req: DraftAiQmsRequest = {
      reference: 'QMS-NEW', version: '1.0', name: 'New QMS', createdByUserId: 'u'
    };
    let created: AiQmsView | undefined;
    service.draft(req).subscribe(r => (created = r));
    tick(150);
    expect(created!.status).toBe('DRAFT');
    expect(created!.coveredAiSystemIds).toEqual([]);
  }));

  it('approve transitions to APPROVED (mock)', fakeAsync(() => {
    let q: AiQmsView | undefined;
    service.approve('qms-2', {
      submittedByUserId: 's', approvedByUserId: 'a', approvalNotes: 'ok'
    }).subscribe(r => (q = r));
    tick(120);
    expect(q!.status).toBe('APPROVED');
    expect(q!.approvedByUserId).toBe('a');
  }));

  it('putInForce supersedes the existing IN_FORCE version of same reference (mock)', fakeAsync(() => {
    // qms-1 is IN_FORCE with reference QMS-AI-MEDPHARM ; putting qms-2 in force supersedes qms-1
    let q2: AiQmsView | undefined;
    service.putInForce('qms-2').subscribe(r => (q2 = r));
    tick(120);
    expect(q2!.status).toBe('IN_FORCE');

    let q1: AiQmsView | undefined;
    service.get('qms-1').subscribe(r => (q1 = r));
    tick(100);
    expect(q1!.status).toBe('SUPERSEDED');
    expect(q1!.supersededByQmsId).toBe('qms-2');
  }));

  it('archive sets ARCHIVED with reason (mock)', fakeAsync(() => {
    let q: AiQmsView | undefined;
    service.archive('qms-1', { reason: 'obsolète' }).subscribe(r => (q = r));
    tick(120);
    expect(q!.status).toBe('ARCHIVED');
    expect(q!.archiveReason).toBe('obsolète');
  }));

  it('POST draft hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    const body: DraftAiQmsRequest = { reference: 'R', version: '1', name: 'N', createdByUserId: 'u' };
    service.draft(body).subscribe();
    const req = httpMock.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(body);
    req.flush({} as AiQmsView);
    httpMock.verify();
  });
});
