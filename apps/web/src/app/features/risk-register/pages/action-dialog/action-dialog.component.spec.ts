import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatRadioModule } from '@angular/material/radio';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { ActionDialogComponent, ActionDialogData, localIsoDate } from './action-dialog.component';

describe('ActionDialogComponent', () => {

  let fixture: ComponentFixture<ActionDialogComponent>;
  let component: ActionDialogComponent;
  let ref: jasmine.SpyObj<MatDialogRef<ActionDialogComponent>>;

  async function setup(data: ActionDialogData): Promise<void> {
    ref = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ActionDialogComponent],
      imports: [SharedModule, MatAutocompleteModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ActionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('en mode CAPA : nature, responsable, description, pas de statut ; rend une saisie rognée', async () => {
    await setup({ mode: 'capa', reference: 'R-014', assignee: 'M. Kone', assignees: ['M. Kone', 'A. Diallo'] });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="action-description"]')).not.toBeNull();
    expect(el.querySelector('[data-test="action-nature"]')).not.toBeNull();
    expect(el.querySelector('[data-test="action-statut"]')).toBeNull();
    expect(el.textContent).toContain('R-014');
    expect(component.form.controls.assignee.value).toBe('M. Kone');
    expect(component.form.controls.kind.value).toBe('PREVENTIVE');

    component.form.patchValue({
      title: '  Carte SPC ', description: ' ', dueDate: '2026-12-01', kind: 'CORRECTIVE', assignee: ' A. Diallo '
    });
    component.submit();
    expect(ref.close).toHaveBeenCalledWith({
      title: 'Carte SPC', description: null, dueDate: '2026-12-01', status: 'TO_START',
      kind: 'CORRECTIVE', assignee: 'A. Diallo'
    });
  });

  it('en mode CAPA : responsable et échéance sont obligatoires', async () => {
    await setup({ mode: 'capa', reference: 'R-014' });
    component.form.patchValue({ title: 'Carte SPC' });
    component.submit();
    expect(ref.close).not.toHaveBeenCalled();
    expect(component.form.controls.assignee.hasError('required')).toBeTrue();
    expect(component.form.controls.dueDate.hasError('required')).toBeTrue();

    component.form.patchValue({ assignee: 'A. Diallo', dueDate: '2026-12-01' });
    component.submit();
    expect(ref.close).toHaveBeenCalled();
  });

  it('propose les noms déjà employés qui contiennent la saisie', async () => {
    await setup({ mode: 'capa', reference: 'R-014', assignees: ['M. Kone', 'A. Diallo', 'B. Kone'] });
    component.form.controls.assignee.setValue('kone');
    expect(component.assigneeOptions).toEqual(['M. Kone', 'B. Kone']);
    expect(localIsoDate(new Date(2026, 0, 5))).toBe('2026-01-05');
  });

  it('en modification d’une action : préremplie, avec son statut', async () => {
    await setup({ mode: 'action', reference: 'O-003', number: 3, title: 'Chiffrer', dueDate: null, status: 'IN_PROGRESS' });
    expect(component.isEdit).toBeTrue();
    expect(component.isCapa).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ACT-3');
    component.submit();
    expect(ref.close).toHaveBeenCalledWith({
      title: 'Chiffrer', description: null, dueDate: null, status: 'IN_PROGRESS', kind: null, assignee: null
    });
    // Hors CAPA, ni responsable ni échéance ne sont exigés.
    expect(component.form.controls.dueDate.hasError('required')).toBeFalse();
  });

  it('sans intitulé, ne ferme pas', async () => {
    await setup({ mode: 'action', reference: 'O-003' });
    component.submit();
    expect(ref.close).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
  });
});
