import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { MeasureResponse } from '../../dmaic.types';
import { DmaicMeasureDialogComponent } from './dmaic-measure-dialog.component';

/**
 * Une mesure alimente directement le calcul de capabilité : elle porte une
 * valeur numérique et la personne qui l'a relevée (traçabilité §11.5). Une
 * série se saisit en une fois : chaque valeur devient une mesure.
 */
describe('DmaicMeasureDialogComponent', () => {
  let component: DmaicMeasureDialogComponent;
  let fixture: ComponentFixture<DmaicMeasureDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DmaicMeasureDialogComponent, MeasureResponse[]>>;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const PROJECT_ID = 'dmaic-1';
  const url = `${environment.apiBaseUrl}/api/v1/dmaic/projects/${PROJECT_ID}/measures`;
  const USER = '11111111-1111-1111-1111-111111111111';
  const OTHER = '22222222-2222-2222-2222-222222222222';

  const saved = (id: string, value: number): MeasureResponse => ({
    id, projectId: PROJECT_ID, value, createdAt: '2026-07-02T08:00:00Z'
  });

  async function setup(): Promise<void> {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<DmaicMeasureDialogComponent, MeasureResponse[]>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [DmaicMeasureDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideDirectoryStub(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { projectId: PROJECT_ID } },
        { provide: AuthService, useValue: { snapshot: () => currentUser } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DmaicMeasureDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  }

  beforeEach(() => {
    currentUser = { userId: USER, tenantId: 't1', displayName: 'Op', roles: ['user'] };
  });

  afterEach(() => {
    http.verify();
    environment.useMockApi = prevMock;
  });

  it('la personne qui mesure est, par défaut, soi-même ; la valeur est obligatoire', async () => {
    await setup();
    expect(component.form.controls.operatorId.value).toBe(USER);
    expect(component.values.length).toBe(1);
    expect(component.form.invalid).toBeTrue();
    component.values.at(0).setValue(10.02);
    expect(component.form.valid).toBeTrue();
  });

  it('sans personne qui mesure, le formulaire reste invalide', async () => {
    currentUser = null;
    await setup();
    component.values.at(0).setValue(10);
    expect(component.form.invalid).toBeTrue();
    component.submit();
    http.expectNone(url);
  });

  it('ajoute et retire des valeurs, sans descendre sous une ni dépasser la limite', async () => {
    await setup();
    component.removeValue(0);
    expect(component.values.length).toBe(1);
    component.addValue();
    component.addValue();
    expect(component.values.length).toBe(3);
    component.removeValue(1);
    expect(component.values.length).toBe(2);
    for (let i = 0; i < 60; i++) component.addValue();
    expect(component.values.length).toBe(component.maxValues);
  });

  it('enregistre chaque valeur comme une mesure, dans l\'ordre, avec la personne choisie', async () => {
    await setup();
    component.addValue();
    component.values.at(0).setValue(10.02);
    component.values.at(1).setValue(9.98);
    component.form.patchValue({ operatorId: OTHER, sourceRef: '  OF-1  ', note: '  ' });
    component.submit();

    const first = http.expectOne(url);
    expect(first.request.body).toEqual(jasmine.objectContaining({ value: 10.02, operatorId: OTHER, sourceRef: 'OF-1' }));
    expect(first.request.body.note).toBeUndefined();
    expect(first.request.body.subgroupId).toBeUndefined();
    first.flush(saved('m1', 10.02));
    const second = http.expectOne(url);
    expect(second.request.body.value).toBe(9.98);
    second.flush(saved('m2', 9.98));

    expect(dialogRef.close).toHaveBeenCalledWith([saved('m1', 10.02), saved('m2', 9.98)]);
    expect(component.submitting).toBeFalse();
  });

  it('une valeur vide bloque tout l\'envoi', async () => {
    await setup();
    component.addValue();
    component.values.at(0).setValue(1);
    component.submit();
    http.expectNone(url);
    expect(component.submitting).toBeFalse();
  });

  it('en cas d\'échec en cours de série, rend les mesures déjà enregistrées', async () => {
    await setup();
    component.addValue();
    component.values.at(0).setValue(1);
    component.values.at(1).setValue(2);
    component.submit();
    http.expectOne(url).flush(saved('m1', 1));
    http.expectOne(url).flush({ title: 'boom' }, { status: 500, statusText: 'KO' });

    expect(dialogRef.close).toHaveBeenCalledWith([saved('m1', 1)]);
    expect(component.submitting).toBeFalse();
  });

  it('un échec dès la première valeur garde la fenêtre ouverte', async () => {
    await setup();
    component.values.at(0).setValue(1);
    component.submit();
    http.expectOne(url).flush({ title: 'boom' }, { status: 500, statusText: 'KO' });
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('annule sans rien envoyer', async () => {
    await setup();
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
