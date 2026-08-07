import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { FiveWhysAnalysis } from '../../../five-whys/five-whys.types';
import { NcService } from '../../nc.service';
import { NcPhoto, NcResponse, NcStatus, VisionAnalysis } from '../../nc.types';
import { NcDetailComponent } from './nc-detail.component';

function buildFiveWhys(overrides: Partial<FiveWhysAnalysis> = {}): FiveWhysAnalysis {
  return {
    id: 'fw-1', ncId: 'nc-1', ncReference: 'NC-2026-1001',
    problem: 'Étiquetage manquant', rootCause: null, steps: [],
    createdAt: '2026-08-06T10:00:00Z', updatedAt: '2026-08-06T10:00:00Z', ...overrides
  };
}

/** Aucune analyse ouverte : le cas par défaut des harnais existants. */
function fiveWhysSpy(existing: FiveWhysAnalysis[] = []): jasmine.SpyObj<FiveWhysService> {
  const spy = jasmine.createSpyObj<FiveWhysService>('FiveWhysService', ['listForNc', 'create']);
  spy.listForNc.and.returnValue(of(existing));
  return spy;
}

function buildNc(overrides: Partial<NcResponse> = {}): NcResponse {
  return {
    id: 'nc-1', reference: 'NC-2026-1001', title: 'Étiquetage manquant',
    category: 'PROCESS', severity: 'MAJOR', status: 'OPEN', origin: 'INTERNAL',
    detectedAt: '2026-06-06T00:00:00Z', createdAt: '2026-06-06T00:00:00Z',
    updatedAt: '2026-06-06T00:00:00Z', ...overrides
  };
}

const PHOTO: NcPhoto = {
  id: 'p1', url: 'https://store/presigned/p1', contentType: 'image/jpeg',
  sizeBytes: 1234, originalFilename: 'champ.jpg', createdAt: '2026-06-06T00:00:00Z'
};

class FakeConnectivity {
  readonly online$ = new BehaviorSubject<boolean>(true);
  isOnline(): boolean { return this.online$.value; }
}

