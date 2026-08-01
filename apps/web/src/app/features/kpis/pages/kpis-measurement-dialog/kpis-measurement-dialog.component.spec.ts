import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { KpisService } from '../../kpis.service';
import { MeasurementResponse } from '../../kpis.types';
import {
  KpisMeasurementDialogComponent, KpisMeasurementDialogData
} from './kpis-measurement-dialog.component';

describe('KpisMeasurementDialogComponent', () => {
  let fixture: ComponentFixture<KpisMeasurementDialogComponent>;
  let component: KpisMeasurementDialogComponent;
  let svc: jasmine.SpyObj<KpisService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<KpisMeasurementDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;

  const recorded: MeasurementResponse = {
    id: 'mes-1', tenantId: 't1', kpiId: 'kpi-1',
    periodStart: '2026-07-01T00:00:00.000Z', periodEnd: '2026-07-31T00:00:00.000Z',
    value: 96.2, unit: '%', source: 'MANUAL', health: 'OK',
    createdAt: '2026-08-01T00:00:00Z'
  };

  async function setup(data: KpisMeasurementDialogData): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<KpisService>('KpisService', ['record']);
    svc.record.and.returnValue(of(recorded));
    dialogRef = jasmine.createSpyObj<MatDialogRef<KpisMeasurementDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [KpisMeasurementDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: KpisService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(KpisMeasurementDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['quality_manager'] };
  });

  it('pré-remplit l’unité du KPI mesuré et la source MANUAL', async () => {
    await setup({ kpiId: 'kpi-1', defaultUnit: '%' });
    const v = component.form.getRawValue();
    expect(v.unit).toBe('%');
    expect(v.source).toBe('MANUAL');
    expect(component.sources).toEqual(['MANUAL', 'COMPUTED', 'IMPORT', 'IOT_AGGREGATED']);
  });

  it('laisse l’unité vide quand le KPI n’en définit aucune', async () => {
    await setup({ kpiId: 'kpi-1' });
    expect(component.form.getRawValue().unit).toBe('');
  });

  it('refuse l’envoi tant que période et valeur ne sont pas saisies', async () => {
    await setup({ kpiId: 'kpi-1', defaultUnit: '%' });
    component.submit();
    expect(svc.record).not.toHaveBeenCalled();
    expect(component.form.controls.value.touched).toBeTrue();
  });

  it('refuse une période dont la fin précède le début', async () => {
    await setup({ kpiId: 'kpi-1', defaultUnit: '%' });
    component.form.patchValue({
      periodStart: '2026-07-31T00:00', periodEnd: '2026-07-01T00:00', value: 96.2
    });
    component.submit();
    expect(svc.record).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('accepte une période instantanée (début = fin)', async () => {
    await setup({ kpiId: 'kpi-1', defaultUnit: '%' });
    component.form.patchValue({
      periodStart: '2026-07-01T00:00', periodEnd: '2026-07-01T00:00', value: 10
    });
    component.submit();
    expect(svc.record).toHaveBeenCalled();
  });

  it('accepte la valeur 0, qui est une mesure légitime', async () => {
    await setup({ kpiId: 'kpi-1', defaultUnit: '%' });
    component.form.patchValue({
      periodStart: '2026-07-01T00:00', periodEnd: '2026-07-31T00:00', value: 0
    });
    component.submit();
    expect(svc.record).toHaveBeenCalled();
    expect(svc.record.calls.mostRecent().args[1].value).toBe(0);
  });

  it('normalise la période en ISO, trace l’auteur du JWT et omet les champs vides', async () => {
    await setup({ kpiId: 'kpi-1', defaultUnit: '%' });
    component.form.patchValue({
      periodStart: '2026-07-01T00:00', periodEnd: '2026-07-31T00:00',
      value: 96.2, unit: '  %  ', source: 'IOT_AGGREGATED', notes: '   '
    });
    component.submit();

    const [kpiId, body] = svc.record.calls.mostRecent().args;
    expect(kpiId).toBe('kpi-1');
    expect(body.periodStart).toBe(new Date('2026-07-01T00:00').toISOString());
    expect(body.periodEnd).toBe(new Date('2026-07-31T00:00').toISOString());
    expect(body.value).toBe(96.2);
    expect(body.unit).toBe('%');
    expect(body.source).toBe('IOT_AGGREGATED');
    expect(body.recordedByUserId).toBe('u1');
    expect(body.notes).toBeUndefined();
    expect(dialogRef.close).toHaveBeenCalledWith(recorded);
  });

  it('enregistre sans auteur quand la session a expiré, le serveur restant maître', async () => {
    session = null;
    await setup({ kpiId: 'kpi-1' });
    component.form.patchValue({
      periodStart: '2026-07-01T00:00', periodEnd: '2026-07-31T00:00', value: 1
    });
    component.submit();
    expect(svc.record.calls.mostRecent().args[1].recordedByUserId).toBeUndefined();
  });

  it('garde le dialogue ouvert quand le serveur refuse la mesure', async () => {
    await setup({ kpiId: 'kpi-1', defaultUnit: '%' });
    svc.record.and.returnValue(throwError(() => ({ status: 422 })));
    component.form.patchValue({
      periodStart: '2026-07-01T00:00', periodEnd: '2026-07-31T00:00', value: 96.2
    });
    component.submit();
    expect(snack.open)
      .toHaveBeenCalledWith('Données refusées par le serveur.', 'OK', { duration: 4000 });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup({ kpiId: 'kpi-1' });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.record).not.toHaveBeenCalled();
  });
});
