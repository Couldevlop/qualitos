import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StakeholdersService } from '../../stakeholders.service';
import { StakeholderSheet } from '../../stakeholders.types';
import { StakeholderCreateDialogComponent, StakeholderCreated } from './stakeholder-create-dialog.component';

/**
 * Inscrire tient en trois champs, et la fenêtre rend la FICHE créée : c'est elle
 * que l'hôte ouvre aussitôt, pour que l'analyse suive l'inscription.
 */
describe('StakeholderCreateDialogComponent', () => {

  const SHEET = { id: 'pi-1', name: 'ARS Occitanie' } as StakeholderSheet;

  let fixture: ComponentFixture<StakeholderCreateDialogComponent>;
  let component: StakeholderCreateDialogComponent;
  let svc: jasmine.SpyObj<StakeholdersService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<StakeholderCreateDialogComponent, StakeholderCreated>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(): Promise<void> {
    svc = jasmine.createSpyObj<StakeholdersService>('StakeholdersService', ['create']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: StakeholdersService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(StakeholderCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => build());

  it('rend la fiche créée, pour que son analyse enchaîne', () => {
    svc.create.and.returnValue(of(SHEET));

    component.form.patchValue({ name: '  ARS Occitanie ', category: 'REGULATORS', side: 'EXTERNAL' });
    component.submit();

    expect(svc.create).toHaveBeenCalledWith({
      name: 'ARS Occitanie', category: 'REGULATORS', side: 'EXTERNAL'
    });
    expect(dialogRef.close).toHaveBeenCalledWith({ sheet: SHEET });
  });

  it('sans nom, n’appelle rien et DIT ce qui bloque', () => {
    component.submit();

    expect(svc.create).not.toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBeTrue();
    // Un bouton grisé sans motif est une impasse : la fenêtre nomme le champ.
    expect(component.blockedReason).toContain('nom');
  });

  it('ne demande que trois champs : ni besoins, ni cotation, ni pertinence', () => {
    expect(Object.keys(component.form.controls).sort())
      .toEqual(['category', 'name', 'side']);
  });

  it('sur échec du serveur, ne ferme pas et laisse corriger', () => {
    svc.create.and.returnValue(throwError(() => ({ status: 409 })));

    component.form.patchValue({ name: 'ARS Occitanie' });
    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });
});
