import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ChangeResponse } from '../../changes.types';
import { ChangesImplementDialogComponent } from './changes-implement-dialog.component';

/**
 * Passer une demande en IMPLEMENTED fige la date de mise en œuvre : c'est elle
 * qui sert de preuve dans le dossier de conformité.
 */
describe('ChangesImplementDialogComponent', () => {
  let component: ChangesImplementDialogComponent;
  let fixture: ComponentFixture<ChangesImplementDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ChangesImplementDialogComponent, ChangeResponse>>;
  let prevMock: boolean;

  const CHANGE_ID = 'chg-1';
  const url = `${environment.apiBaseUrl}/api/v1/changes/${CHANGE_ID}/implement`;

  const implemented: ChangeResponse = {
    id: CHANGE_ID, tenantId: 't1', code: 'CHG-2026-014', title: 'Procédure stérilisation',
    type: 'DOCUMENT', priority: 'HIGH', status: 'IMPLEMENTED', requesterUserId: 'u1',
    implementedAt: '2026-08-01', createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-08-01T08:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<ChangesImplementDialogComponent, ChangeResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [ChangesImplementDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { changeId: CHANGE_ID } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChangesImplementDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('propose la date du jour au format ISO court', () => {
    expect(component.form.controls.implementedAt.value)
      .toBe(new Date().toISOString().slice(0, 10));
    expect(component.form.valid).toBeTrue();
  });

  it('refuse d\'implémenter sans date', () => {
    component.form.controls.implementedAt.setValue('');
    expect(component.form.controls.implementedAt.hasError('required')).toBeTrue();

    component.submit();
    http.expectNone(url);
    expect(component.form.controls.implementedAt.touched).toBeTrue();
  });

  it('poste la date de mise en œuvre choisie', () => {
    component.form.controls.implementedAt.setValue('2026-08-01');
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ implementedAt: '2026-08-01' });

    req.flush(implemented);
    expect(dialogRef.close).toHaveBeenCalledWith(implemented);
  });

  it('garde le dialogue ouvert quand la demande n\'est pas approuvée', () => {
    component.submit();
    http.expectOne(url).flush({ title: 'invalid state' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
