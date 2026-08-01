import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { PdcaStepResponse } from '../../pdca.types';
import { PdcaStepDialogComponent, PdcaStepDialogData } from './pdca-step-dialog.component';

/**
 * L'étape est toujours rattachée à une phase de la roue : le dialogue présélectionne
 * la phase courante du cycle pour éviter les étapes classées au mauvais endroit.
 */
describe('PdcaStepDialogComponent', () => {
  let component: PdcaStepDialogComponent;
  let fixture: ComponentFixture<PdcaStepDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<PdcaStepDialogComponent, PdcaStepResponse>>;
  let prevMock: boolean;

  const stepsUrl = `${environment.apiBaseUrl}/api/v1/pdca/cycles/c1/steps`;

  const created: PdcaStepResponse = {
    id: 's1', cycleId: 'c1', phase: 'DO', title: 'Poka-Yoke', status: 'PENDING',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z'
  };

  async function setup(data: PdcaStepDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<PdcaStepDialogComponent, PdcaStepResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [PdcaStepDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PdcaStepDialogComponent);
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

  it('présélectionne la phase courante du cycle', async () => {
    await setup({ cycleId: 'c1', defaultPhase: 'CHECK' });
    expect(component.form.controls.phase.value).toBe('CHECK');
  });

  it('retombe sur PLAN quand le cycle n\'est sur aucune phase exploitable', async () => {
    await setup({ cycleId: 'c1' });
    expect(component.form.controls.phase.value).toBe('PLAN');
    expect(component.phases).toEqual(['PLAN', 'DO', 'CHECK', 'ACT']);
  });

  it('n\'envoie rien tant que le titre est vide', async () => {
    await setup({ cycleId: 'c1' });
    component.submit();
    http.expectNone(stepsUrl);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('poste l\'étape sur le cycle, sans champs optionnels vides', async () => {
    await setup({ cycleId: 'c1', defaultPhase: 'DO' });
    component.form.patchValue({ title: '  Poka-Yoke  ', description: '  ', dueDate: '' });
    component.submit();

    const req = http.expectOne(stepsUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      title: 'Poka-Yoke', description: undefined, phase: 'DO', dueDate: undefined
    });

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('transmet l\'échéance et la description quand elles sont renseignées', async () => {
    await setup({ cycleId: 'c1' });
    component.form.patchValue({
      title: 'Audit interne', description: '  Vérifier le mode opératoire  ', dueDate: '2026-09-30'
    });
    component.submit();

    const req = http.expectOne(stepsUrl);
    expect(req.request.body.dueDate).toBe('2026-09-30');
    expect(req.request.body.description).toBe('Vérifier le mode opératoire');
    req.flush(created);
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await setup({ cycleId: 'c1' });
    component.form.controls.title.setValue('Étape');
    component.submit();
    component.submit();

    http.expectOne(stepsUrl).flush(created);
    expect(dialogRef.close).toHaveBeenCalledTimes(1);
  });

  it('garde le dialogue ouvert quand le cycle refuse l\'ajout (409 état terminal)', async () => {
    await setup({ cycleId: 'c1' });
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.controls.title.setValue('Étape tardive');
    component.submit();
    http.expectOne(stepsUrl).flush({ title: 'Conflict' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    expect(snackSpy).toHaveBeenCalledWith(
      'État incompatible — rechargez la page.', 'OK', { duration: 4000 });
  });

  it('ferme le dialogue sans résultat à l\'annulation', async () => {
    await setup({ cycleId: 'c1' });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
