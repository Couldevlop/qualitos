import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { ConsentsService } from './consents.service';

describe('ConsentsService (mock mode)', () => {
  let service: ConsentsService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(ConsentsService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('searchBySubject hashes the identifier and matches seeded consents', (done) => {
    service.searchBySubject('alice@example.fr').subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      expect(items.every(c => c.subjectIdentifierHash.startsWith('fnv1a:'))).toBeTrue();
      done();
    });
  });

  it('searchByPurpose filters on purpose code', (done) => {
    service.searchByPurpose('newsletter.marketing').subscribe(items => {
      expect(items.every(c => c.purposeCode === 'newsletter.marketing')).toBeTrue();
      done();
    });
  });

  it('active returns the granted consent for a subject+purpose', (done) => {
    service.active('alice@example.fr', 'newsletter.marketing').subscribe(c => {
      expect(c).not.toBeNull();
      expect(c!.active).toBeTrue();
      done();
    });
  });

  it('grant then withdraw toggles status and active flag', (done) => {
    service.grant({
      subjectIdentifier: 'carol@example.fr', purposeCode: 'p', purposeVersion: '1',
      source: 'WEB_FORM', grantedByUserId: 'u'
    }).subscribe(c => {
      expect(c.status).toBe('GRANTED');
      expect(c.active).toBeTrue();
      service.withdraw(c.id, { actorUserId: 'u', reason: 'opt-out' }).subscribe(w => {
        expect(w.status).toBe('WITHDRAWN');
        expect(w.active).toBeFalse();
        done();
      });
    });
  });
});
