import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaCaseResponse, CapaEvidence, CapaStatus } from '../../capa.types';
import { CapaDetailComponent } from './capa-detail.component';

/**
 * Preuves jointes au dossier CAPA (§4.2, ISO 9001 §10.2).
 *
 * <p>Une CAPA se clôt sur une vérification d'efficacité, et l'efficacité se
 * prouve. Ce que l'écran doit tenir : énoncer les bornes plutôt que de les faire
 * heurter, distinguer les refus au lieu de dire « non » sans dire quoi corriger,
 * et ne jamais laisser croire à un dossier sans preuve quand c'est le stockage
 * qui est coupé.
 */
describe('CapaDetailComponent — preuves du dossier', () => {

  const CASE_ID = '44444444-4444-4444-4444-444444444444';

  let fixture: ComponentFixture<CapaDetailComponent>;
  let component: CapaDetailComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const dossier = (status: CapaStatus = 'OPEN'): CapaCaseResponse => ({
    id: CASE_ID, tenantId: 't1', title: 'Étiquetage manquant', type: 'CORRECTIVE',
    criticity: 'HIGH', status, sourceType: 'INTERNAL', ownerId: 'u1',
    createdAt: '2026-08-01T00:00:00Z', updatedAt: '2026-08-01T00:00:00Z', actions: []
  });

  const preuve = (over: Partial<CapaEvidence> = {}): CapaEvidence => ({
    id: 'evd-1', capaId: CASE_ID, contentType: 'application/pdf', sizeBytes: 2048,
    originalFilename: 'releve.pdf', createdAt: '2026-08-07T10:00:00Z',
    url: 'https://stockage.example/x?sig=abc', ...over
  });

  function setup(status: CapaStatus = 'OPEN'): void {
    capa.getCase.and.returnValue(of(dossier(status)));
    fixture = TestBed.createComponent(CapaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  function texte(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  beforeEach(async () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['getCase', 'listEvidences', 'uploadEvidence', 'deleteEvidence',
       'listActionEvidences', 'uploadActionEvidence', 'deleteActionEvidence']);
    capa.listEvidences.and.returnValue(of([]));
    // Les preuves d'actions se chargent aussi à l'ouverture (ADR 0052) : sans
    // ce doublage, la fiche partirait en erreur avant même d'afficher le bloc
    // « Preuves » que ce fichier teste.
    capa.listActionEvidences.and.returnValue(of([]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [CapaDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: jasmine.createSpyObj('Router', ['navigate']) },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: CASE_ID }) } } }
      ]
    }).compileComponents();
  });

  // --- lecture ------------------------------------------------------------------

  it('charge les preuves du dossier à l\'ouverture', () => {
    capa.listEvidences.and.returnValue(of([preuve()]));

    setup();

    expect(capa.listEvidences).toHaveBeenCalledWith(CASE_ID);
    expect(component.evidences$.value.length).toBe(1);
  });

  it('affiche le nom, le poids et un lien de lecture', () => {
    capa.listEvidences.and.returnValue(of([preuve({ sizeBytes: 3 * 1024 * 1024 })]));

    setup();

    expect(texte()).toContain('releve.pdf');
    expect(texte()).toContain('3,0 Mo');
    const lien = (fixture.nativeElement as HTMLElement)
      .querySelector('.evidence__name') as HTMLAnchorElement;
    expect(lien.getAttribute('target')).toBe('_blank');
    // Une URL signée pointe hors application : sans noopener, la page ouverte
    // garde une prise sur la nôtre.
    expect(lien.getAttribute('rel')).toContain('noopener');
  });

  it('dit ce que vaut un dossier sans preuve au lieu de rester muet', () => {
    setup();

    expect(texte()).toContain('Aucune preuve jointe');
  });

  it('annonce le stockage coupé plutôt qu\'un dossier vide', () => {
    // Un 503 rendu comme une liste vide ferait croire à l'absence de preuve :
    // c'est l'inverse de ce qu'un auditeur doit comprendre.
    capa.listEvidences.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 503, error: { type: 'https://qualitos.io/errors/storage-disabled' }
    })));

    setup();

    expect(component.evidenceStorageDisabled$.value).toBeTrue();
    expect(texte()).toContain('stockage des pièces jointes est désactivé');
    expect((fixture.nativeElement as HTMLElement).querySelector('.add-evidence-btn')).toBeNull();
  });

  // --- dépôt -----------------------------------------------------------------------

  it('ajoute la pièce déposée à la liste', () => {
    setup();
    capa.uploadEvidence.and.returnValue(of(preuve({ id: 'evd-neuve' })));

    component.onEvidenceSelected(evenementFichier());

    expect(capa.uploadEvidence).toHaveBeenCalled();
    expect(component.evidences$.value.map(e => e.id)).toEqual(['evd-neuve']);
    expect(component.uploadingEvidence$.value).toBeFalse();
  });

  it('distingue les refus : trop lourd, format, borne atteinte', () => {
    setup();
    const messages: string[] = [];
    snack.open.and.callFake((m: string) => { messages.push(m); return {} as never; });

    [413, 400, 409].forEach(status => {
      capa.uploadEvidence.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
      component.onEvidenceSelected(evenementFichier());
    });

    expect(messages[0]).toContain('10 Mo');
    expect(messages[1]).toContain('Format refusé');
    expect(messages[2]).toContain('limite');
    // Trois refus, trois messages différents : dire « non » sans dire quoi
    // corriger renvoie l'utilisateur à ses suppositions.
    expect(new Set(messages).size).toBe(3);
  });

  it('ne tente rien quand aucun fichier n\'est choisi', () => {
    setup();

    const input = document.createElement('input');
    component.onEvidenceSelected({ target: input } as unknown as Event);

    expect(capa.uploadEvidence).not.toHaveBeenCalled();
  });

  // --- bornes et verrou ---------------------------------------------------------------

  it('énonce la limite atteinte au lieu de la faire découvrir au clic', () => {
    capa.listEvidences.and.returnValue(of(
      Array.from({ length: 10 }, (_, i) => preuve({ id: 'evd-' + i }))));

    setup();

    expect(component.canAddEvidence('OPEN')).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).querySelector('.add-evidence-btn')).toBeNull();
    expect(texte()).toContain('Dix preuves');
    expect(component.evidenceCountLabel()).toBe('10 / 10');
  });

  it('ferme le dépôt sur un dossier clos, mais en laisse lire les preuves', () => {
    capa.listEvidences.and.returnValue(of([preuve()]));

    setup('CLOSED');

    expect(component.canAddEvidence('CLOSED')).toBeFalse();
    expect(texte()).toContain('releve.pdf');
    expect(texte()).toContain('dossier est clos');
  });

  it('ferme aussi le dépôt sur un dossier rejeté', () => {
    setup('REJECTED');

    expect(component.canAddEvidence('REJECTED')).toBeFalse();
  });

  // --- retrait ---------------------------------------------------------------------------

  it('retire la pièce après confirmation', () => {
    capa.listEvidences.and.returnValue(of([preuve({ id: 'evd-1' }), preuve({ id: 'evd-2' })]));
    setup();
    capa.deleteEvidence.and.returnValue(of(void 0));

    component.removeEvidence(preuve({ id: 'evd-1' }));

    expect(dialog.open).toHaveBeenCalled();
    expect(capa.deleteEvidence).toHaveBeenCalledWith(CASE_ID, 'evd-1');
    expect(component.evidences$.value.map(e => e.id)).toEqual(['evd-2']);
  });

  it('ne retire rien si la confirmation est refusée', () => {
    capa.listEvidences.and.returnValue(of([preuve()]));
    setup();
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);

    component.removeEvidence(preuve());

    expect(capa.deleteEvidence).not.toHaveBeenCalled();
    expect(component.evidences$.value.length).toBe(1);
  });

  it('garde la pièce quand le serveur refuse le retrait', () => {
    capa.listEvidences.and.returnValue(of([preuve()]));
    setup();
    capa.deleteEvidence.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.removeEvidence(preuve());

    expect(snack.open).toHaveBeenCalled();
    expect(component.evidences$.value.length).toBe(1);
    expect(component.removingEvidenceId$.value).toBeNull();
  });

  // --- présentation -------------------------------------------------------------------------

  it('rend les poids lisibles', () => {
    setup();

    expect(component.formatSize(512)).toBe('512 o');
    expect(component.formatSize(2048)).toBe('2 Ko');
    expect(component.formatSize(5 * 1024 * 1024)).toBe('5,0 Mo');
  });

  it('donne une icône par famille de document', () => {
    setup();

    expect(component.evidenceIcon('application/pdf')).toBe('picture_as_pdf');
    expect(component.evidenceIcon('image/png')).toBe('image');
    expect(component.evidenceIcon(
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')).toBe('table_chart');
    expect(component.evidenceIcon(
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document')).toBe('description');
    expect(component.evidenceIcon('application/zip')).toBe('attach_file');
  });

  function evenementFichier(): Event {
    const fichier = new File(['%PDF-1.7'], 'releve.pdf', { type: 'application/pdf' });
    const transfert = new DataTransfer();
    transfert.items.add(fichier);
    const input = document.createElement('input');
    input.type = 'file';
    input.files = transfert.files;
    return { target: input } as unknown as Event;
  }
});
