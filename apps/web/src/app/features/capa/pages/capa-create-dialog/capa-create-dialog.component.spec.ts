import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaCaseResponse } from '../../capa.types';
import { CapaCreateDialogComponent } from './capa-create-dialog.component';

/**
 * §18.2 #2 — le propriétaire du cas vient du JWT, jamais d'une saisie : si la
 * session est perdue, la création doit être refusée côté client plutôt que de
 * partir sans propriétaire et de revenir en 400.
 */
describe('CapaCreateDialogComponent', () => {
  let component: CapaCreateDialogComponent;
  let fixture: ComponentFixture<CapaCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaCreateDialogComponent, CapaCaseResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;
  const OWNER = '11111111-1111-1111-1111-111111111111';

  const created: CapaCaseResponse = {
    id: 'c1', tenantId: 't1', title: 'Recalibration', type: 'CORRECTIVE',
    criticity: 'MEDIUM', status: 'OPEN', sourceType: 'INTERNAL', ownerId: OWNER,
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z', actions: []
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: OWNER, tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaCreateDialogComponent, CapaCaseResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [CapaCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('propose des valeurs par défaut exploitables sans saisie supplémentaire', () => {
    expect(component.form.controls.type.value).toBe('CORRECTIVE');
    expect(component.form.controls.criticity.value).toBe('MEDIUM');
    expect(component.form.controls.sourceType.value).toBe('INTERNAL');
    expect(component.types).toEqual(['CORRECTIVE', 'PREVENTIVE']);
    expect(component.criticities).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
  });

  it('couvre les six origines de cas prévues par le modèle', () => {
    expect(component.sourceTypes.map(s => s.value))
      .toEqual(['NON_CONFORMITY', 'AUDIT', 'COMPLAINT', 'INTERNAL', 'IOT_ALERT', 'OTHER']);
    expect(component.sourceTypes.every(s => !!s.label)).toBeTrue();
  });

  it('exige un titre et n\'envoie rien tant qu\'il manque', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(base);
    expect(component.form.controls.title.touched).toBeTrue();
    expect(component.submitting).toBeFalse();
  });

  it('refuse un titre au-delà de 255 caractères (contrainte serveur rejouée)', () => {
    component.form.controls.title.setValue('x'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(base);
  });

  it('bloque la création quand la session a expiré plutôt que d\'envoyer un cas sans propriétaire', () => {
    currentUser = null;
    component.form.controls.title.setValue('Recalibration');
    component.submit();
    http.expectNone(base);
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('normalise les champs texte et omet les optionnels vides', () => {
    component.form.patchValue({
      title: '  Recalibration robot  ',
      description: '   ',
      type: 'PREVENTIVE',
      criticity: 'CRITICAL',
      sourceType: 'AUDIT',
      sourceRef: '  AUD-2026-Q2  ',
      dueDate: ''
    });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Recalibration robot');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.sourceRef).toBe('AUD-2026-Q2');
    expect(req.request.body.dueDate).toBeUndefined();
    expect(req.request.body.type).toBe('PREVENTIVE');
    expect(req.request.body.criticity).toBe('CRITICAL');
    expect(req.request.body.sourceType).toBe('AUDIT');
    // Le propriétaire provient du JWT, pas du formulaire.
    expect(req.request.body.ownerId).toBe(OWNER);

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet l\'échéance quand elle est saisie', () => {
    component.form.patchValue({ title: 'Cas daté', dueDate: '2026-12-31' });
    component.submit();
    const req = http.expectOne(base);
    expect(req.request.body.dueDate).toBe('2026-12-31');
    req.flush(created);
  });

  it('garde le dialogue ouvert et le formulaire réutilisable quand le serveur refuse', () => {
    component.form.controls.title.setValue('Recalibration');
    component.submit();
    http.expectOne(base).flush({ title: 'invalid' }, { status: 400, statusText: 'Bad Request' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    expect(component.form.controls.title.value).toBe('Recalibration');
  });

  it('n\'envoie pas deux fois la même création sur un double clic', () => {
    component.form.controls.title.setValue('Recalibration');
    component.submit();
    const req = http.expectOne(base);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(base);

    req.flush(created);
  });

  it('ferme le dialogue sans rien créer à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(base);
  });
});
