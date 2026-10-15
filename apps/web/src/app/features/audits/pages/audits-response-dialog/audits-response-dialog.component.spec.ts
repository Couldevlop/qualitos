import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsService } from '../../audits.service';
import { ChecklistItemResponse } from '../../audits.types';
import { AuditsResponseDialogComponent, AuditsResponseDialogData } from './audits-response-dialog.component';

/** Répondre à une question d'audit : l'auditeur prend toujours position, conforme ou non. */
describe('AuditsResponseDialogComponent', () => {
  let fixture: ComponentFixture<AuditsResponseDialogComponent>;
  let component: AuditsResponseDialogComponent;
  let audits: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsResponseDialogComponent, ChecklistItemResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(over: Partial<AuditsResponseDialogData> = {}): Promise<void> {
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['respondChecklistItem']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [AuditsResponseDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: audits },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { planId: 'p-1', itemId: 'i-1', question: 'Procédure à jour ?', ...over } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(AuditsResponseDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('reprend une réponse déjà donnée', async () => {
    await build({ initialResponse: 'Vu en revue', initialConformant: false });
    expect(component.form.getRawValue()).toEqual({ response: 'Vu en revue', conformant: false });
  });

  it('exige une prise de position avant d\'enregistrer', async () => {
    await build();
    component.submit();
    expect(audits.respondChecklistItem).not.toHaveBeenCalled();
    expect(component.form.controls.conformant.touched).toBeTrue();
  });

  it('enregistre la réponse rognée et la rend au parent', async () => {
    await build();
    const item = { id: 'i-1' } as ChecklistItemResponse;
    audits.respondChecklistItem.and.returnValue(of(item));

    component.form.patchValue({ response: '  RAS  ', conformant: true });
    component.submit();

    expect(audits.respondChecklistItem).toHaveBeenCalledWith('p-1', 'i-1', { response: 'RAS', conformant: true });
    expect(dialogRef.close).toHaveBeenCalledWith(item);
  });

  it('garde la fenêtre ouverte sur un refus du serveur', async () => {
    await build();
    audits.respondChecklistItem.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ response: '', conformant: false });

    component.submit();

    expect(audits.respondChecklistItem).toHaveBeenCalledWith('p-1', 'i-1', { response: undefined, conformant: false });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('se ferme sans rien rendre à l\'annulation', async () => {
    await build();
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
