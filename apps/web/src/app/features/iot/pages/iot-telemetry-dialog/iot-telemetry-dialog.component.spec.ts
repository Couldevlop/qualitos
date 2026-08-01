import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IotService } from '../../iot.service';
import { TelemetryResponse } from '../../iot.types';
import {
  IotTelemetryDialogComponent, IotTelemetryDialogData, toInstant
} from './iot-telemetry-dialog.component';

describe('IotTelemetryDialogComponent', () => {
  let fixture: ComponentFixture<IotTelemetryDialogComponent>;
  let component: IotTelemetryDialogComponent;
  let svc: jasmine.SpyObj<IotService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IotTelemetryDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const created: TelemetryResponse = {
    id: 'e1', tenantId: 't1', deviceId: 'd1', metric: 'temperature', valueNumeric: 4.2,
    valueText: null, unit: '°C', source: 'MANUAL',
    recordedAt: '2026-07-31T10:00:00.000Z', ingestedAt: '2026-07-31T10:00:01.000Z'
  };

  const data: IotTelemetryDialogData = {
    deviceId: 'd1', deviceName: 'Réfrigérateur pharmacie',
    knownMetrics: ['humidity', 'temperature']
  };

  async function setup(): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<IotService>('IotService', ['ingestTelemetry']);
    svc.ingestTelemetry.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<IotTelemetryDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [IotTelemetryDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IotService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IotTelemetryDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => { await setup(); });

  it('bloque la soumission tant qu’aucune valeur n’est saisie', () => {
    component.form.patchValue({ metric: 'temperature' });
    expect(component.form.hasError('valueRequired')).toBeTrue();
    component.submit();
    expect(svc.ingestTelemetry).not.toHaveBeenCalled();
  });

  it('accepte une mesure purement textuelle', () => {
    component.form.patchValue({ metric: 'porte', valueText: 'OUVERTE' });
    expect(component.form.hasError('valueRequired')).toBeFalse();
    component.submit();
    expect(svc.ingestTelemetry).toHaveBeenCalledWith('d1', {
      metric: 'porte', valueNumeric: undefined, valueText: 'OUVERTE',
      unit: undefined, recordedAt: undefined, source: 'MANUAL'
    });
  });

  it('accepte la valeur zéro comme une mesure valide', () => {
    component.form.patchValue({ metric: 'temperature', valueNumeric: 0 });
    expect(component.form.hasError('valueRequired')).toBeFalse();
  });

  it('envoie la mesure numérique avec son unité et sa source', () => {
    component.form.patchValue({
      metric: ' temperature ', valueNumeric: 9.4, unit: ' °C ', source: 'MQTT'
    });
    component.submit();
    expect(svc.ingestTelemetry).toHaveBeenCalledWith('d1', {
      metric: 'temperature', valueNumeric: 9.4, valueText: undefined,
      unit: '°C', recordedAt: undefined, source: 'MQTT'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('convertit un horodatage local en instant UTC', () => {
    const local = '2026-07-31T10:30';
    component.form.patchValue({ metric: 'temperature', valueNumeric: 4, recordedAt: local });
    component.submit();
    const sent = svc.ingestTelemetry.calls.mostRecent().args[1];
    expect(sent.recordedAt).toBe(new Date(local).toISOString());
  });

  it('laisse le serveur horodater quand le champ est vide', () => {
    expect(toInstant('')).toBeUndefined();
    expect(toInstant('pas une date')).toBeUndefined();
  });

  it('recopie une métrique déjà relevée', () => {
    component.useMetric('humidity');
    expect(component.form.controls.metric.value).toBe('humidity');
    expect(component.form.controls.metric.dirty).toBeTrue();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', () => {
    svc.ingestTelemetry.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ metric: 'temperature', valueNumeric: 4 });
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.ingestTelemetry).not.toHaveBeenCalled();
  });

  it('rappelle les métriques déjà relevées sur l’équipement', () => {
    const chips = (fixture.nativeElement as HTMLElement).querySelectorAll('.known__chip');
    expect(chips.length).toBe(2);
    expect(chips[0].textContent).toContain('humidity');
  });
});
