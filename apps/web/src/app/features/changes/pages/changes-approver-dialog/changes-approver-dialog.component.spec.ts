import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApprovalResponse } from '../../changes.types';
import { ChangesApproverDialogComponent } from './changes-approver-dialog.component';

/**
 * L'approbateur est désigné par son identifiant technique : un format libre
 * partirait au serveur pour revenir en 400 sans explication utile.
 */
describe('ChangesApproverDialogComponent', () => {
  let component: ChangesApproverDialogComponent;
  let fixture: ComponentFixture<ChangesApproverDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ChangesApproverDialogComponent, ApprovalResponse>>;
  let prevMock: boolean;

  const CHANGE_ID = 'chg-1';
  const APPROVER = '22222222-2222-2222-2222-222222222222';
  const url = `${environment.apiBaseUrl}/api/v1/changes/${CHANGE_ID}/approvers`;

  const added: ApprovalResponse = {
    id: 'a1', tenantId: 't1', changeId: CHANGE_ID, approverUserId: APPROVER,
    approvalLevel: 2, decision: 'PENDING', createdAt: '2026-07-01T08:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<ChangesApproverDialogComponent, ApprovalResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ChangesApproverDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { changeId: CHANGE_ID } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangesApproverDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige un identifiant d\'approbateur au format UUID', () => {
    expect(component.form.controls.approverUserId.hasError('required')).toBeTrue();

    component.form.controls.approverUserId.setValue('qa-manager');
    expect(component.form.controls.approverUserId.hasError('pattern')).toBeTrue();
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.approverUserId.touched).toBeTrue();

    component.form.controls.approverUserId.setValue(APPROVER);
    expect(component.form.controls.approverUserId.valid).toBeTrue();
  });

  it('refuse un niveau d\'approbation inférieur à 1', () => {
    component.form.controls.approverUserId.setValue(APPROVER);
    component.form.controls.approvalLevel.setValue(0);
    expect(component.form.controls.approvalLevel.hasError('min')).toBeTrue();

    component.submit();
    http.expectNone(url);
  });

  it('propose le niveau 1 par défaut et poste l\'approbateur normalisé', () => {
    expect(component.form.controls.approvalLevel.value).toBe(1);

    component.form.patchValue({ approverUserId: APPROVER, approvalLevel: 2 });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ approverUserId: APPROVER, approvalLevel: 2 });

    req.flush(added);
    expect(dialogRef.close).toHaveBeenCalledWith(added);
  });

  it('garde le dialogue ouvert quand l\'approbateur est déjà présent', () => {
    component.form.controls.approverUserId.setValue(APPROVER);
    component.submit();
    http.expectOne(url).flush({ title: 'duplicate' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
