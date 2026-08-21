import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import { ProductResponse } from '../../products.types';
import { ProductFormDialogComponent } from './product-form-dialog.component';

/**
 * Le formulaire produit.
 *
 * <p>Le point qui compte : la référence est verrouillée en modification. Elle
 * sert de clé humaine et se retrouve citée par le PFMEA, le control plan et les
 * non-conformités — la renommer rendrait tout cela illisible sans rien casser
 * de visible, ce qui est la pire forme de casse.
 */
describe('ProductFormDialogComponent', () => {

  const product = (over: Partial<ProductResponse> = {}): ProductResponse => ({
    id: 'p-1', code: 'REF-4471', designation: 'Support moteur', status: 'ACTIVE',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z', ...over
  });

  let fixture: ComponentFixture<ProductFormDialogComponent>;
  let component: ProductFormDialogComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ProductFormDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function setup(data: { product?: ProductResponse } | null): Promise<void> {
    service = jasmine.createSpyObj<ProductsService>('ProductsService', ['create', 'update']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<ProductFormDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ProductFormDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductFormDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('ouvre un formulaire vide quand aucun produit n\'est fourni', async () => {
    await setup(null);

    expect(component.editing).toBeFalse();
    expect(component.form.get('code')!.enabled).toBeTrue();
    expect(component.form.get('designation')!.value).toBe('');
    expect(component.form.invalid).toBeTrue();
  });

  it('verrouille la référence en modification', async () => {
    await setup({ product: product() });

    expect(component.editing).toBeTrue();
    expect(component.form.get('code')!.disabled).toBeTrue();
    expect(component.form.get('code')!.value).toBe('REF-4471');
    expect(component.form.get('designation')!.value).toBe('Support moteur');
  });

  it('refuse une référence qui ne respecte pas le format', async () => {
    await setup(null);

    component.form.patchValue({ code: 'réf 4471', designation: 'Support' });
    expect(component.form.get('code')!.valid).toBeFalse();

    component.form.patchValue({ code: 'REF_4471-A.2' });
    expect(component.form.get('code')!.valid).toBeTrue();
  });

  it('crée le produit et rend la réponse au dialogue', async () => {
    await setup(null);
    const cree = product({ id: 'p-9' });
    service.create.and.returnValue(of(cree));

    component.form.patchValue({ code: 'REF-9', designation: 'Bielle', family: 'Fonderie' });
    component.save();

    expect(service.create).toHaveBeenCalledWith(jasmine.objectContaining({
      code: 'REF-9', designation: 'Bielle', family: 'Fonderie'
    }));
    expect(dialogRef.close).toHaveBeenCalledWith(cree);
  });

  it('omet les champs laissés vides plutôt que d\'envoyer des chaînes vides', async () => {
    await setup(null);
    service.create.and.returnValue(of(product()));

    component.form.patchValue({ code: 'REF-9', designation: 'Bielle' });
    component.save();

    const payload = service.create.calls.mostRecent().args[0] as unknown as Record<string, unknown>;
    expect(payload['family']).toBeUndefined();
    expect(payload['revisionIndex']).toBeUndefined();
    expect(payload['customerLabel']).toBeUndefined();
    expect(payload['siteLabel']).toBeUndefined();
  });

  it('met à jour sans jamais renvoyer la référence', async () => {
    await setup({ product: product() });
    service.update.and.returnValue(of(product({ designation: 'Support moteur v2' })));

    component.form.patchValue({ designation: 'Support moteur v2', siteLabel: 'Site A' });
    component.save();

    expect(service.update).toHaveBeenCalledWith('p-1', jasmine.objectContaining({
      designation: 'Support moteur v2', siteLabel: 'Site A'
    }));
    const payload = service.update.calls.mostRecent().args[1] as unknown as Record<string, unknown>;
    expect('code' in payload).toBeFalse();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('ne fait rien tant que le formulaire est invalide', async () => {
    await setup(null);

    component.save();

    expect(service.create).not.toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  it('n\'envoie pas deux fois la même création', async () => {
    await setup(null);
    service.create.and.returnValue(of(product()));
    component.form.patchValue({ code: 'REF-9', designation: 'Bielle' });

    component.save();
    component.save();

    expect(service.create).toHaveBeenCalledTimes(1);
  });

  it('nomme le conflit de référence plutôt que de parler d\'échec', async () => {
    await setup(null);
    service.create.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ code: 'REF-9', designation: 'Bielle' });

    component.save();

    expect(component.saving).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('référence');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('signale les autres échecs sans fermer le dialogue', async () => {
    await setup(null);
    service.create.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ code: 'REF-9', designation: 'Bielle' });

    component.save();

    expect(snack.open.calls.mostRecent().args[0]).toContain('Enregistrement impossible');
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  it('ferme sans rien rendre à l\'annulation', async () => {
    await setup(null);

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
