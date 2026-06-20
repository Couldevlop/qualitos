import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';

import { AnnotationsPanelComponent } from './annotations-panel.component';
import { DashboardAnnotationService } from '../dashboard-annotation.service';
import { DashboardAnnotation } from '../dashboard-annotation.types';

describe('AnnotationsPanelComponent', () => {
  let fixture: ComponentFixture<AnnotationsPanelComponent>;
  let component: AnnotationsPanelComponent;
  let svc: jasmine.SpyObj<DashboardAnnotationService>;

  const a1: DashboardAnnotation = {
    id: 'a1', tenantId: 't', authorId: 'u', chartKey: 'exec.trend',
    anchorLabel: null, body: 'Hello', createdAt: '2026-06-20T10:00:00Z', deletable: true
  };
  const a2: DashboardAnnotation = { ...a1, id: 'a2', body: 'Second', deletable: false };

  beforeEach(async () => {
    svc = jasmine.createSpyObj('DashboardAnnotationService', ['list', 'create', 'delete']);
    await TestBed.configureTestingModule({
      declarations: [AnnotationsPanelComponent],
      imports: [FormsModule],
      providers: [{ provide: DashboardAnnotationService, useValue: svc }]
    }).compileComponents();

    fixture = TestBed.createComponent(AnnotationsPanelComponent);
    component = fixture.componentInstance;
    component.chartKey = 'exec.trend';
  });

  it('loads annotations on chartKey change', () => {
    svc.list.and.returnValue(of([a1, a2]));
    fixture.detectChanges(); // triggers ngOnChanges via input? set manually:
    component.ngOnChanges({ chartKey: { currentValue: 'exec.trend', previousValue: undefined, firstChange: true, isFirstChange: () => true } });
    expect(svc.list).toHaveBeenCalledWith('exec.trend');
    expect(component.annotations$.value.length).toBe(2);
    expect(component.loading$.value).toBe(false);
  });

  it('surfaces a load error', () => {
    svc.list.and.returnValue(throwError(() => new Error('boom')));
    component.reload();
    expect(component.error$.value).toBeTruthy();
    expect(component.loading$.value).toBe(false);
  });

  it('add() posts the draft and prepends the created annotation', () => {
    svc.list.and.returnValue(of([]));
    component.reload();
    svc.create.and.returnValue(of(a1));
    component.draft = '  Hello  ';
    component.add();
    expect(svc.create).toHaveBeenCalledWith({ chartKey: 'exec.trend', anchorLabel: null, body: 'Hello' });
    expect(component.annotations$.value[0].id).toBe('a1');
    expect(component.draft).toBe('');
  });

  it('add() ignores a blank draft', () => {
    component.draft = '   ';
    component.add();
    expect(svc.create).not.toHaveBeenCalled();
  });

  it('add() surfaces an error', () => {
    svc.create.and.returnValue(throwError(() => new Error('x')));
    component.draft = 'x';
    component.add();
    expect(component.error$.value).toBeTruthy();
    expect(component.submitting$.value).toBe(false);
  });

  it('remove() deletes a deletable annotation', () => {
    svc.list.and.returnValue(of([a1, a2]));
    component.reload();
    svc.delete.and.returnValue(of(void 0));
    component.remove(a1);
    expect(svc.delete).toHaveBeenCalledWith('a1');
    expect(component.annotations$.value.find(x => x.id === 'a1')).toBeUndefined();
  });

  it('remove() ignores a non-deletable annotation', () => {
    component.remove(a2);
    expect(svc.delete).not.toHaveBeenCalled();
  });

  it('remove() surfaces a forbidden error', () => {
    svc.list.and.returnValue(of([a1]));
    component.reload();
    svc.delete.and.returnValue(throwError(() => new Error('403')));
    component.remove(a1);
    expect(component.error$.value).toBeTruthy();
  });

  it('passes anchorLabel when creating', () => {
    component.anchorLabel = 'Machine';
    svc.create.and.returnValue(of(a1));
    component.draft = 'note';
    component.add();
    expect(svc.create).toHaveBeenCalledWith(
      jasmine.objectContaining({ anchorLabel: 'Machine' }));
  });

  it('trackById returns id', () => {
    expect(component.trackById(0, a1)).toBe('a1');
  });
});
