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
      name: 'QualiConseil', legalName: '', legalForm: '',
      registrationNumber: '123 456 789', vatNumber: '',
      addressLine1: '', addressLine2: '', postalCode: '', city: '', countryCode: '',
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

  /**
   * L'identite complete repart vers le serveur. Elle designe le cabinet sur un
   * accord d'engagement : un champ saisi puis perdu a l'envoi ferait croire a
   * une fiche remplie alors que l'accord continuerait de ne nommer qu'un nom
   * d'usage.
   */
  it('renvoie toute l’identité saisie', () => {
    component.form.patchValue({
      legalName: 'QualiConseil & Associés SARL', legalForm: 'SARL',
      vatNumber: 'FR32812345678', addressLine1: '12 rue de la Qualité',
      postalCode: '75011', city: 'Paris', countryCode: 'fr'
    });

    component.submit();

    const envoye = dialogRef.close.calls.mostRecent().args[0];
    expect(envoye.legalName).toBe('QualiConseil & Associés SARL');
    expect(envoye.legalForm).toBe('SARL');
    expect(envoye.vatNumber).toBe('FR32812345678');
    expect(envoye.addressLine1).toBe('12 rue de la Qualité');
    expect(envoye.postalCode).toBe('75011');
    expect(envoye.city).toBe('Paris');
    // « fr » est un code pays juste, ecrit en minuscules : on le normalise
    // plutot que de le refuser.
    expect(envoye.countryCode).toBe('FR');
  });

  /** Un champ vide part en `null` : « renseigné à vide » n'est pas une mention. */
  it('envoie null, et non une chaîne vide, pour ce qui n’est pas renseigné', () => {
    component.form.patchValue({ legalName: '  ', vatNumber: '', countryCode: '' });

    component.submit();

    const envoye = dialogRef.close.calls.mostRecent().args[0];
    expect(envoye.legalName).toBeNull();
    expect(envoye.vatNumber).toBeNull();
    expect(envoye.countryCode).toBeNull();
  });

  /**
   * UN CODE PAYS QUI N'EN EST PAS UN BLOQUE L'ENVOI. La contrainte existe en
   * base (V13) : laisser partir « France » ferait echouer l'enregistrement
   * avec une erreur de base que personne ne sait lire, apres que la fenetre
   * s'est fermee en donnant l'impression que c'etait pris.
   */
  it('refuse un pays qui n’est pas deux lettres, sans fermer la fenêtre', () => {
    component.form.patchValue({ countryCode: 'France' });

    component.submit();

    expect(component.form.controls.countryCode.valid).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });
});
