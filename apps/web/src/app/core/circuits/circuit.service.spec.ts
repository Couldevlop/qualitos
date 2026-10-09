import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { CircuitService } from './circuit.service';
import { CircuitRun } from './circuit.types';

describe('CircuitService', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/circuits`;
  let service: CircuitService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(CircuitService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lit et remplace le circuit d’un type d’objet', () => {
    service.circuit('document-version').subscribe(c => expect(c.steps.length).toBe(1));
    http.expectOne(`${endpoint}/document-version`)
      .flush({ subject: 'document-version', steps: [{ name: 'R', roleCode: 'QUALITY_MANAGER', minApprovals: 1 }] });

    service.save('document-version', []).subscribe(c => expect(c.steps).toEqual([]));
    const req = http.expectOne(`${endpoint}/document-version`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ steps: [] });
    req.flush({ subject: 'document-version', steps: [] });
  });

  it('rend le passage d’un objet, ou null s’il n’en a jamais eu', () => {
    const passage = { id: 'r', status: 'IN_PROGRESS', currentStep: 0 } as CircuitRun;
    let lu: CircuitRun | null | undefined;
    service.run('document-version', 'v1').subscribe(r => lu = r);
    http.expectOne(`${endpoint}/document-version/runs/v1`).flush(passage);
    expect(lu).toEqual(passage);

    service.run('document-version', 'v2').subscribe(r => lu = r);
    http.expectOne(`${endpoint}/document-version/runs/v2`).flush(null, { status: 204, statusText: 'No Content' });
    expect(lu).toBeNull();
  });
});
