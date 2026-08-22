import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import { ProductOperationResponse } from '../../products.types';
import { OperationDialogComponent, OperationDialogData } from './operation-dialog.component';

/**
 * La saisie d'une opération de gamme.
 *
 * <p>Le code d'opération est le mot commun entre le PFMEA et le control plan :
 * il est donc obligatoire, et unique sur un produit. Le refus de doublon doit se
 * lire comme tel — « ce code existe déjà » — et non comme un échec
 * d'enregistrement quelconque, sinon l'utilisateur ressaisit la même chose.
 */
describe('OperationDialogComponent', () => {

  const operation = (over: Partial<ProductOperationResponse> = {}): ProductOperationResponse => ({
    id: 'op-1', sequenceNo: 20, code: 'OP20', label: 'Perçage',
    workstation: 'Poste 4', ...over
  });

  let fixture: ComponentFixture<OperationDialogComponent>;
  let component: OperationDialogComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<OperationDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function setup(data: Partial<OperationDialogData> = {}): Promise<void> {
    service = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['addOperation', 'updateOperation']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<OperationDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [OperationDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { productId: 'p-1', ...data } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OperationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('exige un code et un libellé', async () => {
    await setup();

    expect(component.editing).toBeFalse();
    expect(component.form.invalid).toBeTrue();

    component.form.patchValue({ code: 'OP10', label: 'Ébavurage' });
    expect(component.form.valid).toBeTrue();
  });

  it('reprend l’opération existante en modification', async () => {
    await setup({ operation: operation() });

    expect(component.editing).toBeTrue();
    expect(component.form.get('code')!.value).toBe('OP20');
    expect(component.form.get('workstation')!.value).toBe('Poste 4');
  });

  it('crée l’opération en convertissant le rang', async () => {
    await setup();
    service.addOperation.and.returnValue(of(operation()));

    component.form.patchValue({ sequenceNo: '30', code: 'OP30', label: 'Contrôle final' });
    component.save();

    const args = service.addOperation.calls.mostRecent().args as unknown as
      [string, Record<string, unknown>];
    expect(args[0]).toBe('p-1');
    expect(args[1]['sequenceNo']).toBe(30);
    expect(args[1]['code']).toBe('OP30');
    expect(args[1]['workstation']).toBeUndefined();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('met à jour l’opération existante plutôt que d’en ajouter une', async () => {
    await setup({ operation: operation() });
    service.updateOperation.and.returnValue(of(operation({ label: 'Perçage Ø10' })));

    component.form.patchValue({ label: 'Perçage Ø10' });
    component.save();

    expect(service.updateOperation).toHaveBeenCalledWith('p-1', 'op-1', jasmine.any(Object));
    expect(service.addOperation).not.toHaveBeenCalled();
  });

  it('ne fait rien tant que le formulaire est invalide', async () => {
    await setup();

    component.save();

    expect(service.addOperation).not.toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  it('n’enregistre pas deux fois', async () => {
    await setup();
    service.addOperation.and.returnValue(of(operation()));
    component.form.patchValue({ code: 'OP30', label: 'Contrôle' });

    component.save();
    component.save();

    expect(service.addOperation).toHaveBeenCalledTimes(1);
  });

  it('nomme le doublon de code plutôt que de parler d’échec', async () => {
    await setup();
    service.addOperation.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ code: 'OP20', label: 'Perçage' });

    component.save();

    expect(component.saving).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('existe déjà');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('signale les autres échecs sans fermer', async () => {
    await setup();
    service.addOperation.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ code: 'OP30', label: 'Contrôle' });

    component.save();

    expect(snack.open.calls.mostRecent().args[0]).toContain('Enregistrement impossible');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ferme sans rien rendre à l’annulation', async () => {
    await setup();

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
