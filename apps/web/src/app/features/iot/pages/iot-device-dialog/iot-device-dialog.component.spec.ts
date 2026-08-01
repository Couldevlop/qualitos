import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IotService } from '../../iot.service';
import { DeviceResponse } from '../../iot.types';
import { IotDeviceDialogComponent, IotDeviceDialogData } from './iot-device-dialog.component';

describe('IotDeviceDialogComponent', () => {
  let fixture: ComponentFixture<IotDeviceDialogComponent>;
  let component: IotDeviceDialogComponent;
  let svc: jasmine.SpyObj<IotService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IotDeviceDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let userId: string | null;

  const existing: DeviceResponse = {
    id: 'd1', tenantId: 't1', code: 'FRIGO-01', name: 'Réfrigérateur pharmacie',
    deviceType: 'SENSOR_TEMPERATURE', protocol: 'MQTT', status: 'ACTIVE',
    location: 'Pharmacie', description: 'Chaîne du froid', metadataJson: null,
    lastSeenAt: null, telemetryCount: 0, createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  };

  async function setup(data: IotDeviceDialogData): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<IotService>('IotService', ['createDevice', 'updateDevice']);
    svc.createDevice.and.returnValue(of(existing));
    svc.updateDevice.and.returnValue(of(existing));
    dialogRef = jasmine.createSpyObj<MatDialogRef<IotDeviceDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [IotDeviceDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IotService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => (userId ? { userId } : null) } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IotDeviceDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => { userId = 'u1'; });

  it('enregistre un équipement avec l’utilisateur courant comme auteur', async () => {
    await setup({ device: null });
    component.form.patchValue({
      code: 'FRIGO-02', name: ' Congélateur ', deviceType: 'SENSOR_TEMPERATURE',
      protocol: 'LORAWAN', location: ' Labo ', description: ''
    });
    component.submit();

    expect(svc.createDevice).toHaveBeenCalledWith({
      code: 'FRIGO-02', name: 'Congélateur', deviceType: 'SENSOR_TEMPERATURE',
      protocol: 'LORAWAN', location: 'Labo', description: undefined, createdBy: 'u1'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('refuse la création sans session valide', async () => {
    userId = null;
    await setup({ device: null });
    component.form.patchValue({ code: 'FRIGO-02', name: 'Congélateur' });
    component.submit();
    expect(svc.createDevice).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('rejette un code que le serveur refuserait, sans appel réseau', async () => {
    await setup({ device: null });
    component.form.patchValue({ code: '-mauvais code', name: 'Sonde' });
    component.submit();
    expect(svc.createDevice).not.toHaveBeenCalled();
    expect(component.form.controls.code.hasError('pattern')).toBeTrue();
  });

  it('rejette un formulaire sans désignation', async () => {
    await setup({ device: null });
    component.form.patchValue({ code: 'FRIGO-03', name: '' });
    component.submit();
    expect(svc.createDevice).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBeTrue();
  });

  it('verrouille le code en édition et envoie un PATCH partiel', async () => {
    await setup({ device: existing });
    expect(component.isEdit).toBeTrue();
    expect(component.form.controls.code.disabled).toBeTrue();
    expect(component.form.controls.name.value).toBe('Réfrigérateur pharmacie');

    component.form.patchValue({ name: 'Réfrigérateur vaccins', description: '' });
    component.submit();

    expect(svc.updateDevice).toHaveBeenCalledWith('d1', {
      name: 'Réfrigérateur vaccins', deviceType: 'SENSOR_TEMPERATURE',
      protocol: 'MQTT', location: 'Pharmacie', description: undefined
    });
    expect(svc.createDevice).not.toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup({ device: null });
    svc.createDevice.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ code: 'FRIGO-04', name: 'Sonde' });
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup({ device: null });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.createDevice).not.toHaveBeenCalled();
  });

  it('propose tous les types et protocoles supportés par le serveur', async () => {
    await setup({ device: null });
    expect(component.deviceTypes.length).toBe(12);
    expect(component.protocols.length).toBe(9);
    expect(component.typeLabel('BIOMED')).toBe('Dispositif biomédical');
    expect(component.protocolLabel('SPARKPLUG_B')).toBe('Sparkplug B');
  });
});
