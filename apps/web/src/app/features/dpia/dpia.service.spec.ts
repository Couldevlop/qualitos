import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { DpiaService } from './dpia.service';

describe('DpiaService (mock mode)', () => {
  let service: DpiaService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(DpiaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('requiresPriorConsultation true for HIGH/SEVERE risk', () => {
    expect(DpiaService.requiresPriorConsultation('HIGH')).toBeTrue();
    expect(DpiaService.requiresPriorConsultation('SEVERE')).toBeTrue();
    expect(DpiaService.requiresPriorConsultation('LOW')).toBeFalse();
  });

  it('lists seeded DPIAs', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a DRAFT DPIA, flagging consultation for SEVERE risk', (done) => {
    service.create({
      reference: 'DPIA-X', title: 'New', initialRiskLevel: 'SEVERE', createdByUserId: 'u'
    }).subscribe(d => {
      expect(d.status).toBe('DRAFT');
      expect(d.consultationRequired).toBeTrue();
      done();
    });
  });

  it('start transitions to IN_PROGRESS', (done) => {
    service.create({
      reference: 'DPIA-S', title: 'Startable', initialRiskLevel: 'LOW', createdByUserId: 'u'
    }).subscribe(d => {
      service.start(d.id, { handledByUserId: 'a' }).subscribe(started => {
        expect(started.status).toBe('IN_PROGRESS');
        done();
      });
    });
  });
});
