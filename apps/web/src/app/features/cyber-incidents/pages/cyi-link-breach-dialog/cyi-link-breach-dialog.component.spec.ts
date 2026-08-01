import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CyiView } from '../../cyi.types';
import { CyiLinkBreachDialogComponent } from './cyi-link-breach-dialog.component';

/**
 * Un incident cyber NIS 2 et une violation de données RGPD sont deux dossiers
 * distincts qu'un même événement peut déclencher : le rattachement se fait par
 * l'identifiant du registre RGPD, dont le format est vérifié avant l'envoi.
 */
describe('CyiLinkBreachDialogComponent', () => {
  let component: CyiLinkBreachDialogComponent;
  let fixture: ComponentFixture<CyiLinkBreachDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CyiLinkBreachDialogComponent, CyiView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents/i-1/link-breach`;
  const BREACH = '22222222-2222-4222-8222-222222222222';

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<CyiLinkBreachDialogComponent, CyiView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CyiLinkBreachDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { id: 'i-1' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CyiLinkBreachDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige un identifiant de violation au format UUID', () => {
    expect(component.form.controls.breachId.hasError('required')).toBeTrue();

    component.form.controls.breachId.setValue('12345');
    expect(component.form.controls.breachId.hasError('pattern')).toBeTrue();

    component.form.controls.breachId.setValue(BREACH);
    expect(component.form.valid).toBeTrue();
  });

  it('n\'envoie rien tant que l\'identifiant est invalide', () => {
    component.form.controls.breachId.setValue('12345');

    component.submit();

    expect(component.form.controls.breachId.touched).toBeTrue();
    http.expectNone(url);
  });

  it('rattache la violation et rend l\'incident mis à jour', () => {
    const updated = { id: 'i-1', linkedBreachId: BREACH } as CyiView;
    component.form.controls.breachId.setValue(`  ${BREACH}  `);

    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ breachId: BREACH });
    req.flush(updated);
    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('affiche un message sûr quand la violation visée n\'existe pas', () => {
    component.form.controls.breachId.setValue(BREACH);

    component.submit();
    http.expectOne(url).flush({}, { status: 404, statusText: 'Not Found' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('Liaison impossible.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.breachId.setValue(BREACH);
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
