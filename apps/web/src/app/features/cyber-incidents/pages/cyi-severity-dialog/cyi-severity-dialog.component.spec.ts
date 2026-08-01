import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CyiSeverity, CyiView } from '../../cyi.types';
import { CyiSeverityDialogComponent } from './cyi-severity-dialog.component';

/**
 * Requalifier un incident en HIGH/CRITICAL le rend « significatif » et ouvre
 * les échéances CSIRT : l'écran doit prévenir avant, sans quoi l'utilisateur
 * découvre des retards réglementaires qu'il vient lui-même de déclencher.
 */
describe('CyiSeverityDialogComponent', () => {
  let component: CyiSeverityDialogComponent;
  let fixture: ComponentFixture<CyiSeverityDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CyiSeverityDialogComponent, CyiView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents/i-1/severity`;

  async function build(current: CyiSeverity): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CyiSeverityDialogComponent, CyiView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CyiSeverityDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { id: 'i-1', current } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CyiSeverityDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  beforeEach(() => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('présélectionne la sévérité courante de l\'incident', async () => {
    await build('MEDIUM');

    expect(component.form.controls.severity.value).toBe('MEDIUM');
    expect(component.severities).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
  });

  it('avertit quand la requalification va ouvrir les échéances CSIRT', async () => {
    await build('MEDIUM');
    expect(component.willEnableNotif()).toBeFalse();

    component.form.controls.severity.setValue('HIGH');
    expect(component.willEnableNotif()).toBeTrue();

    component.form.controls.severity.setValue('CRITICAL');
    expect(component.willEnableNotif()).toBeTrue();
  });

  it('n\'avertit pas quand les échéances étaient déjà ouvertes', async () => {
    await build('CRITICAL');
    component.form.controls.severity.setValue('HIGH');

    expect(component.willEnableNotif()).toBeFalse();
  });

  it('n\'avertit pas pour une requalification à la baisse', async () => {
    await build('CRITICAL');
    component.form.controls.severity.setValue('LOW');

    expect(component.willEnableNotif()).toBeFalse();
  });

  it('envoie la nouvelle sévérité et rend l\'incident mis à jour', async () => {
    const updated = { id: 'i-1', severity: 'HIGH' } as CyiView;
    await build('MEDIUM');
    component.form.controls.severity.setValue('HIGH');

    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ severity: 'HIGH' });
    req.flush(updated);
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('affiche un message sûr quand la sévérité est figée côté serveur', async () => {
    await build('MEDIUM');

    component.submit();
    http.expectOne(url).flush({}, { status: 409, statusText: 'Conflict' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build('MEDIUM');
    component.submit();
    const inflight = http.expectOne(url);

    component.submit();

    http.expectNone(url);
    inflight.flush({ id: 'i-1' } as CyiView);
  });

  it('ferme sans rien envoyer quand la saisie est annulée', async () => {
    await build('MEDIUM');

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
