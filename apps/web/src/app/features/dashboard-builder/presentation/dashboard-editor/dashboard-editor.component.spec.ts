import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { DashboardBuilderModule } from '../../dashboard-builder.module';
import { DashboardBuilderService } from '../../application/dashboard-builder.service';
import { DashboardExportResult, DashboardLayout, Widget } from '../../domain/dashboard.model';
import { DashboardEditorComponent } from './dashboard-editor.component';

class FakeService {
  saved: DashboardLayout[] = [];
  private counter = 0;

  newDefaultLayout = (name: string): DashboardLayout => ({
    name,
    shared: false,
    widgets: [{
      id: 'seed', type: 'kpi', title: 'Seed',
      position: { x: 0, y: 0, cols: 3, rows: 3 }, config: { kpiId: 'fpy' }
    }]
  });

  get = jasmine.createSpy('get').and.callFake((id: string): any =>
    of<DashboardLayout>({
      id, name: 'Existant', shared: true,
      widgets: [{
        id: 'w1', type: 'bar', title: 'Bar',
        position: { x: 1, y: 1, cols: 4, rows: 3 }, config: {}
      }]
    }));

  save = jasmine.createSpy('save').and.callFake((l: DashboardLayout): any => {
    const saved = { ...l, id: 'new-id', version: 1 };
    this.saved.push(saved);
    return of(saved);
  });

  update = jasmine.createSpy('update').and.callFake((id: string, l: DashboardLayout): any =>
    of({ ...l, id, version: (l.version ?? 1) + 1 }));

  generateId = (): string => 'gen_' + (++this.counter);

  onFilter = () => of(null);
}

function setup(paramId: string | null): {
  fixture: ComponentFixture<DashboardEditorComponent>;
  component: DashboardEditorComponent;
  svc: FakeService;
} {
  const svc = new FakeService();
  TestBed.configureTestingModule({
    imports: [DashboardBuilderModule, HttpClientTestingModule, NoopAnimationsModule, RouterTestingModule],
    providers: [
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap(paramId ? { id: paramId } : {})) } }
    ]
  });
  TestBed.overrideProvider(DashboardBuilderService, { useValue: svc });
  const fixture = TestBed.createComponent(DashboardEditorComponent);
  const component = fixture.componentInstance;
  return { fixture, component, svc };
}

describe('DashboardEditorComponent', () => {

  it('loads a default layout for the "new" route', () => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    expect(component.isNew).toBeTrue();
    expect(component.items.length).toBe(1);
    expect(component.items[0].widget.type).toBe('kpi');
    expect(component.options.minCols).toBe(12);
    expect(component.options.draggable?.enabled).toBeTrue();
    expect(component.options.resizable?.enabled).toBeTrue();
    expect(component.options.enableEmptyCellDrop).toBeTrue();
  });

  it('loads an existing layout by id', () => {
    const { fixture, component, svc } = setup('abc');
    fixture.detectChanges();
    expect(svc.get).toHaveBeenCalledWith('abc');
    expect(component.isNew).toBeFalse();
    expect(component.items[0].widget.id).toBe('w1');
    expect(component.items[0].x).toBe(1);
    expect(component.items[0].cols).toBe(4);
  });

  it('exposes a non-empty palette', () => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    expect(component.palette.length).toBeGreaterThanOrEqual(8);
  });

  it('addWidget appends an item below the existing ones and selects it', () => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    const before = component.items.length;
    component.addWidget('gauge');
    expect(component.items.length).toBe(before + 1);
    const added = component.items[component.items.length - 1];
    expect(added.widget.type).toBe('gauge');
    expect(added.y).toBeGreaterThanOrEqual(3);          // sous le seed (rows 3)
    expect(component.selected?.id).toBe(added.widget.id);
  });

  it('drag from palette then drop on empty cell creates a widget at that cell', () => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    const before = component.items.length;
    const entry = component.palette.find(e => e.type === 'line')!;
    const dt = new DataTransfer();
    component.onPaletteDragStart({ dataTransfer: dt } as unknown as DragEvent, entry);
    // simule le callback emptyCellDrop de gridster
    (component.options.emptyCellDropCallback as Function)(
      { dataTransfer: dt } as unknown as DragEvent, { x: 6, y: 2, cols: 1, rows: 1 });
    expect(component.items.length).toBe(before + 1);
    const created = component.items[component.items.length - 1];
    expect(created.widget.type).toBe('line');
    expect(created.x).toBe(6);
    expect(created.y).toBe(2);
  });

  it('emptyCellDrop is a no-op without a type', () => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    const before = component.items.length;
    (component.options.emptyCellDropCallback as Function)(
      { dataTransfer: new DataTransfer() } as unknown as DragEvent, { x: 0, y: 0, cols: 1, rows: 1 });
    expect(component.items.length).toBe(before);
  });

  it('onWidgetConfigured replaces the widget and keeps the position', () => {
    const { fixture, component } = setup('abc');
    fixture.detectChanges();
    const original = component.items[0].widget;
    const updated: Widget = { ...original, title: 'Renommé' };
    component.onWidgetConfigured(updated);
    expect(component.items[0].widget.title).toBe('Renommé');
    expect(component.items[0].x).toBe(1);               // position préservée
    expect(component.layout.widgets[0].title).toBe('Renommé');
  });

  it('removeWidget deletes the item and clears the selection', () => {
    const { fixture, component } = setup('abc');
    fixture.detectChanges();
    component.select(component.items[0]);
    const id = component.items[0].widget.id;
    component.removeWidget(id);
    expect(component.items.length).toBe(0);
    expect(component.selected).toBeNull();
  });

  it('save() on a new layout calls save and flips isNew', () => {
    const { fixture, component, svc } = setup(null);
    fixture.detectChanges();
    component.save();
    expect(svc.save).toHaveBeenCalled();
    expect(component.isNew).toBeFalse();
    // les positions de la grille sont sérialisées dans le layout sauvegardé
    expect(svc.saved[0].widgets.length).toBe(1);
  });

  it('save() on an existing layout calls update', () => {
    const { fixture, component, svc } = setup('abc');
    fixture.detectChanges();
    component.save();
    expect(svc.update).toHaveBeenCalledWith('abc', jasmine.any(Object));
  });

  it('save() surfaces an error without throwing', () => {
    const { fixture, component, svc } = setup(null);
    fixture.detectChanges();
    svc.save.and.returnValue(throwError(() => new Error('boom')));
    component.save();
    expect(component.saving).toBeFalse();
  });

  it('shows a not-found message when the layout cannot be loaded', () => {
    const { fixture, component, svc } = setup('missing');
    svc.get.and.returnValue(throwError(() => new Error('404')));
    fixture.detectChanges();
    expect(component.loading).toBeFalse();
  });

  it('onSharedChange / onNameChange update the layout', () => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    component.onNameChange('Mon dashboard');
    component.onSharedChange(true);
    expect(component.layout.name).toBe('Mon dashboard');
    expect(component.layout.shared).toBeTrue();
  });

  it('syncs grid positions back into the layout on item change', () => {
    const { fixture, component } = setup(null);
    fixture.detectChanges();
    // simule un déplacement gridster : mutation in-place + callback
    component.items[0].x = 5;
    component.items[0].y = 4;
    (component.options.itemChangeCallback as Function)();
    expect(component.layout.widgets[0].position.x).toBe(5);
    expect(component.layout.widgets[0].position.y).toBe(4);
  });
});

