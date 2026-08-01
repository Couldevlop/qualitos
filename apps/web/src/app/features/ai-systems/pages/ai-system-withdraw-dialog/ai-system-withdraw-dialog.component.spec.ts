import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AiSystemsService } from '../../ai-systems.service';
import { AiSystemView } from '../../ai-systems.types';
import { AiSystemWithdrawDialogComponent } from './ai-system-withdraw-dialog.component';

describe('AiSystemWithdrawDialogComponent', () => {
  let fixture: ComponentFixture<AiSystemWithdrawDialogComponent>;
  let component: AiSystemWithdrawDialogComponent;
  let svc: jasmine.SpyObj<AiSystemsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AiSystemWithdrawDialogComponent, AiSystemView | undefined>>;

  const system: AiSystemView = {
    id: 'id-1', tenantId: 't-1', reference: 'AISYS-TRIAGE', name: 'Triage urgences',
    description: null, providerName: null, intendedPurpose: 'Priorisation',
    riskClassification: 'HIGH', role: 'DEPLOYER', generalPurpose: false, status: 'REGISTERED',
    conformityAssessmentEvidenceUrl: null, ceMarkingNumber: null,
    humanOversightDescription: null, transparencyMeasures: null, dataGovernanceNotes: null,
    linkedDpiaId: null, linkedProcessingActivityIds: [], linkedAutomatedDecisionIds: [],
    effectiveFrom: null, effectiveTo: null, withdrawalReason: null,
    createdByUserId: 'u-1', createdAt: '2026-07-01T09:00:00Z', updatedAt: '2026-07-01T09:00:00Z',
    prohibited: false, requiresConformityAssessment: true, requiresTransparency: true
  };

  const withdrawn: AiSystemView = {
    ...system, status: 'WITHDRAWN', withdrawalReason: 'Projet arrêté'
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj<AiSystemsService>('AiSystemsService', ['withdraw']);
    svc.withdraw.and.returnValue(of(withdrawn));
    dialogRef = jasmine.createSpyObj<MatDialogRef<AiSystemWithdrawDialogComponent, AiSystemView | undefined>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [AiSystemWithdrawDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AiSystemsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { system } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AiSystemWithdrawDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('rappelle la référence concernée et le caractère définitif', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('AISYS-TRIAGE');
    expect(text).toContain('L\'abandon est définitif');
  });

  it('exige un motif : le domaine le refuse sans', () => {
    component.submit();

    expect(svc.withdraw).not.toHaveBeenCalled();
    expect(component.form.controls.reason.touched).toBeTrue();
  });

  it('envoie le motif détouré et ferme avec la fiche à jour', () => {
    component.form.controls.reason.setValue('  Projet arrêté  ');

    component.submit();

    expect(svc.withdraw).toHaveBeenCalledWith('id-1', { reason: 'Projet arrêté' });
    expect(dialogRef.close).toHaveBeenCalledWith(withdrawn);
  });

  it('remonte un message sûr quand le serveur refuse l\'abandon', () => {
    svc.withdraw.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.form.controls.reason.setValue('Projet arrêté');

    component.submit();

    expect(component.error).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.reason.setValue('Projet arrêté');
    component.submitting = true;

    component.submit();

    expect(svc.withdraw).not.toHaveBeenCalled();
  });

  it('ferme sans rien envoyer à l\'annulation', () => {
    component.cancel();

    expect(svc.withdraw).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(undefined);
  });
});
