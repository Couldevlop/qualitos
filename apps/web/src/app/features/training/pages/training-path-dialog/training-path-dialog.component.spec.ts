import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { PathResponse } from '../../training.types';
import { TrainingPathDialogComponent, TrainingPathDialogData } from './training-path-dialog.component';

describe('TrainingPathDialogComponent', () => {
  let fixture: ComponentFixture<TrainingPathDialogComponent>;
  let component: TrainingPathDialogComponent;
  let svc: jasmine.SpyObj<TrainingService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<TrainingPathDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;

  const existing: PathResponse = {
    id: 'path-1', tenantId: 't1', code: 'yellow-belt', name: 'Yellow Belt',
    description: 'Bases qualité', targetRole: 'Opérateur', durationHours: 14,
    passingScore: 70, validityMonths: 36, status: 'ACTIVE', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  };

  async function setup(data: TrainingPathDialogData | null): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<TrainingService>('TrainingService', ['createPath', 'updatePath']);
    svc.createPath.and.returnValue(of(existing));
    svc.updatePath.and.returnValue(of(existing));
    dialogRef = jasmine.createSpyObj<MatDialogRef<TrainingPathDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [TrainingPathDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TrainingPathDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['quality_manager'] };
  });

  // ---- Création --------------------------------------------------------------

  it('propose un parcours vierge avec les valeurs métier par défaut', async () => {
    await setup(null);
    expect(component.isEdit).toBeFalse();
    const v = component.form.getRawValue();
    expect(v.durationHours).toBe(8);
    expect(v.passingScore).toBe(70);
    expect(component.form.controls.code.enabled).toBeTrue();
  });

  it('crée le parcours au nom de l’utilisateur du JWT, champs vides omis', async () => {
    await setup(null);
    component.form.patchValue({
      code: ' lean-basics ', name: '  Lean — bases  ', description: '   ',
      targetRole: ' Opérateur ', durationHours: 7, passingScore: 80, validityMonths: null
    });
    component.submit();

    expect(svc.createPath).toHaveBeenCalledWith({
      code: 'lean-basics', name: 'Lean — bases', description: undefined,
      targetRole: 'Opérateur', durationHours: 7, passingScore: 80,
      validityMonths: undefined, createdBy: 'u1'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('refuse un code qui ne respecte pas le motif serveur', async () => {
    await setup(null);
    component.form.patchValue({ code: 'Yellow Belt!', name: 'X' });
    component.submit();
    expect(svc.createPath).not.toHaveBeenCalled();
    expect(component.form.controls.code.touched).toBeTrue();
  });

  it('refuse une durée hors bornes serveur (1 à 10 000 h)', async () => {
    await setup(null);
    component.form.patchValue({ code: 'ok-code', name: 'X', durationHours: 0 });
    component.submit();
    expect(svc.createPath).not.toHaveBeenCalled();

    component.form.patchValue({ durationHours: 10001 });
    component.submit();
    expect(svc.createPath).not.toHaveBeenCalled();
  });

  it('refuse un score de réussite hors de l’intervalle 0-100', async () => {
    await setup(null);
    component.form.patchValue({ code: 'ok-code', name: 'X', passingScore: 101 });
    component.submit();
    expect(svc.createPath).not.toHaveBeenCalled();
  });

  it('n’envoie rien à la création si la session a expiré', async () => {
    session = null;
    await setup(null);
    component.form.patchValue({ code: 'ok-code', name: 'X' });
    expect(() => component.submit()).toThrowError('No session');
    expect(svc.createPath).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  // ---- Édition ---------------------------------------------------------------

  it('verrouille le code en édition et pré-remplit le parcours', async () => {
    await setup({ path: existing });
    expect(component.isEdit).toBeTrue();
    expect(component.form.controls.code.disabled).toBeTrue();
    const v = component.form.getRawValue();
    expect(v.code).toBe('yellow-belt');
    expect(v.validityMonths).toBe(36);
  });

  it('met à jour le parcours existant sans renvoyer son code', async () => {
    await setup({ path: existing });
    component.form.patchValue({ name: ' Yellow Belt v2 ', targetRole: '  ' });
    component.submit();

    expect(svc.updatePath).toHaveBeenCalledWith('path-1', {
      name: 'Yellow Belt v2', description: 'Bases qualité', targetRole: undefined,
      durationHours: 14, passingScore: 70, validityMonths: 36
    });
    expect(svc.createPath).not.toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup({ path: existing });
    svc.updatePath.and.returnValue(throwError(() => ({ status: 400 })));
    component.submit();
    expect(snack.open)
      .toHaveBeenCalledWith('Champs invalides — vérifiez le formulaire.', 'OK', { duration: 4000 });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup(null);
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.createPath).not.toHaveBeenCalled();
  });
});
