import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { PrivacyNoticesService } from './privacy-notices.service';

describe('PrivacyNoticesService (mock mode)', () => {
  let service: PrivacyNoticesService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(PrivacyNoticesService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded notices', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('creates a DRAFT notice', (done) => {
    service.create({
      reference: 'PN-X', version: '1', language: 'fr', title: 'T',
      contentMarkdown: '# Notice', createdByUserId: 'u'
    }).subscribe(n => {
      expect(n.status).toBe('DRAFT');
      expect(n.reference).toBe('PN-X');
      done();
    });
  });

  it('versions returns every notice sharing the same reference', (done) => {
    service.create({
      reference: 'PN-MULTI', version: '1', language: 'fr', title: 'A',
      contentMarkdown: 'x', createdByUserId: 'u'
    }).subscribe(() => {
      service.create({
        reference: 'PN-MULTI', version: '2', language: 'en', title: 'B',
        contentMarkdown: 'y', createdByUserId: 'u'
      }).subscribe(() => {
        service.versions('PN-MULTI').subscribe(versions => {
          expect(versions.length).toBe(2);
          expect(versions.every(v => v.reference === 'PN-MULTI')).toBeTrue();
          done();
        });
      });
    });
  });
});
