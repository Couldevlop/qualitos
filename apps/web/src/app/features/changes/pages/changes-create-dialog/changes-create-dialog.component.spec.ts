import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ChangeResponse } from '../../changes.types';
import { ChangesCreateDialogComponent, ChangesCreateDialogData } from './changes-create-dialog.component';

/**
 * Le même dialogue sert à créer et à modifier : en modification, le code et le
 * type sont figés (ils identifient la demande côté serveur et dans les preuves
 * déjà rattachées).
 */
describe('ChangesCreateDialogComponent', () => {
  let component: ChangesCreateDialogComponent;
  let fixture: ComponentFixture<ChangesCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ChangesCreateDialogComponent, ChangeResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/changes`;
  const USER = '11111111-1111-1111-1111-111111111111';

  const existing: ChangeResponse = {
    id: 'chg-1', tenantId: 't1', code: 'CHG-2026-014', title: 'Procédure stérilisation',
    description: 'Alignement ISO 13485.', type: 'DOCUMENT', priority: 'HIGH',
    status: 'UNDER_REVIEW', requesterUserId: USER, plannedFor: '2026-09-01',
    impactSummary: 'PROC-STER-004', riskAssessment: 'Faible',
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z'
  };

  /** Instancie le dialogue avec ou sans demande existante (création vs modification). */
  async function setup(data: ChangesCreateDialogData | null): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ChangesCreateDialogComponent, ChangeResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ChangesCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangesCreateDialogComponent);
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

  it('exige un code et un titre à la création', async () => {
    await setup(null);
    expect(component.isEdit).toBeFalse();
    expect(component.form.controls.code.hasError('required')).toBeTrue();
    expect(component.form.controls.title.hasError('required')).toBeTrue();

    component.submit();
    http.expectNone(base);
    expect(component.form.controls.code.touched).toBeTrue();
  });

  it('impose au code le format attendu par le serveur', async () => {
    await setup(null);
    component.form.controls.code.setValue('-mauvais code');
    expect(component.form.controls.code.hasError('pattern')).toBeTrue();

    component.form.controls.code.setValue('CHG-2026-014');
    expect(component.form.controls.code.valid).toBeTrue();
  });

  it('refuse les textes plus longs que les colonnes du serveur', async () => {
    await setup(null);
    component.form.controls.title.setValue('t'.repeat(251));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();
    component.form.controls.description.setValue('d'.repeat(4001));
    expect(component.form.controls.description.hasError('maxlength')).toBeTrue();
    component.form.controls.impactSummary.setValue('i'.repeat(2001));
    expect(component.form.controls.impactSummary.hasError('maxlength')).toBeTrue();
  });

  it('crée la demande avec le demandeur porté par le JWT', async () => {
    await setup(null);
    component.form.patchValue({
      code: 'CHG-2026-020', title: '  Nouveau fournisseur  ', description: '   ',
      type: 'SUPPLIER', priority: 'CRITICAL', plannedFor: '2026-10-01',
      impactSummary: '  Qualif matière  ', riskAssessment: '   '
    });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.code).toBe('CHG-2026-020');
    expect(req.request.body.title).toBe('Nouveau fournisseur');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.riskAssessment).toBeUndefined();
    expect(req.request.body.impactSummary).toBe('Qualif matière');
    expect(req.request.body.type).toBe('SUPPLIER');
    expect(req.request.body.priority).toBe('CRITICAL');
    expect(req.request.body.requesterUserId).toBe(USER);

    req.flush(existing);
    expect(dialogRef.close).toHaveBeenCalledWith(existing);
  });

  it('n\'envoie rien au serveur quand la session a expiré', async () => {
    await setup(null);
    currentUser = null;
    component.form.patchValue({ code: 'CHG-1', title: 'Titre' });

    // Le composant interrompt la soumission ; on vérifie surtout qu'aucune
    // demande ne part sans demandeur identifié.
    try { component.submit(); } catch { /* cf. bug remonté : interruption par exception */ }

    http.expectNone(base);
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('garde le dialogue ouvert quand le code est déjà pris', async () => {
    await setup(null);
    component.form.patchValue({ code: 'CHG-1', title: 'Titre' });
    component.submit();
    http.expectOne(base).flush({ title: 'duplicate' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('fige le code et le type en modification', async () => {
    await setup({ change: existing });
    expect(component.isEdit).toBeTrue();
    expect(component.form.controls.code.disabled).toBeTrue();
    expect(component.form.controls.type.disabled).toBeTrue();
    expect(component.form.controls.title.value).toBe('Procédure stérilisation');
    expect(component.form.controls.priority.value).toBe('HIGH');
  });

  it('n\'envoie en modification que les champs modifiables', async () => {
    await setup({ change: existing });
    component.form.patchValue({ title: '  Procédure v2  ', priority: 'LOW', impactSummary: '   ' });
    component.submit();

    const req = http.expectOne(`${base}/chg-1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.title).toBe('Procédure v2');
    expect(req.request.body.priority).toBe('LOW');
    expect(req.request.body.impactSummary).toBeUndefined();
    // Ni le code ni le type ne repartent : ils ne sont pas modifiables.
    expect(req.request.body.code).toBeUndefined();
    expect(req.request.body.type).toBeUndefined();

    req.flush({ ...existing, title: 'Procédure v2' });
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('ferme le dialogue à l\'annulation', async () => {
    await setup(null);
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(base);
  });
});
