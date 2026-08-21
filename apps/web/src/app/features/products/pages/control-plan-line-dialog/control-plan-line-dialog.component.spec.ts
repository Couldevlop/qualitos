import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FmeaService } from '../../../fmea/fmea.service';
import { FmeaItemResponse, FmeaProjectResponse } from '../../../fmea/fmea.types';
import { ProductsService } from '../../products.service';
import { ControlPlanLineView, ProductOperationResponse } from '../../products.types';
import {
  ControlPlanLineDialogComponent, ControlPlanLineDialogData
} from './control-plan-line-dialog.component';

/**
 * La saisie d'une ligne de control plan.
 *
 * <p>Deux exigences se croisent ici. Le lien vers une ligne de PFMEA est
 * proposé mais jamais imposé — un plan se remplit par passes, et bloquer la
 * saisie empêcherait de commencer. Et les deux listes d'aide (opérations de
 * gamme, lignes de PFMEA) ne sont que des commodités : leur indisponibilité ne
 * doit pas empêcher d'écrire la ligne, sans quoi une panne du module risques
 * emporterait la saisie du control plan avec elle.
 */
describe('ControlPlanLineDialogComponent', () => {

  const operation = (over: Partial<ProductOperationResponse> = {}): ProductOperationResponse => ({
    id: 'op-1', sequenceNo: 10, code: 'OP10', label: 'Perçage', ...over
  });

  const item = (over: Partial<FmeaItemResponse> = {}): FmeaItemResponse => ({
    id: 'it-1', tenantId: 't-1', projectId: 'f-1', sequenceNo: 1,
    failureMode: 'Diamètre hors tolérance',
    severity: 7, occurrence: 4, detection: 3, rpn: 84, critical: false,
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z', ...over
  });

  const project = (): FmeaProjectResponse => ({
    id: 'f-1', name: 'PFMEA support moteur', status: 'ACTIVE', type: 'PROCESS_FMEA'
  } as FmeaProjectResponse);

  const line = (over: Partial<ControlPlanLineView> = {}): ControlPlanLineView => ({
    id: 'l-1', sequenceNo: 20, characteristicLabel: 'Diamètre alésage',
    characteristicType: 'PRODUCT', ...over
  });

  let fixture: ComponentFixture<ControlPlanLineDialogComponent>;
  let component: ControlPlanLineDialogComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let fmea: jasmine.SpyObj<FmeaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ControlPlanLineDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function setup(data: Partial<ControlPlanLineDialogData> = {}): Promise<void> {
    service = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['operations', 'addLine', 'updateLine']);
    fmea = jasmine.createSpyObj<FmeaService>('FmeaService', ['list', 'listItems']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<ControlPlanLineDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    service.operations.and.returnValue(of([operation()]));
    fmea.list.and.returnValue(of({
      content: [project()], totalElements: 1, totalPages: 1, number: 0, size: 1
    }));
    fmea.listItems.and.returnValue(of({
      content: [item()], totalElements: 1, totalPages: 1, number: 0, size: 1
    }));

    await TestBed.configureTestingModule({
      declarations: [ControlPlanLineDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: FmeaService, useValue: fmea },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { productId: 'p-1', planId: 'cp-1', ...data } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ControlPlanLineDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('charge les opérations de gamme et les lignes du PFMEA en vigueur', async () => {
    await setup();

    expect(service.operations).toHaveBeenCalledWith('p-1');
    expect(fmea.list).toHaveBeenCalledWith(0, 50, 'ACTIVE', 'PROCESS_FMEA', 'p-1');
    expect(fmea.listItems).toHaveBeenCalledWith('f-1');
    expect(component.operations.length).toBe(1);
    expect(component.fmeaItems.length).toBe(1);
  });

  it('n’interroge aucune ligne quand le produit n’a pas de PFMEA en vigueur', async () => {
    await setup();
    fmea.list.and.returnValue(of({
      content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
    }));
    fmea.listItems.calls.reset();

    component.ngOnInit();

    expect(fmea.listItems).not.toHaveBeenCalled();
    expect(component.fmeaItems).toEqual([]);
  });

  it('laisse saisir la ligne même si les listes d’aide échouent', async () => {
    await setup();
    service.operations.and.returnValue(throwError(() => new Error('indisponible')));
    fmea.list.and.returnValue(throwError(() => new Error('indisponible')));

    component.ngOnInit();

    expect(component.operations).toEqual([]);
    expect(component.fmeaItems).toEqual([]);
    expect(component.form.get('characteristicLabel')!.enabled).toBeTrue();
  });

  it('exige la caractéristique et rien d’autre pour commencer', async () => {
    await setup();

    expect(component.form.invalid).toBeTrue();
    component.form.patchValue({ characteristicLabel: 'Diamètre alésage' });
    expect(component.form.valid).toBeTrue();
    expect(component.form.get('fmeaItemId')!.value).toBeNull();
  });

  it('reprend la ligne existante en modification', async () => {
    await setup({ line: line({ specialClass: 'SAFETY', fmeaItemId: 'it-1', sampleSize: 5 }) });

    expect(component.editing).toBeTrue();
    expect(component.form.get('sequenceNo')!.value).toBe(20);
    expect(component.form.get('specialClass')!.value).toBe('SAFETY');
    expect(component.form.get('fmeaItemId')!.value).toBe('it-1');
  });

  it('crée la ligne en convertissant les nombres et en omettant les vides', async () => {
    await setup();
    service.addLine.and.returnValue(of(line()));

    component.form.patchValue({
      characteristicLabel: 'Diamètre alésage', sequenceNo: '30',
      toleranceLower: '9.8', toleranceUpper: '10.2', sampleSize: ''
    });
    component.save();

    expect(service.addLine).toHaveBeenCalled();
    const args = service.addLine.calls.mostRecent().args as unknown as
      [string, string, Record<string, unknown>];
    expect(args[0]).toBe('p-1');
    expect(args[1]).toBe('cp-1');
    expect(args[2]['sequenceNo']).toBe(30);
    expect(args[2]['toleranceLower']).toBe(9.8);
    expect(args[2]['toleranceUpper']).toBe(10.2);
    expect(args[2]['sampleSize']).toBeUndefined();
    expect(args[2]['machine']).toBeUndefined();
    expect(args[2]['fmeaItemId']).toBeUndefined();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('met à jour la ligne existante plutôt que d’en créer une seconde', async () => {
    await setup({ line: line() });
    service.updateLine.and.returnValue(of(line({ characteristicLabel: 'Alésage 10' })));

    component.form.patchValue({ characteristicLabel: 'Alésage 10' });
    component.save();

    expect(service.updateLine).toHaveBeenCalledWith('p-1', 'cp-1', 'l-1', jasmine.any(Object));
    expect(service.addLine).not.toHaveBeenCalled();
  });

  it('ne fait rien tant que la caractéristique manque', async () => {
    await setup();

    component.save();

    expect(service.addLine).not.toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  it('n’enregistre pas deux fois', async () => {
    await setup();
    service.addLine.and.returnValue(of(line()));
    component.form.patchValue({ characteristicLabel: 'Diamètre' });

    component.save();
    component.save();

    expect(service.addLine).toHaveBeenCalledTimes(1);
  });

  it('explique qu’un plan approuvé demande une révision, plutôt que « échec »', async () => {
    await setup();
    service.addLine.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ characteristicLabel: 'Diamètre' });

    component.save();

    expect(component.saving).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('révision');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('signale les autres échecs sans fermer', async () => {
    await setup();
    service.addLine.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ characteristicLabel: 'Diamètre' });

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
