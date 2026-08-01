import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApprovalResponse } from '../../changes.types';
import { ChangesDecisionDialogComponent } from './changes-decision-dialog.component';

/**
 * Une décision engage l'approbateur : elle est signée par l'identité du JWT et,
 * en cas de rejet, motivée (le motif remonte dans la demande et dans l'audit).
 */
describe('ChangesDecisionDialogComponent', () => {
  let component: ChangesDecisionDialogComponent;
  let fixture: ComponentFixture<ChangesDecisionDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ChangesDecisionDialogComponent, ApprovalResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const CHANGE_ID = 'chg-1';
  const USER = '11111111-1111-1111-1111-111111111111';
  const url = `${environment.apiBaseUrl}/api/v1/changes/${CHANGE_ID}/decisions`;

  const recorded: ApprovalResponse = {
    id: 'a1', tenantId: 't1', changeId: CHANGE_ID, approverUserId: USER,
    approvalLevel: 1, decision: 'APPROVED', createdAt: '2026-07-01T08:00:00Z'
  };

  async function setup(decision: 'APPROVED' | 'REJECTED'): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ChangesDecisionDialogComponent, ApprovalResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ChangesDecisionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { changeId: CHANGE_ID, decision } },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangesDecisionDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: USER, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('annonce une approbation sans ton d\'alerte et n\'exige pas de commentaire', async () => {
    await setup('APPROVED');
    expect(component.title).toBe('Approuver la demande');
    expect(component.danger).toBeFalse();
    expect(component.form.valid).toBeTrue();
  });

  it('exige un motif pour un rejet et le signale comme destructeur', async () => {
    await setup('REJECTED');
    expect(component.title).toBe('Rejeter la demande');
    expect(component.danger).toBeTrue();
    expect(component.form.controls.comment.hasError('required')).toBeTrue();

    component.submit();
    http.expectNone(url);
    expect(component.form.controls.comment.touched).toBeTrue();
  });

  it('refuse un commentaire au-delà de la limite serveur', async () => {
    await setup('APPROVED');
    component.form.controls.comment.setValue('c'.repeat(1001));
    expect(component.form.controls.comment.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(url);
  });

  it('ne décide pas au nom d\'un utilisateur inconnu', async () => {
    await setup('APPROVED');
    currentUser = null;
    component.submit();
    http.expectNone(url);
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('poste la décision signée par l\'utilisateur du JWT', async () => {
    await setup('REJECTED');
    component.form.controls.comment.setValue('  Risque non couvert  ');
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      approverUserId: USER, decision: 'REJECTED', comment: 'Risque non couvert'
    });

    req.flush({ ...recorded, decision: 'REJECTED', comment: 'Risque non couvert' });
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('omet un commentaire vide sur une approbation', async () => {
    await setup('APPROVED');
    component.form.controls.comment.setValue('   ');
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.body.comment).toBeUndefined();
    expect(req.request.body.decision).toBe('APPROVED');
    req.flush(recorded);
  });

  it('garde le dialogue ouvert quand la décision est refusée par le serveur', async () => {
    await setup('APPROVED');
    component.submit();
    http.expectOne(url).flush({ title: 'not an approver' }, { status: 403, statusText: 'Forbidden' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', async () => {
    await setup('APPROVED');
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
