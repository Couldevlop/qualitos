import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaCaseResponse } from '../../capa.types';
import { CapaEditDialogComponent, CapaEditDialogData } from './capa-edit-dialog.component';

/**
 * L'édition ne porte que sur les champs descriptifs : le statut, le type et le
 * propriétaire suivent le cycle de vie du cas et ne sont pas modifiables ici.
 */
describe('CapaEditDialogComponent', () => {
  let component: CapaEditDialogComponent;
  let fixture: ComponentFixture<CapaEditDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>>;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/capa/cases`;

  const existing: CapaCaseResponse = {
    id: 'c1', tenantId: 't1', title: 'Recalibration robot', description: 'Suite NC ligne 3.',
    type: 'CORRECTIVE', criticity: 'HIGH', status: 'IN_PROGRESS',
    sourceType: 'NON_CONFORMITY', sourceRef: 'NC-2026-018', ownerId: 'u1',
    dueDate: '2026-05-30',
    createdAt: '2026-04-01T00:00:00Z', updatedAt: '2026-04-02T00:00:00Z', actions: []
  };

  async function build(data: CapaEditDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<CapaEditDialogComponent, CapaCaseResponse>>(
      'MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [CapaEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaEditDialogComponent);
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

  it('reprend les valeurs existantes du cas', async () => {
    await build({ capa: existing });
    expect(component.form.controls.title.value).toBe('Recalibration robot');
    expect(component.form.controls.description.value).toBe('Suite NC ligne 3.');
    expect(component.form.controls.criticity.value).toBe('HIGH');
    expect(component.form.controls.sourceRef.value).toBe('NC-2026-018');
    expect(component.form.controls.dueDate.value).toBe('2026-05-30');
    expect(component.criticities).toEqual(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']);
  });

  it('remplace les champs optionnels absents par du vide plutôt que « undefined »', async () => {
    await build({ capa: { ...existing, description: undefined, sourceRef: undefined, dueDate: undefined } });
    expect(component.form.controls.description.value).toBe('');
    expect(component.form.controls.sourceRef.value).toBe('');
    expect(component.form.controls.dueDate.value).toBe('');
  });

  it('n\'envoie rien quand le titre a été vidé', async () => {
    await build({ capa: existing });
    component.form.controls.title.setValue('');
    component.submit();
    http.expectNone(`${base}/c1`);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('envoie une mise à jour partielle sur l\'identifiant du cas ouvert', async () => {
    await build({ capa: existing });
    component.form.patchValue({
      title: '  Recalibration robot cobot-3  ',
      description: '   ',
      criticity: 'CRITICAL',
      sourceRef: '  ',
      dueDate: '2026-06-30'
    });
    component.submit();

    const req = http.expectOne(`${base}/c1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.title).toBe('Recalibration robot cobot-3');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.sourceRef).toBeUndefined();
    expect(req.request.body.criticity).toBe('CRITICAL');
    expect(req.request.body.dueDate).toBe('2026-06-30');

    const updated = { ...existing, criticity: 'CRITICAL' as const };
    req.flush(updated);
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('garde le dialogue ouvert quand le cas a changé entre-temps (409)', async () => {
    await build({ capa: existing });
    component.submit();
    http.expectOne(`${base}/c1`).flush({ title: 'stale' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build({ capa: existing });
    component.submit();
    const req = http.expectOne(`${base}/c1`);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(`${base}/c1`);
    req.flush(existing);
  });

  it('ferme sans rien modifier à l\'annulation', async () => {
    await build({ capa: existing });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(`${base}/c1`);
  });
});
