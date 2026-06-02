import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { StandardsService } from './standards.service';

describe('StandardsService', () => {
  let service: StandardsService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(StandardsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('listCatalog returns mocked standards in mock mode', (done) => {
    environment.useMockApi = true;
    service.listCatalog().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      expect(page.content.some(s => s.code === 'iso-9001')).toBeTrue();
      done();
    });
  });

  it('listAdoptions returns mocked adoptions in mock mode', (done) => {
    environment.useMockApi = true;
    service.listAdoptions().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('getAlignment GETs the adoption alignment endpoint (no mock)', () => {
    environment.useMockApi = false;
    service.getAlignment('ad1').subscribe();
    const req = httpMock.expectOne(`${environment.apiBaseUrl}/api/v1/standards/adoptions/ad1/alignment`);
    expect(req.request.method).toBe('GET');
    req.flush({});
    httpMock.verify();
  });
});