describe('DashboardEditorComponent (export PDF signé §7.4)', () => {
  let component: DashboardEditorComponent;
  let svc: jasmine.SpyObj<DashboardBuilderService>;
  let snack: jasmine.SpyObj<{ open: (m: string, a?: string, c?: unknown) => void }>;

  const exportResult: DashboardExportResult = {
    blob: new Blob(['%PDF'], { type: 'application/pdf' }),
    fileName: 'dashboard-exec.pdf',
    verificationCode: 'abcDEF012345_-xy',
    sha256: 'a'.repeat(64),
    anchorRef: 'tx-1'
  };

  beforeEach(() => {
    svc = jasmine.createSpyObj('DashboardBuilderService', ['get', 'exportPdf']);
    snack = jasmine.createSpyObj('MatSnackBar', ['open']);
    // Construction directe : route/router/catalog non sollicités par l'export.
    component = new DashboardEditorComponent(
      svc as never,
      { entries: () => [] } as never,                    // WidgetCatalogService
      { paramMap: of(convertToParamMap({})) } as never,  // ActivatedRoute
      { navigate: () => {} } as never,                   // Router
      snack as never);
    component.layout = { id: 'd1', name: 'Exec', shared: false, widgets: [] };
    component.isNew = false;
  });

  it('télécharge le PDF signé et affiche le code de vérification', () => {
    svc.exportPdf.and.returnValue(of(exportResult));
    const clickSpy = jasmine.createSpy('click');
    spyOn(document, 'createElement').and.returnValue({ click: clickSpy } as never);
    spyOn(URL, 'createObjectURL').and.returnValue('blob:x');
    spyOn(URL, 'revokeObjectURL');

    component.exportPdf();

    expect(svc.exportPdf).toHaveBeenCalledWith(component.layout);
    expect(clickSpy).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:x');
    expect(component.exporting).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith(
      jasmine.stringMatching(/abcDEF012345_-xy/), 'OK', jasmine.anything());
  });

  it('refuse d\'exporter un dashboard non enregistré', () => {
    component.layout = { name: 'New', shared: false, widgets: [] };
    component.exportPdf();
    expect(svc.exportPdf).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalledWith(
      jasmine.stringMatching(/Enregistrez/), 'OK', jasmine.anything());
  });

  it('affiche une erreur et coupe le spinner en cas d\'échec', () => {
    svc.exportPdf.and.returnValue(throwError(() => new Error('boom')));
    component.exportPdf();
    expect(component.exporting).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith(
      jasmine.stringMatching(/chec/), 'OK', jasmine.anything());
  });
});
