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
 * laisserait un doute au moment de valider.
 *
 * <p>Et l'on n'y choisit plus de « genre » : il décidait du formulaire qu'on
 * verrait en ouvrant le livrable, donc de ce qu'on aurait le droit d'y mettre,
 * avant même d'avoir travaillé le sujet. À sa place, un texte libre.
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

  it('ne propose plus de genre, mais un artefact attendu en texte libre', async () => {
    await setup({ phaseTitle: 'Planification' });

    // Le genre decidait du formulaire qu'on verrait en ouvrant le livrable :
    // le remplacer par un texte n'interdit plus rien.
    expect(hote().querySelector('mat-select')).toBeNull();
    expect(hote().querySelector('[data-test=artefact-attendu]')).not.toBeNull();
  });

  it('reprend le libellé en reformulation, et le rend rogné', async () => {
    await setup({
      phaseTitle: 'Planification',
      deliverable: {
        id: 'l1', position: 1, label: 'AMDEC processus',
        expectedArtifact: 'Tableau PFMEA', ppap: true,
        status: 'IN_PROGRESS', percentComplete: 40, done: false, evidenceCount: 0
      }
    });
    expect(component.editing).toBeTrue();
    expect(component.form.getRawValue().label).toBe('AMDEC processus');
    // L'artefact et la marque viennent du livrable : un dialogue qui les
    // remettrait a zero effacerait silencieusement ce qui etait decrit.
    expect(component.form.getRawValue().expectedArtifact).toBe('Tableau PFMEA');
    expect(component.form.getRawValue().ppap).toBeTrue();

    component.form.setValue({
      label: '  AMDEC processus (PFMEA)  ', expectedArtifact: '  Tableau PFMEA  ', ppap: true
    });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      label: 'AMDEC processus (PFMEA)', expectedArtifact: 'Tableau PFMEA', ppap: true
    });
  });

  it('rend null pour un artefact laissé vide, jamais une chaîne blanche', async () => {
    await setup({ phaseTitle: 'Planification' });

    component.form.setValue({ label: 'Plan projet', expectedArtifact: '   ', ppap: false });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      label: 'Plan projet', expectedArtifact: null, ppap: false
    });
  });

  it('refuse un livrable réduit à des espaces', async () => {
    // `Validators.required` laisse passer '   ' : le dialogue rendait alors un
    // libellé vide, et la liste affichait une puce sans texte.
    await setup({ phaseTitle: 'Planification' });
    component.form.setValue({ label: '   ', expectedArtifact: '', ppap: false });

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
