import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { TenantSummary } from '../../../features/admin/admin.types';
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

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ClientEditDialogComponent>>('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ClientEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: ACME }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ClientEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('part de l’état réel du client', () => {
    expect(component.form.getRawValue()).toEqual({ name: 'ACME', plan: 'STARTER' });
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

  it('rend le nom nettoyé et l’offre choisie', () => {
    component.form.patchValue({ name: '  ACME Industries  ', plan: 'PRO' });

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({ name: 'ACME Industries', plan: 'PRO' });
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