describe('NcDetailComponent — section photos', () => {
  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;
  let connectivity: FakeConnectivity;

  function setup(nc: NcResponse, photos: NcPhoto[] = []): void {
    svc.getNc.and.returnValue(of(nc));
    svc.listPhotos.and.returnValue(of(photos));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();        // ngOnInit : crée nc$ + premier reload$.next()
    // reload$ est un Subject : l'async pipe ne s'abonne qu'au CD suivant ;
    // on ré-émet pour que la vue reçoive la NC (parité avec le flux réel asynchrone).
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<NcService>('NcService', [
      'getNc', 'listPhotos', 'uploadPhoto', 'deletePhoto', 'analyzePhotoVision'
    ]);
    connectivity = new FakeConnectivity();

    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: ConnectivityService, useValue: connectivity },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1' }) } },
        { provide: FiveWhysService, useValue: fiveWhysSpy() },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'nc-1' }) } }
        }
      ]
    }).compileComponents();
  });

  it('rend une vignette par photo (img présignée + lien plein écran)', () => {
    setup(buildNc(), [PHOTO]);
    const el: HTMLElement = fixture.nativeElement;
    const imgs = el.querySelectorAll('.photo-thumb img');
    expect(imgs.length).toBe(1);
    expect((imgs[0] as HTMLImageElement).getAttribute('src')).toBe(PHOTO.url!);
    const link = el.querySelector('.photo-thumb a') as HTMLAnchorElement;
    expect(link.getAttribute('target')).toBe('_blank');
    expect(link.getAttribute('rel')).toContain('noopener');
  });

  it('affiche l’état vide quand il n’y a aucune photo', () => {
    setup(buildNc(), []);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.photos-empty')).toBeTruthy();
    expect(el.querySelector('.photo-thumb')).toBeNull();
  });

  it('affiche le bouton d’ajout pour une NC ouverte', () => {
    setup(buildNc({ status: 'OPEN' }), []);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.add-photo-btn')).toBeTruthy();
  });

  it('masque le bouton d’ajout quand la NC est CLOSED', () => {
    setup(buildNc({ status: 'CLOSED', origin: 'INTERNAL', closedAt: '2026-06-06T00:00:00Z' }), []);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.add-photo-btn')).toBeNull();
  });

  it('masque le bouton d’ajout quand la NC est CANCELLED', () => {
    setup(buildNc({ status: 'CANCELLED' }), []);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.add-photo-btn')).toBeNull();
  });

  it('affiche le message storage-disabled sur 503 (au lieu d’une erreur brute)', () => {
    svc.getNc.and.returnValue(of(buildNc()));
    svc.listPhotos.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 503, error: { type: 'https://qualitos.io/errors/storage-disabled' } })));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(component.storageDisabled$.value).toBeTrue();
    expect(el.querySelector('.storage-disabled')).toBeTruthy();
    expect(el.querySelector('.add-photo-btn')?.hasAttribute('disabled')).toBeTrue();
  });

  it('désactive le bouton d’ajout hors-ligne', () => {
    setup(buildNc(), []);
    connectivity.online$.next(false);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    const btn = el.querySelector('.add-photo-btn') as HTMLButtonElement;
    expect(btn.disabled).toBeTrue();
    expect(el.querySelector('.photos-offline-note')).toBeTruthy();
  });

  it('canAddPhoto est vrai hors état terminal, faux sinon', () => {
    setup(buildNc(), []);
    expect(component.canAddPhoto('OPEN')).toBeTrue();
    expect(component.canAddPhoto('RESOLVED')).toBeTrue();
    expect(component.canAddPhoto('CLOSED')).toBeFalse();
    expect(component.canAddPhoto('CANCELLED')).toBeFalse();
  });

  it('uploadPhoto réussi ajoute la vignette en tête', () => {
    setup(buildNc(), []);
    const newPhoto: NcPhoto = { ...PHOTO, id: 'p2', url: 'https://store/p2' };
    svc.uploadPhoto.and.returnValue(of(newPhoto));
    const file = new File([new Uint8Array([1])], 'x.png', { type: 'image/png' });
    component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
    fixture.detectChanges();
    expect(svc.uploadPhoto).toHaveBeenCalledWith('nc-1', file);
    expect(component.photos$.value[0].id).toBe('p2');
  });

  it('upload renvoyant 503 storage-disabled bascule l’UI sans snackbar d’erreur brute', () => {
    setup(buildNc(), []);
    const snack = TestBed.inject(MatSnackBar);
    const snackSpy = spyOn(snack, 'open');
    svc.uploadPhoto.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 503, error: { type: 'https://qualitos.io/errors/storage-disabled' } })));
    const file = new File([new Uint8Array([1])], 'x.png', { type: 'image/png' });
    component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
    expect(component.storageDisabled$.value).toBeTrue();
    expect(snackSpy).not.toHaveBeenCalled();
  });

  it('deletePhoto confirme puis appelle le service et retire la vignette', () => {
    setup(buildNc(), [PHOTO]);
    const dialog = TestBed.inject(MatDialog);
    spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(true) } as never);
    svc.deletePhoto.and.returnValue(of(void 0));
    component.deletePhoto(PHOTO);
    expect(svc.deletePhoto).toHaveBeenCalledWith('nc-1', 'p1');
    expect(component.photos$.value.length).toBe(0);
  });

  it('deletePhoto annulé (confirm=false) n’appelle pas le service', () => {
    setup(buildNc(), [PHOTO]);
    const dialog = TestBed.inject(MatDialog);
    spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(false) } as never);
    component.deletePhoto(PHOTO);
    expect(svc.deletePhoto).not.toHaveBeenCalled();
    expect(component.photos$.value.length).toBe(1);
  });

  // --- analyse Vision 5S par IA ---------------------------------------------

  const VISION: VisionAnalysis = {
    imageSha256: 'abc', width: 1280, height: 720,
    score: { seiri: 70, seiton: 85, seiso: 55, seiketsu: 90, shitsuke: 65, overall: 73 },
    findings: [
      { pillar: 'SEIRI', description: 'Encombrement zone passage', severity: 'HIGH', confidence: 0.91, bbox: [1, 2, 3, 4] },
      { pillar: 'SEISO', description: 'Salissure au sol', severity: 'MEDIUM', confidence: 0.6, bbox: null }
    ]
  };

  function vfile(): File {
    return new File([new Uint8Array([1])], 'zone.png', { type: 'image/png' });
  }

  it('affiche l’invite initiale tant qu’aucune analyse n’a tourné', () => {
    setup(buildNc(), []);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.vision-card')).toBeTruthy();
    expect(el.querySelector('.vision-scores')).toBeNull();
  });

  it('analyse réussie : rend le score 5S (5 piliers + global) et les findings', () => {
    setup(buildNc(), []);
    svc.analyzePhotoVision.and.returnValue(of(VISION));
    component.onVisionFileSelected({ target: { files: [vfile()], value: '' } } as unknown as Event);
    fixture.detectChanges();

    expect(svc.analyzePhotoVision).toHaveBeenCalled();
    const el: HTMLElement = fixture.nativeElement;
    // score global
    expect(el.querySelector('.score-overall-value')?.textContent?.trim()).toBe('73');
    // 5 barres de piliers
    expect(el.querySelectorAll('.score-bars li').length).toBe(5);
    // 2 findings
    const findings = el.querySelectorAll('.finding');
    expect(findings.length).toBe(2);
    expect(findings[0].querySelector('.finding-pillar')?.textContent?.trim()).toBe('SEIRI');
    expect(findings[0].querySelector('.finding-conf')?.textContent?.trim()).toBe('91%');
  });

  it('503 vision-unavailable : état doux dédié, pas de snackbar brute', () => {
    setup(buildNc(), []);
    const snack = TestBed.inject(MatSnackBar);
    const snackSpy = spyOn(snack, 'open');
    svc.analyzePhotoVision.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 503, error: { type: 'https://qualitos.io/errors/vision-unavailable' } })));
    component.onVisionFileSelected({ target: { files: [vfile()], value: '' } } as unknown as Event);
    fixture.detectChanges();

    expect(component.visionUnavailable$.value).toBeTrue();
    expect(snackSpy).not.toHaveBeenCalled();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.vision-unavailable')).toBeTruthy();
    expect((el.querySelector('.analyze-btn') as HTMLButtonElement).disabled).toBeTrue();
  });

  it('413 : snackbar d’erreur, pas d’état unavailable', () => {
    setup(buildNc(), []);
    const snack = TestBed.inject(MatSnackBar);
    const snackSpy = spyOn(snack, 'open');
    svc.analyzePhotoVision.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 413 })));
    component.onVisionFileSelected({ target: { files: [vfile()], value: '' } } as unknown as Event);
    expect(component.visionUnavailable$.value).toBeFalse();
    expect(snackSpy).toHaveBeenCalled();
  });

  it('désactive le bouton d’analyse hors-ligne', () => {
    setup(buildNc(), []);
    connectivity.online$.next(false);
    fixture.detectChanges();
    const el: HTMLElement = fixture.nativeElement;
    expect((el.querySelector('.analyze-btn') as HTMLButtonElement).disabled).toBeTrue();
    expect(el.querySelector('.vision-offline-note')).toBeTruthy();
  });

  it('clearVision efface le résultat affiché', () => {
    setup(buildNc(), []);
    svc.analyzePhotoVision.and.returnValue(of(VISION));
    component.onVisionFileSelected({ target: { files: [vfile()], value: '' } } as unknown as Event);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.vision-scores')).toBeTruthy();
    component.clearVision();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.vision-scores')).toBeNull();
  });

  it('visionScoreClass mappe les paliers (good/warn/bad)', () => {
    setup(buildNc(), []);
    expect(component.visionScoreClass(85)).toBe('score-good');
    expect(component.visionScoreClass(70)).toBe('score-warn');
    expect(component.visionScoreClass(40)).toBe('score-bad');
  });

  it('visionConfidencePct arrondit la confiance du modèle en pourcentage', () => {
    setup(buildNc(), []);
    expect(component.visionConfidencePct(0.914)).toBe(91);
    expect(component.visionConfidencePct(0.915)).toBe(92);
    expect(component.visionConfidencePct(1)).toBe(100);
  });

  it('visionSeverityClass reste défini même si le backend renvoie une sévérité vide', () => {
    setup(buildNc(), []);
    expect(component.visionSeverityClass('HIGH')).toBe('vsev vsev-high');
    expect(component.visionSeverityClass('')).toBe('vsev vsev-unknown');
  });

  it('un échec de photos non lié au stockage laisse la fiche utilisable', () => {
    svc.getNc.and.returnValue(of(buildNc()));
    svc.listPhotos.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.storageDisabled$.value).toBeFalse();
    expect(component.photos$.value).toEqual([]);
    expect(fixture.nativeElement.querySelector('.storage-disabled')).toBeNull();
  });

  it('ne téléverse rien quand la sélection de fichier est annulée', () => {
    setup(buildNc(), []);
    component.onFileSelected({ target: { files: null, value: 'x' } } as unknown as Event);
    expect(svc.uploadPhoto).not.toHaveBeenCalled();
  });

  it('n\'ouvre pas le sélecteur de fichier pendant un téléversement', () => {
    setup(buildNc(), []);
    const input = { click: jasmine.createSpy('click') } as unknown as HTMLInputElement;
    component.uploading$.next(true);
    component.triggerFilePicker(input);
    expect(input.click).not.toHaveBeenCalled();

    component.uploading$.next(false);
    component.triggerFilePicker(input);
    expect(input.click).toHaveBeenCalled();
  });

  it('n\'ouvre pas le sélecteur d\'analyse pendant une inférence', () => {
    setup(buildNc(), []);
    const input = { click: jasmine.createSpy('click') } as unknown as HTMLInputElement;
    component.visionAnalyzing$.next(true);
    component.triggerVisionPicker(input);
    expect(input.click).not.toHaveBeenCalled();
  });

  it('ne lance aucune analyse quand la sélection d\'image est annulée', () => {
    setup(buildNc(), []);
    component.onVisionFileSelected({ target: { files: [], value: 'x' } } as unknown as Event);
    expect(svc.analyzePhotoVision).not.toHaveBeenCalled();
  });

  it('explique un refus de fichier (413/400) et un ajout sur NC clôturée (409)', () => {
    setup(buildNc(), []);
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    const file = new File([new Uint8Array([1])], 'x.png', { type: 'image/png' });

    svc.uploadPhoto.and.returnValue(throwError(() => new HttpErrorResponse({ status: 413 })));
    component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
    expect(snackSpy.calls.mostRecent().args[0]).toContain('10 Mo maximum');

    svc.uploadPhoto.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.onFileSelected({ target: { files: [file], value: '' } } as unknown as Event);
    expect(snackSpy.calls.mostRecent().args[0]).toContain('clôturée ou annulée');
    expect(component.uploading$.value).toBeFalse();
  });

  it('ne lance pas deux suppressions de photo en parallèle', () => {
    setup(buildNc(), [PHOTO]);
    const dialog = TestBed.inject(MatDialog);
    const openSpy = spyOn(dialog, 'open');
    component.deletingId$.next('p1');
    component.deletePhoto(PHOTO);
    expect(openSpy).not.toHaveBeenCalled();
  });

  it('signale l\'échec de suppression sans retirer la vignette', () => {
    setup(buildNc(), [PHOTO]);
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({ afterClosed: () => of(true) } as never);
    svc.deletePhoto.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.deletePhoto(PHOTO);

    expect(component.photos$.value.length).toBe(1);
    expect(component.deletingId$.value).toBeNull();
    expect(snackSpy).toHaveBeenCalledWith(
      'Erreur serveur — réessayez dans un instant.', 'OK', { duration: 4000 });
  });
});

