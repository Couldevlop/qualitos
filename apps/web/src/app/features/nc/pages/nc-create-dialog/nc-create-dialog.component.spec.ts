import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatRadioModule } from '@angular/material/radio';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, Subject } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { InMemoryQueueStore, OfflineQueueStore } from '../../../../core/offline/offline-queue.store';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../../products/products.service';
import { NcResponse } from '../../nc.types';
import { NcCreateDialogComponent } from './nc-create-dialog.component';

/** Connectivité pilotable (navigator.onLine est read-only). */
class FakeConnectivity {
  online = true;
  private readonly subject = new Subject<boolean>();
  readonly online$ = this.subject.asObservable();
  isOnline(): boolean { return this.online; }
}

/**
 * Saisie terrain (§4.3 / §15.3) : le formulaire doit rester utilisable sans
 * session complète et sans réseau — mais jamais au prix d'un envoi silencieux
 * de champs vides ou d'une NC perdue.
 */
describe('NcCreateDialogComponent', () => {
  let component: NcCreateDialogComponent;
  let fixture: ComponentFixture<NcCreateDialogComponent>;
  let http: HttpTestingController;
  let dialogRef: jasmine.SpyObj<MatDialogRef<NcCreateDialogComponent, NcResponse>>;
  let connectivity: FakeConnectivity;
  let currentUser: AuthUser | null;
  let prevMock: boolean;

  const endpoint = `${environment.apiBaseUrl}/api/v1/nc`;

  const created: NcResponse = {
    id: 'srv-1', reference: 'NC-2026-9001', title: 'Étiquetage lot manquant',
    category: 'PROCESS', severity: 'MAJOR', status: 'OPEN', origin: 'INTERNAL',
    detectedAt: '2026-07-01T00:00:00Z', createdAt: '2026-07-01T00:00:00Z',
    updatedAt: '2026-07-01T00:00:00Z'
  };

  beforeEach(async () => {
    prevMock = environment.useMockApi;
    environment.useMockApi = false;
    currentUser = { userId: 'u1', tenantId: 't1', displayName: 'Op', roles: ['user'] };
    connectivity = new FakeConnectivity();
    dialogRef = jasmine.createSpyObj<MatDialogRef<NcCreateDialogComponent, NcResponse>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [NcCreateDialogComponent],
      imports: [SharedModule, UiModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: OfflineQueueStore, useClass: InMemoryQueueStore },
        { provide: ConnectivityService, useValue: connectivity },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        // Le référentiel produit est une aide à la saisie : ce banc de test
        // vérifie le formulaire lui-même, pas le catalogue qui l'alimente.
        { provide: ProductsService, useValue: { list: () => of([]), failureModeSuggestions: () => of([]) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NcCreateDialogComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    environment.useMockApi = prevMock;
    http.verify();
  });

  it('propose des valeurs par défaut exploitables en une saisie terrain', () => {
    expect(component.form.controls.category.value).toBe('PROCESS');
    expect(component.form.controls.severity.value).toBe('MAJOR');
    expect(component.categories.length).toBe(7);
    expect(component.severities).toEqual(['MINOR', 'MAJOR', 'CRITICAL']);
  });

  it('exige un titre avant tout envoi', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(endpoint);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('refuse un titre ou une zone au-delà de 255 caractères', () => {
    component.form.controls.title.setValue('x'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();

    component.form.controls.title.setValue('OK');
    component.form.controls.zone.setValue('z'.repeat(256));
    expect(component.form.controls.zone.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(endpoint);
  });

  it('nettoie les champs optionnels vides plutôt que d\'envoyer des chaînes blanches', () => {
    component.form.patchValue({
      title: '  Étiquetage lot manquant  ', zone: '   ', description: '  ', photoUrls: '  \n \n'
    });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Étiquetage lot manquant');
    expect(req.request.body.zone).toBeUndefined();
    expect(req.request.body.description).toBeUndefined();
    expect(req.request.body.photoUrls).toBeUndefined();
    expect(req.request.body.geoLat).toBeUndefined();
    expect(req.request.body.geoLng).toBeUndefined();
    // horodatage de détection posé côté client (saisie terrain)
    expect(Date.parse(req.request.body.detectedAt)).not.toBeNaN();
    expect(req.request.body.reporterId).toBe('u1');

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('normalise le bloc photos : une URL par ligne, sans ligne vide ni espaces', () => {
    component.form.patchValue({
      title: 'NC',
      photoUrls: '  https://a/1.jpg  \n\n   \n https://a/2.jpg\n'
    });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.photoUrls).toBe('https://a/1.jpg\nhttps://a/2.jpg');
    req.flush(created);
  });

  it('reste utilisable sans session : la NC part sans déclarant plutôt que d\'être bloquée', () => {
    currentUser = null;
    component.form.controls.title.setValue('NC anonyme');
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.reporterId).toBeUndefined();
    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('hors-ligne : annonce la mise en file au lieu d\'une création confirmée', async () => {
    connectivity.online = false;
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.patchValue({ title: 'Zone blanche', severity: 'CRITICAL' });
    component.submit();
    // la mise en file passe par le store (promesse) : on laisse filer la microtâche
    await new Promise<void>(resolve => setTimeout(resolve, 0));

    http.expectNone(endpoint);   // rien ne part sur le réseau
    expect(dialogRef.close).toHaveBeenCalled();
    const closed = dialogRef.close.calls.mostRecent().args[0] as NcResponse;
    expect(closed.pendingSync).toBeTrue();
    expect(snackSpy.calls.mostRecent().args[0])
      .toContain('synchronisée au retour du réseau');
  });

  it('ignore un second envoi tant que le premier est en vol (anti double-déclaration)', () => {
    component.form.controls.title.setValue('NC');
    component.submit();
    component.submit();

    http.expectOne(endpoint).flush(created);
    expect(dialogRef.close).toHaveBeenCalledTimes(1);
  });

  it('garde le dialogue ouvert quand le serveur refuse la saisie (400)', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.controls.title.setValue('NC');
    component.submit();
    http.expectOne(endpoint).flush(
      { title: 'ConstraintViolation: nc.title' }, { status: 400, statusText: 'Bad Request' });

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
    expect(snackSpy).toHaveBeenCalledWith(
      'Champs invalides — vérifiez le formulaire.', 'OK', { duration: 4000 });
  });

  it('ferme le dialogue sans résultat à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });

  // --- géolocalisation terrain -----------------------------------------------

  it('renseigne la position arrondie au micro-degré (~10 cm, pas de précision inutile)', () => {
    spyOn(navigator.geolocation, 'getCurrentPosition').and.callFake(
      ((ok: PositionCallback) => ok({
        coords: { latitude: 48.85661234567, longitude: 2.35221987654 }
      } as GeolocationPosition)) as typeof navigator.geolocation.getCurrentPosition);

    component.useMyLocation();

    expect(component.form.controls.geoLat.value).toBe(48.856612);
    expect(component.form.controls.geoLng.value).toBe(2.35222);
    expect(component.locating).toBeFalse();
  });

  it('signale l\'échec de localisation et réarme le bouton', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    spyOn(navigator.geolocation, 'getCurrentPosition').and.callFake(
      ((_ok: PositionCallback, ko: PositionErrorCallback) =>
        ko({ code: 1, message: 'denied' } as GeolocationPositionError)
      ) as typeof navigator.geolocation.getCurrentPosition);

    component.useMyLocation();

    expect(component.locating).toBeFalse();
    expect(component.form.controls.geoLat.value).toBeNull();
    expect(snackSpy).toHaveBeenCalled();
  });

  it('ne relance pas la localisation tant que la précédente n\'a pas répondu', () => {
    const geoSpy = spyOn(navigator.geolocation, 'getCurrentPosition');
    component.locating = true;
    component.useMyLocation();
    expect(geoSpy).not.toHaveBeenCalled();
  });

  it('explique l\'absence de géolocalisation sur l\'appareil au lieu de planter', () => {
    Object.defineProperty(navigator, 'geolocation', { value: undefined, configurable: true });
    try {
      const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
      component.useMyLocation();
      expect(component.locating).toBeFalse();
      expect(snackSpy).toHaveBeenCalled();
    } finally {
      Reflect.deleteProperty(navigator, 'geolocation');
    }
  });

  it('retire la position posée sur la fiche', () => {
    component.form.patchValue({ geoLat: 48.1, geoLng: 2.2 });
    component.clearLocation();
    expect(component.form.controls.geoLat.value).toBeNull();
    expect(component.form.controls.geoLng.value).toBeNull();
  });

  it('joint la position au corps de la déclaration quand elle est renseignée', () => {
    component.form.patchValue({ title: 'NC géolocalisée', geoLat: 48.856612, geoLng: 2.35222 });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.geoLat).toBe(48.856612);
    expect(req.request.body.geoLng).toBe(2.35222);
    req.flush(created);
  });
});
