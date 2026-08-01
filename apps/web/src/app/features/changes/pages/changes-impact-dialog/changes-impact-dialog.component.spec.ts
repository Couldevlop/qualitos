import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ImpactResponse } from '../../changes.types';
import { ChangesImpactDialogComponent } from './changes-impact-dialog.component';

/**
 * Un impact relie la demande à une entité réelle du référentiel transverse
 * (§3.6) : la cible est donc désignée par son identifiant technique, pas par un
 * texte libre.
 */
describe('ChangesImpactDialogComponent', () => {
  let component: ChangesImpactDialogComponent;
  let fixture: ComponentFixture<ChangesImpactDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ChangesImpactDialogComponent, ImpactResponse>>;
  let prevMock: boolean;

  const CHANGE_ID = 'chg-1';
  const TARGET = 'd1e2f3a4-1111-2222-3333-444455556666';
  const url = `${environment.apiBaseUrl}/api/v1/changes/${CHANGE_ID}/impacts`;

  const added: ImpactResponse = {
    id: 'i1', tenantId: 't1', changeId: CHANGE_ID, targetType: 'TRAINING_PATH',
    targetId: TARGET, createdAt: '2026-07-01T08:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<ChangesImpactDialogComponent, ImpactResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ChangesImpactDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { changeId: CHANGE_ID } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangesImpactDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('couvre les huit familles d\'entités impactables du référentiel', () => {
    expect(component.targetTypes.map(t => t.value)).toEqual([
      'DOCUMENT', 'TRAINING_PATH', 'SUPPLIER', 'IOT_DEVICE',
      'FMEA_PROJECT', 'PDCA_CYCLE', 'STANDARD', 'OTHER'
    ]);
    expect(component.targetTypes.every(t => t.label.length > 0)).toBeTrue();
    expect(component.form.controls.targetType.value).toBe('DOCUMENT');
  });

  it('exige une cible au format UUID', () => {
    expect(component.form.controls.targetId.hasError('required')).toBeTrue();

    component.form.controls.targetId.setValue('procédure 4');
    expect(component.form.controls.targetId.hasError('pattern')).toBeTrue();
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.targetId.touched).toBeTrue();
  });

  it('refuse des notes plus longues que la colonne serveur', () => {
    component.form.controls.targetId.setValue(TARGET);
    component.form.controls.notes.setValue('n'.repeat(1001));
    expect(component.form.controls.notes.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(url);
  });

  it('rattache la cible choisie à la demande avec ses notes normalisées', () => {
    component.form.patchValue({ targetType: 'TRAINING_PATH', targetId: TARGET, notes: '  À requalifier  ' });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      targetType: 'TRAINING_PATH', targetId: TARGET, notes: 'À requalifier'
    });

    req.flush(added);
    expect(dialogRef.close).toHaveBeenCalledWith(added);
  });

  it('omet les notes laissées vides', () => {
    component.form.patchValue({ targetId: TARGET, notes: '   ' });
    component.submit();
    const req = http.expectOne(url);
    expect(req.request.body.notes).toBeUndefined();
    req.flush(added);
  });

  it('garde le dialogue ouvert quand l\'impact est déjà référencé', () => {
    component.form.controls.targetId.setValue(TARGET);
    component.submit();
    http.expectOne(url).flush({ title: 'duplicate' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
