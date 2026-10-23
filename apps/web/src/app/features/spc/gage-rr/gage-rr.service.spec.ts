import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../../environments/environment';
import { GOLDEN_GRID, OPERATORS, sheet, summary } from './gage-rr.fixtures';
import { GageRrService } from './gage-rr.service';
import { GageRrRequest } from './gage-rr.types';

describe('GageRrService', () => {
  let service: GageRrService;
  let http: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1/spc/gage-rr`;

  const request: GageRrRequest = {
    partReference: 'Arbre AX-210', characteristic: null, instrument: null, lsl: 9.9, usl: 10.1,
    operatorCount: 3, partCount: 10, trialCount: 3, sigmaMultiplier: 5.15, studyDate: '2026-10-19',
    responsibleUserId: null, responsibleName: null, operators: OPERATORS, measurements: GOLDEN_GRID
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(GageRrService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('liste le registre', () => {
    service.list().subscribe(r => expect(r.length).toBe(1));
    const req = http.expectOne(base);
    expect(req.request.method).toBe('GET');
    req.flush([summary()]);
  });

  it('lit une étude, en encodant son identifiant', () => {
    service.get('a/b').subscribe(s => expect(s.study.id).toBe('g1'));
    const req = http.expectOne(`${base}/a%2Fb`);
    expect(req.request.method).toBe('GET');
    req.flush(sheet());
  });

  it('crée une étude', () => {
    service.create(request).subscribe(s => expect(s.study.ndc).toBe(6));
    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(sheet());
  });

  it('révise une étude', () => {
    service.update('g1', request).subscribe();
    const req = http.expectOne(`${base}/g1`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.operators.length).toBe(3);
    req.flush(sheet());
  });

  it('supprime une étude', () => {
    service.delete('g1').subscribe();
    const req = http.expectOne(`${base}/g1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
