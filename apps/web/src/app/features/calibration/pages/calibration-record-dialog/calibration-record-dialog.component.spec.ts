import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CalibrationService, toLocalDate } from '../../calibration.service';
import { RecordResponse } from '../../calibration.types';
import {
  CalibrationRecordDialogComponent, CalibrationRecordDialogData
} from './calibration-record-dialog.component';

describe('CalibrationRecordDialogComponent', () => {
  let fixture: ComponentFixture<CalibrationRecordDialogComponent>;
  let component: CalibrationRecordDialogComponent;
  let svc: jasmine.SpyObj<CalibrationService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CalibrationRecordDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const created: RecordResponse = {
    id: 'r1', tenantId: 't1', equipmentId: 'e1', performedOn: '2026-06-01',
    performedByUserId: 'u1', performedByOrg: null, result: 'PASS',
    measurements: null, certificateReference: null, nextDueOverride: null,
    createdAt: '2026-06-01T00:00:00Z'
  };

  async function setup(data: CalibrationRecordDialogData): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<CalibrationService>('CalibrationService', ['addRecord']);
    svc.addRecord.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<CalibrationRecordDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CalibrationRecordDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CalibrationService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1' }) } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CalibrationRecordDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('pré-remplit la date du jour et le verdict conforme', async () => {
    await setup({ equipmentId: 'e1', critical: false });
    expect(component.form.controls.performedOn.value).toBe(toLocalDate(new Date()));
    expect(component.form.controls.result.value).toBe('PASS');
  });

  it('trace l’opérateur quand il déclare avoir réalisé la mesure', async () => {
    await setup({ equipmentId: 'e1', critical: false });
    component.form.patchValue({
      performedOn: '2026-06-01', result: 'CONDITIONAL',
      measurements: ' 0,01 mm ', certificateReference: ' CERT-1 '
    });
    component.submit();

    expect(svc.addRecord).toHaveBeenCalledWith('e1', {
      performedOn: '2026-06-01', result: 'CONDITIONAL', performedByUserId: 'u1',
      performedByOrg: undefined, certificateReference: 'CERT-1',
      measurements: '0,01 mm', nextDueOverride: undefined
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('n’attribue pas la mesure à l’utilisateur quand elle est sous-traitée', async () => {
    await setup({ equipmentId: 'e1', critical: false });
    component.form.patchValue({
      performedOn: '2026-06-01', performedByMe: false,
      performedByOrg: 'Labo COFRAC', nextDueOverride: '2027-06-01'
    });
    component.submit();

    expect(svc.addRecord).toHaveBeenCalledWith('e1', jasmine.objectContaining({
      performedByUserId: undefined,
      performedByOrg: 'Labo COFRAC',
      nextDueOverride: '2027-06-01'
    }));
  });

  it('avertit qu’un résultat non conforme déclasse un instrument critique', async () => {
    await setup({ equipmentId: 'e1', critical: true });
    expect(component.failWarning).toBeFalse();
    component.form.patchValue({ result: 'FAIL' });
    fixture.detectChanges();
    expect(component.failWarning).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.warn')).toBeTruthy();
  });

  it('n’avertit pas pour un instrument non critique', async () => {
    await setup({ equipmentId: 'e1', critical: false });
    component.form.patchValue({ result: 'FAIL' });
    fixture.detectChanges();
    expect(component.failWarning).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).querySelector('.warn')).toBeNull();
  });

  it('rejette une saisie sans date', async () => {
    await setup({ equipmentId: 'e1', critical: false });
    component.form.patchValue({ performedOn: '' });
    component.submit();
    expect(svc.addRecord).not.toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup({ equipmentId: 'e1', critical: false });
    svc.addRecord.and.returnValue(throwError(() => ({ status: 409 })));
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup({ equipmentId: 'e1', critical: false });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.addRecord).not.toHaveBeenCalled();
  });
});
