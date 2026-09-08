import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import {
  ApqpDeliverableDialogComponent, ApqpDeliverableDialogData
} from './apqp-deliverable-dialog.component';

/**
 * Le dialogue qui ajoute ou reformule un livrable.
 *
 * <p>Le sous-titre y porte la phase : sur un cycle de cinq à six phases portant
 * chacune une dizaine de livrables, un dialogue qui ne dirait pas où l'on écrit
 * laisserait un doute au moment de valider. C'est le point que ce banc tient.
 */
describe('ApqpDeliverableDialogComponent', () => {

  let fixture: ComponentFixture<ApqpDeliverableDialogComponent>;
  let component: ApqpDeliverableDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ApqpDeliverableDialogComponent>>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  async function setup(data: ApqpDeliverableDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ApqpDeliverableDialogComponent>>(
      'MatDialogRef', ['close']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpDeliverableDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpDeliverableDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('affiche la phase où l’on écrit', async () => {
    await setup({ phaseTitle: 'Conception du processus' });

    expect(hote().textContent).toContain('Conception du processus');
  });

  it('ouvre vide en ajout, et nomme ce qui bloque', async () => {
    await setup({ phaseTitle: 'Planification' });

    expect(component.editing).toBeFalse();
    expect(component.form.getRawValue().label).toBe('');
    expect(component.blockedReason).toContain('livrable');
  });

  it('reprend le libellé en reformulation, et le rend rogné', async () => {
    await setup({
      phaseTitle: 'Planification',
      deliverable: { id: 'l1', position: 1, label: 'AMDEC processus' }
    });
    expect(component.editing).toBeTrue();
    expect(component.form.getRawValue().label).toBe('AMDEC processus');

    component.form.setValue({ label: '  AMDEC processus (PFMEA)  ' });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({ label: 'AMDEC processus (PFMEA)' });
  });

  it('refuse un livrable réduit à des espaces', async () => {
    // `Validators.required` laisse passer '   ' : le dialogue rendait alors un
    // libellé vide, et la liste affichait une puce sans texte.
    await setup({ phaseTitle: 'Planification' });
    component.form.setValue({ label: '   ' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.label.touched).toBeTrue();
  });

  it('ne rend rien quand on renonce', async () => {
    await setup({ phaseTitle: 'Planification' });

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
