import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { PdcaService } from './pdca.service';

describe('PdcaService (mock mode)', () => {
  let service: PdcaService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(PdcaService);
  });

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
