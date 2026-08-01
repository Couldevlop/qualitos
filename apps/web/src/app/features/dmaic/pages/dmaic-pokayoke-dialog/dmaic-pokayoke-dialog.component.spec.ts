import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { environment } from '../../../../../environments/environment';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AssignmentResponse, DeviceSummary } from '../../dmaic.types';
import { DmaicPokaYokeDialogComponent } from './dmaic-pokayoke-dialog.component';

/**
 * Le catalogue Poka-Yoke est chargé à l'ouverture et refiltré côté serveur :
 * une panne du catalogue doit rester lisible sans casser le dialogue.
 */
describe('DmaicPokaYokeDialogComponent', () => {
  let component: DmaicPokaYokeDialogComponent;
  let fixture: ComponentFixture<DmaicPokaYokeDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<DmaicPokaYokeDialogComponent, AssignmentResponse>>;
  let prevMock: boolean;

  const PROJECT_ID = 'dmaic-1';
  const catalog = `${environment.apiBaseUrl}/api/v1/dmaic/pokayoke`;
  const assignUrl = `${environment.apiBaseUrl}/api/v1/dmaic/projects/${PROJECT_ID}/pokayoke`;

  const device: DeviceSummary = {
    id: 'pk-1', code: 'PK-INT-001', name: 'Verrouillage capot',
    type: 'PREVENTION', mechanism: 'INTERLOCK'
  };

  const assignment: AssignmentResponse = {
    id: 'as1', projectId: PROJECT_ID, deviceId: 'pk-1', deviceCode: 'PK-INT-001',
    deviceName: 'Verrouillage capot', deviceType: 'PREVENTION', status: 'PROPOSED',
    createdAt: '2026-07-02T08:00:00Z', updatedAt: '2026-07-02T08:00:00Z'
  };

  function flushCatalog(content: DeviceSummary[] = [device]): void {
    http.expectOne(r => r.url === catalog)
      .flush({ content, totalElements: content.length, totalPages: 1, number: 0, size: 100 });
    fixture.detectChanges();
  }

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    dialogRef = jasmine.createSpyObj<MatDialogRef<DmaicPokaYokeDialogComponent, AssignmentResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [DmaicPokaYokeDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { projectId: PROJECT_ID } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DmaicPokaYokeDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('charge tout le catalogue à l\'ouverture, sans filtre', (done) => {
    fixture.detectChanges();
    const req = http.expectOne(r => r.url === catalog);
    expect(req.request.params.get('size')).toBe('100');
    expect(req.request.params.has('type')).toBeFalse();
    expect(req.request.params.has('mechanism')).toBeFalse();
    req.flush({ content: [device], totalElements: 1, totalPages: 1, number: 0, size: 100 });
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.loadError).toBeNull();
    component.devices$.subscribe(devices => {
      expect(devices.map(d => d.code)).toEqual(['PK-INT-001']);
      done();
    });
  });

  it('refiltre côté serveur quand le type puis le mécanisme changent', () => {
    fixture.detectChanges();
    flushCatalog();

    component.typeFilter.setValue('DETECTION');
    const byType = http.expectOne(r => r.url === catalog);
    expect(byType.request.params.get('type')).toBe('DETECTION');
    byType.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 });

    component.mechanismFilter.setValue('VISION');
    const byBoth = http.expectOne(r => r.url === catalog);
    expect(byBoth.request.params.get('type')).toBe('DETECTION');
    expect(byBoth.request.params.get('mechanism')).toBe('VISION');
    byBoth.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 100 });
  });

  it('affiche un message sûr quand le catalogue est indisponible, puis l\'efface au rechargement', () => {
    fixture.detectChanges();
    http.expectOne(r => r.url === catalog)
      .flush({ title: 'boom' }, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.loadError).toContain('Erreur serveur');
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')).not.toBeNull();

    component.refreshDevices();
    flushCatalog();
    expect(component.loadError).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.banner-error')).toBeNull();
  });

  it('exige le choix d\'un dispositif avant d\'assigner', () => {
    fixture.detectChanges();
    flushCatalog();

    expect(component.form.controls.deviceId.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(assignUrl);
    expect(component.form.controls.deviceId.touched).toBeTrue();
  });

  it('assigne le dispositif au projet avec sa note normalisée', () => {
    fixture.detectChanges();
    flushCatalog();

    component.form.patchValue({ deviceId: 'pk-1', note: '  déploiement atelier 3  ' });
    component.submit();

    const req = http.expectOne(assignUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ deviceId: 'pk-1', note: 'déploiement atelier 3' });
    req.flush(assignment);
    expect(dialogRef.close).toHaveBeenCalledWith(assignment);
  });

  it('omet la note quand elle n\'est pas renseignée', () => {
    fixture.detectChanges();
    flushCatalog();

    component.form.controls.deviceId.setValue('pk-1');
    component.submit();
    const req = http.expectOne(assignUrl);
    expect(req.request.body.note).toBeUndefined();
    req.flush(assignment);
  });

  it('ne ferme pas le dialogue quand l\'assignation est refusée', () => {
    fixture.detectChanges();
    flushCatalog();

    component.form.controls.deviceId.setValue('pk-1');
    component.submit();
    http.expectOne(assignUrl).flush({ title: 'duplicate' }, { status: 409, statusText: 'Conflict' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme le dialogue à l\'annulation', () => {
    fixture.detectChanges();
    flushCatalog();

    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(assignUrl);
  });
});
