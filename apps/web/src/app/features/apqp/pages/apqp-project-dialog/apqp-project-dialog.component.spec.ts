import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  ApqpProjectDialogComponent, ApqpProjectDialogData
} from './apqp-project-dialog.component';

/**
 * Le dialogue qui crée ou modifie un projet APQP.
 *
 * <p>Ce que ce banc tient : le nom est exigé et rendu rogné (un nom réduit à des
 * espaces afficherait une ligne vide dans la liste), le type est repris en
 * modification — le remettre à « NPI » ferait d'une correction de nom une
 * requalification silencieuse — et les champs facultatifs laissés vides partent
 * à `null` plutôt qu'en chaîne blanche.
 */
describe('ApqpProjectDialogComponent', () => {

  let fixture: ComponentFixture<ApqpProjectDialogComponent>;
  let component: ApqpProjectDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ApqpProjectDialogComponent>>;

  async function setup(data: ApqpProjectDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ApqpProjectDialogComponent>>(
      'MatDialogRef', ['close']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpProjectDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpProjectDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('ouvre vide en création, et nomme ce qui bloque', async () => {
    await setup({});

    expect(component.editing).toBeFalse();
    expect(component.form.getRawValue().name).toBe('');
    expect(component.form.getRawValue().type).toBe('NPI');
    expect(component.blockedReason).toContain('nom');
  });

  it('rend le nom rogné et les champs vides à null', async () => {
    await setup({});

    component.form.setValue({
      name: '  Support moteur  ', type: 'TOW',
      customer: '   ', reference: '', description: ''
    });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      name: 'Support moteur', type: 'TOW',
      customer: null, reference: null, description: null
    });
  });

  it('reprend le projet en modification, type compris', async () => {
    await setup({
      project: {
        id: 'pr1', name: 'Transfert ligne 4', type: 'TOW', customer: 'Stellantis',
        reference: 'TL4', description: 'Reprise du poste 3',
        deliverablesTotal: 8, deliverablesDone: 2, ppapTotal: 3, ppapDone: 0,
        createdAt: '2026-09-01T08:00:00Z', updatedAt: '2026-09-01T08:00:00Z'
      }
    });

    expect(component.editing).toBeTrue();
    const valeurs = component.form.getRawValue();
    expect(valeurs.name).toBe('Transfert ligne 4');
    // Le remettre a « NPI » ferait d'une correction de nom une requalification
    // silencieuse du projet.
    expect(valeurs.type).toBe('TOW');
    expect(valeurs.customer).toBe('Stellantis');
    expect(component.dialogTitle).toContain('Modifier');
  });

  it('refuse un nom réduit à des espaces', async () => {
    // `Validators.required` laisse passer '   ' : le dialogue rendait alors un
    // nom vide, et la liste affichait une ligne sans texte.
    await setup({});
    component.form.patchValue({ name: '   ' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBeTrue();
  });

  it('ne rend rien quand on renonce', async () => {
    await setup({});

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
