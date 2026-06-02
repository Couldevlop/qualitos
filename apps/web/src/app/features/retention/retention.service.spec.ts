import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import {
  RetentionService, daysToDuration, describeDuration, durationToDays
} from './retention.service';

describe('Retention duration helpers', () => {
  it('durationToDays parses ISO-8601 PnD', () => {
    expect(durationToDays('P1095D')).toBe(1095);
    expect(durationToDays('P0D')).toBe(0);
    expect(durationToDays('')).toBe(0);
  });

  it('daysToDuration converts months/years to days', () => {
    expect(daysToDuration(3, 'YEAR')).toBe('P1095D');
    expect(daysToDuration(6, 'MONTH')).toBe('P180D');
    expect(daysToDuration(10, 'DAY')).toBe('P10D');
  });

  it('describeDuration renders human labels', () => {
    expect(describeDuration('P1095D')).toBe('3 ans (1095 j)');
    expect(describeDuration('P180D')).toBe('6 mois (180 j)');
    expect(describeDuration('P1D')).toBe('1 jour');
  });
});

describe('RetentionService (mock mode)', () => {
  let service: RetentionService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(RetentionService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded rules', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a DRAFT rule', (done) => {
    service.create({
      dataCategoryCode: 'CAT', dataCategoryLabel: 'Cat', retentionPeriod: 'P365D',
      legalBasis: 'LEGAL_OBLIGATION', createdByUserId: 'u'
    }).subscribe(r => {
      expect(r.status).toBe('DRAFT');
      done();
    });
  });

  it('activate transitions a rule to ACTIVE', (done) => {
    service.create({
      dataCategoryCode: 'UNIQUE-CAT', dataCategoryLabel: 'X', retentionPeriod: 'P30D',
      legalBasis: 'CONSENT', createdByUserId: 'u'
    }).subscribe(r => {
      service.activate(r.id).subscribe(a => {
        expect(a.status).toBe('ACTIVE');
        done();
      });
    });
  });
});
