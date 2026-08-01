import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { SkillResponse } from '../../training.types';
import { TrainingSkillDialogComponent, TrainingSkillDialogData } from './training-skill-dialog.component';

describe('TrainingSkillDialogComponent', () => {
  let fixture: ComponentFixture<TrainingSkillDialogComponent>;
  let component: TrainingSkillDialogComponent;
  let svc: jasmine.SpyObj<TrainingService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<TrainingSkillDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const existing: SkillResponse = {
    id: 'skill-1', tenantId: 't1', code: 'spc-control-charts', name: 'Cartes de contrôle SPC',
    description: 'X-R, X-S, EWMA', category: 'DMAIC',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  };

  async function setup(data: TrainingSkillDialogData | null): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<TrainingService>('TrainingService', ['createSkill', 'updateSkill']);
    svc.createSkill.and.returnValue(of(existing));
    svc.updateSkill.and.returnValue(of(existing));
    dialogRef = jasmine.createSpyObj<MatDialogRef<TrainingSkillDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [TrainingSkillDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TrainingSkillDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('ouvre un formulaire de création vierge', async () => {
    await setup(null);
    expect(component.isEdit).toBeFalse();
    expect(component.form.getRawValue()).toEqual({ code: '', name: '', description: '', category: '' });
    expect(component.form.valid).toBeFalse();
  });

  it('crée la compétence en nettoyant les champs et en omettant les vides', async () => {
    await setup(null);
    component.form.patchValue({
      code: ' ishikawa-facilitation ', name: ' Animation Ishikawa ',
      description: '  ', category: ' Méthodes '
    });
    component.submit();

    expect(svc.createSkill).toHaveBeenCalledWith({
      code: 'ishikawa-facilitation', name: 'Animation Ishikawa',
      description: undefined, category: 'Méthodes'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('refuse un code qui ne respecte pas le motif serveur', async () => {
    await setup(null);
    component.form.patchValue({ code: 'SPC Charts', name: 'X' });
    component.submit();
    expect(svc.createSkill).not.toHaveBeenCalled();
    expect(component.form.controls.code.touched).toBeTrue();
  });

  it('refuse un nom absent', async () => {
    await setup(null);
    component.form.patchValue({ code: 'ok-code', name: '' });
    component.submit();
    expect(svc.createSkill).not.toHaveBeenCalled();
  });

  it('verrouille le code en édition et pré-remplit la compétence', async () => {
    await setup({ skill: existing });
    expect(component.isEdit).toBeTrue();
    expect(component.form.controls.code.disabled).toBeTrue();
    expect(component.form.getRawValue().category).toBe('DMAIC');
  });

  it('met à jour la compétence existante sans renvoyer son code', async () => {
    await setup({ skill: existing });
    component.form.patchValue({ name: ' SPC avancé ', category: '   ' });
    component.submit();
    expect(svc.updateSkill).toHaveBeenCalledWith('skill-1', {
      name: 'SPC avancé', description: 'X-R, X-S, EWMA', category: undefined
    });
    expect(svc.createSkill).not.toHaveBeenCalled();
  });

  it('affiche l’erreur serveur sans fermer le dialogue', async () => {
    await setup(null);
    svc.createSkill.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ code: 'ok-code', name: 'X' });
    component.submit();
    expect(snack.open)
      .toHaveBeenCalledWith('État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup(null);
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.createSkill).not.toHaveBeenCalled();
  });
});
