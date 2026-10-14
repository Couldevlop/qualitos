import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsService } from '../../audits.service';
import { FindingResponse } from '../../audits.types';
import { AuditsFindingDialogComponent } from './audits-finding-dialog.component';

/**
 * Non-régression « impossible d'ajouter un constat ».
 *
 * Le refus du serveur sur un audit qui n'est pas en cours (409) revenait sous
 * le message générique « État incompatible — rechargez la page », qu'aucun
 * rechargement ne levait. Le dialogue dit désormais la règle et le geste.
 * L'auteur n'est plus envoyé : le serveur le lit dans le jeton.
 */
describe('AuditsFindingDialogComponent', () => {
  let fixture: ComponentFixture<AuditsFindingDialogComponent>;
  let component: AuditsFindingDialogComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsFindingDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<AuditsService>('AuditsService', ['addFinding']);
    dialogRef = jasmine.createSpyObj<MatDialogRef<AuditsFindingDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [AuditsFindingDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { planId: 'p1', checklistItemId: 'i1' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditsFindingDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('envoie le constat sans auteur, lié à la question d\'origine', () => {
    svc.addFinding.and.returnValue(of({ id: 'f1' } as FindingResponse));
    component.form.patchValue({ type: 'MAJOR_NC', description: '  Écart constaté  ', clauseRef: ' 8.5 ' });

    component.submit();

    expect(svc.addFinding).toHaveBeenCalledWith('p1', {
      type: 'MAJOR_NC',
      description: 'Écart constaté',
      clauseRef: '8.5',
      photoUrl: undefined,
      checklistItemId: 'i1'
    });
    const payload = svc.addFinding.calls.mostRecent().args[1] as unknown as Record<string, unknown>;
    expect('raisedBy' in payload).toBeFalse();
    expect(dialogRef.close).toHaveBeenCalledWith({ id: 'f1' } as FindingResponse);
  });

  it('409 : explique qu\'il faut démarrer l\'audit, sans renvoyer recharger la page', () => {
    svc.addFinding.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ description: 'Écart' });

    component.submit();

    const message = snack.open.calls.mostRecent().args[0];
    expect(message).toContain('démarrez l\'audit');
    expect(message).not.toContain('rechargez');
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('autre erreur : message générique, dialogue laissé ouvert', () => {
    svc.addFinding.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ description: 'Écart' });

    component.submit();

    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('refuse un constat sans description', () => {
    component.submit();
    expect(svc.addFinding).not.toHaveBeenCalled();
  });
});
