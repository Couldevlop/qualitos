import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IotService } from '../../iot.service';
import { ThresholdResponse } from '../../iot.types';
import {
  IotThresholdDialogComponent, IotThresholdDialogData
} from './iot-threshold-dialog.component';

describe('IotThresholdDialogComponent', () => {
  let fixture: ComponentFixture<IotThresholdDialogComponent>;
  let component: IotThresholdDialogComponent;
  let svc: jasmine.SpyObj<IotService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IotThresholdDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let userId: string | null;

  const existing: ThresholdResponse = {
    id: 't1', tenantId: 't1', deviceId: 'd1', metric: 'temperature',
    minValue: 2, maxValue: 8, capaCriticity: 'HIGH', capaOwnerId: 'owner-42',
    enabled: true, fmeaItemId: null, openPdcaCycle: false,
    createdAt: '2026-01-01T00:00:00Z'
  };

  function dialogData(threshold: ThresholdResponse | null): IotThresholdDialogData {
    return {
      deviceId: 'd1', deviceName: 'Réfrigérateur pharmacie',
      threshold, knownMetrics: ['humidity', 'temperature']
    };
  }

  async function setup(threshold: ThresholdResponse | null): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<IotService>('IotService', ['createThreshold', 'updateThreshold']);
    svc.createThreshold.and.returnValue(of(existing));
    svc.updateThreshold.and.returnValue(of(existing));
    dialogRef = jasmine.createSpyObj<MatDialogRef<IotThresholdDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [IotThresholdDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IotService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => (userId ? { userId } : null) } },
        { provide: MAT_DIALOG_DATA, useValue: dialogData(threshold) }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IotThresholdDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => { userId = 'u1'; });

  it('exige au moins une borne, comme le serveur', async () => {
    await setup(null);
    component.form.patchValue({ metric: 'temperature' });
    expect(component.form.hasError('boundsRequired')).toBeTrue();
    component.submit();
    expect(svc.createThreshold).not.toHaveBeenCalled();
  });

  it('refuse une borne basse supérieure à la borne haute', async () => {
    await setup(null);
    component.form.patchValue({ metric: 'temperature', minValue: 9, maxValue: 2 });
    expect(component.form.hasError('boundsInverted')).toBeTrue();
    component.submit();
    expect(svc.createThreshold).not.toHaveBeenCalled();
  });

  it('accepte une seule borne', async () => {
    await setup(null);
    component.form.patchValue({ metric: 'temperature', maxValue: 8 });
    expect(component.form.valid).toBeTrue();
  });

  it('rattache le seuil créé à l’équipement et à l’utilisateur courant', async () => {
    await setup(null);
    component.form.patchValue({
      metric: ' temperature ', minValue: 2, maxValue: 8,
      capaCriticity: 'CRITICAL', enabled: true, openPdcaCycle: true
    });
    component.submit();

    expect(svc.createThreshold).toHaveBeenCalledWith({
      deviceId: 'd1', metric: 'temperature', minValue: 2, maxValue: 8,
      capaCriticity: 'CRITICAL', capaOwnerId: 'u1', enabled: true,
      fmeaItemId: null, openPdcaCycle: true
    });
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('refuse la création sans session valide', async () => {
    userId = null;
    await setup(null);
    component.form.patchValue({ metric: 'temperature', maxValue: 8 });
    component.submit();
    expect(svc.createThreshold).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('valide le format de la fiche FMEA liée', async () => {
    await setup(null);
    component.form.patchValue({ metric: 'temperature', maxValue: 8, fmeaItemId: 'pas-un-uuid' });
    expect(component.form.controls.fmeaItemId.hasError('pattern')).toBeTrue();
    component.form.patchValue({ fmeaItemId: '2f1c1c8e-9f2a-4c3b-8a11-5c0d9a7b6e41' });
    expect(component.form.controls.fmeaItemId.valid).toBeTrue();
  });

  it('conserve le responsable CAPA d’origine en édition', async () => {
    await setup(existing);
    expect(component.isEdit).toBeTrue();
    expect(component.form.controls.metric.value).toBe('temperature');

    component.form.patchValue({ maxValue: 6 });
    component.submit();

    expect(svc.updateThreshold).toHaveBeenCalledWith('t1', {
      deviceId: 'd1', metric: 'temperature', minValue: 2, maxValue: 6,
      capaCriticity: 'HIGH', capaOwnerId: 'owner-42', enabled: true,
      fmeaItemId: null, openPdcaCycle: false
    });
    expect(svc.createThreshold).not.toHaveBeenCalled();
  });

  it('recopie une métrique déjà relevée', async () => {
    await setup(null);
    component.useMetric('humidity');
    expect(component.form.controls.metric.value).toBe('humidity');
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup(null);
    svc.createThreshold.and.returnValue(throwError(() => ({ status: 400 })));
    component.form.patchValue({ metric: 'temperature', maxValue: 8 });
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup(null);
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.createThreshold).not.toHaveBeenCalled();
  });

  it('libelle les quatre criticités CAPA', async () => {
    await setup(null);
    expect(component.criticities.length).toBe(4);
    expect(component.criticityLabel('LOW')).toBe('Faible');
    expect(component.criticityLabel('CRITICAL')).toBe('Critique');
  });
});
