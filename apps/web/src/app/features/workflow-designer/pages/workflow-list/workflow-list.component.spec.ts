import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { WorkflowDesignerService } from '../../workflow-designer.service';
import { WorkflowSummary } from '../../workflow-designer.types';
import { WorkflowListComponent } from './workflow-list.component';

describe('WorkflowListComponent', () => {
  let component: WorkflowListComponent;
  let fixture: ComponentFixture<WorkflowListComponent>;
  let svc: jasmine.SpyObj<WorkflowDesignerService>;

  const draft: WorkflowSummary = {
    id: 'w1', name: 'Processus CAPA', description: 'desc', status: 'DRAFT',
    version: 1, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  };
  const published: WorkflowSummary = { ...draft, id: 'w2', name: 'Revue', status: 'PUBLISHED', version: 3 };

  beforeEach(async () => {
    svc = jasmine.createSpyObj<WorkflowDesignerService>('WorkflowDesignerService',
      ['list', 'publish', 'archive']);
    svc.list.and.returnValue(of({
      content: [draft, published], totalElements: 2, totalPages: 1, number: 0, size: 2
    }));
    svc.publish.and.returnValue(of({ ...draft, status: 'PUBLISHED', bpmnXml: '<xml/>' }));
    svc.archive.and.returnValue(of({ ...draft, status: 'ARCHIVED', bpmnXml: '<xml/>' }));

    await TestBed.configureTestingModule({
      declarations: [WorkflowListComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: WorkflowDesignerService, useValue: svc }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(WorkflowListComponent);
    component = fixture.componentInstance;
  });

  it('renders the page title', () => {
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Designer de workflow');
  });

  it('exposes the canonical workflow statuses', () => {
    expect(component.statuses).toEqual(['DRAFT', 'PUBLISHED', 'ARCHIVED']);
  });

  it('exposes one workflow per loaded item', (done) => {
    fixture.detectChanges();
    component.workflows$.subscribe(list => {
      expect(list.length).toBe(2);
      expect(list.map(w => w.id)).toEqual(['w1', 'w2']);
      done();
    });
  });

  it('computes status badge classes', () => {
    expect(component.statusBadgeClass('DRAFT')).toBe('wf-badge wf-draft');
    expect(component.statusBadgeClass('PUBLISHED')).toBe('wf-badge wf-published');
  });

  it('createNew navigates to the new editor route', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.createNew();
    expect(nav).toHaveBeenCalledWith(['/workflow-designer', 'new']);
  });

  it('open navigates to the workflow editor', () => {
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate');
    component.open(draft);
    expect(nav).toHaveBeenCalledWith(['/workflow-designer', 'w1']);
  });

  it('publish calls the service and stops event propagation', () => {
    const ev = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);
    component.publish(draft, ev);
    expect(ev.stopPropagation).toHaveBeenCalled();
    expect(svc.publish).toHaveBeenCalledWith('w1');
  });

  it('archive calls the service', () => {
    const ev = jasmine.createSpyObj<Event>('Event', ['stopPropagation']);
    component.archive(published, ev);
    expect(svc.archive).toHaveBeenCalledWith('w2');
  });
});
