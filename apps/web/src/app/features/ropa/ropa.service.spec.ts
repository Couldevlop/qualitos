import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { RopaService } from './ropa.service';

describe('RopaService (mock mode)', () => {
  let service: RopaService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(RopaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded processing activities', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by status', (done) => {
    service.list('ACTIVE').subscribe(items => {
      expect(items.every(a => a.status === 'ACTIVE')).toBeTrue();
      done();
    });
  });

  it('resolves by reference', (done) => {
    service.getByReference('RH-PAYROLL-FR').subscribe(a => {
      expect(a.reference).toBe('RH-PAYROLL-FR');
      done();
    });
  });

  it('creates a DRAFT activity', (done) => {
    service.create({
      reference: 'NEW-REF', name: 'New', purposes: 'p', lawfulBasis: 'CONSENT',
      controllerName: 'QualitOS', controllerContact: 'dpo@qualitos.io',
      specialCategoriesProcessed: false, createdByUserId: 'u'
    }).subscribe(a => {
      expect(a.status).toBe('DRAFT');
      done();
    });
  });

  it('activate transitions to ACTIVE and sets effectiveFrom', (done) => {
    service.activate('ropa-3').subscribe(a => {
      expect(a.status).toBe('ACTIVE');
      expect(a.effectiveFrom).toBeTruthy();
      done();
    });
  });
});
