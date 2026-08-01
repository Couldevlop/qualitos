import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CyiView } from '../../cyi.types';
import { CyiDetectDialogComponent } from './cyi-detect-dialog.component';

/**
 * Le formulaire rejoue les contraintes du serveur (format de référence, dates
 * cohérentes) pour expliquer un refus pendant la saisie plutôt qu'après un 400,
 * et refuse de signaler sans session — l'identité du déclarant est une preuve
 * NIS 2, elle ne peut pas être inventée côté client.
 */
describe('CyiDetectDialogComponent', () => {
  let component: CyiDetectDialogComponent;
  let fixture: ComponentFixture<CyiDetectDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CyiDetectDialogComponent, CyiView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const endpoint = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents`;
  const USER = '11111111-1111-1111-1111-111111111111';

  /** Format attendu par un `<input type="datetime-local">`. */
  function local(offsetMs: number): string {
    const d = new Date(Date.now() + offsetMs);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function fillValid(): void {
    component.form.patchValue({
      reference: 'NIS2-INC-2026-042',
      title: '  Exfiltration suspectée  ',
      severity: 'HIGH',
      incidentType: 'DATA_BREACH',
      detectedAt: local(-3600000),
      occurredAt: local(-7200000),
      estimatedAffectedUsers: 120
    });
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: USER, tenantId: 't-1', displayName: 'RSSI', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<CyiDetectDialogComponent, CyiView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CyiDetectDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CyiDetectDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  // ---- Validation ---------------------------------------------------------------

  it('démarre sur un incident non significatif, sans échéance CSIRT', () => {
    expect(component.form.controls.severity.value).toBe('MEDIUM');
    expect(component.isSignificant()).toBeFalse();
  });

  it('annonce le déclenchement des échéances dès la sévérité HIGH', () => {
    component.form.controls.severity.setValue('HIGH');
    expect(component.isSignificant()).toBeTrue();

    component.form.controls.severity.setValue('CRITICAL');
    expect(component.isSignificant()).toBeTrue();

    component.form.controls.severity.setValue('LOW');
    expect(component.isSignificant()).toBeFalse();
  });

  it('impose le format de référence attendu par le serveur', () => {
    component.form.controls.reference.setValue('nis2-inc-1');
    expect(component.form.controls.reference.hasError('pattern')).toBeTrue();

    component.form.controls.reference.setValue('NIS2-INC-2026-042');
    expect(component.form.controls.reference.valid).toBeTrue();
  });

  it('n\'accepte comme violation liée qu\'un UUID, ou rien', () => {
    component.form.controls.linkedBreachId.setValue('pas-un-uuid');
    expect(component.form.controls.linkedBreachId.valid).toBeFalse();

    component.form.controls.linkedBreachId.setValue('');
    expect(component.form.controls.linkedBreachId.valid).toBeTrue();

    component.form.controls.linkedBreachId.setValue('22222222-2222-4222-8222-222222222222');
    expect(component.form.controls.linkedBreachId.valid).toBeTrue();
  });

  it('refuse un nombre d\'utilisateurs impactés négatif', () => {
    component.form.controls.estimatedAffectedUsers.setValue(-1);
    expect(component.form.controls.estimatedAffectedUsers.hasError('min')).toBeTrue();
  });

  // ---- Garde-fous avant envoi -----------------------------------------------------

  it('n\'envoie rien tant que le formulaire est incomplet', () => {
    component.submit();

    expect(component.form.controls.reference.touched).toBeTrue();
    http.expectNone(endpoint);
  });

  it('refuse une détection datée dans le futur', () => {
    fillValid();
    component.form.controls.detectedAt.setValue(local(3600000));

    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('La date de détection ne peut pas être dans le futur.');
    http.expectNone(endpoint);
  });

  it('refuse une survenue postérieure à la détection', () => {
    fillValid();
    component.form.controls.occurredAt.setValue(local(-60000));
    component.form.controls.detectedAt.setValue(local(-3600000));

    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('La date de survenue doit être ≤ à la date de détection.');
    http.expectNone(endpoint);
  });

  it('refuse de signaler sans session valide', () => {
    fillValid();
    currentUser = null;

    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Session expirée — veuillez vous reconnecter.');
    http.expectNone(endpoint);
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    fillValid();
    component.submit();
    const inflight = http.expectOne(endpoint);

    component.submit();

    http.expectNone(endpoint);
    inflight.flush({ id: 'i-1' } as CyiView);
  });

  // ---- Envoi ----------------------------------------------------------------------

  it('normalise la saisie avant de l\'envoyer au registre', () => {
    fillValid();
    component.form.patchValue({
      description: '  Chiffrement détecté par l\'EDR.  ',
      affectedAssets: 'LAPTOP-RH-042\n\n  SRV-FILE-01  ',
      affectedServices: 'Partage RH'
    });

    component.submit();

    const req = http.expectOne(endpoint);
    const body = req.request.body;
    expect(req.request.method).toBe('POST');
    expect(body.title).toBe('Exfiltration suspectée');
    expect(body.description).toBe('Chiffrement détecté par l\'EDR.');
    expect(body.affectedAssets).toEqual(['LAPTOP-RH-042', 'SRV-FILE-01']);
    expect(body.affectedServices).toEqual(['Partage RH']);
    expect(body.detectedAt).toMatch(/Z$/);
    expect(body.reportedByUserId).toBe(USER);
    req.flush({ id: 'i-1' } as CyiView);
  });

  it('omet les champs optionnels laissés vides', () => {
    fillValid();
    component.form.controls.occurredAt.setValue('');

    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.occurredAt).toBeUndefined();
    expect(req.request.body.linkedBreachId).toBeUndefined();
    req.flush({ id: 'i-1' } as CyiView);
  });

  it('rend l\'incident créé au composant appelant', () => {
    const created = { id: 'i-neuf' } as CyiView;
    fillValid();

    component.submit();
    http.expectOne(endpoint).flush(created);

    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('affiche un message sûr et rouvre la saisie quand la référence est déjà prise', () => {
    fillValid();

    component.submit();
    http.expectOne(endpoint).flush({ title: 'duplicate key incidents_reference_key' },
      { status: 409, statusText: 'Conflict' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ferme sans rien envoyer quand la saisie est annulée', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(endpoint);
  });
});
