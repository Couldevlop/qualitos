import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { PmmService } from './pmm.service';
import { FREQUENCY_DAYS, PmmDraftRequest, PmmPlanView } from './pmm.types';

describe('PmmService', () => {
  let service: PmmService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/ai-act/pmm`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(PmmService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded plans (mock)', fakeAsync(() => {
    let items: PmmPlanView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('overdueReviews returns only ACTIVE plans past due (mock)', fakeAsync(() => {
    let items: PmmPlanView[] | undefined;
    service.overdueReviews().subscribe(r => (items = r));
    tick(120);
    expect(items!.every(p => p.status === 'ACTIVE')).toBeTrue();
    // seed-002 has nextReviewDueAt in the past
    expect(items!.some(p => p.id === 'pmm-seed-002')).toBeTrue();
  }));

  it('draft creates a DRAFT plan (mock)', fakeAsync(() => {
    const req: PmmDraftRequest = {
      reference: 'PMM-NEW', aiSystemId: 'sys', name: 'New plan', reviewFrequency: 'MONTHLY',
      createdByUserId: 'u'
    };
    let created: PmmPlanView | undefined;
    service.draft(req).subscribe(r => (created = r));
    tick(140);
    expect(created!.status).toBe('DRAFT');
    expect(created!.nextReviewDueAt).toBeNull();
  }));

  it('activate moves DRAFT to ACTIVE (mock)', fakeAsync(() => {
    let p: PmmPlanView | undefined;
    service.activate('pmm-seed-003').subscribe(r => (p = r));
    tick(120);
    expect(p!.status).toBe('ACTIVE');
    expect(p!.activatedAt).toBeTruthy();
  }));

  it('recordReview computes nextReviewDueAt from frequency (mock)', fakeAsync(() => {
    let p: PmmPlanView | undefined;
    service.recordReview('pmm-seed-001', { reviewedByUserId: 'u' }).subscribe(r => (p = r));
    tick(120);
    expect(p!.lastReviewedAt).toBeTruthy();
    const expectedDelta = FREQUENCY_DAYS['QUARTERLY'] * 86400000;
    const delta = new Date(p!.nextReviewDueAt!).getTime() - new Date(p!.lastReviewedAt!).getTime();
    expect(Math.abs(delta - expectedDelta)).toBeLessThan(1000);
  }));

  it('recordReview rejects non-ACTIVE plan with 409 (mock)', fakeAsync(() => {
    let err: any;
    service.recordReview('pmm-seed-003', { reviewedByUserId: 'u' }).subscribe({ error: e => (err = e) });
    tick(120);
    expect(err.status).toBe(409);
  }));

  it('close sets CLOSED and effectiveTo (mock)', fakeAsync(() => {
    let p: PmmPlanView | undefined;
    service.close('pmm-seed-001', { reason: 'fin de vie' }).subscribe(r => (p = r));
    tick(120);
    expect(p!.status).toBe('CLOSED');
    expect(p!.closureReason).toBe('fin de vie');
  }));

  it('GET list hits the API in non-mock mode', () => {
    environment.useMockApi = false;
    service.list('ACTIVE').subscribe();
    const req = httpMock.expectOne(r => r.url === endpoint && r.params.get('status') === 'ACTIVE');
    expect(req.request.method).toBe('GET');
    req.flush([]);
    httpMock.verify();
  });
});
