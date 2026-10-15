import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsService } from '../../audits.service';
import { ChecklistItemResponse } from '../../audits.types';
import { AuditsChecklistDialogComponent } from './audits-checklist-dialog.component';

/** Ajouter une question à la liste de contrôle d'un audit. */
describe('AuditsChecklistDialogComponent', () => {
  let fixture: ComponentFixture<AuditsChecklistDialogComponent>;
  let component: AuditsChecklistDialogComponent;
  let audits: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsChecklistDialogComponent, ChecklistItemResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['addChecklistItem']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [AuditsChecklistDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: audits },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { planId: 'p-1' } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(AuditsChecklistDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('n\'ajoute rien sans question', () => {
    component.submit();
    expect(audits.addChecklistItem).not.toHaveBeenCalled();
    expect(component.form.controls.question.touched).toBeTrue();
  });

  it('ajoute la question, champs rognés, et la rend au parent', () => {
    const item = { id: 'i-9' } as ChecklistItemResponse;
    audits.addChecklistItem.and.returnValue(of(item));

    component.form.patchValue({ question: '  Revue faite ?  ', clauseRef: ' 9.3 ', expectedEvidence: '  ', weight: 2 });
    component.submit();

    expect(audits.addChecklistItem).toHaveBeenCalledWith('p-1', {
      question: 'Revue faite ?', clauseRef: '9.3', expectedEvidence: undefined, weight: 2
    });
    expect(dialogRef.close).toHaveBeenCalledWith(item);
  });

  it('garde la fenêtre ouverte sur un refus du serveur', () => {
    audits.addChecklistItem.and.returnValue(throwError(() => ({ status: 400 })));
    component.form.patchValue({ question: 'Revue faite ?' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('se ferme sans rien rendre à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
