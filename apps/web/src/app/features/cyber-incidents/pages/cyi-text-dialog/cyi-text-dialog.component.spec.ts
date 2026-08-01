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
import { CyiTextDialogComponent } from './cyi-text-dialog.component';

/**
 * Clôture et rejet partagent ce formulaire mais n'ont pas la même exigence :
 * un rejet sans motif effacerait la traçabilité d'un incident écarté, alors
 * qu'une clôture sans notes reste légitime.
 */
describe('CyiTextDialogComponent', () => {
  let component: CyiTextDialogComponent;
  let fixture: ComponentFixture<CyiTextDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CyiTextDialogComponent, CyiView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents/i-1`;

  async function build(mode: 'CLOSE' | 'REJECT'): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CyiTextDialogComponent, CyiView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CyiTextDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { id: 'i-1', mode } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CyiTextDialogComponent);
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

  it('distingue clôture et rejet dans son intitulé', async () => {
    await build('CLOSE');
    expect(component.title).toContain('Clôturer');
    expect(component.hint).toContain('définitive');

    await build('REJECT');
    expect(component.title).toContain('Rejeter');
    expect(component.hint).toContain('faux positif');
  });

  it('exige un motif pour rejeter un incident', async () => {
    await build('REJECT');
    expect(component.form.controls.text.hasError('required')).toBeTrue();

    component.submit();

    expect(component.form.controls.text.touched).toBeTrue();
    http.expectNone(`${base}/reject`);
  });

  it('accepte une clôture sans notes', async () => {
    await build('CLOSE');
    expect(component.form.valid).toBeTrue();

    component.submit();

    const req = http.expectOne(`${base}/close`);
    expect(req.request.body.closureNotes).toBeUndefined();
    req.flush({ id: 'i-1' } as CyiView);
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('envoie les notes de clôture normalisées', async () => {
    await build('CLOSE');
    component.form.controls.text.setValue('  RETEX diffusé au COMEX.  ');

    component.submit();

    const req = http.expectOne(`${base}/close`);
    expect(req.request.body).toEqual({ closureNotes: 'RETEX diffusé au COMEX.' });
    req.flush({ id: 'i-1' } as CyiView);
    expect(snack.open.calls.mostRecent().args[0] as string).toBe('Incident clôturé.');
  });

  it('envoie le motif de rejet sur la route dédiée', async () => {
    await build('REJECT');
    component.form.controls.text.setValue('Doublon de NIS2-INC-2026-002.');

    component.submit();

    const req = http.expectOne(`${base}/reject`);
    expect(req.request.body).toEqual({ reason: 'Doublon de NIS2-INC-2026-002.' });
    req.flush({ id: 'i-1' } as CyiView);
    expect(snack.open.calls.mostRecent().args[0] as string).toBe('Incident rejeté.');
  });

  it('affiche un message sûr quand le serveur refuse l\'opération', async () => {
    await build('CLOSE');

    component.submit();
    http.expectOne(`${base}/close`).flush({}, { status: 409, statusText: 'Conflict' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build('CLOSE');
    component.submit();
    const inflight = http.expectOne(`${base}/close`);

    component.submit();

    http.expectNone(`${base}/close`);
    inflight.flush({ id: 'i-1' } as CyiView);
  });

  it('ferme sans rien envoyer quand la saisie est annulée', async () => {
    await build('REJECT');

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(`${base}/reject`);
  });
});
