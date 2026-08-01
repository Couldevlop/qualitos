import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Complaint } from '../../complaints.types';
import { ComplaintCreateDialogComponent } from './complaint-create-dialog.component';

/**
 * Le formulaire rejoue les contraintes du serveur (format du code, e-mail,
 * UUID) pour expliquer un refus pendant la saisie plutôt qu'après un 400.
 */
describe('ComplaintCreateDialogComponent', () => {
  let component: ComplaintCreateDialogComponent;
  let fixture: ComponentFixture<ComplaintCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ComplaintCreateDialogComponent, Complaint>>;
  let currentUser: AuthUser | null;

  const base = `${environment.apiBaseUrl}/api/v1/complaints`;
  const AUTHOR = '11111111-1111-1111-1111-111111111111';

  const created: Complaint = {
    id: 'c1', tenantId: 't1', code: 'REC-1', channel: 'EMAIL',
    customerName: null, customerEmail: null, customerExternalId: null,
    subject: 'Sujet', description: null, severity: 'MEDIUM', category: 'OTHER',
    status: 'RECEIVED', supplierId: null, capaCaseId: null, assignedToUserId: null,
    satisfactionScore: null, receivedAt: null, firstResponseAt: null,
    resolvedAt: null, closedAt: null, rejectionReason: null,
    createdBy: AUTHOR, createdAt: null, updatedAt: null
  };

  beforeEach(async () => {
    currentUser = { userId: AUTHOR, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<ComplaintCreateDialogComponent, Complaint>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ComplaintCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ComplaintCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('propose un code conforme au format attendu par le serveur', () => {
    expect(component.form.controls.code.value).toMatch(/^REC-\d{8}-[0-9A-F]{4}$/);
    expect(component.form.controls.code.valid).toBeTrue();
  });

  it('refuse un code hors format', () => {
    component.form.controls.code.setValue('.mauvais code');
    expect(component.form.controls.code.hasError('pattern')).toBeTrue();
  });

  it('exige un objet', () => {
    expect(component.form.controls.subject.hasError('required')).toBeTrue();
    expect(component.form.invalid).toBeTrue();
  });

  it('valide l\'e-mail et le format UUID des références', () => {
    component.form.controls.customerEmail.setValue('pas-un-email');
    expect(component.form.controls.customerEmail.hasError('email')).toBeTrue();

    component.form.controls.supplierId.setValue('123');
    expect(component.form.controls.supplierId.hasError('pattern')).toBeTrue();

    component.form.controls.supplierId.setValue('3f1b5c8e-9a2d-4c7b-8e1f-0a2b3c4d5e6f');
    expect(component.form.controls.supplierId.valid).toBeTrue();
  });

  it('bloque la création quand la session ne porte pas d\'UUID utilisateur', () => {
    currentUser = { userId: 'demo', tenantId: 't1', displayName: 'X', roles: [] };
    expect(component.authorId).toBeNull();

    component.form.controls.subject.setValue('Sujet');
    component.submit();
    http.expectNone(base);

    currentUser = null;
    expect(component.authorId).toBeNull();
  });

  it('envoie null pour les champs vides et un instant ISO pour la date de réception', () => {
    component.form.patchValue({
      code: 'REC-2026-0001',
      channel: 'PHONE',
      subject: '  Retard de livraison  ',
      description: '   ',
      severity: 'HIGH',
      category: 'DELIVERY',
      customerName: 'Acme',
      receivedAt: '2026-07-01T10:30'
    });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.body.code).toBe('REC-2026-0001');
    expect(req.request.body.subject).toBe('Retard de livraison');
    expect(req.request.body.description).toBeNull();
    expect(req.request.body.customerEmail).toBeNull();
    expect(req.request.body.supplierId).toBeNull();
    expect(req.request.body.createdBy).toBe(AUTHOR);
    expect(req.request.body.receivedAt).toBe(new Date('2026-07-01T10:30').toISOString());

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('laisse le serveur horodater quand la date de réception est vide', () => {
    component.form.controls.subject.setValue('Sujet');
    component.submit();
    const req = http.expectOne(base);
    expect(req.request.body.receivedAt).toBeNull();
    req.flush(created);
  });

  it('ne ferme pas le dialogue quand le serveur refuse la création', () => {
    component.form.controls.subject.setValue('Sujet');
    component.submit();
    http.expectOne(base).flush({ title: 'duplicate' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('n\'envoie rien tant que le formulaire est invalide', () => {
    component.submit();
    http.expectNone(base);
    expect(component.form.controls.subject.touched).toBeTrue();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
