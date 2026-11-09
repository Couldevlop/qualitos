import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { PartnerFirm } from '../../../core/partner/partner.types';
import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import { FirmEditDialogComponent } from './firm-edit-dialog.component';

/**
 * « La fiche d'un cabinet se corrige. »
 *
 * Un cabinet déménage, change de référent, régularise son immatriculation. Ce
 * qui ne change pas, c'est son repère : il nomme le cabinet dans des accords
 * que des clients ont signés.
 */
describe('FirmEditDialogComponent', () => {

  let fixture: ComponentFixture<FirmEditDialogComponent>;
  let component: FirmEditDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FirmEditDialogComponent>>;

  const CABINET = {
    id: 'f1', slug: 'qualiconseil', name: 'QualiConseil', registrationNumber: '123 456 789',
    contactEmail: 'contact@qualiconseil.fr', contactPhone: '+33 1 02 03 04 05',
    active: true, consultantCount: 2, grantedCount: 1, createdAt: '2026-10-01T00:00:00Z'
  } as PartnerFirm;

  async function monter(firm: PartnerFirm = CABINET): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<FirmEditDialogComponent>>('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [FirmEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: firm }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(FirmEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => monter());

  it('part de la fiche réelle du cabinet', () => {
    expect(component.form.getRawValue()).toEqual({
      name: 'QualiConseil', registrationNumber: '123 456 789',
      contactEmail: 'contact@qualiconseil.fr', contactPhone: '+33 1 02 03 04 05'
    });
  });

  it('affiche les champs vides plutôt que « null » quand la fiche est incomplète', async () => {
    await monter({ ...CABINET, registrationNumber: null, contactEmail: null, contactPhone: null });

    expect(component.form.getRawValue().registrationNumber).toBe('');
    expect(component.form.getRawValue().contactEmail).toBe('');
  });

  it('renvoie le repère inchangé avec la fiche corrigée', () => {
    component.form.patchValue({ name: '  QualiConseil & Associés  ' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({
      slug: 'qualiconseil', name: 'QualiConseil & Associés'
    }));
  });

  it('rend « aucune valeur » pour un champ vidé, et non une chaîne vide', () => {
    component.form.patchValue({ contactPhone: '   ' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({ contactPhone: null }));
  });

  it('refuse une adresse de contact qui n’en est pas une', () => {
    component.form.patchValue({ contactEmail: 'pas-une-adresse' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ferme sans rien rendre quand on renonce', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
