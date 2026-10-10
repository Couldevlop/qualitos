import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { EditionService } from './edition.service';
import { EditionView, SAAS } from './edition.types';

describe('EditionService', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/edition`;
  let service: EditionService;
  let http: HttpTestingController;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(EditionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    environment.useMockApi = prevMock;
  });

  it('lit l’édition une seule fois par session', () => {
    const onprem = { ...SAAS, edition: 'ONPREM', licenseStatus: 'VALID' } as EditionView;
    const vus: string[] = [];
    service.edition().subscribe(e => vus.push(e));
    service.current().subscribe(v => vus.push(v.licenseStatus));
    http.expectOne(endpoint).flush(onprem);
    service.edition().subscribe(e => vus.push(e));
    expect(vus).toEqual(['ONPREM', 'VALID', 'ONPREM']);
  });

  it('une lecture en échec retombe sur l’édition SaaS', () => {
    let vu: EditionView | undefined;
    service.current().subscribe(v => vu = v);
    http.expectOne(endpoint).flush('x', { status: 503, statusText: 'KO' });
    expect(vu).toEqual(SAAS);
  });

  it('en démonstration sans serveur, c’est l’édition SaaS', () => {
    environment.useMockApi = true;
    const s = new EditionService(null as never);
    let vu: string | undefined;
    s.edition().subscribe(e => vu = e);
    expect(vu).toBe('SAAS');
  });
});
