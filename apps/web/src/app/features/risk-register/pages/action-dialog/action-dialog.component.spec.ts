import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { ActionDialogComponent, ActionDialogData } from './action-dialog.component';

describe('ActionDialogComponent', () => {

  let fixture: ComponentFixture<ActionDialogComponent>;
  let component: ActionDialogComponent;
  let ref: jasmine.SpyObj<MatDialogRef<ActionDialogComponent>>;

  async function setup(data: ActionDialogData): Promise<void> {
    ref = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ActionDialogComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ActionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('en création : intitulé, échéance et statut ; rend une saisie rognée', async () => {
    await setup({ mode: 'action', reference: 'O-003' });
    const el = fixture.nativeElement as HTMLElement;
    expect(component.isEdit).toBeFalse();
    expect(el.textContent).toContain('O-003');
    expect(el.querySelector('[data-test="action-statut"]')).not.toBeNull();

    component.form.patchValue({ title: '  Chiffrer le gain ', dueDate: '2026-12-01' });
    component.submit();
    expect(ref.close).toHaveBeenCalledWith({ title: 'Chiffrer le gain', dueDate: '2026-12-01', status: 'TO_START' });
  });

  it('en modification d’une action : préremplie, avec son statut', async () => {
    await setup({ mode: 'action', reference: 'O-003', number: 3, title: 'Chiffrer', dueDate: null, status: 'IN_PROGRESS' });
    expect(component.isEdit).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ACT-3');
    component.submit();
    expect(ref.close).toHaveBeenCalledWith({ title: 'Chiffrer', dueDate: null, status: 'IN_PROGRESS' });
  });

  it('n’ouvre plus de CAPA : ni nature, ni responsable, ni description (ADR 0088)', async () => {
    await setup({ mode: 'action', reference: 'O-003' });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="action-nature"]')).toBeNull();
    expect(el.querySelector('[data-test="action-responsable"]')).toBeNull();
    expect(el.querySelector('[data-test="action-description"]')).toBeNull();
  });

  it('sans intitulé, ne ferme pas', async () => {
    await setup({ mode: 'action', reference: 'O-003' });
    component.submit();
    expect(ref.close).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
  });
});
