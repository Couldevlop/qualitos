import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { Nis2MeasureView } from '../../nis2m.types';
import { Nis2mEditDialogComponent } from './nis2m-edit-dialog.component';

/**
 * L'édition repart de la mesure existante : les listes (preuves, rattachements)
 * sont présentées une par ligne et doivent revenir au serveur sous forme de
 * tableaux — un aller-retour qui perdrait des lignes effacerait des preuves.
 */
describe('Nis2mEditDialogComponent', () => {
  let component: Nis2mEditDialogComponent;
  let fixture: ComponentFixture<Nis2mEditDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<Nis2mEditDialogComponent, Nis2MeasureView>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/nis2/risk-measures/m-1`;
  const OWNER = '11111111-1111-1111-1111-111111111111';
  const ACTIVITY = '44444444-4444-4444-8444-444444444444';
  const AGREEMENT = '55555555-5555-4555-8555-555555555555';

  const row = (over: Partial<Nis2MeasureView> = {}): Nis2MeasureView => ({
    id: 'm-1', tenantId: 't-1', reference: 'NIS2-MFA-001',
    category: 'MFA_AND_COMMUNICATIONS', title: 'MFA obligatoire',
    description: 'FIDO2 sur Keycloak.', status: 'VERIFIED',
    ownerUserId: OWNER, maturityLevel: 4, residualRiskRating: 'LOW',
    criticalRiskJustification: null, reviewIntervalDays: 180,
    effectiveFrom: null, effectiveTo: null,
    lastReviewedAt: null, reviewedByUserId: null, nextReviewDueAt: null,
    evidenceUrls: ['https://wiki.local/a.pdf', 'https://wiki.local/b.pdf'],
    linkedProcessingActivityIds: [ACTIVITY], linkedProcessorAgreementIds: [AGREEMENT],
    notes: 'Note existante.', createdByUserId: 'u-1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    reviewOverdue: false, criticalResidualRisk: false,
    ...over
  });

  async function build(data: Nis2MeasureView): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<Nis2mEditDialogComponent, Nis2MeasureView>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [Nis2mEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { row: data } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(Nis2mEditDialogComponent);
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

  it('pré-remplit le formulaire avec la mesure existante', async () => {
    await build(row());

    expect(component.form.controls.title.value).toBe('MFA obligatoire');
    expect(component.form.controls.maturityLevel.value).toBe(4);
    expect(component.form.controls.reviewIntervalDays.value).toBe(180);
    expect(component.form.controls.ownerUserId.value).toBe(OWNER);
    expect(component.form.controls.evidenceUrlsRaw.value)
      .toBe('https://wiki.local/a.pdf\nhttps://wiki.local/b.pdf');
    expect(component.form.controls.linkedActivitiesRaw.value).toBe(ACTIVITY);
    expect(component.form.valid).toBeTrue();
  });

  it('tolère une mesure sans description, notes ni rattachement', async () => {
    await build(row({
      description: null, notes: null, ownerUserId: null, criticalRiskJustification: null,
      evidenceUrls: null, linkedProcessingActivityIds: null, linkedProcessorAgreementIds: null
    }));

    expect(component.form.controls.description.value).toBe('');
    expect(component.form.controls.evidenceUrlsRaw.value).toBe('');
    expect(component.form.valid).toBeTrue();
  });

  it('rejette une preuve qui n\'est pas une URL http(s)', async () => {
    await build(row());
    component.form.controls.evidenceUrlsRaw.setValue('https://ok.local/a.pdf\nfichier-local.pdf');

    expect(component.form.controls.evidenceUrlsRaw.hasError('lines')).toBeTrue();
  });

  it('rejette un rattachement qui n\'est pas un UUID', async () => {
    await build(row());
    component.form.controls.linkedAgreementsRaw.setValue('DPA-2026-01');

    expect(component.form.controls.linkedAgreementsRaw.hasError('lines')).toBeTrue();
  });

  it('n\'envoie rien quand le titre a été effacé', async () => {
    await build(row());
    component.form.controls.title.setValue('');

    component.submit();

    expect(component.form.controls.title.touched).toBeTrue();
    http.expectNone(url);
  });

  it('exige une justification écrite pour un risque résiduel CRITICAL', async () => {
    await build(row());
    component.form.controls.residualRiskRating.setValue('CRITICAL');

    expect(component.isCritical()).toBeTrue();
    component.submit();

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Justification obligatoire pour un risque résiduel CRITICAL.');
    http.expectNone(url);
  });

  it('renvoie les listes reconstruites et les textes normalisés', async () => {
    await build(row());
    component.form.patchValue({
      title: '  MFA étendu aux prestataires  ',
      description: '   ',
      evidenceUrlsRaw: 'https://wiki.local/a.pdf\n\n  https://wiki.local/c.pdf  ',
      notes: '  Revu en comité.  '
    });

    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.title).toBe('MFA étendu aux prestataires');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.evidenceUrls).toEqual(['https://wiki.local/a.pdf', 'https://wiki.local/c.pdf']);
    expect(req.request.body.linkedProcessingActivityIds).toEqual([ACTIVITY]);
    expect(req.request.body.linkedProcessorAgreementIds).toEqual([AGREEMENT]);
    expect(req.request.body.notes).toBe('Revu en comité.');
    req.flush(row());
  });

  it('rend la mesure mise à jour au composant appelant', async () => {
    const updated = row({ title: 'MFA étendu' });
    await build(row());

    component.submit();
    http.expectOne(url).flush(updated);

    expect(snack.open).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('affiche un message sûr quand le serveur refuse la mise à jour', async () => {
    await build(row());

    component.submit();
    http.expectOne(url).flush({}, { status: 409, statusText: 'Conflict' });

    expect(snack.open.calls.mostRecent().args[0] as string).toBe('État incompatible — rechargez la page.');
    expect(component.submitting).toBeFalse();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build(row());
    component.submit();
    const inflight = http.expectOne(url);

    component.submit();

    http.expectNone(url);
    inflight.flush(row());
  });

  it('ferme sans rien envoyer quand la saisie est annulée', async () => {
    await build(row());

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
