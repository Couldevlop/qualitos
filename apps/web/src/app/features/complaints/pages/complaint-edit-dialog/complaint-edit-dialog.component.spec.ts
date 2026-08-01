import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Complaint } from '../../complaints.types';
import { ComplaintEditDialogComponent } from './complaint-edit-dialog.component';

describe('ComplaintEditDialogComponent', () => {
  let component: ComplaintEditDialogComponent;
  let fixture: ComponentFixture<ComplaintEditDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ComplaintEditDialogComponent, Complaint>>;

  const ID = '3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f';
  const base = `${environment.apiBaseUrl}/api/v1/complaints`;

  const complaint: Complaint = {
    id: ID, tenantId: 't1', code: 'REC-1', channel: 'EMAIL',
    customerName: 'Acme', customerEmail: 'qa@acme.test', customerExternalId: null,
    subject: 'Colis endommagé', description: 'Carton écrasé.',
    severity: 'HIGH', category: 'DELIVERY', status: 'UNDER_INVESTIGATION',
    supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: null, firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: 'u1', createdAt: null, updatedAt: null
  };

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj<MatDialogRef<ComplaintEditDialogComponent, Complaint>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ComplaintEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { complaint } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ComplaintEditDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('pré-remplit le formulaire avec le dossier courant', () => {
    expect(component.form.controls.subject.value).toBe('Colis endommagé');
    expect(component.form.controls.severity.value).toBe('HIGH');
    expect(component.form.controls.customerEmail.value).toBe('qa@acme.test');
    expect(component.form.valid).toBeTrue();
  });

  it('exige un objet non vide', () => {
    component.form.controls.subject.setValue('');
    expect(component.form.invalid).toBeTrue();
    component.submit();
    http.expectNone(`${base}/${ID}`);
  });

  it('envoie null pour les champs vidés (mise à jour partielle côté serveur)', () => {
    component.form.patchValue({ description: '   ', customerName: '', category: 'QUALITY' });
    component.submit();

    const req = http.expectOne(`${base}/${ID}`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.description).toBeNull();
    expect(req.request.body.customerName).toBeNull();
    expect(req.request.body.category).toBe('QUALITY');
    expect(req.request.body.subject).toBe('Colis endommagé');

    req.flush({ ...complaint, category: 'QUALITY' });
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('refuse un UUID de fournisseur mal formé', () => {
    component.form.controls.supplierId.setValue('abc');
    expect(component.form.controls.supplierId.hasError('pattern')).toBeTrue();
  });

  it('ne ferme pas le dialogue quand le serveur refuse la modification', () => {
    component.submit();
    http.expectOne(`${base}/${ID}`)
      .flush({ title: 'terminal' }, { status: 409, statusText: 'Conflict' });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
