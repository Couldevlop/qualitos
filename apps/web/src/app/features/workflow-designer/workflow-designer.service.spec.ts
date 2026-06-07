import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import {
  HttpTestingController, provideHttpClientTesting
} from '@angular/common/http/testing';

import { environment } from '../../../environments/environment';
import { WorkflowDesignerService } from './workflow-designer.service';

describe('WorkflowDesignerService (mock mode)', () => {
  let service: WorkflowDesignerService;
  let prevMock: boolean;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = true;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(WorkflowDesignerService);
  });

  afterEach(() => { environment.useMockApi = prevMock; });

  it('lists seeded workflows', (done) => {
    service.list().subscribe(page => {
      expect(page.content.length).toBeGreaterThan(0);
      done();
    });
  });

  it('filters by status', (done) => {
    service.list(0, 50, { status: 'PUBLISHED' }).subscribe(page => {
      expect(page.content.every(w => w.status === 'PUBLISHED')).toBeTrue();
      done();
    });
  });

  it('creates a DRAFT workflow at version 1', (done) => {
    service.create({ name: 'New WF', bpmnXml: service.emptyDiagram() }).subscribe(wf => {
      expect(wf.status).toBe('DRAFT');
      expect(wf.version).toBe(1);
      expect(wf.name).toBe('New WF');
      done();
    });
  });

  it('update increments version and applies fields', (done) => {
    service.create({ name: 'WF', bpmnXml: service.emptyDiagram() }).subscribe(created => {
      service.update(created.id, { name: 'Renamed' }).subscribe(updated => {
        expect(updated.name).toBe('Renamed');
        expect(updated.version).toBe(2);
        done();
      });
    });
  });

  it('publish transitions to PUBLISHED', (done) => {
    service.create({ name: 'WF', bpmnXml: service.emptyDiagram() }).subscribe(created => {
      service.publish(created.id).subscribe(wf => {
        expect(wf.status).toBe('PUBLISHED');
        done();
      });
    });
  });

  it('archive transitions to ARCHIVED', (done) => {
    service.create({ name: 'WF', bpmnXml: service.emptyDiagram() }).subscribe(created => {
      service.archive(created.id).subscribe(wf => {
        expect(wf.status).toBe('ARCHIVED');
        done();
      });
    });
  });

  it('delete removes the workflow from the list', (done) => {
    service.create({ name: 'ToDelete', bpmnXml: service.emptyDiagram() }).subscribe(created => {
      service.delete(created.id).subscribe(() => {
        service.get(created.id).subscribe(fetched => {
          expect(fetched.id).not.toBe(created.id);
          done();
        });
      });
    });
  });

  it('emptyDiagram returns a BPMN definitions skeleton', () => {
    expect(service.emptyDiagram()).toContain('bpmn:definitions');
    expect(service.emptyDiagram()).toContain('bpmn:process');
  });
});

describe('WorkflowDesignerService (HTTP mode)', () => {
  let service: WorkflowDesignerService;
  let httpMock: HttpTestingController;
  let prevMock: boolean;
  const base = `${environment.apiBaseUrl}/api/v1/workflow/definitions`;

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(WorkflowDesignerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { httpMock.verify(); environment.useMockApi = prevMock; });

  it('GET list hits the definitions endpoint with paging', () => {
    service.list(1, 20).subscribe();
    const req = httpMock.expectOne(r => r.url === base);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('20');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 1, size: 20 });
  });

  it('POST create sends name + bpmnXml', () => {
    service.create({ name: 'X', bpmnXml: '<xml/>' }).subscribe();
    const req = httpMock.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'X', bpmnXml: '<xml/>' });
    req.flush({});
  });

  it('PUT update targets the id', () => {
    service.update('abc', { name: 'Y' }).subscribe();
    const req = httpMock.expectOne(`${base}/abc`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('POST publish targets /publish', () => {
    service.publish('abc').subscribe();
    const req = httpMock.expectOne(`${base}/abc/publish`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('POST archive targets /archive', () => {
    service.archive('abc').subscribe();
    const req = httpMock.expectOne(`${base}/abc/archive`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('DELETE targets the id', () => {
    service.delete('abc').subscribe();
    const req = httpMock.expectOne(`${base}/abc`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});