/**
 * Le workflow NC (§4.3) n'autorise qu'une transition par état : ces gardes
 * évitent d'exposer une action que le serveur refuserait, et empêchent le
 * double envoi d'une transition déjà en vol.
 */
describe('NcDetailComponent — workflow et escalade CAPA', () => {
  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;
  let router: Router;
  let routeId: string;
  let currentUser: { userId: string } | null;

  const UUID = '22222222-2222-2222-2222-222222222222';

  function setup(nc: NcResponse = buildNc()): void {
    svc.getNc.and.returnValue(of(nc));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    svc.getNc.calls.reset();   // les rechargements se comptent à partir d'ici
  }

  function confirmWith(answer: boolean): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(answer) } as never);
  }

  beforeEach(async () => {
    routeId = UUID;
    currentUser = { userId: 'u1' };
    svc = jasmine.createSpyObj<NcService>('NcService', [
      'getNc', 'listPhotos', 'startAnalysis', 'defineAction', 'close', 'cancel',
      'escalateToCapa', 'uploadPhoto', 'deletePhoto', 'analyzePhotoVision'
    ]);
    svc.listPhotos.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: ConnectivityService, useValue: new FakeConnectivity() },
        { provide: AuthService, useValue: { snapshot: () => currentUser } },
        { provide: FiveWhysService, useValue: fiveWhysSpy() },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => routeId } } } }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  it('refuse un identifiant malformé et renvoie vers la liste sans appeler l\'API', () => {
    routeId = 'nc-1/../../secrets';
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(svc.getNc).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/nc']);
    expect(snackSpy).toHaveBeenCalled();
  });

  it('accepte un identifiant de démonstration nc-… pour rester utilisable sans backend', () => {
    routeId = 'nc-42';
    svc.getNc.and.returnValue(of(buildNc({ id: 'nc-42' })));
    fixture = TestBed.createComponent(NcDetailComponent);
    fixture.detectChanges();

    expect(svc.getNc).toHaveBeenCalledWith('nc-42');
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('démarre l\'analyse, confirme et recharge la fiche', () => {
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    svc.startAnalysis.and.returnValue(of(buildNc({ status: 'UNDER_ANALYSIS' })));

    component.startAnalysis();

    expect(svc.startAnalysis).toHaveBeenCalledWith(UUID);
    expect(snackSpy).toHaveBeenCalled();
    expect(svc.getNc).toHaveBeenCalledTimes(1);
    expect(component.acting$.value).toBeFalse();
  });

  it('route chaque action vers sa transition serveur', () => {
    setup();
    svc.defineAction.and.returnValue(of(buildNc({ status: 'ACTION_DEFINED' })));
    component.defineAction();
    expect(svc.defineAction).toHaveBeenCalledWith(UUID);

    svc.close.and.returnValue(of(buildNc({ status: 'CLOSED' })));
    component.close();
    expect(svc.close).toHaveBeenCalledWith(UUID);
  });

  it('ignore une seconde transition tant que la première est en vol', () => {
    setup();
    svc.startAnalysis.and.returnValue(new Subject<NcResponse>());

    component.startAnalysis();
    component.startAnalysis();

    expect(svc.startAnalysis).toHaveBeenCalledTimes(1);
    expect(component.acting$.value).toBeTrue();
  });

  it('signale un refus de transition sans recharger ni exposer le détail serveur', () => {
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    svc.close.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { detail: 'IllegalStateException: NC not RESOLVED' }
    })));

    component.close();

    expect(snackSpy).toHaveBeenCalledWith(
      'État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(svc.getNc).not.toHaveBeenCalled();
    expect(component.acting$.value).toBeFalse();
  });

  it('demande confirmation avant d\'annuler la non-conformité', () => {
    setup();
    confirmWith(true);
    svc.cancel.and.returnValue(of(buildNc({ status: 'CANCELLED' })));

    component.cancel();

    expect(svc.cancel).toHaveBeenCalledWith(UUID);
    expect(svc.getNc).toHaveBeenCalledTimes(1);
  });

  it('n\'annule rien quand la confirmation est refusée', () => {
    setup();
    confirmWith(false);
    component.cancel();
    expect(svc.cancel).not.toHaveBeenCalled();
  });

  it('escalade en CAPA avec le pilote issu de la session', () => {
    setup();
    confirmWith(true);
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    svc.escalateToCapa.and.returnValue(of(buildNc({ capaCaseId: 'capa-1' })));

    component.escalateToCapa();

    expect(svc.escalateToCapa).toHaveBeenCalledWith(UUID, { ownerId: 'u1' });
    expect(snackSpy).toHaveBeenCalled();
    expect(svc.getNc).toHaveBeenCalledTimes(1);
  });

  it('refuse l\'escalade sans session plutôt que de créer une CAPA sans pilote', () => {
    setup();
    currentUser = null;
    const openSpy = confirmWith(true);
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');

    component.escalateToCapa();

    expect(openSpy).not.toHaveBeenCalled();
    expect(svc.escalateToCapa).not.toHaveBeenCalled();
    expect(snackSpy).toHaveBeenCalled();
  });

  it('n\'escalade pas quand la confirmation est refusée', () => {
    setup();
    confirmWith(false);
    component.escalateToCapa();
    expect(svc.escalateToCapa).not.toHaveBeenCalled();
  });

  it('signale l\'échec d\'escalade et réarme les actions', () => {
    setup();
    confirmWith(true);
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    svc.escalateToCapa.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    component.escalateToCapa();

    expect(snackSpy).toHaveBeenCalledWith(
      'Erreur serveur — réessayez dans un instant.', 'OK', { duration: 4000 });
    expect(component.acting$.value).toBeFalse();
    expect(svc.getNc).not.toHaveBeenCalled();
  });

  it('ouvre le dialogue de résolution sur la NC courante et recharge après résolution', () => {
    setup();
    const openSpy = spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(buildNc({ status: 'RESOLVED' })) } as never);

    component.openResolve(buildNc({ id: UUID, reference: 'NC-2026-1001' }));

    expect(openSpy.calls.mostRecent().args[1]?.data)
      .toEqual({ ncId: UUID, reference: 'NC-2026-1001' });
    expect(svc.getNc).toHaveBeenCalledTimes(1);
  });

  it('ne recharge pas la fiche quand la résolution est abandonnée', () => {
    setup();
    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(undefined) } as never);

    component.openResolve(buildNc());

    expect(svc.getNc).not.toHaveBeenCalled();
  });

  it('n\'autorise qu\'une seule transition par état du workflow', () => {
    setup();
    const statuses: NcStatus[] =
      ['OPEN', 'UNDER_ANALYSIS', 'ACTION_DEFINED', 'RESOLVED', 'CLOSED', 'CANCELLED'];
    const allowed = statuses.map(s => [
      component.canStartAnalysis(s), component.canDefineAction(s),
      component.canResolve(s), component.canClose(s)
    ].filter(Boolean).length);

    expect(allowed).toEqual([1, 1, 1, 1, 0, 0]);
    expect(component.canStartAnalysis('OPEN')).toBeTrue();
    expect(component.canDefineAction('UNDER_ANALYSIS')).toBeTrue();
    expect(component.canResolve('ACTION_DEFINED')).toBeTrue();
    expect(component.canClose('RESOLVED')).toBeTrue();
  });

  it('interdit annulation et escalade sur une NC déjà close ou annulée', () => {
    setup();
    expect(component.canCancel('OPEN')).toBeTrue();
    expect(component.canEscalate('RESOLVED')).toBeTrue();
    expect(component.canCancel('CLOSED')).toBeFalse();
    expect(component.canEscalate('CANCELLED')).toBeFalse();
  });

  it('découpe les URLs de photos saisies ligne à ligne, sans blanc ni ligne vide', () => {
    setup();
    expect(component.photoList('  https://a/1.jpg \r\n\n  https://a/2.jpg  \n'))
      .toEqual(['https://a/1.jpg', 'https://a/2.jpg']);
    expect(component.photoList('')).toEqual([]);
    expect(component.photoList(undefined)).toEqual([]);
  });

  it('dérive les classes de badge du statut et de la sévérité', () => {
    setup();
    expect(component.statusBadgeClass('ACTION_DEFINED')).toBe('badge badge-action_defined');
    expect(component.severityBadgeClass('MINOR')).toBe('sev sev-minor');
  });

  it('revient à la liste des non-conformités', () => {
    setup();
    component.goBack();
    expect(router.navigate).toHaveBeenCalledWith(['/nc']);
  });
});

