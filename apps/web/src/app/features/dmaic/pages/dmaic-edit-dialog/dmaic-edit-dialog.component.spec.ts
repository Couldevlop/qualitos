import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DmaicProjectResponse } from '../../dmaic.types';
import { DmaicEditDialogComponent } from './dmaic-edit-dialog.component';

describe('DmaicEditDialogComponent', () => {
  let component: DmaicEditDialogComponent;
  let fixture: ComponentFixture<DmaicEditDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DmaicEditDialogComponent, DmaicProjectResponse>>;
  let prevMock: boolean;

  const ID = 'dmaic-1';
  const url = `${environment.apiBaseUrl}/api/v1/dmaic/projects/${ID}`;

  const project: DmaicProjectResponse = {
    id: ID, tenantId: 't1', title: 'Rebut ligne A',
    problemStatement: 'Rebut à 3,2 %.', goalStatement: 'Revenir à 1,2 %.',
    championId: 'champ-1', targetCompletionDate: '2026-09-30',
    phase: 'MEASURE', status: 'ACTIVE', blackBeltId: 'bb',
    specLowerLimit: 9.95, specUpperLimit: 10.05, specTarget: 10, specUnit: 'mm',
    estimatedSavingsEur: 84000, measureCount: 4, pokaYokeCount: 1,
    createdAt: '2026-07-01T08:00:00Z', updatedAt: '2026-07-01T08:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<DmaicEditDialogComponent, DmaicProjectResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [DmaicEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { project } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DmaicEditDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('préremplit le formulaire avec la charte et les spécifications existantes', () => {
    expect(component.form.controls.title.value).toBe('Rebut ligne A');
    expect(component.form.controls.problemStatement.value).toBe('Rebut à 3,2 %.');
    expect(component.form.controls.championId.value).toBe('champ-1');
    expect(component.form.controls.specTarget.value).toBe(10);
    expect(component.form.controls.specUnit.value).toBe('mm');
    expect(component.form.valid).toBeTrue();
  });

  it('n\'envoie rien si le titre est effacé', () => {
    component.form.controls.title.setValue('');
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('envoie un PATCH sur le projet et remonte la version à jour', () => {
    component.form.patchValue({ title: '  Rebut ligne A v2  ', goalStatement: '   ', specUnit: 'cm' });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.title).toBe('Rebut ligne A v2');
    // Un champ vidé par l'utilisateur n'est pas envoyé comme chaîne vide.
    expect(req.request.body.goalStatement).toBeUndefined();
    expect(req.request.body.specUnit).toBe('cm');
    expect(req.request.body.specLowerLimit).toBe(9.95);

    const updated = { ...project, title: 'Rebut ligne A v2' };
    req.flush(updated);
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
  });

  it('garde le dialogue ouvert quand le serveur signale un conflit', () => {
    component.submit();
    http.expectOne(url).flush({ title: 'stale' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
