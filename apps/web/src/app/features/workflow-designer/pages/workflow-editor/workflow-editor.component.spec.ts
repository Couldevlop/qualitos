import { ComponentFixture, TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { WorkflowDesignerService } from '../../workflow-designer.service';
import { WorkflowDefinition } from '../../workflow-designer.types';
import {
  BpmnModelerCtor, BpmnModelerLike, WorkflowEditorComponent
} from './workflow-editor.component';

/**
 * Faux BpmnModeler : remplace la vraie lib bpmn-js (jamais chargée en test).
 * importXML/saveXML/destroy sont stubés ; saveXML renvoie un XML déterministe.
 */
class FakeModeler implements BpmnModelerLike {
  static lastInstance: FakeModeler | null = null;
  imported: string | null = null;
  destroyed = false;
  constructor(_opts: { container: HTMLElement }) { FakeModeler.lastInstance = this; }
  async importXML(xml: string): Promise<{ warnings: unknown[] }> { this.imported = xml; return { warnings: [] }; }
  async saveXML(): Promise<{ xml?: string }> { return { xml: '<bpmn:definitions>saved</bpmn:definitions>' }; }
  destroy(): void { this.destroyed = true; }
}

/** Sous-classe testable : injecte le faux modeler sans import() dynamique réel. */
class TestableEditor extends WorkflowEditorComponent {
  override async loadModelerCtor(): Promise<BpmnModelerCtor> {
    return FakeModeler as unknown as BpmnModelerCtor;
  }
}

function setup(routeId: string, getResult?: WorkflowDefinition) {
  const svc = jasmine.createSpyObj<WorkflowDesignerService>('WorkflowDesignerService',
    ['get', 'create', 'update', 'publish', 'emptyDiagram']);
  svc.emptyDiagram.and.returnValue('<bpmn:definitions/>');
  if (getResult) svc.get.and.returnValue(of(getResult));
  svc.create.and.returnValue(of(getResult ?? definition('created', 'DRAFT')));
  svc.update.and.returnValue(of(getResult ?? definition('w1', 'DRAFT')));
  svc.publish.and.returnValue(of(definition('w1', 'PUBLISHED')));

  TestBed.configureTestingModule({
    declarations: [TestableEditor],
    imports: [SharedModule, FormsModule, NoopAnimationsModule],
    providers: [
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideRouter([]),
      { provide: WorkflowDesignerService, useValue: svc },
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => routeId } } } }
    ]
  });
  const fixture = TestBed.createComponent(TestableEditor) as ComponentFixture<TestableEditor>;
  return { fixture, component: fixture.componentInstance, svc };
}

function definition(id: string, status: WorkflowDefinition['status']): WorkflowDefinition {
  return {
    id, name: 'WF', description: 'd', bpmnXml: '<bpmn:definitions>loaded</bpmn:definitions>',
    status, version: 2, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  };
}

describe('WorkflowEditorComponent (bpmn-js mocked)', () => {
  afterEach(() => { FakeModeler.lastInstance = null; });

  it('new mode: starts as new with an empty diagram, no backend get', () => {
    const { fixture, component, svc } = setup('new');
    fixture.detectChanges();
    expect(component.isNew).toBeTrue();
    expect(component.loading).toBeFalse();
    expect(svc.get).not.toHaveBeenCalled();
  });

  it('initializes the mocked modeler and imports XML into the canvas', fakeAsync(() => {
    const { fixture } = setup('new');
    fixture.detectChanges(); // ngOnInit + ngAfterViewInit -> tryInit
    tick();                  // resolve loadModelerCtor + importXML
    fixture.detectChanges();
    expect(FakeModeler.lastInstance).toBeTruthy();
    expect(FakeModeler.lastInstance?.imported).toContain('bpmn:definitions');
    flush(); // drain any leftover async timers
  }));

  it('edit mode: loads the definition from the service', () => {
    const { fixture, component, svc } = setup('w1', definition('w1', 'DRAFT'));
    fixture.detectChanges();
    expect(svc.get).toHaveBeenCalledWith('w1');
    expect(component.name).toBe('WF');
    expect(component.version).toBe(2);
  });

  it('published workflow is read-only', () => {
    const { fixture, component } = setup('w1', definition('w1', 'PUBLISHED'));
    fixture.detectChanges();
    expect(component.readonly).toBeTrue();
  });

  it('draft workflow is editable', () => {
    const { fixture, component } = setup('w1', definition('w1', 'DRAFT'));
    fixture.detectChanges();
    expect(component.readonly).toBeFalse();
  });

  it('save on a new workflow calls create with the saved XML', fakeAsync(() => {
    const { fixture, component, svc } = setup('new');
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture.detectChanges();
    tick(); // modeler ready
    component.name = 'My WF';
    void component.save();
    tick();
    expect(svc.create).toHaveBeenCalled();
    const arg = svc.create.calls.mostRecent().args[0];
    expect(arg.name).toBe('My WF');
    expect(arg.bpmnXml).toContain('saved'); // came from FakeModeler.saveXML
    flush(); // drain snackBar timer
  }));

  it('save on an existing draft calls update', fakeAsync(() => {
    const { fixture, component, svc } = setup('w1', definition('w1', 'DRAFT'));
    fixture.detectChanges();
    tick();
    void component.save();
    tick();
    expect(svc.update).toHaveBeenCalledWith('w1', jasmine.objectContaining({ name: 'WF' }));
    flush(); // drain snackBar timer
  }));

  it('save is a no-op when read-only', fakeAsync(() => {
    const { fixture, component, svc } = setup('w1', definition('w1', 'ARCHIVED'));
    fixture.detectChanges();
    tick();
    void component.save();
    tick();
    expect(svc.update).not.toHaveBeenCalled();
  }));

  it('publish calls the service and applies the new status', () => {
    const { fixture, component, svc } = setup('w1', definition('w1', 'DRAFT'));
    fixture.detectChanges();
    component.publish();
    expect(svc.publish).toHaveBeenCalledWith('w1');
    expect(component.status).toBe('PUBLISHED');
  });

  it('destroys the modeler on component destroy', fakeAsync(() => {
    const { fixture } = setup('new');
    fixture.detectChanges();
    tick();
    const instance = FakeModeler.lastInstance;
    fixture.destroy();
    expect(instance?.destroyed).toBeTrue();
  }));
});
