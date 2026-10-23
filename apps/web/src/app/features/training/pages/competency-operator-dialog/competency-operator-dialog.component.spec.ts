import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { STUB_PERSON, provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { CompetencyMatrixService } from '../../competency-matrix/competency-matrix.service';
import { CompetencyMatrixView } from '../../competency-matrix/competency-matrix.types';
import {
  CompetencyOperatorDialogComponent, CompetencyOperatorDialogData
} from './competency-operator-dialog.component';

/** Inscrire un opérateur : une personne de l'annuaire, jamais un nom tapé, et son atelier. */
describe('CompetencyOperatorDialogComponent', () => {
  const VIEW: CompetencyMatrixView = { workshops: ['Usinage'], defaultWorkshops: true, operators: [], skills: [], cells: [] };

  let fixture: ComponentFixture<CompetencyOperatorDialogComponent>;
  let component: CompetencyOperatorDialogComponent;
  let service: jasmine.SpyObj<CompetencyMatrixService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CompetencyOperatorDialogComponent, CompetencyMatrixView>>;

  async function build(data: CompetencyOperatorDialogData): Promise<void> {
    service = jasmine.createSpyObj<CompetencyMatrixService>('CompetencyMatrixService', ['addOperator']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [CompetencyOperatorDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideDirectoryStub(),
        { provide: CompetencyMatrixService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CompetencyOperatorDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const choisir = (): void => {
    component.form.controls.userId.setValue(STUB_PERSON.accountId);
    component.onPerson(STUB_PERSON);
  };

  it('propose l\'atelier filtré, ou à défaut le premier atelier', async () => {
    await build({ workshops: ['Usinage', 'Assemblage'], workshop: 'Assemblage', excludeIds: [] });
    expect(component.form.controls.workshop.value).toBe('Assemblage');
    TestBed.resetTestingModule();
    await build({ workshops: ['Usinage', 'Assemblage'], workshop: 'Retiré', excludeIds: [] });
    expect(component.form.controls.workshop.value).toBe('Usinage');
    TestBed.resetTestingModule();
    await build({ workshops: [], excludeIds: [] });
    expect(component.form.controls.workshop.value).toBe('');
  });

  it('n\'envoie rien sans personne choisie dans l\'annuaire', async () => {
    await build({ workshops: ['Usinage'], excludeIds: [] });
    component.form.controls.userId.setValue(STUB_PERSON.accountId);
    component.submit();
    expect(service.addOperator).not.toHaveBeenCalled();
    expect(component.form.controls.workshop.touched).toBeTrue();
  });

  it('inscrit la personne par son compte et son nom, puis rend la matrice', async () => {
    await build({ workshops: ['Usinage'], excludeIds: ['u-déjà'] });
    service.addOperator.and.returnValue(of(VIEW));
    choisir();
    component.submit();
    expect(service.addOperator).toHaveBeenCalledWith({
      userId: STUB_PERSON.accountId, userName: STUB_PERSON.displayName, workshop: 'Usinage'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(VIEW);
    expect(component.submitting).toBeFalse();
  });

  it('dit en clair qu\'une personne figure déjà dans la matrice', async () => {
    await build({ workshops: ['Usinage'], excludeIds: [] });
    service.addOperator.and.returnValue(throwError(() => ({ status: 409 })));
    choisir();
    component.submit();
    fixture.detectChanges();
    expect(component.error).toBe('Cette personne figure déjà dans la matrice.');
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-test="operateur-erreur"]')?.textContent)
      .toContain('Cette personne figure déjà dans la matrice.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('signale tout autre refus', async () => {
    await build({ workshops: ['Usinage'], excludeIds: [] });
    service.addOperator.and.returnValue(throwError(() => ({ status: 404 })));
    choisir();
    component.submit();
    expect(component.error).toBe('L\'opérateur n\'a pas pu être ajouté.');
  });

  it('se ferme sans rien rendre à l\'annulation', async () => {
    await build({ workshops: ['Usinage'], excludeIds: [] });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
