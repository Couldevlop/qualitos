import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { DashboardBuilderService } from './dashboard-builder.service';
import { DashboardLayout } from '../domain/dashboard.model';
import { DASHBOARD_LAYOUT_REPOSITORY } from '../domain/dashboard-builder.tokens';
import { DashboardLayoutRepository } from '../domain/dashboard-layout.repository';

class FakeRepo implements DashboardLayoutRepository {
  saved: DashboardLayout[] = [];
  list = jasmine.createSpy('list').and.returnValue(of([]));
  get = jasmine.createSpy('get').and.callFake((id: string) =>
    of({ id, name: 'X', widgets: [], shared: false }));
  save = jasmine.createSpy('save').and.callFake((l: DashboardLayout) => {
    const saved = { ...l, id: 'id-1', version: 1 } as DashboardLayout;
    this.saved.push(saved);
    return of(saved);
  });
  update = jasmine.createSpy('update').and.callFake((id: string, l: DashboardLayout) =>
    of({ ...l, id, version: (l.version ?? 1) + 1 }));
  delete = jasmine.createSpy('delete').and.returnValue(of(void 0));
}

describe('DashboardBuilderService', () => {
  let svc: DashboardBuilderService;
  let repo: FakeRepo;

  beforeEach(() => {
    repo = new FakeRepo();
    TestBed.configureTestingModule({
      providers: [
        DashboardBuilderService,
        { provide: DASHBOARD_LAYOUT_REPOSITORY, useValue: repo }
      ]
    });
    svc = TestBed.inject(DashboardBuilderService);
  });

  it('newDefaultLayout returns a single KPI widget', () => {
    const layout = svc.newDefaultLayout('Exec');
    expect(layout.name).toBe('Exec');
    expect(layout.widgets.length).toBe(1);
    expect(layout.widgets[0].type).toBe('kpi');
  });

  it('save delegates to the port', () => {
    svc.save({ name: 'A', widgets: [], shared: false }).subscribe(l => {
      expect(l.id).toBe('id-1');
    });
    expect(repo.save).toHaveBeenCalled();
  });

  it('update delegates to the port', () => {
    svc.update('xyz', { name: 'A', widgets: [], shared: false }).subscribe(l => {
      expect(l.id).toBe('xyz');
    });
    expect(repo.update).toHaveBeenCalled();
  });

  it('list delegates to the port', () => {
    svc.list().subscribe(rows => expect(rows).toEqual([]));
    expect(repo.list).toHaveBeenCalled();
  });

  it('get delegates to the port', () => {
    svc.get('abc').subscribe(l => expect(l.id).toBe('abc'));
    expect(repo.get).toHaveBeenCalledWith('abc');
  });

  it('delete delegates to the port', () => {
    svc.delete('abc').subscribe();
    expect(repo.delete).toHaveBeenCalledWith('abc');
  });

  it('emitFilter / onFilter pipe values to subscribers', (done) => {
    svc.onFilter().subscribe(f => {
      if (f) {
        expect(f.field).toBe('site');
        done();
      }
    });
    svc.emitFilter({ sourceWidgetId: 'w1', field: 'site', value: 'A' });
  });

  it('generateId returns a unique-ish id', () => {
    const a = svc.generateId();
    const b = svc.generateId();
    expect(a).not.toBe(b);
    expect(a.startsWith('w_')).toBeTrue();
  });
});
