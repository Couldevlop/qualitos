import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaActionResponse } from '../../capa.types';
import { CapaActionDialogComponent } from './capa-action-dialog.component';

/**
 * §4.2 / ISO 9001 §10.2 — une action naît toujours sous un cas existant et
 * dans l'état PENDING : c'est son avancement qui débloque la résolution du cas.
 */
describe('CapaActionDialogComponent', () => {
  let component: CapaActionDialogComponent;
  let fixture: ComponentFixture<CapaActionDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaActionDialogComponent, CapaActionResponse>>;
  let prevMock: boolean;

  const url = `${environment.apiBaseUrl}/api/v1/capa/cases/c1/actions`;

  const created: CapaActionResponse = {
    id: 'a1', capaId: 'c1', title: 'Recalibrer la sonde', status: 'PENDING'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaActionDialogComponent, CapaActionResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [CapaActionDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { caseId: 'c1' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaActionDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige un titre d\'action', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(url);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('refuse un titre au-delà de 255 caractères', () => {
    component.form.controls.title.setValue('a'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(url);
  });

  it('crée l\'action sous le cas passé en donnée de dialogue', () => {
    component.form.patchValue({
      title: '  Recalibrer la sonde  ', description: '   ', dueDate: ''
    });
    component.submit();

    const req = http.expectOne(url);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Recalibrer la sonde');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.dueDate).toBeUndefined();

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet description et échéance quand elles sont saisies', () => {
    component.form.patchValue({
      title: 'Recalibrer', description: '  Sonde T° hebdo  ', dueDate: '2026-09-15'
    });
    component.submit();
    const req = http.expectOne(url);
    expect(req.request.body.description).toBe('Sonde T° hebdo');
    expect(req.request.body.dueDate).toBe('2026-09-15');
    req.flush(created);
  });

  it('ne ferme pas le dialogue quand le cas n\'accepte plus d\'action (409)', () => {
    component.form.controls.title.setValue('Recalibrer');
    component.submit();
    http.expectOne(url).flush({ title: 'closed case' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.title.setValue('Recalibrer');
    component.submit();
    const req = http.expectOne(url);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(url);
    req.flush(created);
  });

  it('ferme sans rien ajouter à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(url);
  });
});
