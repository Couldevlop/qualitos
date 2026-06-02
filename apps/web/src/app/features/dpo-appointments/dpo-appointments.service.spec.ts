import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { DpoAppointmentsService } from './dpo-appointments.service';
import { DpoAppointmentView, ProposeDpoRequest } from './dpo-appointments.types';

describe('DpoAppointmentsService', () => {
  let service: DpoAppointmentsService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const endpoint = `${environment.apiBaseUrl}/api/v1/gdpr/dpo-appointments`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DpoAppointmentsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded appointments (mock)', fakeAsync(() => {
    let items: DpoAppointmentView[] | undefined;
    service.list().subscribe(r => (items = r));
    tick(120);
    expect(items!.length).toBeGreaterThan(0);
  }));

  it('findActiveByScope returns the active appointment of a scope (mock)', fakeAsync(() => {
    let a: DpoAppointmentView | null | undefined;
    service.findActiveByScope('GROUPE').subscribe(r => (a = r));
    tick(100);
    expect(a!.scope).toBe('GROUPE');
    expect(a!.status).toBe('ACTIVE');
  }));

  it('findActiveByScope returns null when no active match (mock)', fakeAsync(() => {
    let a: DpoAppointmentView | null | undefined = undefined;
    service.findActiveByScope('UNKNOWN_SCOPE').subscribe(r => (a = r));
    tick(100);
    expect(a).toBeNull();
  }));

  it('propose creates a PROPOSED appointment (mock)', fakeAsync(() => {
    const req: ProposeDpoRequest = {
      reference: 'DPO-NEW', dpoFullName: 'Test', dpoEmail: 't@x.io',
      dpoType: 'INTERNAL', scope: 'FILIALE-X', createdByUserId: 'u'
    };
    let created: DpoAppointmentView | undefined;
    service.propose(req).subscribe(r => (created = r));
    tick(150);
    expect(created!.status).toBe('PROPOSED');
  }));

  it('activate ends the prior ACTIVE appointment of the same scope (mock)', fakeAsync(() => {
    // dpo-1 is ACTIVE scope GROUPE; create a new GROUPE appointment and activate it
    let created: DpoAppointmentView | undefined;
    service.propose({
      reference: 'DPO-GROUPE-2027', dpoFullName: 'Successeur', dpoEmail: 's@x.io',
      dpoType: 'INTERNAL', scope: 'GROUPE', createdByUserId: 'u'
    }).subscribe(r => (created = r));
    tick(150);

    let activated: DpoAppointmentView | undefined;
    service.activate(created!.id, {
      effectiveFrom: new Date().toISOString(),
      regulatorNotifiedAt: new Date().toISOString(),
      regulatorNotificationReference: 'CNIL-X'
    }).subscribe(r => (activated = r));
    tick(120);
    expect(activated!.status).toBe('ACTIVE');

    let prior: DpoAppointmentView | undefined;
    service.get('dpo-1').subscribe(r => (prior = r));
    tick(100);
    expect(prior!.status).toBe('ENDED');
  }));

  it('cancel sets CANCELLED with reason (mock)', fakeAsync(() => {
    let a: DpoAppointmentView | undefined;
    service.cancel('dpo-2', { reason: 'abandon' }).subscribe(r => (a = r));
    tick(120);
    expect(a!.status).toBe('CANCELLED');
    expect(a!.endReason).toBe('abandon');
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
