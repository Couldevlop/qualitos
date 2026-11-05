import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { RequirementRequest, STAKEHOLDER_LIMITS } from '../../stakeholders.types';
import {
  StakeholderRequirementDialogComponent,
  StakeholderRequirementDialogData
} from './stakeholder-requirement-dialog.component';

/**
 * Une exigence s'énonce ; tout le reste se complète plus tard. Et son
 * responsable est une PERSONNE de l'annuaire, affichée par son nom.
 */
describe('StakeholderRequirementDialogComponent', () => {

  const AMINA: DirectoryUser = {
    id: 'i-1', accountId: 'aaaaaaaa-1111-4111-8111-111111111111',
    displayName: 'Amina Dridi', email: 'amina@client.test', active: true
  };

  let fixture: ComponentFixture<StakeholderRequirementDialogComponent>;
  let component: StakeholderRequirementDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<StakeholderRequirementDialogComponent,
                                             RequirementRequest>>;

  async function build(data: StakeholderRequirementDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService',
      ['search', 'getByAccount']);
    directory.search.and.returnValue(
      of({ content: [AMINA], page: 0, size: 20, totalElements: 1, totalPages: 1 }));
    directory.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderRequirementDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        { provide: DirectoryService, useValue: directory },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(StakeholderRequirementDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => build({ stakeholderName: 'ARS Occitanie' }));

  it('rappelle la partie prenante concernée', () => {
    expect(component.subtitle).toContain('ARS Occitanie');
  });

  it('avec la seule exigence, rend une demande dont le reste est absent', () => {
    component.form.patchValue({ requirement: '  Déclarer tout EIG sous 48 h  ' });
    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      requirement: 'Déclarer tout EIG sous 48 h',
      need: null, origin: null, nature: null, risk: null, riskLevel: null,
      control: null, indicator: null, responsible: null, evidence: null,
      status: 'TO_ANALYSE'
    });
  });

  it('sans exigence, ne ferme pas et DIT ce qui bloque', () => {
    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.controls.requirement.touched).toBeTrue();
    expect(component.blockedReason).toBeTruthy();
  });

  it('au-delà de la longueur acceptée, bloque avant l’aller-retour', () => {
    component.form.patchValue({ requirement: 'x'.repeat(STAKEHOLDER_LIMITS.requirement + 1) });

    expect(component.form.invalid).toBeTrue();
    expect(component.blockedReason).toBeTruthy();
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('le responsable part par son NOM, jamais par son identifiant', () => {
    component.form.patchValue({ requirement: 'Déclarer tout EIG sous 48 h' });
    component.onResponsible(AMINA);
    component.submit();

    const sent = dialogRef.close.calls.mostRecent().args[0] as RequirementRequest;
    expect(sent.responsible).toBe('Amina Dridi');
    expect(sent.responsible).not.toContain(AMINA.accountId);
  });

  it('vidé, le responsable redevient absent', () => {
    component.form.patchValue({ requirement: 'Déclarer tout EIG sous 48 h' });
    component.onResponsible(AMINA);
    component.onResponsible(null);
    component.submit();

    const sent = dialogRef.close.calls.mostRecent().args[0] as RequirementRequest;
    expect(sent.responsible).toBeNull();
  });

  it('annulée, ne rend rien', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
