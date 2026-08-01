import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { PdcaCycleResponse } from '../../pdca.types';
import { PdcaCreateDialogComponent } from './pdca-create-dialog.component';

/**
 * Le pilote (ownerId) vient de la session, jamais d'une saisie : un cycle sans
 * pilote identifié n'est pas traçable (§16 rôles, §11.5 audit).
 */
describe('PdcaCreateDialogComponent', () => {
  let component: PdcaCreateDialogComponent;
  let fixture: ComponentFixture<PdcaCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<PdcaCreateDialogComponent, PdcaCycleResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/pdca/cycles`;

  const created: PdcaCycleResponse = {
    id: 'c1', tenantId: 't1', title: 'Réduction des rebuts', status: 'PLAN',
    ownerId: 'u1', createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    steps: []
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: 'u1', tenantId: 't1', displayName: 'QM', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<PdcaCreateDialogComponent, PdcaCycleResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [PdcaCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PdcaCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige un titre et n\'envoie rien tant que le formulaire est invalide', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(base);
    expect(component.form.controls.title.touched).toBeTrue();
    expect(component.submitting).toBeFalse();
  });

  it('refuse un titre au-delà de 255 caractères (limite serveur rejouée à la saisie)', () => {
    component.form.controls.title.setValue('x'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(base);
  });

  it('bloque la création sans session utilisateur au lieu de créer un cycle sans pilote', () => {
    currentUser = null;
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.controls.title.setValue('Cycle sans session');
    component.submit();

    http.expectNone(base);
    expect(snackSpy).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('nettoie les espaces, omet une description vide et joint le pilote de la session', () => {
    component.form.patchValue({ title: '  Réduction des rebuts  ', description: '   ' });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      title: 'Réduction des rebuts', description: undefined, ownerId: 'u1'
    });

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet la description quand elle est renseignée', () => {
    component.form.patchValue({ title: 'Cycle', description: '  Objectif -30% NC  ' });
    component.submit();
    const req = http.expectOne(base);
    expect(req.request.body.description).toBe('Objectif -30% NC');
    req.flush(created);
  });

  it('ignore un second envoi tant que le premier est en vol (anti double-création)', () => {
    component.form.controls.title.setValue('Cycle');
    component.submit();
    component.submit();

    const req = http.expectOne(base);   // un seul appel malgré le double clic
    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledTimes(1);
  });

  it('garde le dialogue ouvert et réarme le bouton quand le serveur refuse', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.controls.title.setValue('Cycle');
    component.submit();
    http.expectOne(base).flush({ title: 'Boom' }, { status: 500, statusText: 'Server Error' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    // message générique : jamais le detail du backend (OWASP A09)
    expect(snackSpy).toHaveBeenCalledWith(
      'Erreur serveur — réessayez dans un instant.', 'OK', { duration: 4000 });
  });

  it('ferme le dialogue sans résultat à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
