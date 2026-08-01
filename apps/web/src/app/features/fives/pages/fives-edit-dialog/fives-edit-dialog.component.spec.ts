import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FiveSAuditResponse } from '../../fives.types';
import { FivesEditDialogComponent, FivesEditDialogData } from './fives-edit-dialog.component';

class FakeConnectivity {
  online = true;
  private readonly subject = new Subject<boolean>();
  readonly online$ = this.subject.asObservable();
  isOnline(): boolean { return this.online; }
}

/**
 * Le champ `datetime-local` n'accepte ni secondes ni suffixe de fuseau : la
 * date serveur doit donc être tronquée à l'ouverture, puis re-convertie en
 * instant ISO à l'envoi. Une régression ici décale silencieusement les audits.
 */
describe('FivesEditDialogComponent', () => {
  let component: FivesEditDialogComponent;
  let fixture: ComponentFixture<FivesEditDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FivesEditDialogComponent, FiveSAuditResponse>>;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/fives/audits`;

  const existing: FiveSAuditResponse = {
    id: 'a1', tenantId: 't1', zone: 'Atelier mécanique A',
    description: 'Audit mensuel ligne 1', status: 'DRAFT', auditorId: 'u1',
    scheduledAt: '2026-09-01T08:30:00Z',
    createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z', items: []
  };

  async function build(data: FivesEditDialogData): Promise<void> {
    dialogRef = jasmine.createSpyObj<MatDialogRef<FivesEditDialogComponent, FiveSAuditResponse>>(
      'MatDialogRef', ['close']);
    await TestBed.configureTestingModule({
      declarations: [FivesEditDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: ConnectivityService, useClass: FakeConnectivity },
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FivesEditDialogComponent);
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

  it('tronque la date serveur au format accepté par le champ datetime-local', async () => {
    await build({ audit: existing });
    expect(component.form.controls.zone.value).toBe('Atelier mécanique A');
    expect(component.form.controls.description.value).toBe('Audit mensuel ligne 1');
    expect(component.form.controls.scheduledAt.value).toBe('2026-09-01T08:30');
  });

  it('laisse le champ date vide quand aucun audit n\'est planifié', async () => {
    await build({ audit: { ...existing, scheduledAt: undefined, description: undefined } });
    expect(component.form.controls.scheduledAt.value).toBe('');
    expect(component.form.controls.description.value).toBe('');
  });

  it('n\'envoie rien quand la zone a été vidée', async () => {
    await build({ audit: existing });
    component.form.controls.zone.setValue('');
    component.submit();
    http.expectNone(`${base}/a1`);
    expect(component.form.controls.zone.touched).toBeTrue();
  });

  it('renvoie la date saisie en instant ISO et normalise la zone', async () => {
    await build({ audit: existing });
    component.form.patchValue({
      zone: '  Atelier mécanique B  ', description: '   ', scheduledAt: '2026-10-05T14:00'
    });
    component.submit();

    const req = http.expectOne(`${base}/a1`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body.zone).toBe('Atelier mécanique B');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.scheduledAt).toBe(new Date('2026-10-05T14:00').toISOString());

    const updated = { ...existing, zone: 'Atelier mécanique B' };
    req.flush(updated);
    expect(dialogRef.close).toHaveBeenCalledWith(updated);
    expect(component.submitting).toBeFalse();
  });

  it('déplanifie l\'audit quand la date est effacée', async () => {
    await build({ audit: existing });
    component.form.controls.scheduledAt.setValue('');
    component.submit();
    const req = http.expectOne(`${base}/a1`);
    expect(req.request.body.scheduledAt).toBeUndefined();
    req.flush(existing);
  });

  it('ne ferme pas le dialogue quand le serveur refuse la modification', async () => {
    await build({ audit: existing });
    component.submit();
    http.expectOne(`${base}/a1`).flush({}, { status: 403, statusText: 'Forbidden' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ignore un second envoi tant que le premier est en vol', async () => {
    await build({ audit: existing });
    component.submit();
    const req = http.expectOne(`${base}/a1`);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(`${base}/a1`);
    req.flush(existing);
  });

  it('ferme sans rien modifier à l\'annulation', async () => {
    await build({ audit: existing });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(`${base}/a1`);
  });
});
