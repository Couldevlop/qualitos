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

  it('laisse catégorie et sévérité vides : une criticité pré-remplie serait validée sans être lue', () => {
    expect(component.form.controls.category.value).toBeNull();
    expect(component.form.controls.severity.value).toBeNull();
    expect(component.categories.length).toBe(7);
    expect(component.severities).toEqual(['MINOR', 'MAJOR', 'CRITICAL']);
  });

  it('exige la catégorie et la sévérité, et dit laquelle manque', () => {
    component.form.patchValue({ title: 'NC', description: 'Constat.' });
    expect(component.blockedReason).toContain('catégorie');
    component.form.patchValue({ category: 'PROCESS' });
    expect(component.blockedReason).toContain('sévérité');

    component.submit();
    http.expectNone(endpoint);
    expect(component.form.controls.severity.touched).toBeTrue();

    component.form.patchValue({ severity: 'MINOR' });
    expect(component.blockedReason).toBeUndefined();
  });

  it('exige un titre avant tout envoi', () => {
    expect(component.form.controls.title.hasError('required')).toBeTrue();
    component.submit();
    http.expectNone(endpoint);
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it("exige une description : un titre seul ne rend pas l’écart instruisible", () => {
    // « Défaut peinture » ne dit ni ce qui a été constaté, ni où, ni ce que ça
    // a produit. Six mois plus tard, l'analyse de cause racine partirait de rien.
    component.form.controls.title.setValue('Défaut peinture');
    expect(component.form.controls.description.hasError('required')).toBeTrue();

    component.submit();

    http.expectNone(endpoint);
    expect(component.form.controls.description.touched).toBeTrue();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('refuse un titre ou une zone au-delà de 255 caractères', () => {
    component.form.controls.title.setValue('x'.repeat(256));
    expect(component.form.controls.title.hasError('maxlength')).toBeTrue();

    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR', title: 'OK', description: 'Constat valide.' });
    component.form.controls.zone.setValue('z'.repeat(256));
    expect(component.form.controls.zone.hasError('maxlength')).toBeTrue();
    component.submit();
    http.expectNone(endpoint);
  });

  it('nettoie les champs optionnels vides plutôt que d\'envoyer des chaînes blanches', () => {
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR',
      title: '  Étiquetage lot manquant  ', zone: '   ',
      description: '  Sachet sorti de ligne sans étiquette de lot.  ',
      photoUrls: '  \n \n'
    });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.title).toBe('Étiquetage lot manquant');
    expect(req.request.body.zone).toBeUndefined();
    // La description, elle, n'est PAS optionnelle : elle est seulement rognée.
    expect(req.request.body.description).toBe('Sachet sorti de ligne sans étiquette de lot.');
    expect(req.request.body.photoUrls).toBeUndefined();
    expect(req.request.body.geoLat).toBeUndefined();
    expect(req.request.body.geoLng).toBeUndefined();
    // horodatage de détection posé côté client (saisie terrain)
    expect(Date.parse(req.request.body.detectedAt)).not.toBeNaN();
    expect(req.request.body.reporterId).toBe('u1');

    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  const photo = (name: string, type = 'image/jpeg', size = 1024): File =>
    new File([new Uint8Array(size)], name, { type });

  const pick = (...files: File[]): void => {
    const input = { files, value: 'x' } as unknown as HTMLInputElement;
    component.onPhotosSelected({ target: input } as unknown as Event);
  };

  it('envoie les photos choisies une fois la NC créée, puis le dit', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pick(photo('a.jpg'), photo('b.png', 'image/png'));
    expect(component.photos.length).toBe(2);

    component.form.patchValue({ category: 'PRODUCT', severity: 'MINOR', title: 'NC', description: 'Constat photographié.' });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.photoUrls).toBeUndefined();
    req.flush(created);

    const first = http.expectOne(`${endpoint}/srv-1/photos`);
    expect(first.request.body instanceof FormData).toBeTrue();
    first.flush({ id: 'p1' });
    http.expectOne(`${endpoint}/srv-1/photos`).flush({ id: 'p2' });

    expect(component.uploaded).toBe(2);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(snackSpy.calls.mostRecent().args[0]).toContain('photos jointes');
  });

  it("garde la NC déclarée quand une photo échoue, et invite à la rejoindre depuis la fiche", () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pick(photo('a.jpg'));
    component.form.patchValue({ category: 'PRODUCT', severity: 'MINOR', title: 'NC', description: 'Constat.' });
    component.submit();

    http.expectOne(endpoint).flush(created);
    http.expectOne(`${endpoint}/srv-1/photos`).flush('boom', { status: 500, statusText: 'KO' });

    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(snackSpy.calls.mostRecent().args[0]).toContain('depuis la fiche');
  });

  it('refuse tout de suite un format ou un poids que le serveur refuserait', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pick(photo('doc.pdf', 'application/pdf'));
    expect(component.photos.length).toBe(0);
    expect(snackSpy.calls.mostRecent().args[0]).toContain('Format non accepté');

    pick(photo('lourde.jpg', 'image/jpeg', component.maxPhotoBytes + 1));
    expect(component.photos.length).toBe(0);
    expect(snackSpy.calls.mostRecent().args[0]).toContain('10 Mo');
  });

  it('plafonne à dix photos et permet d'en retirer une', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pick(...Array.from({ length: 11 }, (_, i) => photo(`p${i}.jpg`)));
    expect(component.photos.length).toBe(component.maxPhotos);
    expect(snackSpy.calls.mostRecent().args[0]).toContain('Dix photos');

    component.removePhoto(component.photos[0]);
    expect(component.photos.length).toBe(component.maxPhotos - 1);
    expect(component.removePhotoAria('a.jpg')).toContain('a.jpg');
    expect(component.formatSize(2048)).toBe('2 Ko');
    expect(component.formatSize(3 * 1024 * 1024)).toBe('3.0 Mo');
  });

  it('reste utilisable sans session : la NC part sans déclarant plutôt que d\'être bloquée', () => {
    currentUser = null;
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR', title: 'NC anonyme', description: 'Constat sans session.' });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.reporterId).toBeUndefined();
    req.flush(created);
    expect(dialogRef.close).toHaveBeenCalledWith(created);
  });

  it('hors-ligne : annonce la mise en file au lieu d\'une création confirmée', async () => {
    connectivity.online = false;
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.patchValue({ category: 'PROCESS',
      title: 'Zone blanche', severity: 'CRITICAL', description: 'Constat hors couverture réseau.'
    });
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

  it("hors-ligne : n'envoie pas les photos (un fichier ne se met pas en file) et le dit", async () => {
    connectivity.online = false;
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pick(photo('a.jpg'));
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR', title: 'Zone blanche', description: 'Constat.' });
    component.submit();
    await new Promise<void>(resolve => setTimeout(resolve, 0));

    http.expectNone(endpoint);
    http.expectNone(req => req.url.endsWith('/photos'));
    expect(snackSpy.calls.mostRecent().args[0]).toContain("photos n'ont pas pu être jointes");
  });

  it('ignore un second envoi tant que le premier est en vol (anti double-déclaration)', () => {
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR', title: 'NC', description: 'Constat en double clic.' });
    component.submit();
    component.submit();

    http.expectOne(endpoint).flush(created);
    expect(dialogRef.close).toHaveBeenCalledTimes(1);
  });

  it('garde le dialogue ouvert quand le serveur refuse la saisie (400)', () => {
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR', title: 'NC', description: 'Constat refusé par le serveur.' });
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
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR', geoLat: 48.1, geoLng: 2.2 });
    component.clearLocation();
    expect(component.form.controls.geoLat.value).toBeNull();
    expect(component.form.controls.geoLng.value).toBeNull();
  });

  it("laisse le serveur trancher l’origine quand la liste n’en impose aucune", () => {
    // Entrée historique `/nc`, qui montre les deux origines : aucune n'est
    // impliquée par l'écran, et le serveur applique son défaut.
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR', title: 'NC', description: 'Constat sans origine imposée.' });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.origin).toBeUndefined();
    req.flush(created);
  });

  it('joint la position au corps de la déclaration quand elle est renseignée', () => {
    component.form.patchValue({ category: 'PROCESS', severity: 'MAJOR',
      title: 'NC géolocalisée', description: 'Constat relevé sur zone.',
      geoLat: 48.856612, geoLng: 2.35222
    });
    component.submit();

    const req = http.expectOne(endpoint);
    expect(req.request.body.geoLat).toBe(48.856612);
    expect(req.request.body.geoLng).toBe(2.35222);
    req.flush(created);
  });
});
