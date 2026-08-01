import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CalibrationService, addDays, toLocalDate } from '../../calibration.service';
import { PlanResponse } from '../../calibration.types';
import {
  CalibrationPlanDialogComponent, CalibrationPlanDialogData
} from './calibration-plan-dialog.component';

describe('CalibrationPlanDialogComponent', () => {
  let fixture: ComponentFixture<CalibrationPlanDialogComponent>;
  let component: CalibrationPlanDialogComponent;
  let svc: jasmine.SpyObj<CalibrationService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CalibrationPlanDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const existing: PlanResponse = {
    id: 'p1', tenantId: 't1', equipmentId: 'e1', frequencyMonths: 6,
    procedureReference: 'MO-CAL-014', tolerance: '± 0,02 mm', accreditationRef: 'COFRAC 2-1234',
    lastCalibratedOn: '2026-05-01', nextDueOn: '2026-11-01', overdue: false,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-05-01T00:00:00Z'
  };

  async function setup(data: CalibrationPlanDialogData): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<CalibrationService>('CalibrationService', ['upsertPlan']);
    svc.upsertPlan.and.returnValue(of(existing));
    dialogRef = jasmine.createSpyObj<MatDialogRef<CalibrationPlanDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CalibrationPlanDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CalibrationService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CalibrationPlanDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('propose une périodicité annuelle et une échéance à un an à la création', async () => {
    await setup({ equipmentId: 'e1', plan: null });
    expect(component.isEdit).toBeFalse();
    expect(component.form.controls.frequencyMonths.value).toBe(12);
    expect(component.form.controls.firstDueOn.value)
      .toBe(toLocalDate(addDays(new Date(), 365)));
  });

  it('pré-remplit le formulaire avec le plan existant', async () => {
    await setup({ equipmentId: 'e1', plan: existing });
    expect(component.isEdit).toBeTrue();
    const v = component.form.getRawValue();
    expect(v.frequencyMonths).toBe(6);
    expect(v.firstDueOn).toBe('2026-11-01');
    expect(v.tolerance).toBe('± 0,02 mm');
  });

  it('enregistre le plan et renvoie le résultat au parent', async () => {
    await setup({ equipmentId: 'e1', plan: null });
    component.form.patchValue({
      frequencyMonths: 3, firstDueOn: '2026-12-01',
      procedureReference: ' MO-CAL-014 ', tolerance: '', accreditationRef: ' COFRAC 2-1234 '
    });
    component.submit();

    expect(svc.upsertPlan).toHaveBeenCalledWith('e1', {
      frequencyMonths: 3, firstDueOn: '2026-12-01',
      procedureReference: 'MO-CAL-014', tolerance: undefined,
      accreditationRef: 'COFRAC 2-1234'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('refuse une périodicité hors bornes serveur (1 à 120 mois)', async () => {
    await setup({ equipmentId: 'e1', plan: null });
    component.form.patchValue({ frequencyMonths: 0 });
    component.submit();
    expect(svc.upsertPlan).not.toHaveBeenCalled();

    component.form.patchValue({ frequencyMonths: 121 });
    component.submit();
    expect(svc.upsertPlan).not.toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup({ equipmentId: 'e1', plan: null });
    svc.upsertPlan.and.returnValue(throwError(() => ({ status: 409 })));
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup({ equipmentId: 'e1', plan: null });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.upsertPlan).not.toHaveBeenCalled();
  });
});
