import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Subject } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FiveSAuditResponse } from '../../fives.types';
import { FivesCreateDialogComponent } from './fives-create-dialog.component';

/** Connectivité pilotable — navigator.onLine est en lecture seule. */
class FakeConnectivity {
  online = true;
  private readonly subject = new Subject<boolean>();
  readonly online$ = this.subject.asObservable();
  isOnline(): boolean { return this.online; }
}

/**
 * Écran de terrain (§15.3) : la création d'audit doit rester possible sans
 * réseau, et la date locale saisie doit partir en instant ISO — sinon le
 * serveur reçoit une heure sans fuseau et planifie l'audit au mauvais moment.
 */
describe('FivesCreateDialogComponent', () => {
  let component: FivesCreateDialogComponent;
  let fixture: ComponentFixture<FivesCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<FivesCreateDialogComponent, FiveSAuditResponse>>;
  let connectivity: FakeConnectivity;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const base = `${environment.apiBaseUrl}/api/v1/fives/audits`;
  const AUDITOR = '11111111-1111-1111-1111-111111111111';

  const created: FiveSAuditResponse = {
    id: 'a1', tenantId: 't1', zone: 'Atelier mécanique A', status: 'DRAFT',
    auditorId: AUDITOR, createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z',
    items: []
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: AUDITOR, tenantId: 't1', displayName: 'Auditeur', roles: ['auditor'] };
    connectivity = new FakeConnectivity();
    dialogRef = jasmine.createSpyObj<MatDialogRef<FivesCreateDialogComponent, FiveSAuditResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [FivesCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        { provide: ConnectivityService, useValue: connectivity },
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FivesCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('exige une zone et n\'envoie rien tant qu\'elle manque', () => {
    expect(component.form.controls.zone.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(base);
    expect(component.form.controls.zone.touched).toBeTrue();
    expect(component.submitting).toBeFalse();
  });

  it('refuse une zone au-delà de 200 caractères', () => {
    component.form.controls.zone.setValue('z'.repeat(201));
    expect(component.form.controls.zone.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(base);
  });

  it('bloque la création quand la session a expiré plutôt que d\'envoyer un audit sans auditeur', () => {
    currentUser = null;
    component.form.controls.zone.setValue('Atelier mécanique A');
    component.submit();
    http.expectNone(base);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('normalise la zone, omet la description vide et convertit la date locale en instant ISO', () => {
    component.form.patchValue({
      zone: '  Atelier mécanique A  ',
      description: '   ',
      scheduledAt: '2026-09-01T08:30'
    });
    component.submit();

    const req = http.expectOne(base);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.zone).toBe('Atelier mécanique A');
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.scheduledAt).toBe(new Date('2026-09-01T08:30').toISOString());
    // L'auditeur provient du JWT, pas du formulaire.
    expect(req.request.body.auditorId).toBe(AUDITOR);

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('laisse le serveur décider quand aucune date n\'est prévue', () => {
    component.form.patchValue({ zone: 'Bloc opératoire B', description: '  CHU — hebdo  ' });
    component.submit();
    const req = http.expectOne(base);
    expect(req.request.body.scheduledAt).toBeUndefined();
    expect(req.request.body.description).toBe('CHU — hebdo');
    req.flush(created);
  });

  it('hors réseau, l\'audit terrain est mis en file et le dialogue se ferme quand même', (done) => {
    connectivity.online = false;
    dialogRef.close.and.callFake((audit?: FiveSAuditResponse) => {
      expect(audit?.pendingSync).toBeTrue();
      expect(audit?.zone).toBe('Zone blanche');
      expect(audit?.id.startsWith('offline-')).toBeTrue();
      // Aucune requête n'est partie : l'opération attend la resynchronisation.
      http.expectNone(base);
      done();
    });

    component.form.controls.zone.setValue('Zone blanche');
    component.submit();
  });

  it('ne ferme pas le dialogue quand le serveur refuse la saisie', () => {
    component.form.controls.zone.setValue('Atelier mécanique A');
    component.submit();
    http.expectOne(base).flush({ title: 'invalid' }, { status: 400, statusText: 'Bad Request' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    expect(component.form.controls.zone.value).toBe('Atelier mécanique A');
  });

  it('ignore un second envoi tant que le premier est en vol', () => {
    component.form.controls.zone.setValue('Atelier mécanique A');
    component.submit();
    const req = http.expectOne(base);
    expect(component.submitting).toBeTrue();

    component.submit();
    http.expectNone(base);

    req.flush(created);
  });

  it('ferme le dialogue sans rien créer à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    http.expectNone(base);
  });
});
