import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CyiView } from '../../cyi.types';
import { CyiMitigateDialogComponent } from './cyi-mitigate-dialog.component';

/**
 * Les mesures d'endiguement sont la preuve NIS 2 que l'incident a été traité :
 * elles sont obligatoires, et l'agent qui les consigne est enregistré.
 */
describe('CyiMitigateDialogComponent', () => {
  let component: CyiMitigateDialogComponent;
  let fixture: ComponentFixture<CyiMitigateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CyiMitigateDialogComponent, CyiView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents/i-1/mitigate`;
  const USER = '11111111-1111-1111-1111-111111111111';

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: USER, tenantId: 't-1', displayName: 'RSSI', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<CyiMitigateDialogComponent, CyiView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CyiMitigateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        { provide: MAT_DIALOG_DATA, useValue: { id: 'i-1' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CyiMitigateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige les mesures d\'endiguement avant tout envoi', () => {
    component.submit();

    expect(component.form.controls.containmentMeasures.touched).toBeTrue();
    http.expectNone(url);
  });

  it('consigne les mesures, l\'impact et l\'agent qui les enregistre', () => {
    component.form.patchValue({
      containmentMeasures: '  Poste isolé, comptes réinitialisés.  ',
      impactDescription: '  Aucun fichier exfiltré.  '
    });

    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      containmentMeasures: 'Poste isolé, comptes réinitialisés.',
      impactDescription: 'Aucun fichier exfiltré.',
      handledByUserId: USER
    });
    req.flush({ id: 'i-1' } as CyiView);
  });

  it('n\'invente pas d\'impact quand le champ est laissé vide', () => {
    component.form.controls.containmentMeasures.setValue('Isolation réseau.');

    component.submit();

    const req = http.expectOne(url);
    expect(req.request.body.impactDescription).toBeUndefined();
    req.flush({ id: 'i-1' } as CyiView);
  });

  it('n\'invente pas d\'agent quand la session a expiré', () => {
    currentUser = null;
    component.form.controls.containmentMeasures.setValue('Isolation réseau.');

    component.submit();

    const req = http.expectOne(url);
    expect(req.request.body.handledByUserId).toBeUndefined();
    req.flush({ id: 'i-1' } as CyiView);
  });

  it('rend l\'incident endigué au composant appelant', () => {
    const updated = { id: 'i-1', status: 'MITIGATED' } as CyiView;
    component.form.controls.containmentMeasures.setValue('Isolation réseau.');

    component.submit();
    http.expectOne(url).flush(updated);

    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('affiche un message sûr quand le serveur refuse la transition', () => {
    component.form.controls.containmentMeasures.setValue('Isolation réseau.');

    component.submit();
    http.expectOne(url).flush({}, { status: 409, statusText: 'Conflict' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.containmentMeasures.setValue('Isolation réseau.');
    component.submit();
    const inflight = http.expectOne(url);

    component.submit();

    http.expectNone(url);
    inflight.flush({ id: 'i-1' } as CyiView);
  });

  it('ferme sans rien envoyer quand la saisie est annulée', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
