import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { TenantIdentityResponse, TenantSummary } from '../../../features/admin/admin.types';
import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import { ClientEditDialogComponent } from './client-edit-dialog.component';

/**
 * « Un client se corrige après sa création. »
 *
 * Ce qui est vérifié ici tient en deux points : la fenêtre part de l'état réel
 * du client, et elle ne laisse pas toucher au repère — qui nomme ce client dans
 * tout ce qui a déjà été consigné sous son nom.
 */
describe('ClientEditDialogComponent', () => {

  let fixture: ComponentFixture<ClientEditDialogComponent>;
  let component: ClientEditDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ClientEditDialogComponent>>;

  const ACME: TenantSummary = {
    id: 't1', slug: 'acme', name: 'ACME', plan: 'STARTER', active: true,
    createdAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-01T00:00:00Z'
  };

  /** Ce que le client porte deja : la fenetre doit en partir, pas d'une page blanche. */
  const IDENTITE: TenantIdentityResponse = {
    slogan: 'Le geste juste', legalName: 'ACME SAS', legalForm: 'SAS',
    registrationNumber: '123 456 789', vatNumber: 'FR12345678901',
    addressLine1: '2 rue des Forges', addressLine2: null,
    postalCode: '69001', city: 'Lyon', countryCode: 'FR',
    contactEmail: 'qualite@acme.test', contactPhone: '+33 4 00 00 00 00',
    primaryColor: '#123456', secondaryColor: null,
    themeCode: 'DENSE_LIGHT', logoAvailable: true
  };

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ClientEditDialogComponent>>('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ClientEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { client: ACME, identity: IDENTITE } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ClientEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('part de l’état réel du client, identité comprise', () => {
    // Le point entier de la fenetre : un formulaire de correction qui s'ouvrirait
    // vide effacerait, au premier enregistrement, tout ce qui n'est pas retape.
    expect(component.form.getRawValue()).toEqual({
      name: 'ACME', plan: 'STARTER', slogan: 'Le geste juste', legalName: 'ACME SAS',
      legalForm: 'SAS', registrationNumber: '123 456 789', vatNumber: 'FR12345678901',
      addressLine1: '2 rue des Forges', postalCode: '69001', city: 'Lyon',
      countryCode: 'FR', contactEmail: 'qualite@acme.test',
      contactPhone: '+33 4 00 00 00 00', primaryColor: '#123456', secondaryColor: '',
      themeCode: 'DENSE_LIGHT'
    });
  });

  it('montre le repère sans le rendre modifiable', () => {
    const hote = fixture.nativeElement as HTMLElement;
    expect(hote.querySelector('[data-test="client-edit-repere"]')!.textContent).toContain('acme');
    expect(hote.querySelector('input[formcontrolname="slug"]')).toBeNull();
  });

  it('ne renvoie rien tant que rien n’a changé', () => {
    // Une écriture sans changement ajouterait un acte vide à la piste d'audit.
    expect(component.unchanged).toBeTrue();

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('rend le nom nettoyé et l’offre choisie, sans toucher au reste', () => {
    component.form.patchValue({ name: '  ACME Industries  ', plan: 'PRO' });

    component.submit();

    // L'identite n'a pas bouge : on ne la renvoie pas. Une ecriture sans
    // changement serait un acte de plus dans la piste d'audit, pour rien.
    expect(dialogRef.close).toHaveBeenCalledWith({
      tenant: { name: 'ACME Industries', plan: 'PRO' }, identity: null, logo: undefined
    });
  });

  it('rend l’identité corrigée sans réécrire l’entreprise', () => {
    component.form.patchValue({ city: 'Villeurbanne', vatNumber: '' });

    component.submit();

    const rendu = dialogRef.close.calls.mostRecent().args[0] as any;
    expect(rendu.tenant).toBeNull();
    expect(rendu.identity.city).toBe('Villeurbanne');
    // Un champ vide EFFACE : c'est la seule facon de retirer un numero de TVA
    // saisi par erreur.
    expect(rendu.identity.vatNumber).toBeNull();
    // Ce qu'on n'a pas touche repart tel quel, et non efface.
    expect(rendu.identity.legalName).toBe('ACME SAS');
  });

  it('demande le retrait du logo déjà en place', () => {
    expect(component.logoDejaLa).toBeTrue();

    component.retirerLogo();
    expect(component.unchanged).toBeFalse();
    component.submit();

    const rendu = dialogRef.close.calls.mostRecent().args[0] as any;
    // `null` dit « retire-le » ; `undefined` dirait « n'y touche pas ».
    expect(rendu.logo).toBeNull();
  });

  it('refuse un pays qui n’est pas un code à deux lettres', () => {
    component.form.patchValue({ countryCode: 'FRA' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('refuse un nom vide', () => {
    component.form.patchValue({ name: '' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ferme sans rien rendre quand on renonce', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
