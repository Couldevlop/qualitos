import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CalibrationService, toLocalDate } from '../../calibration.service';
import { MsaResponse } from '../../calibration.types';
import { CalibrationMsaDialogComponent } from './calibration-msa-dialog.component';

describe('CalibrationMsaDialogComponent', () => {
  let fixture: ComponentFixture<CalibrationMsaDialogComponent>;
  let component: CalibrationMsaDialogComponent;
  let svc: jasmine.SpyObj<CalibrationService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CalibrationMsaDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let userId: string | null;

  const created: MsaResponse = {
    id: 'm1', tenantId: 't1', equipmentId: 'e1', type: 'GAGE_R_R',
    performedOn: '2026-06-01', studyValue: 12.5, passingThreshold: 30,
    result: 'PASS', notes: null, createdBy: 'u1', createdAt: '2026-06-01T00:00:00Z'
  };

  async function setup(): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<CalibrationService>('CalibrationService', ['addMsa']);
    svc.addMsa.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<CalibrationMsaDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CalibrationMsaDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CalibrationService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => (userId ? { userId } : null) } },
        { provide: MAT_DIALOG_DATA, useValue: { equipmentId: 'e1' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CalibrationMsaDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => { userId = 'u1'; });

  it('propose les quatre types MSA et les trois verdicts', async () => {
    await setup();
    expect(component.types.map(t => t.value))
      .toEqual(['GAGE_R_R', 'BIAS', 'LINEARITY', 'STABILITY']);
    expect(component.results.map(r => r.value)).toEqual(['PASS', 'MARGINAL', 'FAIL']);
    expect(component.form.controls.performedOn.value).toBe(toLocalDate(new Date()));
  });

  it('enregistre l’étude avec l’auteur courant', async () => {
    await setup();
    component.form.patchValue({
      type: 'BIAS', performedOn: '2026-06-01', studyValue: 0.4,
      passingThreshold: 1, result: 'MARGINAL', notes: ' à revoir '
    });
    component.submit();

    expect(svc.addMsa).toHaveBeenCalledWith('e1', {
      type: 'BIAS', performedOn: '2026-06-01', studyValue: 0.4,
      passingThreshold: 1, result: 'MARGINAL', notes: 'à revoir', createdBy: 'u1'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('omet le seuil quand il n’est pas renseigné', async () => {
    await setup();
    component.form.patchValue({ studyValue: 12.5 });
    component.submit();
    expect(svc.addMsa).toHaveBeenCalledWith('e1', jasmine.objectContaining({
      passingThreshold: undefined, notes: undefined
    }));
  });

  it('exige une valeur d’étude', async () => {
    await setup();
    component.submit();
    expect(svc.addMsa).not.toHaveBeenCalled();
    expect(component.form.controls.studyValue.touched).toBeTrue();
  });

  it('refuse l’enregistrement sans session valide', async () => {
    userId = null;
    await setup();
    component.form.patchValue({ studyValue: 12.5 });
    component.submit();
    expect(svc.addMsa).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup();
    svc.addMsa.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ studyValue: 12.5 });
    component.submit();
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup();
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.addMsa).not.toHaveBeenCalled();
  });
});
