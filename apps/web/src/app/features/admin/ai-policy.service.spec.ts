import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { AiPolicyService } from './ai-policy.service';

describe('AiPolicyService', () => {
  const base = `${environment.apiBaseUrl}/api/v1/ai/policy`;
  let service: AiPolicyService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AiPolicyService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lit la politique du tenant sans jamais nommer le tenant', () => {
    service.get().subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it("n'envoie que le fournisseur à épingler", () => {
    service.pin('ollama').subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ provider: 'ollama' });
    req.flush({});
  });

  it('revient au défaut de la plateforme par un DELETE', () => {
    service.release().subscribe();
    const req = http.expectOne(base);
    expect(req.request.method).toBe('DELETE');
    req.flush({});
  });
});
