import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { PdcaService } from './pdca.service';

describe('PdcaService (mock mode)', () => {
  let service: PdcaService;
  let prevMock: boolean;

  beforeEach(() => {
    // Ce spec teste le chemin mock : on force le flag (indépendant du défaut
    // d'environment.ts, qui peut être basculé en oidc/no-mock pour la démo).
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(PdcaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('returns demo cycles', (done) => {
    service.listCycles().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      expect(page.content[0].title).toBeTruthy();
      done();
    });
  });

  it('filters by status', (done) => {
    service.listCycles(0, 20, 'CHECK').subscribe(page => {
      expect(page.content.every(c => c.status === 'CHECK')).toBeTrue();
      done();
    });
  });

  it('returns a cycle by id', (done) => {
    service.getCycle('demo-2').subscribe(c => {
      expect(c.id).toBe('demo-2');
      done();
    });
  });
});
