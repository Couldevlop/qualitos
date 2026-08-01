import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Complaint, ComplaintResponseEntry } from '../../complaints.types';
import { ComplaintResponseDialogComponent } from './complaint-response-dialog.component';

describe('ComplaintResponseDialogComponent', () => {
  let component: ComplaintResponseDialogComponent;
  let fixture: ComponentFixture<ComplaintResponseDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ComplaintResponseDialogComponent, ComplaintResponseEntry>>;
  let currentUser: AuthUser | null;

  const ID = '3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f';
  const AUTHOR = '11111111-1111-1111-1111-111111111111';
  const base = `${environment.apiBaseUrl}/api/v1/complaints`;

  const complaint: Complaint = {
    id: ID, tenantId: 't1', code: 'REC-1', channel: 'PHONE',
    customerName: null, customerEmail: null, customerExternalId: null,
    subject: 'Sujet', description: null, severity: 'MEDIUM', category: 'OTHER',
    status: 'UNDER_INVESTIGATION', supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: null, firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: 'u1', createdAt: null, updatedAt: null
  };

  const entry: ComplaintResponseEntry = {
    id: 'r1', tenantId: 't1', complaintId: ID, authorUserId: AUTHOR, channel: 'PHONE',
    body: 'Bonjour', internalNote: false, sentAt: null, createdAt: null
  };

  beforeEach(async () => {
    currentUser = { userId: AUTHOR, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<ComplaintResponseDialogComponent, ComplaintResponseEntry>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ComplaintResponseDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { complaint } },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ComplaintResponseDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('reprend le canal de la réclamation par défaut', () => {
    expect(component.form.controls.channel.value).toBe('PHONE');
  });

  it('avertit des conséquences d\'un message client, pas d\'une note interne', () => {
    expect(component.customerFacing).toBeTrue();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.notice')?.textContent).toContain('Répondue');

    component.form.controls.internalNote.setValue(true);
    fixture.detectChanges();
    expect(component.customerFacing).toBeFalse();
    expect(el.querySelector('.notice')?.textContent).toContain('interne');
  });

  it('exige un corps de message', () => {
    expect(component.form.controls.body.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(`${base}/${ID}/responses`);
  });

  it('envoie le message au fil et ferme le dialogue', () => {
    component.form.patchValue({ body: '  Nous revenons vers vous.  ' });
    component.submit();

    const req = http.expectOne(`${base}/${ID}/responses`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      authorUserId: AUTHOR, channel: 'PHONE',
      body: 'Nous revenons vers vous.', internalNote: false
    });

    req.flush(entry);
    expect(dialogRef.close).toHaveBeenCalledWith(entry);
  });

  it('marque une note interne comme telle', () => {
    component.form.patchValue({ body: 'Analyse en cours', internalNote: true, channel: 'API' });
    component.submit();

    const req = http.expectOne(`${base}/${ID}/responses`);
    expect(req.request.body.internalNote).toBeTrue();
    expect(req.request.body.channel).toBe('API');
    req.flush({ ...entry, internalNote: true, channel: 'API' });
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('bloque l\'ajout quand la session ne porte pas d\'UUID utilisateur', () => {
    currentUser = null;
    expect(component.authorId).toBeNull();
    component.form.patchValue({ body: 'Test' });
    component.submit();
    http.expectNone(`${base}/${ID}/responses`);
  });

  it('ne ferme pas le dialogue quand le serveur refuse le message', () => {
    component.form.patchValue({ body: 'Test' });
    component.submit();
    http.expectOne(`${base}/${ID}/responses`)
      .flush({ title: 'terminal' }, { status: 409, statusText: 'Conflict' });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
