import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CoqLabel, CoqLine } from '../../cost-of-quality.types';
import { CoqEntryDialogComponent, CoqEntryDialogData } from './coq-entry-dialog.component';

/**
 * La fenêtre d'une ligne de coût.
 *
 * <p>Le cœur : les champs pièces n'existent que pour un libellé de contrôle de
 * pièces — livré ou déclaré tel à la saisie d'un texte libre —, et y sont alors
 * tous obligatoires.
 */
describe('CoqEntryDialogComponent', () => {

  let fixture: ComponentFixture<CoqEntryDialogComponent>;
  let component: CoqEntryDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CoqEntryDialogComponent>>;

  const RECEPTION: CoqLabel = {
    id: 'l-rec', category: 'APPRAISAL', code: 'APPRAISAL_INCOMING_INSPECTION', name: 'Réception',
    partControl: true, builtIn: true
  };
  const ESSAIS: CoqLabel = {
    id: 'l-ess', category: 'APPRAISAL', code: 'APPRAISAL_TESTING_CALIBRATION', name: 'Essais',
    partControl: false, builtIn: true
  };
  const TRI: CoqLabel = {
    id: 'l-tri', category: 'APPRAISAL', code: null, name: 'Tri 100 %', partControl: false, builtIn: false
  };

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  async function setup(overrides: Partial<CoqEntryDialogData> = {}): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CoqEntryDialogComponent>>('MatDialogRef', ['close']);
    const data: CoqEntryDialogData = {
      category: 'APPRAISAL', labels: [RECEPTION, ESSAIS, TRI], defaultDate: '2026-09-01',
      readOnly: false, currency: 'EUR', ...overrides
    };
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [CoqEntryDialogComponent],
      imports: [SharedModule, UiModule, MatAutocompleteModule, MatDatepickerModule, MatNativeDateModule,
        NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CoqEntryDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function remplirCommun(): void {
    component.form.patchValue({ amount: 120, responsible: '  M. Alaoui  ' });
  }

  it('s’ouvre vide sur la date proposée, et dit ce qui bloque', async () => {
    await setup();

    expect(component.form.controls.label.value).toBe('');
    expect(CoqEntryDialogComponent.enDateIso(component.form.controls.imputationDate.value)).toBe('2026-09-01');
    expect(component.blockedReason).toContain('libellé');
    expect(component.partControl).toBeFalse();
  });

  it('un libellé ordinaire n’exige que le montant, le responsable et la date', async () => {
    await setup();
    component.form.controls.label.setValue('Essais et étalonnage');
    remplirCommun();
    fixture.detectChanges();

    expect(component.selected?.id).toBe('l-ess');
    expect(hote().querySelector('[data-test="bloc-pieces"]')).toBeNull();
    expect(component.form.valid).toBeTrue();

    component.submit();
    expect(dialogRef.close).toHaveBeenCalledWith({
      labelId: 'l-ess',
      entry: { amount: 120, responsible: 'M. Alaoui', imputationDate: '2026-09-01', comment: undefined }
    });
  });

  it('un libellé de contrôle de pièces rend les quatre champs pièces obligatoires', async () => {
    await setup();
    component.form.controls.label.setValue('contrôle réception matières');
    remplirCommun();
    fixture.detectChanges();

    expect(component.partControl).toBeTrue();
    expect(hote().querySelector('[data-test="bloc-pieces"]')).not.toBeNull();
    expect(component.form.valid).toBeFalse();
    expect(component.blockedReason).toContain('lot');

    component.form.patchValue({
      partReference: ' P-4410 ', partQuantity: 12, lot: ' L-2609 ',
      receivedOrMadeOn: new Date(2026, 8, 12)
    });
    expect(component.form.valid).toBeTrue();

    component.submit();
    expect(dialogRef.close).toHaveBeenCalledWith({
      labelId: 'l-rec',
      entry: {
        amount: 120, responsible: 'M. Alaoui', imputationDate: '2026-09-01', comment: undefined,
        partReference: 'P-4410', partQuantity: 12, lot: 'L-2609', receivedOrMadeOn: '2026-09-12'
      }
    });
  });

  it('un nombre de pièces nul ou décimal est refusé', async () => {
    await setup({ presetLabelId: 'l-rec' });
    const q = component.form.controls.partQuantity;

    q.setValue(0);
    expect(q.valid).toBeFalse();
    q.setValue(2.5);
    expect(q.valid).toBeFalse();
    q.setValue(3);
    expect(q.valid).toBeTrue();
  });

  it('revenir à un libellé ordinaire lève l’obligation des champs pièces', async () => {
    await setup({ presetLabelId: 'l-rec' });
    remplirCommun();
    expect(component.form.valid).toBeFalse();

    component.form.controls.label.setValue('Essais et étalonnage');
    expect(component.form.valid).toBeTrue();
  });

  it('un texte libre devient un libellé neuf, pièces ou non selon la case', async () => {
    await setup();
    component.form.controls.label.setValue('Tri 200 %');
    remplirCommun();
    fixture.detectChanges();

    expect(component.isNewLabel).toBeTrue();
    expect(hote().querySelector('[data-test="nouveau-pieces"]')).not.toBeNull();
    expect(component.form.valid).toBeTrue();

    component.form.controls.newPartControl.setValue(true);
    expect(component.partControl).toBeTrue();
    expect(component.form.valid).toBeFalse();
    component.form.patchValue({ partReference: 'P', partQuantity: 1, lot: 'L', receivedOrMadeOn: new Date(2026, 8, 1) });

    component.submit();
    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({
      newLabel: { name: 'Tri 200 %', partControl: true }
    }));
  });

  it('un libellé saisi existant se reconnaît à son nom, sans créer de doublon', async () => {
    await setup();
    component.form.controls.label.setValue('tri 100 %');

    expect(component.selected?.id).toBe('l-tri');
    expect(component.isNewLabel).toBeFalse();
  });

  it('la liste filtre le catalogue sur le texte tapé', async () => {
    await setup();
    component.form.controls.label.setValue('étal');

    expect(component.suggestions.map(l => l.id)).toEqual(['l-ess']);
  });

  it('une ligne ouverte reprend ses valeurs, dates comprises, sans décalage de fuseau', async () => {
    const ligne: CoqLine = {
      entryId: 'e1', labelId: 'l-rec', labelCode: 'APPRAISAL_INCOMING_INSPECTION', labelName: 'Réception',
      partControl: true, amount: 574, entryCount: 1, responsible: 'Mme Diallo', imputationDate: '2026-09-18',
      comment: 'Lot trié', partReference: 'P-1', partQuantity: 40, lot: 'L-1', receivedOrMadeOn: '2026-09-17'
    };
    await setup({ line: ligne });

    expect(component.title).toContain('Contrôle réception matières');
    expect(component.form.controls.label.value).toBe('Contrôle réception matières');
    const date = component.form.controls.imputationDate.value!;
    expect([date.getFullYear(), date.getMonth(), date.getDate()]).toEqual([2026, 8, 18]);
    expect(component.form.valid).toBeTrue();
  });

  it('en lecture seule, tout est désactivé et rien ne se rend', async () => {
    await setup({ presetLabelId: 'l-ess', readOnly: true });

    expect(component.form.disabled).toBeTrue();
    expect(hote().textContent).toContain('Consultation seule');
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('un formulaire invalide ne se ferme pas et marque les champs', async () => {
    await setup();
    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.responsible.touched).toBeTrue();
  });

  it('annuler ferme sans rien rendre', async () => {
    await setup();
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });

  it('les conversions de date tiennent sur les composantes locales', () => {
    expect(CoqEntryDialogComponent.enDateIso(new Date(2026, 0, 5))).toBe('2026-01-05');
    expect(CoqEntryDialogComponent.enDateIso(null)).toBeNull();
    expect(CoqEntryDialogComponent.enDateIso(new Date('invalide'))).toBeNull();
    expect(CoqEntryDialogComponent.enDateLocale('bof')).toBeNull();
    expect(CoqEntryDialogComponent.enDateLocale(null)).toBeNull();
  });
});
