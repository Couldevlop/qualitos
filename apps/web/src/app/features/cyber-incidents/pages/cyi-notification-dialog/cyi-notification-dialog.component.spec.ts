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
import { CyiNotificationDialogComponent, CyiNotificationMode } from './cyi-notification-dialog.component';

/**
 * Les trois notifications CSIRT de l'article 23 (24h / 72h / 1 mois) partagent
 * ce formulaire : c'est le mode reçu qui décide de la route appelée et du texte
 * affiché. Une erreur d'aiguillage enverrait un rapport final à la place d'une
 * alerte initiale — invisible à l'écran, grave au regard du régulateur.
 */
describe('CyiNotificationDialogComponent', () => {
  let component: CyiNotificationDialogComponent;
  let fixture: ComponentFixture<CyiNotificationDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CyiNotificationDialogComponent, CyiView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/nis2/cyber-incidents/i-1`;

  function local(offsetMs: number): string {
    const d = new Date(Date.now() + offsetMs);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  async function build(mode: CyiNotificationMode): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CyiNotificationDialogComponent, CyiView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CyiNotificationDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { id: 'i-1', mode } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CyiNotificationDialogComponent);
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

  it('nomme le délai réglementaire correspondant au mode', async () => {
    await build('EARLY_WARNING');
    expect(component.title).toContain('24h');
    expect(component.hint).toContain('24h');

    await build('INITIAL_ASSESSMENT');
    expect(component.title).toContain('72h');
    expect(component.hint).toContain('72h');

    await build('FINAL_REPORT');
    expect(component.title).toContain('1 mois');
    expect(component.hint).toContain('1 mois');
  });

  it('aiguille chaque mode vers sa propre route', async () => {
    const routes: Array<[CyiNotificationMode, string]> = [
      ['EARLY_WARNING', 'early-warning'],
      ['INITIAL_ASSESSMENT', 'initial-assessment'],
      ['FINAL_REPORT', 'final-report']
    ];

    for (const [mode, path] of routes) {
      await build(mode);
      // Date explicite : la valeur par défaut du champ est calculée en UTC puis
      // relue en heure locale, ce qui la rend « future » à l'ouest de Greenwich.
      component.form.patchValue({ reference: 'CSIRT-FR-2026-A14', sentAt: local(-3600000) });

      component.submit();

      const req = http.expectOne(`${base}/${path}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.reference).toBe('CSIRT-FR-2026-A14');
      expect(req.request.body.sentAt).toMatch(/Z$/);
      req.flush({ id: 'i-1' } as CyiView);
      expect(dialogRef.close).toHaveBeenCalled();
    }
  });

  it('exige la référence remise par le CSIRT', async () => {
    await build('EARLY_WARNING');

    component.submit();

    expect(component.form.controls.reference.touched).toBeTrue();
    http.expectNone(`${base}/early-warning`);
  });

  it('refuse un envoi daté dans le futur', async () => {
    await build('EARLY_WARNING');
    component.form.patchValue({ reference: 'CSIRT-1', sentAt: local(3600000) });

    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('La date d\'envoi ne peut pas être dans le futur.');
    http.expectNone(`${base}/early-warning`);
  });

  it('normalise la référence saisie avec des espaces', async () => {
    await build('FINAL_REPORT');
    component.form.patchValue({ reference: '  CSIRT-FR-2026-A14/3  ', sentAt: local(-3600000) });

    component.submit();

    const req = http.expectOne(`${base}/final-report`);
    expect(req.request.body.reference).toBe('CSIRT-FR-2026-A14/3');
    req.flush({ id: 'i-1' } as CyiView);
  });

  it('affiche un message sûr quand le serveur refuse la notification', async () => {
    await build('EARLY_WARNING');
    component.form.patchValue({ reference: 'CSIRT-1', sentAt: local(-3600000) });

    component.submit();
    http.expectOne(`${base}/early-warning`).flush({}, { status: 409, statusText: 'Conflict' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build('EARLY_WARNING');
    component.form.patchValue({ reference: 'CSIRT-1', sentAt: local(-3600000) });
    component.submit();
    const inflight = http.expectOne(`${base}/early-warning`);

    component.submit();

    http.expectNone(`${base}/early-warning`);
    inflight.flush({ id: 'i-1' } as CyiView);
  });

  it('ferme sans rien envoyer quand la saisie est annulée', async () => {
    await build('EARLY_WARNING');

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(`${base}/early-warning`);
  });
});
