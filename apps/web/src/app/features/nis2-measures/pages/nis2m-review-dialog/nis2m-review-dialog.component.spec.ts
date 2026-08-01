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
import { Nis2MeasureView } from '../../nis2m.types';
import { Nis2mReviewDialogComponent } from './nis2m-review-dialog.component';

/**
 * Première vérification et revue périodique partagent ce formulaire : le mode
 * décide de la route, et l'identité du réviseur vient de la session — c'est
 * elle qui fait foi comme preuve de revue au sens de l'article 21.
 */
describe('Nis2mReviewDialogComponent', () => {
  let component: Nis2mReviewDialogComponent;
  let fixture: ComponentFixture<Nis2mReviewDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<Nis2mReviewDialogComponent, Nis2MeasureView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/nis2/risk-measures/m-1`;
  const USER = '11111111-1111-1111-1111-111111111111';

  function local(offsetMs: number): string {
    const d = new Date(Date.now() + offsetMs);
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  async function build(mode: 'VERIFY' | 'REVIEW'): Promise<void> {
    currentUser = { userId: USER, tenantId: 't-1', displayName: 'RSSI', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<Nis2mReviewDialogComponent, Nis2MeasureView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [Nis2mReviewDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        { provide: MAT_DIALOG_DATA, useValue: { id: 'm-1', mode } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Nis2mReviewDialogComponent);
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

  it('distingue la vérification initiale de la revue périodique dans son intitulé', async () => {
    await build('VERIFY');
    expect(component.title).toContain('VERIFIED');
    expect(component.hint).toContain('testée');

    await build('REVIEW');
    expect(component.title).toContain('revue périodique');
    expect(component.hint).toContain('recalculée');
  });

  it('aiguille chaque mode vers sa propre route', async () => {
    const routes: Array<['VERIFY' | 'REVIEW', string]> = [['VERIFY', 'verify'], ['REVIEW', 'review']];

    for (const [mode, path] of routes) {
      await build(mode);
      // Date explicite : la valeur par défaut du champ est calculée en UTC puis
      // relue en heure locale, ce qui la rend « future » à l'ouest de Greenwich.
      component.form.controls.reviewedAt.setValue(local(-3600000));

      component.submit();

      const req = http.expectOne(`${base}/${path}`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body.reviewedByUserId).toBe(USER);
      expect(req.request.body.reviewedAt).toMatch(/Z$/);
      req.flush({ id: 'm-1' } as Nis2MeasureView);
      expect(dialogRef.close).toHaveBeenCalled();
      expect(snack.open).toHaveBeenCalled();
    }
  });

  it('refuse une revue datée dans le futur', async () => {
    await build('REVIEW');
    component.form.controls.reviewedAt.setValue(local(3600000));

    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('La date de revue ne peut pas être dans le futur.');
    http.expectNone(`${base}/review`);
  });

  it('refuse d\'enregistrer une revue sans session valide', async () => {
    await build('REVIEW');
    component.form.controls.reviewedAt.setValue(local(-3600000));
    currentUser = null;

    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Session expirée — veuillez vous reconnecter.');
    http.expectNone(`${base}/review`);
  });

  it('n\'envoie rien quand la date de revue a été effacée', async () => {
    await build('VERIFY');
    component.form.controls.reviewedAt.setValue('');

    component.submit();

    expect(component.form.controls.reviewedAt.touched).toBeTrue();
    http.expectNone(`${base}/verify`);
  });

  it('affiche un message sûr quand le serveur refuse la revue', async () => {
    await build('VERIFY');
    component.form.controls.reviewedAt.setValue(local(-3600000));

    component.submit();
    http.expectOne(`${base}/verify`).flush({}, { status: 409, statusText: 'Conflict' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build('VERIFY');
    component.form.controls.reviewedAt.setValue(local(-3600000));
    component.submit();
    const inflight = http.expectOne(`${base}/verify`);

    component.submit();

    http.expectNone(`${base}/verify`);
    inflight.flush({ id: 'm-1' } as Nis2MeasureView);
  });

  it('ferme sans rien envoyer quand la saisie est annulée', async () => {
    await build('VERIFY');

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(`${base}/verify`);
  });
});
