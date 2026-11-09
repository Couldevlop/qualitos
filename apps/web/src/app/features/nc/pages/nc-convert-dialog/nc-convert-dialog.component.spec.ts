import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { STUB_PERSON, provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { NcSeverity } from '../../nc.types';
import {
  NcConvertDialogComponent, NcConvertDialogData
} from './nc-convert-dialog.component';

/**
 * « Une non-conformité devient un PDCA ou un projet DMAIC selon sa gravité. »
 *
 * <p>Ce que ces tests tiennent : la gravité PROPOSE la méthode et l'écran le
 * dit — un écran qui choisirait en silence laisserait croire que la plateforme
 * décide seule ; la personne qui tient le dossier peut en décider autrement, et
 * l'écran le signale alors ; l'échéance n'existe que pour un projet, parce
 * qu'un cycle PDCA n'en porte pas et qu'un champ ignoré par le serveur est une
 * promesse non tenue.
 */
describe('NcConvertDialogComponent', () => {

  let fixture: ComponentFixture<NcConvertDialogComponent>;
  let component: NcConvertDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<NcConvertDialogComponent>>;

  const ecart = (severity: NcSeverity): NcConvertDialogData => ({
    reference: 'NC-2026-0001', title: 'Jeu excessif sur le mandrin', severity
  });

  async function monter(severity: NcSeverity = 'MAJOR'): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<NcConvertDialogComponent>>('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [NcConvertDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, MatDatepickerModule,
                MatNativeDateModule, NoopAnimationsModule],
      providers: [
        provideDirectoryStub(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: ecart(severity) }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NcConvertDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => monter());

  // ---------- la gravité propose ----------

  it('propose un cycle pour un écart majeur', () => {
    expect(component.proposed).toBe('PDCA');
    expect(component.form.getRawValue().method).toBe('PDCA');
  });

  it('propose un cycle pour un écart mineur', async () => {
    await monter('MINOR');

    expect(component.form.getRawValue().method).toBe('PDCA');
  });

  it('propose un projet pour un écart critique', async () => {
    await monter('CRITICAL');

    expect(component.proposed).toBe('DMAIC');
    expect(component.form.getRawValue().method).toBe('DMAIC');
  });

  it('dit pourquoi, au lieu de choisir en silence', () => {
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('planifier, agir, vérifier');
  });

  it('signale que la méthode retenue n’est pas celle que la gravité appelait', () => {
    expect(component.overridden).toBeFalse();

    component.form.patchValue({ method: 'DMAIC' });
    fixture.detectChanges();

    expect(component.overridden).toBeTrue();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="ecart-regle"]')).not.toBeNull();
  });

  // ---------- ce qui n'est pas redemandé, et ce qui l'est ----------

  it('ne redemande ni l’intitulé ni l’énoncé du problème', () => {
    // Ils sont déjà écrits sur l'écart, et le serveur les reprend : deux
    // énoncés du même problème divergeraient à la première correction.
    expect(Object.keys(component.form.controls).sort())
      .toEqual(['dueDate', 'method', 'pilotId']);
  });

  it('rappelle en lecture ce que le dossier reprendra de l’écart', () => {
    const repris = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="repris"]')!.textContent ?? '';
    expect(repris).toContain('Jeu excessif sur le mandrin');
    // La gravité s'affiche traduite, jamais en code brut.
    expect(repris).toContain('Majeure');
    expect(repris).not.toContain('MAJOR');
  });

  it('n’ouvre rien sans la personne qui mène le travail', () => {
    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('rend la méthode et le pilote choisi', () => {
    component.form.patchValue({ pilotId: STUB_PERSON.accountId });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      method: 'PDCA', pilotId: STUB_PERSON.accountId, dueDate: null
    });
  });

  // ---------- l'échéance, réservée au projet ----------

  it('ne montre l’échéance que pour un projet DMAIC', async () => {
    const champ = () => (fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="echeance"]');
    expect(component.dmaic).toBeFalse();
    expect(champ()).toBeNull();

    await monter('CRITICAL');

    expect(component.dmaic).toBeTrue();
    expect(champ()).not.toBeNull();
  });

  it('rend l’échéance en jour, sans fuseau', async () => {
    await monter('CRITICAL');
    component.form.patchValue({
      pilotId: STUB_PERSON.accountId,
      // Un 1er mars local : converti par un UTC naïf, il repartirait au 28 ou
      // 29 février selon le fuseau de la machine.
      dueDate: new Date(2027, 2, 1)
    });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      method: 'DMAIC', pilotId: STUB_PERSON.accountId, dueDate: '2027-03-01'
    });
  });

  it('n’envoie pas une échéance saisie puis rebasculée sur un cycle', async () => {
    // Le serveur l'ignorerait, et l'utilisateur croirait l'avoir posée.
    await monter('CRITICAL');
    component.form.patchValue({
      pilotId: STUB_PERSON.accountId, dueDate: new Date(2027, 2, 1), method: 'PDCA'
    });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      method: 'PDCA', pilotId: STUB_PERSON.accountId, dueDate: null
    });
  });

  it('ferme sans rien rendre quand on renonce', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });

  it('tient la même règle que le serveur, et se lit seule', () => {
    expect(NcConvertDialogComponent.methodFor('MINOR')).toBe('PDCA');
    expect(NcConvertDialogComponent.methodFor('MAJOR')).toBe('PDCA');
    expect(NcConvertDialogComponent.methodFor('CRITICAL')).toBe('DMAIC');
  });
});
