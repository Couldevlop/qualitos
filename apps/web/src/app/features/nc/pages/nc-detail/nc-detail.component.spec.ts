import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { BehaviorSubject, of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { NcService } from '../../nc.service';
import { NcPhoto, NcResponse, VisionAnalysis } from '../../nc.types';
import { NcDetailComponent } from './nc-detail.component';

function buildNc(overrides: Partial<NcResponse> = {}): NcResponse {
  return {
    id: 'nc-1', reference: 'NC-2026-1001', title: 'Étiquetage manquant',
    category: 'PROCESS', severity: 'MAJOR', status: 'OPEN',
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
    setup(buildNc({ status: 'CLOSED', closedAt: '2026-06-06T00:00:00Z' }), []);
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
});
