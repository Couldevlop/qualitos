import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { STUB_PERSON, provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { CompetencyMatrixService } from '../../competency-matrix/competency-matrix.service';
import { CompetencyMatrixView } from '../../competency-matrix/competency-matrix.types';
import { CompetencySkillDialogComponent, CompetencySkillDialogData } from './competency-skill-dialog.component';

/** Ajouter une compétence : intitulé, catégorie, et un formateur de l'annuaire — obligatoire. */
describe('CompetencySkillDialogComponent', () => {
  const VIEW: CompetencyMatrixView = { workshops: ['Usinage'], defaultWorkshops: true, operators: [], skills: [], cells: [] };

  let fixture: ComponentFixture<CompetencySkillDialogComponent>;
  let component: CompetencySkillDialogComponent;
  let service: jasmine.SpyObj<CompetencyMatrixService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CompetencySkillDialogComponent, CompetencyMatrixView>>;

  async function build(data: CompetencySkillDialogData = { categories: ['Usinage', 'Qualité', 'Électricité'] }): Promise<void> {
    service = jasmine.createSpyObj<CompetencyMatrixService>('CompetencyMatrixService', ['addSkill']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [CompetencySkillDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, MatAutocompleteModule, NoopAnimationsModule],
      providers: [
        provideDirectoryStub(),
        { provide: CompetencyMatrixService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CompetencySkillDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const remplir = (name = '  Soudure orbitale ', category = ' Assemblage '): void => {
    component.form.patchValue({ name, category, trainerUserId: STUB_PERSON.accountId });
    component.onTrainer(STUB_PERSON);
  };

  it('reprend la catégorie filtrée, et propose les catégories qui correspondent à la saisie, sans accents', async () => {
    await build({ categories: ['Usinage', 'Qualité', 'Électricité'], category: 'Qualité' });
    expect(component.form.controls.category.value).toBe('Qualité');
    component.form.controls.category.setValue('elec');
    expect(component.categoryOptions).toEqual(['Électricité']);
    component.form.controls.category.setValue('');
    expect(component.categoryOptions.length).toBe(3);
  });

  it('refuse d\'ajouter sans formateur', async () => {
    await build();
    component.form.patchValue({ name: 'Soudure', category: 'Assemblage' });
    component.submit();
    expect(component.form.controls.trainerUserId.hasError('required')).toBeTrue();
    expect(service.addSkill).not.toHaveBeenCalled();

    component.form.controls.trainerUserId.setValue(STUB_PERSON.accountId);
    component.submit();
    expect(service.addSkill).not.toHaveBeenCalled();
  });

  it('refuse un intitulé fait d\'espaces', async () => {
    await build();
    remplir('   ', 'Assemblage');
    component.submit();
    expect(component.form.controls.name.value).toBe('');
    expect(service.addSkill).not.toHaveBeenCalled();
  });

  it('ajoute la compétence, nettoyée, avec le nom du formateur, puis rend la matrice', async () => {
    await build();
    service.addSkill.and.returnValue(of(VIEW));
    remplir();
    component.submit();
    expect(service.addSkill).toHaveBeenCalledWith({
      name: 'Soudure orbitale', category: 'Assemblage',
      trainerUserId: STUB_PERSON.accountId, trainerName: STUB_PERSON.displayName
    });
    expect(dialogRef.close).toHaveBeenCalledWith(VIEW);
    expect(component.submitting).toBeFalse();
  });

  it('dit en clair que la compétence figure déjà dans la matrice', async () => {
    await build();
    service.addSkill.and.returnValue(throwError(() => ({ status: 409 })));
    remplir();
    component.submit();
    fixture.detectChanges();
    expect(component.error).toBe('Cette compétence figure déjà dans la matrice.');
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-test="competence-erreur"]')).not.toBeNull();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('signale tout autre refus', async () => {
    await build();
    service.addSkill.and.returnValue(throwError(() => ({ status: 404 })));
    remplir();
    component.submit();
    expect(component.error).toBe('La compétence n\'a pas pu être ajoutée.');
  });

  it('se ferme sans rien rendre à l\'annulation', async () => {
    await build();
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
