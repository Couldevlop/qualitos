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
import { Nis2MeasureView } from '../../nis2m.types';
import { Nis2mPlanDialogComponent } from './nis2m-plan-dialog.component';

/**
 * Le formulaire rejoue les règles du serveur : référence normalisée, intervalle
 * de revue borné, et surtout justification obligatoire d'un risque résiduel
 * CRITICAL — l'article 21 impose que ce choix soit motivé devant la direction.
 */
describe('Nis2mPlanDialogComponent', () => {
  let component: Nis2mPlanDialogComponent;
  let fixture: ComponentFixture<Nis2mPlanDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<Nis2mPlanDialogComponent, Nis2MeasureView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const endpoint = `${environment.apiBaseUrl}/api/v1/nis2/risk-measures`;
  const USER = '11111111-1111-1111-1111-111111111111';
  const UUID = '44444444-4444-4444-8444-444444444444';

  function fillValid(): void {
    component.form.patchValue({
      reference: 'NIS2-CRYPTO-004',
      title: '  Chiffrement des sauvegardes  ',
      category: 'CRYPTOGRAPHY'
    });
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: USER, tenantId: 't-1', displayName: 'RSSI', roles: ['quality_manager'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<Nis2mPlanDialogComponent, Nis2MeasureView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [Nis2mPlanDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Nis2mPlanDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  // ---- Validation ---------------------------------------------------------------

  it('propose les dix catégories de l\'article 21 et un risque par défaut modéré', () => {
    expect(component.categories.length).toBe(10);
    expect(component.risks).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
    expect(component.form.controls.residualRiskRating.value).toBe('MEDIUM');
    expect(component.isCritical()).toBeFalse();
  });

  it('impose le format de référence attendu par le serveur', () => {
    component.form.controls.reference.setValue('nis2-crypto');
    expect(component.form.controls.reference.hasError('pattern')).toBeTrue();

    component.form.controls.reference.setValue('NIS2-CRYPTO-004');
    expect(component.form.controls.reference.valid).toBeTrue();
  });

  it('borne la maturité entre 1 et 5', () => {
    component.form.controls.maturityLevel.setValue(0);
    expect(component.form.controls.maturityLevel.hasError('min')).toBeTrue();

    component.form.controls.maturityLevel.setValue(6);
    expect(component.form.controls.maturityLevel.hasError('max')).toBeTrue();
  });

  it('borne l\'intervalle de revue entre 30 jours et 3 ans', () => {
    component.form.controls.reviewIntervalDays.setValue(29);
    expect(component.form.controls.reviewIntervalDays.hasError('min')).toBeTrue();

    component.form.controls.reviewIntervalDays.setValue(1096);
    expect(component.form.controls.reviewIntervalDays.hasError('max')).toBeTrue();
  });

  it('n\'accepte comme preuve qu\'une URL http(s) par ligne', () => {
    component.form.controls.evidenceUrlsRaw.setValue('https://wiki.local/a.pdf\nftp://interdit');
    expect(component.form.controls.evidenceUrlsRaw.hasError('lines')).toBeTrue();

    component.form.controls.evidenceUrlsRaw.setValue('https://wiki.local/a.pdf\n\nhttp://wiki.local/b.pdf');
    expect(component.form.controls.evidenceUrlsRaw.valid).toBeTrue();
  });

  it('n\'accepte comme rattachement qu\'un UUID par ligne', () => {
    component.form.controls.linkedActivitiesRaw.setValue('pas-un-uuid');
    expect(component.form.controls.linkedActivitiesRaw.hasError('lines')).toBeTrue();

    component.form.controls.linkedActivitiesRaw.setValue(UUID);
    expect(component.form.controls.linkedActivitiesRaw.valid).toBeTrue();
  });

  it('n\'accepte comme propriétaire qu\'un UUID, ou rien', () => {
    component.form.controls.ownerUserId.setValue('quelqu\'un');
    expect(component.form.controls.ownerUserId.valid).toBeFalse();

    component.form.controls.ownerUserId.setValue('');
    expect(component.form.controls.ownerUserId.valid).toBeTrue();
  });

  // ---- Garde-fous avant envoi -----------------------------------------------------

  it('n\'envoie rien tant que le formulaire est incomplet', () => {
    component.submit();

    expect(component.form.controls.reference.touched).toBeTrue();
    http.expectNone(endpoint);
  });

  it('exige une justification écrite pour un risque résiduel CRITICAL', () => {
    fillValid();
    component.form.controls.residualRiskRating.setValue('CRITICAL');
    component.form.controls.criticalRiskJustification.setValue('   ');

    expect(component.isCritical()).toBeTrue();
    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Justification obligatoire pour un risque résiduel CRITICAL.');
    http.expectNone(endpoint);
  });

  it('refuse de planifier sans session valide', () => {
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
    inflight.flush({ id: 'm-1' } as Nis2MeasureView);
  });

  // ---- Envoi ----------------------------------------------------------------------

  it('normalise la saisie avant de l\'envoyer au registre', () => {
    fillValid();
    component.form.patchValue({
      description: '  Sauvegardes chiffrées AES-256.  ',
      ownerUserId: UUID,
      maturityLevel: 3,
      reviewIntervalDays: 90,
      evidenceUrlsRaw: 'https://wiki.local/a.pdf\n\n  https://wiki.local/b.pdf  ',
      linkedActivitiesRaw: UUID,
      notes: '  À revoir avec la DSI.  '
    });

    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      reference: 'NIS2-CRYPTO-004',
      category: 'CRYPTOGRAPHY',
      title: 'Chiffrement des sauvegardes',
      description: 'Sauvegardes chiffrées AES-256.',
      ownerUserId: UUID,
      maturityLevel: 3,
      residualRiskRating: 'MEDIUM',
      criticalRiskJustification: undefined,
      reviewIntervalDays: 90,
      evidenceUrls: ['https://wiki.local/a.pdf', 'https://wiki.local/b.pdf'],
      linkedProcessingActivityIds: [UUID],
      linkedProcessorAgreementIds: [],
      notes: 'À revoir avec la DSI.',
      createdByUserId: USER
    });
    req.flush({ id: 'm-1' } as Nis2MeasureView);
  });

  it('transmet la justification d\'un risque CRITICAL dûment motivé', () => {
    fillValid();
    component.form.patchValue({
      residualRiskRating: 'CRITICAL',
      criticalRiskJustification: '  Hébergeur unique, arbitrage direction en cours.  '
    });

    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.residualRiskRating).toBe('CRITICAL');
    expect(req.request.body.criticalRiskJustification)
      .toBe('Hébergeur unique, arbitrage direction en cours.');
    req.flush({ id: 'm-1' } as Nis2MeasureView);
  });

  it('rend la mesure créée au composant appelant', () => {
    const created = { id: 'm-neuve' } as Nis2MeasureView;
    fillValid();

    component.submit();
    http.expectOne(endpoint).flush(created);

    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('affiche un message sûr et rouvre la saisie quand le serveur refuse', () => {
    fillValid();

    component.submit();
    http.expectOne(endpoint).flush({ title: 'duplicate key measures_reference_key' },
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
