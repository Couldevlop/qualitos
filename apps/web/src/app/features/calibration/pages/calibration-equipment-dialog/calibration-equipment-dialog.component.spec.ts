import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CalibrationService } from '../../calibration.service';
import { EquipmentResponse } from '../../calibration.types';
import {
  CalibrationEquipmentDialogComponent, CalibrationEquipmentDialogData
} from './calibration-equipment-dialog.component';

describe('CalibrationEquipmentDialogComponent', () => {
  let fixture: ComponentFixture<CalibrationEquipmentDialogComponent>;
  let component: CalibrationEquipmentDialogComponent;
  let svc: jasmine.SpyObj<CalibrationService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CalibrationEquipmentDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let userId: string | null;

  const existing: EquipmentResponse = {
    id: 'e1', tenantId: 't1', code: 'CAL-001', name: 'Pied à coulisse',
    manufacturer: 'Mitutoyo', model: 'CD-15', serialNumber: 'SN-42', location: 'Atelier A',
    status: 'ACTIVE', critical: true, iotDeviceId: null, ownerUserId: null,
    createdBy: 'u1', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  };

  async function setup(data: CalibrationEquipmentDialogData): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<CalibrationService>('CalibrationService',
      ['createEquipment', 'updateEquipment']);
    svc.createEquipment.and.returnValue(of(existing));
    svc.updateEquipment.and.returnValue(of(existing));
    dialogRef = jasmine.createSpyObj<MatDialogRef<CalibrationEquipmentDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CalibrationEquipmentDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CalibrationService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => (userId ? { userId } : null) } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CalibrationEquipmentDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => { userId = 'u1'; });

  it('crée un équipement avec l’utilisateur courant comme auteur', async () => {
    await setup({ equipment: null });
    component.form.patchValue({
      code: 'CAL-002', name: ' Micromètre ', manufacturer: ' Mitutoyo ',
      model: '', serialNumber: '', location: ' Labo ', critical: true
    });
    component.submit();

    expect(svc.createEquipment).toHaveBeenCalledWith({
      code: 'CAL-002', name: 'Micromètre', manufacturer: 'Mitutoyo',
      model: undefined, serialNumber: undefined, location: 'Labo',
      critical: true, createdBy: 'u1'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('refuse la création sans session valide', async () => {
    userId = null;
    await setup({ equipment: null });
    component.form.patchValue({ code: 'CAL-002', name: 'Micromètre' });
    component.submit();
    expect(svc.createEquipment).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('rejette un formulaire incomplet sans appeler le serveur', async () => {
    await setup({ equipment: null });
    component.form.patchValue({ code: 'X', name: '' });
    component.submit();
    expect(svc.createEquipment).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBeTrue();
  });

  it('verrouille le code en édition et envoie un PATCH partiel', async () => {
    await setup({ equipment: existing });
    expect(component.isEdit).toBeTrue();
    expect(component.form.controls.code.disabled).toBeTrue();
    expect(component.form.controls.name.value).toBe('Pied à coulisse');

    component.form.patchValue({ name: 'Pied à coulisse numérique', serialNumber: '' });
    component.submit();

    expect(svc.updateEquipment).toHaveBeenCalledWith('e1', {
      name: 'Pied à coulisse numérique', manufacturer: 'Mitutoyo', model: 'CD-15',
      serialNumber: undefined, location: 'Atelier A', critical: true
    });
    expect(svc.createEquipment).not.toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup({ equipment: null });
    svc.createEquipment.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ code: 'CAL-003', name: 'Cale étalon' });
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup({ equipment: null });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.createEquipment).not.toHaveBeenCalled();
  });
});
