import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { Nis2MeasuresService } from './nis2m.service';

describe('Nis2MeasuresService (mock mode)', () => {
  let service: Nis2MeasuresService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(Nis2MeasuresService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded measures', (done) => {
    service.list().subscribe(items => {
      expect(items.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by status', (done) => {
    service.list().subscribe(all => {
      const status = all[0].status;
      service.list(status).subscribe(items => {
        expect(items.every(m => m.status === status)).toBeTrue();
        done();
      });
    });
  });

  it('listByCategory filters on the measure category', (done) => {
    service.list().subscribe(all => {
      const category = all[0].category;
      service.listByCategory(category).subscribe(items => {
        expect(items.every(m => m.category === category)).toBeTrue();
        done();
      });
    });
  });

  it('resolves a measure by id', (done) => {
    service.list().subscribe(all => {
      const id = all[0].id;
      service.get(id).subscribe(found => {
        expect(found.id).toBe(id);
        done();
      });
    });
  });
});
