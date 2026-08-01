import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { MeasureResponse } from '../../dmaic.types';
import { DmaicMeasureDialogComponent } from './dmaic-measure-dialog.component';

/**
 * Une mesure alimente directement le calcul de capabilité : elle doit porter une
 * valeur numérique et l'opérateur qui l'a relevée (traçabilité §11.5).
 */
describe('DmaicMeasureDialogComponent', () => {
  let component: DmaicMeasureDialogComponent;
  let fixture: ComponentFixture<DmaicMeasureDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DmaicMeasureDialogComponent, MeasureResponse>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const PROJECT_ID = 'dmaic-1';
  const url = `${environment.apiBaseUrl}/api/v1/dmaic/projects/${PROJECT_ID}/measures`;
  const USER = '11111111-1111-1111-1111-111111111111';

  const saved: MeasureResponse = {
    id: 'm1', projectId: PROJECT_ID, value: 10.02, createdAt: '2026-07-02T08:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: USER, tenantId: 't1', displayName: 'Op', roles: ['user'] };
    dialogRef = jasmine.createSpyObj<MatDialogRef<DmaicMeasureDialogComponent, MeasureResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [DmaicMeasureDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { projectId: PROJECT_ID } },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DmaicMeasureDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige une valeur avant tout envoi', () => {
    expect(component.form.controls.value.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.value.touched).toBeTrue();
  });

  it('accepte la valeur zéro, qui est une mesure légitime', () => {
    component.form.controls.value.setValue(0);
    expect(component.form.valid).toBeTrue();
    component.submit();
    const req = http.expectOne(url);
    expect(req.request.body.value).toBe(0);
    req.flush({ ...saved, value: 0 });
  });

  it('refuse un sous-groupe ou une référence trop longs pour le serveur', () => {
    component.form.controls.value.setValue(10);
    component.form.controls.subgroupId.setValue('g'.repeat(101));
    expect(component.form.controls.subgroupId.hasError('maxlength')).toBeTrue();
    component.form.controls.subgroupId.setValue('g1');
    component.form.controls.sourceRef.setValue('r'.repeat(256));
    expect(component.form.controls.sourceRef.hasError('maxlength')).toBeTrue();

    component.submit();
    http.expectNone(url);
  });

  it('joint l\'opérateur du JWT et n\'envoie pas les champs laissés vides', () => {
    component.form.patchValue({
      value: 10.02, subgroupId: '  g1  ', sourceRef: '   ', recordedAt: '2026-07-02T08:00', note: '  RAS  '
    });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.value).toBe(10.02);
    expect(req.request.body.subgroupId).toBe('g1');
    expect(req.request.body.sourceRef).toBeUndefined();
    expect(req.request.body.recordedAt).toBe('2026-07-02T08:00');
    expect(req.request.body.note).toBe('RAS');
    expect(req.request.body.operatorId).toBe(USER);

    req.flush(saved);
    expect(dialogRef.close).toHaveBeenCalledWith(saved);
  });

  it('laisse le serveur horodater quand aucune date n\'est saisie', () => {
    component.form.controls.value.setValue(9.98);
    component.submit();
    const req = http.expectOne(url);
    expect(req.request.body.recordedAt).toBeUndefined();
    req.flush(saved);
  });

  it('ne ferme pas le dialogue quand l\'enregistrement est refusé', () => {
    component.form.controls.value.setValue(10);
    component.submit();
    http.expectOne(url).flush({ title: 'invalid' }, { status: 422, statusText: 'Unprocessable Entity' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
