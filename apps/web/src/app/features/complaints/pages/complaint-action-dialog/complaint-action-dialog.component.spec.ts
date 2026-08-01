import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Complaint } from '../../complaints.types';
import { ComplaintActionDialogComponent, ComplaintActionMode } from './complaint-action-dialog.component';

/**
 * Un seul dialogue sert quatre transitions : chaque mode doit garder SES
 * validations et SON appel serveur — c'est précisément ce que ces tests fixent.
 */
describe('ComplaintActionDialogComponent', () => {
  let component: ComplaintActionDialogComponent;
  let fixture: ComponentFixture<ComplaintActionDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ComplaintActionDialogComponent, Complaint>>;
  let currentUser: AuthUser | null;

  const ID = '3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f';
  const OTHER_UUID = '22222222-2222-2222-2222-222222222222';
  const ME = '11111111-1111-1111-1111-111111111111';
  const base = `${environment.apiBaseUrl}/api/v1/complaints`;

  const complaint = (over: Partial<Complaint> = {}): Complaint => ({
    id: ID, tenantId: 't1', code: 'REC-1', channel: 'EMAIL',
    customerName: null, customerEmail: null, customerExternalId: null,
    subject: 'Sujet', description: null, severity: 'MEDIUM', category: 'OTHER',
    status: 'UNDER_INVESTIGATION', supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: null, firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: 'u1', createdAt: null, updatedAt: null, ...over
  });

  async function setup(mode: ComplaintActionMode, over: Partial<Complaint> = {}): Promise<void> {
    currentUser = { userId: ME, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<ComplaintActionDialogComponent, Complaint>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ComplaintActionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { complaint: complaint(over), mode } },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ComplaintActionDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  it('assigne : UUID obligatoire, valeur courante reprise, raccourci « me l\'assigner »', async () => {
    await setup('ASSIGN', { assignedToUserId: OTHER_UUID });

    expect(component.form.controls.text.value).toBe(OTHER_UUID);
    expect(component.form.controls.score.disabled).toBeTrue();
    expect(component.fieldRequired).toBeTrue();
    expect(component.submitColor).toBe('primary');
    expect(component.canAssignToSelf).toBeTrue();

    component.assignToSelf();
    expect(component.form.controls.text.value).toBe(ME);
    expect(component.canAssignToSelf).toBeFalse();

    component.form.controls.text.setValue('pas-un-uuid');
    expect(component.form.invalid).toBeTrue();
    component.submit();
    http.expectNone(`${base}/${ID}/assign`);

    component.form.controls.text.setValue(OTHER_UUID);
    component.submit();
    const req = http.expectOne(`${base}/${ID}/assign`);
    expect(req.request.body).toEqual({ assigneeUserId: OTHER_UUID });
    req.flush(complaint({ assignedToUserId: OTHER_UUID }));
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('rejette : motif obligatoire, action présentée comme terminale', async () => {
    await setup('REJECT');

    expect(component.isLongText).toBeTrue();
    expect(component.submitColor).toBe('warn');
    expect(component.form.invalid).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).querySelector('.warn')?.textContent)
      .toContain('définitivement');

    component.form.controls.text.setValue('  Hors périmètre  ');
    component.submit();
    const req = http.expectOne(`${base}/${ID}/reject`);
    expect(req.request.body).toEqual({ reason: 'Hors périmètre' });
    req.flush(complaint({ status: 'REJECTED' }));
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('résout : dossier CAPA facultatif', async () => {
    await setup('RESOLVE');

    expect(component.fieldRequired).toBeFalse();
    expect(component.form.valid).toBeTrue();

    component.submit();
    const withoutCapa = http.expectOne(`${base}/${ID}/resolve`);
    expect(withoutCapa.request.body).toEqual({ capaCaseId: null });
    withoutCapa.flush(complaint({ status: 'RESOLVED' }));
  });

  it('résout : le CAPA renseigné doit être un UUID et part dans la requête', async () => {
    await setup('RESOLVE');

    component.form.controls.text.setValue('capa-9');
    expect(component.form.invalid).toBeTrue();

    component.form.controls.text.setValue(OTHER_UUID);
    component.submit();
    const req = http.expectOne(`${base}/${ID}/resolve`);
    expect(req.request.body).toEqual({ capaCaseId: OTHER_UUID });
    req.flush(complaint({ status: 'RESOLVED', capaCaseId: OTHER_UUID }));
  });

  it('satisfaction : note bornée 0..10, champ texte neutralisé', async () => {
    await setup('SATISFACTION', { status: 'RESOLVED', satisfactionScore: 6 });

    expect(component.isTextMode).toBeFalse();
    expect(component.form.controls.text.disabled).toBeTrue();
    expect(component.form.controls.score.value).toBe(6);

    component.form.controls.score.setValue(12);
    expect(component.form.controls.score.hasError('max')).toBeTrue();
    component.form.controls.score.setValue(-1);
    expect(component.form.controls.score.hasError('min')).toBeTrue();

    component.form.controls.score.setValue(9);
    component.submit();
    const req = http.expectOne(`${base}/${ID}/satisfaction`);
    expect(req.request.body).toEqual({ score: 9 });
    req.flush(complaint({ status: 'RESOLVED', satisfactionScore: 9 }));
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('propose des libellés propres à chaque mode', async () => {
    await setup('SATISFACTION', { status: 'CLOSED' });
    expect(component.title).toContain('Satisfaction');
    expect(component.subtitle).toContain('0 à 10');
    expect(component.fieldLabel).toContain('0 à 10');
    expect(component.submitLabel).toContain('note');
    expect(component.submitIcon).toBe('sentiment_satisfied');
    expect(component.canAssignToSelf).toBeFalse();
  });

  it('ne ferme pas le dialogue quand le serveur refuse la transition', async () => {
    await setup('REJECT');
    component.form.controls.text.setValue('Doublon');
    component.submit();
    http.expectOne(`${base}/${ID}/reject`)
      .flush({ title: 'nope' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', async () => {
    await setup('ASSIGN');
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
