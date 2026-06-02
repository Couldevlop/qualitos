import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { IshikawaService } from './ishikawa.service';

describe('IshikawaService (mock mode)', () => {
  let service: IshikawaService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(IshikawaService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded diagrams', (done) => {
    service.listDiagrams().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters diagrams by status', (done) => {
    service.listDiagrams(0, 50, 'VALIDATED').subscribe(page => {
      expect(page.content.every(d => d.status === 'VALIDATED')).toBeTrue();
      done();
    });
  });

  it('creates a DRAFT diagram', (done) => {
    service.createDiagram({ problemStatement: 'Pb test', mode: 'SIX_M', ownerId: 'u' }).subscribe(d => {
      expect(d.status).toBe('DRAFT');
      expect(d.causes).toEqual([]);
      done();
    });
  });

  it('updates a diagram status', (done) => {
    service.updateDiagram('ish-1', { status: 'IN_REVIEW' }).subscribe(d => {
      expect(d.status).toBe('IN_REVIEW');
      done();
    });
  });

  it('adds a cause to a diagram', (done) => {
    service.addCause('ish-2', { category: 'METHODS', label: 'Procédure absente' }).subscribe(c => {
      expect(c.diagramId).toBe('ish-2');
      expect(c.category).toBe('METHODS');
      done();
    });
  });

  it('suggests AI causes per branch', (done) => {
    service.suggestCauses('ish-1').subscribe(causes => {
      expect(causes.length).toBeGreaterThan(0);
      expect(causes[0].category).toBeTruthy();
      done();
    });
  });

  it('deletes a diagram', (done) => {
    service.createDiagram({ problemStatement: 'A supprimer', mode: 'SIX_M', ownerId: 'u' }).subscribe(d => {
      service.deleteDiagram(d.id).subscribe(() => {
        service.getDiagram(d.id).subscribe(found => {
          expect(found.id).not.toBe(d.id);
          done();
        });
      });
    });
  });
});
