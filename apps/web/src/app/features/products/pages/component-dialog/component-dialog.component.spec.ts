import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import { ProductComponentResponse } from '../../products.types';
import { ComponentDialogComponent, ComponentDialogData } from './component-dialog.component';

/**
 * La saisie d'une ligne de nomenclature.
 *
 * <p>La quantité est facultative — toutes les nomenclatures ne se comptent pas
 * en pièces — mais une quantité SAISIE doit partir en nombre : la laisser filer
 * en chaîne ferait échouer la validation côté serveur sur un champ que
 * l'utilisateur a pourtant renseigné correctement.
 */
describe('ComponentDialogComponent', () => {

  const composant = (over: Partial<ProductComponentResponse> = {}): ProductComponentResponse => ({
    id: 'c-1', sequenceNo: 20, reference: 'VIS-M6', label: 'Vis M6x20',
    quantity: 4, unit: 'pc', ...over
  });

  let fixture: ComponentFixture<ComponentDialogComponent>;
  let component: ComponentDialogComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ComponentDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function setup(data: Partial<ComponentDialogData> = {}): Promise<void> {
    service = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['addComponent', 'updateComponent']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<ComponentDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ComponentDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { productId: 'p-1', ...data } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ComponentDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('propose un rang de dix pour une première ligne', async () => {
    await setup();

    expect(component.editing).toBeFalse();
    expect(component.form.get('sequenceNo')!.value).toBe(10);
    expect(component.form.invalid).toBeTrue();
  });

  it('reprend la ligne existante en modification', async () => {
    await setup({ component: composant() });

    expect(component.editing).toBeTrue();
    expect(component.form.get('reference')!.value).toBe('VIS-M6');
    expect(component.form.get('quantity')!.value).toBe(4);
  });

  it('crée la ligne en convertissant rang et quantité', async () => {
    await setup();
    service.addComponent.and.returnValue(of(composant()));

    component.form.patchValue({ sequenceNo: '30', reference: 'VIS-M6', quantity: '4' });
    component.save();

    const args = service.addComponent.calls.mostRecent().args as unknown as
      [string, Record<string, unknown>];
    expect(args[0]).toBe('p-1');
    expect(args[1]['sequenceNo']).toBe(30);
    expect(args[1]['quantity']).toBe(4);
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('laisse la quantité absente quand elle n’a pas été saisie', async () => {
    await setup();
    service.addComponent.and.returnValue(of(composant()));

    component.form.patchValue({ reference: 'VIS-M6', quantity: '' });
    component.save();

    const args = service.addComponent.calls.mostRecent().args as unknown as
      [string, Record<string, unknown>];
    expect(args[1]['quantity']).toBeUndefined();
    expect(args[1]['label']).toBeUndefined();
    expect(args[1]['unit']).toBeUndefined();
  });

  it('met à jour la ligne existante plutôt que d’en ajouter une', async () => {
    await setup({ component: composant() });
    service.updateComponent.and.returnValue(of(composant({ quantity: 6 })));

    component.form.patchValue({ quantity: 6 });
    component.save();

    expect(service.updateComponent).toHaveBeenCalledWith('p-1', 'c-1', jasmine.any(Object));
    expect(service.addComponent).not.toHaveBeenCalled();
  });

  it('ne fait rien tant que la référence manque', async () => {
    await setup();

    component.save();

    expect(service.addComponent).not.toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  it('n’enregistre pas deux fois', async () => {
    await setup();
    service.addComponent.and.returnValue(of(composant()));
    component.form.patchValue({ reference: 'VIS-M6' });

    component.save();
    component.save();

    expect(service.addComponent).toHaveBeenCalledTimes(1);
  });

  it('signale l’échec sans fermer le dialogue', async () => {
    await setup();
    service.addComponent.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ reference: 'VIS-M6' });

    component.save();

    expect(component.saving).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Enregistrement impossible');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ferme sans rien rendre à l’annulation', async () => {
    await setup();

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
