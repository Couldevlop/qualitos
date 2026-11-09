import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { STUB_PERSON, provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { RiskRegisterService } from '../../../risk-register/risk-register.service';
import { ImpactLevel } from '../../stakeholders.types';
import {
  StakeholderRiskDialogComponent, StakeholderRiskDialogData
} from './stakeholder-risk-dialog.component';

/**
 * « Le risque qu'une exigence fait courir entre au registre des risques. »
 *
 * Ce dialogue est le seul chemin de cette phrase. Ce qui est vérifié ici, c'est
 * d'abord ce qu'il ne demande pas : ni l'intitulé, ni la cause, ni l'effet —
 * ils sont déjà écrits sur l'exigence, et les ressaisir produirait deux énoncés
 * du même risque. Et ce qu'il refuse de laisser vide : le processus et la
 * personne qui en répond, sans lesquels le registre n'accepte aucune ligne.
 */
describe('StakeholderRiskDialogComponent', () => {
  let fixture: ComponentFixture<StakeholderRiskDialogComponent>;
  let component: StakeholderRiskDialogComponent;
  let registre: jasmine.SpyObj<RiskRegisterService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<StakeholderRiskDialogComponent>>;

  const exigence: StakeholderRiskDialogData = {
    requirement: 'Tracer les lots jusqu\'au client final',
    risk: 'Rappel impossible en cas de contamination',
    riskLevel: 'MEDIUM'
  };

  async function monter(data: StakeholderRiskDialogData = exigence,
                        processus = of({ processes: ['Production', 'Logistique'], sites: [], owners: [] })
                        ): Promise<void> {
    registre = jasmine.createSpyObj<RiskRegisterService>('RiskRegisterService', ['suggestions']);
    registre.suggestions.and.returnValue(processus as never);
    dialogRef = jasmine.createSpyObj<MatDialogRef<StakeholderRiskDialogComponent>>('MatDialogRef', ['close']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderRiskDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, MatAutocompleteModule, NoopAnimationsModule],
      providers: [
        provideDirectoryStub(),
        { provide: RiskRegisterService, useValue: registre },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StakeholderRiskDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => monter());

  it('ne demande ni l\'intitulé, ni la cause, ni l\'effet du risque', () => {
    // Les trois viennent de l'exigence : un champ de plus ici, et deux énoncés
    // du même risque divergeraient à la première révision.
    expect(Object.keys(component.form.controls).sort())
      .toEqual(['ownerId', 'probability', 'process', 'severity', 'type']);
  });

  it('rappelle en lecture ce que le registre reprendra de l\'exigence', () => {
    const repris = fixture.nativeElement.querySelector('[data-test="repris"]').textContent;
    expect(repris).toContain(exigence.risk);
    expect(repris).toContain(exigence.requirement);
  });

  it('préremplit la gravité par l\'impact estimé de l\'exigence', () => {
    expect(component.form.getRawValue().severity).toBe(3);
  });

  it('laisse la gravité au cran médian quand l\'exigence n\'a pas été cotée', async () => {
    await monter({ ...exigence, riskLevel: null });

    expect(component.form.getRawValue().severity).toBe(3);
  });

  it('prend le cran correspondant pour un impact faible ou fort', async () => {
    for (const [niveau, note] of [['LOW', 2], ['HIGH', 4]] as [ImpactLevel, number][]) {
      await monter({ ...exigence, riskLevel: niveau });
      expect(component.form.getRawValue().severity).toBe(note);
    }
  });

  it('propose les processus déjà employés dans le registre', () => {
    expect(component.processes).toEqual(['Production', 'Logistique']);
  });

  it('laisse le champ processus libre quand le registre ne répond pas', async () => {
    // La liste est un confort. Si le registre refuse la requête — droits,
    // réseau —, l'inscription doit rester possible.
    await monter(exigence, throwError(() => new Error('403')) as never);

    expect(component.processes).toEqual([]);
    expect(component.form.controls.process.enabled).toBeTrue();
  });

  it('n\'inscrit rien sans processus', () => {
    component.form.patchValue({ process: '   ', ownerId: STUB_PERSON.accountId });
    component.ownerPicked(STUB_PERSON);

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('n\'inscrit rien sans la personne qui répond du risque', () => {
    // Le registre affiche un propriétaire sur chaque ligne : une ligne dont
    // personne ne répond ne se revoit jamais.
    component.form.patchValue({ process: 'Production' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('rend la nature, le processus, les deux notes et le propriétaire choisi', () => {
    component.form.patchValue({
      type: 'HEALTH_SAFETY', process: '  Production  ', severity: 4, probability: 2,
      ownerId: STUB_PERSON.accountId
    });
    component.ownerPicked(STUB_PERSON);

    component.submit();

    expect(dialogRef.close).toHaveBeenCalledWith({
      type: 'HEALTH_SAFETY', process: 'Production', severity: 4, probability: 2,
      ownerId: STUB_PERSON.accountId, ownerName: STUB_PERSON.displayName
    });
  });

  it('montre la criticité brute telle que le registre la calculera', () => {
    component.form.patchValue({ severity: 4, probability: 3 });
    fixture.detectChanges();

    expect(component.score).toBe(12);
    expect(fixture.nativeElement.querySelector('[data-test="score"]').textContent).toContain('12');
  });

  it('ferme sans rien rendre quand on renonce', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
