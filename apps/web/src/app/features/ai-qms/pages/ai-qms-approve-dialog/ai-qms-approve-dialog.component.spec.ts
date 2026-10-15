import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { STUB_PERSON, provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { AiQmsService } from '../../ai-qms.service';
import { AiQmsView } from '../../ai-qms.types';
import { AiQmsApproveDialogComponent, AiQmsApproveDialogData } from './ai-qms-approve-dialog.component';

/**
 * Approuver un QMS : le soumissionnaire se choisit dans l'annuaire (ADR 0092),
 * jamais en tapant un identifiant, et ne peut pas être l'approbateur
 * (séparation des rôles, AI Act art. 17).
 */
describe('AiQmsApproveDialogComponent', () => {
  const ME = '99999999-9999-4999-8999-999999999999';

  let fixture: ComponentFixture<AiQmsApproveDialogComponent>;
  let component: AiQmsApproveDialogComponent;
  let svc: jasmine.SpyObj<AiQmsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AiQmsApproveDialogComponent, AiQmsView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let user: AuthUser | null;

  async function build(data: AiQmsApproveDialogData): Promise<void> {
    svc = jasmine.createSpyObj<AiQmsService>('AiQmsService', ['approve']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [AiQmsApproveDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideDirectoryStub(),
        { provide: AiQmsService, useValue: svc },
        { provide: AuthService, useValue: { snapshot: () => user } },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(AiQmsApproveDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    user = { userId: ME, tenantId: 't-1', displayName: 'Moi', roles: ['quality_manager'] };
  });

  it('propose d\'office le soumissionnaire connu, par son nom', async () => {
    await build({ qmsId: 'q-1', submittedByUserId: STUB_PERSON.accountId });
    fixture.detectChanges();

    expect(component.form.controls.submittedByUserId.value).toBe(STUB_PERSON.accountId);
    const input = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="user-picker-input"]') as HTMLInputElement;
    expect(input.value).toBe(STUB_PERSON.displayName);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('UUID');
  });

  it('ne se propose pas soi-même comme soumissionnaire', async () => {
    await build({ qmsId: 'q-1', submittedByUserId: ME });

    expect(component.excluded).toEqual([ME]);
    expect(component.form.controls.submittedByUserId.value).toBeNull();
    expect(component.form.valid).toBeFalse();
  });

  it('approuve avec le soumissionnaire choisi et soi-même comme approbateur', async () => {
    await build({ qmsId: 'q-1' });
    const approved = { id: 'q-1' } as AiQmsView;
    svc.approve.and.returnValue(of(approved));

    component.form.patchValue({ submittedByUserId: STUB_PERSON.accountId, approvalNotes: '  RAS  ' });
    component.submit();

    expect(svc.approve).toHaveBeenCalledWith('q-1', {
      submittedByUserId: STUB_PERSON.accountId, approvedByUserId: ME, approvalNotes: 'RAS'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(approved);
  });

  it('refuse que le soumissionnaire soit l\'approbateur', async () => {
    await build({ qmsId: 'q-1' });
    component.form.patchValue({ submittedByUserId: ME });

    component.submit();

    expect(svc.approve).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('n\'envoie rien sans session ni sans soumissionnaire', async () => {
    await build({ qmsId: 'q-1' });
    component.submit();
    expect(svc.approve).not.toHaveBeenCalled();

    user = null;
    component.form.patchValue({ submittedByUserId: STUB_PERSON.accountId });
    component.submit();
    expect(svc.approve).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('signale un refus du serveur sans fermer la fenêtre', async () => {
    await build({ qmsId: 'q-1' });
    svc.approve.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ submittedByUserId: STUB_PERSON.accountId });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('se ferme sans rien rendre à l\'annulation', async () => {
    await build({ qmsId: 'q-1' });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
