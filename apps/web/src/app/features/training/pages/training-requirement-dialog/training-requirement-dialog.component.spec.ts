import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { SkillRequirementResponse, SkillResponse } from '../../training.types';
import {
  TrainingRequirementDialogComponent, TrainingRequirementDialogData
} from './training-requirement-dialog.component';

describe('TrainingRequirementDialogComponent', () => {
  let fixture: ComponentFixture<TrainingRequirementDialogComponent>;
  let component: TrainingRequirementDialogComponent;
  let svc: jasmine.SpyObj<TrainingService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<TrainingRequirementDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const skill = (id: string, code: string): SkillResponse => ({
    id, tenantId: 't1', code, name: code.toUpperCase(), category: 'Méthodes',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
  });

  const attached: SkillRequirementResponse = {
    id: 'req-1', pathId: 'path-1', skillId: 'skill-2', targetLevel: 3,
    createdAt: '2026-06-01T00:00:00Z'
  };

  const catalog = [skill('skill-1', 'ishikawa'), skill('skill-2', 'spc'), skill('skill-3', 'capa')];
  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  async function setup(data: TrainingRequirementDialogData, catalogFails = false): Promise<void> {
    TestBed.resetTestingModule();
    svc = jasmine.createSpyObj<TrainingService>('TrainingService', ['listSkills', 'attachRequirement']);
    svc.listSkills.and.returnValue(
      catalogFails ? throwError(() => ({ status: 500 })) : of(page(catalog)));
    svc.attachRequirement.and.returnValue(of(attached));
    dialogRef = jasmine.createSpyObj<MatDialogRef<TrainingRequirementDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [TrainingRequirementDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TrainingRequirementDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('charge tout le catalogue de compétences en une page', async () => {
    await setup({ pathId: 'path-1' });
    expect(svc.listSkills).toHaveBeenCalledWith(0, 200);
    expect(component.loading).toBeFalse();
    expect(component.skills$.value.length).toBe(3);
  });

  it('masque les compétences déjà exigées par le parcours', async () => {
    await setup({ pathId: 'path-1', excludeSkillIds: ['skill-1', 'skill-3'] });
    expect(component.skills$.value.map(s => s.id)).toEqual(['skill-2']);
  });

  it('signale un catalogue indisponible sans bloquer le dialogue', async () => {
    await setup({ pathId: 'path-1' }, true);
    expect(component.loadError).toBe('Erreur serveur — réessayez dans un instant.');
    expect(component.loading).toBeFalse();
    expect(component.skills$.value).toEqual([]);
  });

  it('propose le niveau PRACTITIONER par défaut sur l’échelle 0-4', async () => {
    await setup({ pathId: 'path-1' });
    expect(component.levels).toEqual([0, 1, 2, 3, 4]);
    expect(component.levelLabels[2]).toBe('PRACTITIONER');
    expect(component.form.getRawValue().targetLevel).toBe(2);
  });

  it('refuse l’envoi tant qu’aucune compétence n’est choisie', async () => {
    await setup({ pathId: 'path-1' });
    component.submit();
    expect(svc.attachRequirement).not.toHaveBeenCalled();
    expect(component.form.controls.skillId.touched).toBeTrue();
  });

  it('refuse un niveau cible hors de l’échelle', async () => {
    await setup({ pathId: 'path-1' });
    component.form.patchValue({ skillId: 'skill-2', targetLevel: 5 });
    component.submit();
    expect(svc.attachRequirement).not.toHaveBeenCalled();
  });

  it('rattache la compétence au parcours du dialogue et rend le résultat au parent', async () => {
    await setup({ pathId: 'path-1' });
    component.form.patchValue({ skillId: 'skill-2', targetLevel: 3 });
    component.submit();
    expect(svc.attachRequirement).toHaveBeenCalledWith('path-1', { skillId: 'skill-2', targetLevel: 3 });
    expect(dialogRef.close).toHaveBeenCalledWith(attached);
  });

  it('garde le dialogue ouvert quand le serveur refuse le rattachement', async () => {
    await setup({ pathId: 'path-1' });
    svc.attachRequirement.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ skillId: 'skill-2', targetLevel: 3 });
    component.submit();
    expect(snack.open)
      .toHaveBeenCalledWith('État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', async () => {
    await setup({ pathId: 'path-1' });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.attachRequirement).not.toHaveBeenCalled();
  });
});