/**
 * Entrée des 5 Pourquoi depuis la fiche de non-conformité.
 *
 * <p>La méthode part d'un écart constaté : sans ce point d'entrée, l'écran des
 * analyses ne pouvait rester que vide — aucun chemin ne permettait d'en ouvrir
 * une. Ce qui se teste ici, c'est qu'un même geste ouvre l'analyse existante ou
 * la crée, sans que l'utilisateur ait à savoir laquelle des deux choses arrive,
 * et qu'une NC clôturée reste relisible sans redevenir modifiable.
 */
describe('NcDetailComponent — entrée 5 Pourquoi', () => {
  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;
  let fiveWhys: jasmine.SpyObj<FiveWhysService>;
  let router: Router;

  const UUID = '33333333-3333-3333-3333-333333333333';

  function setup(nc: NcResponse = buildNc({ id: UUID })): void {
    svc.getNc.and.returnValue(of(nc));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  function bouton(): HTMLButtonElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector('button.five-whys-btn');
  }

  async function configure(existing: FiveWhysAnalysis[]): Promise<void> {
    fiveWhys = fiveWhysSpy(existing);
    svc = jasmine.createSpyObj<NcService>('NcService', ['getNc', 'listPhotos']);
    svc.listPhotos.and.returnValue(of([]));

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: ConnectivityService, useValue: new FakeConnectivity() },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1' }) } },
        { provide: FiveWhysService, useValue: fiveWhys },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: UUID }) } } }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  }

  it('interroge les analyses ouvertes sur CETTE non-conformité', async () => {
    await configure([]);
    setup();

    expect(fiveWhys.listForNc).toHaveBeenCalledWith(UUID);
    expect(component.fiveWhys$.value).toEqual([]);
  });

  it('affiche le nombre d\'analyses déjà ouvertes', async () => {
    await configure([buildFiveWhys({ id: 'fw-1' }), buildFiveWhys({ id: 'fw-2' })]);
    setup();

    const compteur = (fixture.nativeElement as HTMLElement).querySelector('.fw-count');
    expect(compteur?.textContent?.trim()).toBe('2');
  });

  it('n\'affiche aucun compteur tant qu\'aucune analyse n\'existe', async () => {
    await configure([]);
    setup();

    expect((fixture.nativeElement as HTMLElement).querySelector('.fw-count')).toBeNull();
  });

  it('ouvre l\'analyse la plus récente sans en créer une seconde', async () => {
    // Le serveur rend les analyses de la plus récente à la plus ancienne.
    await configure([buildFiveWhys({ id: 'fw-recent' }), buildFiveWhys({ id: 'fw-ancienne' })]);
    setup();

    component.openFiveWhys();

    expect(fiveWhys.create).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/five-whys', 'fw-recent']);
  });

  it('crée l\'analyse puis l\'ouvre quand il n\'y en a aucune', async () => {
    await configure([]);
    fiveWhys.create.and.returnValue(of(buildFiveWhys({ id: 'fw-neuve' })));
    setup();

    component.openFiveWhys();

    expect(fiveWhys.create).toHaveBeenCalledWith({ ncId: UUID });
    expect(router.navigate).toHaveBeenCalledWith(['/five-whys', 'fw-neuve']);
    // L'analyse créée rejoint la liste : un second clic ne doit pas en créer une autre.
    expect(component.fiveWhys$.value.map(a => a.id)).toEqual(['fw-neuve']);
  });

  it('ne crée qu\'une analyse même sur double clic', async () => {
    await configure([]);
    const pending = new Subject<FiveWhysAnalysis>();
    fiveWhys.create.and.returnValue(pending.asObservable());
    setup();

    component.openFiveWhys();
    component.openFiveWhys();

    expect(fiveWhys.create).toHaveBeenCalledTimes(1);
    pending.next(buildFiveWhys({ id: 'fw-neuve' }));
    pending.complete();
    expect(component.fiveWhysBusy$.value).toBeFalse();
  });

  it('signale un refus de création sans bloquer le bouton', async () => {
    await configure([]);
    fiveWhys.create.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 409, error: { title: 'Invalid Non-Conformity State' } })));
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    setup();

    component.openFiveWhys();

    expect(snackSpy).toHaveBeenCalled();
    expect(component.fiveWhysBusy$.value).toBeFalse();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('ferme l\'entrée sur une NC clôturée sans analyse', async () => {
    await configure([]);
    setup(buildNc({ id: UUID, status: 'CLOSED' }));

    expect(component.canOpenFiveWhys('CLOSED')).toBeFalse();
    expect(bouton()?.disabled).toBeTrue();
  });

  it('laisse relire l\'analyse d\'une NC clôturée : c\'est elle qui explique la décision', async () => {
    await configure([buildFiveWhys({ id: 'fw-1', rootCause: 'Presse mal réglée' })]);
    setup(buildNc({ id: UUID, status: 'CLOSED' }));

    expect(component.canOpenFiveWhys('CLOSED')).toBeTrue();

    component.openFiveWhys();

    expect(router.navigate).toHaveBeenCalledWith(['/five-whys', 'fw-1']);
  });

  it('laisse la fiche utilisable quand la liste des analyses échoue', async () => {
    await configure([]);
    fiveWhys.listForNc.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    setup();

    expect(component.fiveWhys$.value).toEqual([]);
    expect((fixture.nativeElement as HTMLElement).querySelector('h1')).not.toBeNull();
  });
});
