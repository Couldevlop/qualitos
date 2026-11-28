import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaEvidence } from '../../capa.types';
import { CapaEvidencePanelComponent } from './capa-evidence-panel.component';

/** Une pièce du dossier : sans action visée. */
function piece(id: string, nom = 'releve.pdf', actionId?: string): CapaEvidence {
  return {
    id, capaId: 'c-1', actionId, contentType: 'application/pdf',
    sizeBytes: 2048, originalFilename: nom, createdAt: '2026-11-19T08:00:00Z',
    url: 'https://exemple.test/' + id
  } as CapaEvidence;
}

describe('CapaEvidencePanelComponent', () => {
  let fixture: ComponentFixture<CapaEvidencePanelComponent>;
  let composant: CapaEvidencePanelComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  function monter(statut = 'IN_PROGRESS'): void {
    fixture = TestBed.createComponent(CapaEvidencePanelComponent);
    composant = fixture.componentInstance;
    composant.capaCaseId = 'c-1';
    composant.statut = statut as never;
    composant.ngOnChanges({ capaCaseId: { currentValue: 'c-1' } } as never);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['listEvidences', 'uploadEvidence', 'deleteEvidence']);
    capa.listEvidences.and.returnValue(of([]));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    await TestBed.configureTestingModule({
      declarations: [CapaEvidencePanelComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: { open: () => undefined } }
      ]
    }).compileComponents();
  });

  it('ne montre que les preuves DU DOSSIER, pas celles des actions', () => {
    // La piece d'une action atteste que CE geste-la a ete fait ; elle se lit
    // dans le tableau du D3, sur sa ligne, et n'a rien a faire ici.
    capa.listEvidences.and.returnValue(of([piece('e-1'), piece('e-2', 'photo.jpg', 'a-9')]));
    monter();

    expect(composant.pieces$.value.map(p => p.id)).toEqual(['e-1']);
    expect(fixture.debugElement.queryAll(By.css('.piece')).length).toBe(1);
  });

  /**
   * LE PANNEAU NE COMMENTE PLUS CE QU'IL N'A PAS.
   *
   * <p>Il ecrivait trois phrases par action -- a quoi sert une preuve,
   * qu'il n'y en a aucune, et que le dossier est clos. Repetees pour
   * chacune des cinq actions d'un ecart, elles remplissaient le D6 d'un
   * texte identique qui noyait les pieces elles-memes. Ce que la preuve
   * atteste se dit une fois, au titre de la discipline.
   */
  it('ne commente pas l absence de preuve : la discipline le dit deja', () => {
    monter();

    expect(fixture.debugElement.query(
      By.css('[data-test="preuves-dossier-vide"]'))).toBeNull();
    expect(fixture.debugElement.query(By.css('.preuves-dossier__aide'))).toBeNull();
    // CE QUI RESTE : le moyen de joindre une piece.
    expect(fixture.debugElement.query(
      By.css('[data-test="preuves-dossier-joindre"]'))).toBeTruthy();
  });

  /**
   * Le verrou de cloture ne s'ecrit plus non plus : une action close se
   * ROUVRE depuis le D8 tant que le dossier est ouvert, donc annoncer que
   * ses preuves « ne changent plus » decourageait a tort.
   */
  it('n annonce plus le verrou sur une action close', () => {
    monter('CLOSED');

    expect(fixture.debugElement.query(By.css('.preuves-dossier__verrou'))).toBeNull();
  });

  it('propose le depot sur un dossier ouvert, et le refuse sur un dossier clos', () => {
    monter();
    expect(composant.peutDeposer).toBeTrue();
    expect(fixture.debugElement.query(By.css('[data-test="preuves-dossier-joindre"]'))).toBeTruthy();

    monter('CLOSED');
    expect(composant.peutDeposer).toBeFalse();
    expect(composant.estTermine).toBeTrue();
    expect(fixture.debugElement.query(By.css('[data-test="preuves-dossier-joindre"]'))).toBeNull();
  });

  /**
   * LE RAPPORT 8D EST UN RESUME. Son D6 rappelle le titre de l'action et celui
   * de ses pieces -- rien d'autre : il repetait sous chaque dossier ce qu'est
   * une preuve, ce qu'il advient d'un dossier clos et ce que vaut un dossier
   * sans piece, et il y proposait le depot. Sur l'ecran du dossier, ou la
   * preuve se DEPOSE, ces phrases et ce geste restent.
   */
  it('se tait et ne propose rien quand le panneau est un resume', () => {
    capa.listEvidences.and.returnValue(of([piece('e-1')]));
    fixture = TestBed.createComponent(CapaEvidencePanelComponent);
    composant = fixture.componentInstance;
    composant.capaCaseId = 'c-1';
    composant.statut = 'IN_PROGRESS' as never;
    composant.sansAide = true;
    composant.lectureSeule = true;
    composant.ngOnChanges({ capaCaseId: { currentValue: 'c-1' } } as never);
    fixture.detectChanges();

    expect(composant.peutDeposer).toBeFalse();
    expect(fixture.debugElement.query(
      By.css('[data-test="preuves-dossier-joindre"]'))).toBeNull();
    expect(fixture.debugElement.query(By.css('.preuves-dossier__aide'))).toBeNull();
    expect(fixture.debugElement.query(By.css('.preuves-dossier__verrou'))).toBeNull();
    // LA PIECE, ELLE, RESTE : c'est ce que le resume doit montrer.
    expect(fixture.debugElement.queryAll(By.css('.piece')).length).toBe(1);
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('releve.pdf');
  });

  it('s\'arrete a dix preuves, en disant pourquoi', () => {
    capa.listEvidences.and.returnValue(of(
      Array.from({ length: 10 }, (_, i) => piece('e-' + i))));
    monter();

    expect(composant.estPlein).toBeTrue();
    expect(composant.peutDeposer).toBeFalse();
    expect(fixture.debugElement.query(By.css('[data-test="preuves-dossier-joindre"]'))).toBeNull();
  });

  it('ajoute la piece deposee a la liste, sans relire tout le dossier', () => {
    monter();
    capa.uploadEvidence.and.returnValue(of(piece('e-neuve', 'controle.pdf')));

    const fichier = new File(['x'], 'controle.pdf', { type: 'application/pdf' });
    const input = { files: [fichier], value: 'c:/faux/controle.pdf' } as unknown as HTMLInputElement;
    composant.fichierChoisi({ target: input } as unknown as Event);

    expect(capa.uploadEvidence).toHaveBeenCalledWith('c-1', fichier);
    expect(composant.pieces$.value.map(p => p.id)).toEqual(['e-neuve']);
    // Vide tout de suite : sans cela, rechoisir le MEME fichier ne declenche rien.
    expect(input.value).toBe('');
    expect(capa.listEvidences).toHaveBeenCalledTimes(1);
  });

  it('annonce le stockage coupe au lieu de proposer un depot voue au refus', () => {
    capa.listEvidences.and.returnValue(throwError(() => ({ status: 501 })));
    monter();

    expect(composant.stockageCoupe).toBeTrue();
    expect(composant.peutDeposer).toBeFalse();
    expect(fixture.debugElement.query(By.css('[data-test="preuves-dossier-stockage-coupe"]'))).toBeTruthy();
    expect(composant.erreur).toBeNull();
  });

  it('dit la panne de lecture plutot que de passer pour un dossier sans preuve', () => {
    capa.listEvidences.and.returnValue(throwError(() => ({ status: 500 })));
    monter();

    expect(composant.erreur).toBeTruthy();
    expect(composant.stockageCoupe).toBeFalse();
    expect(fixture.debugElement.query(By.css('[data-test="preuves-dossier-erreur"]'))).toBeTruthy();
  });

  it('retire la piece apres confirmation, et la garde si on se ravise', () => {
    capa.listEvidences.and.returnValue(of([piece('e-1')]));
    monter();
    capa.deleteEvidence.and.returnValue(of(undefined));

    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    composant.retirer(piece('e-1'));
    expect(capa.deleteEvidence).not.toHaveBeenCalled();
    expect(composant.pieces$.value.length).toBe(1);

    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    composant.retirer(piece('e-1'));
    expect(capa.deleteEvidence).toHaveBeenCalledWith('c-1', 'e-1');
    expect(composant.pieces$.value.length).toBe(0);
  });

  it('nomme la piece dans le libelle d\'accessibilite du retrait', () => {
    monter();
    expect(composant.retirerAria(piece('e-1', 'releve-mars.pdf'))).toContain('releve-mars.pdf');
  });

  /**
   * LE PANNEAU DIT COMBIEN IL PORTE. Le sommaire des huit disciplines coche
   * « preuves » d'apres ce nombre, et seul le panneau le connait : la fiche de
   * l'ecart sait quels dossiers existent, pas ce qu'ils portent.
   */
  it('annonce son compte une fois la lecture finie, zero compris', () => {
    capa.listEvidences.and.returnValue(of([piece('e-1'), piece('e-2', 'photo.jpg', 'a-9')]));
    monter();
    const vus: number[] = [];
    composant.counted.subscribe(n => vus.push(n));

    // Seules les pieces DU DOSSIER comptent : celle qui vise une action se lit
    // sur sa ligne, au D3.
    composant.ngOnChanges({ capaCaseId: { currentValue: 'c-1' } } as never);
    expect(vus).toEqual([1]);

    capa.listEvidences.and.returnValue(of([]));
    composant.ngOnChanges({ capaCaseId: { currentValue: 'c-1' } } as never);
    expect(vus).toEqual([1, 0]);
  });

  it('annonce son compte meme quand la lecture echoue', () => {
    capa.listEvidences.and.returnValue(throwError(() => ({ status: 500 })));
    monter();
    const vus: number[] = [];
    composant.counted.subscribe(n => vus.push(n));

    // Le sommaire ne doit pas rester en attente d'un nombre qui ne viendra plus.
    composant.ngOnChanges({ capaCaseId: { currentValue: 'c-1' } } as never);
    expect(vus).toEqual([0]);
  });

  it('redit son compte quand une piece entre ou sort', () => {
    capa.listEvidences.and.returnValue(of([piece('e-1')]));
    monter();
    const vus: number[] = [];
    composant.counted.subscribe(n => vus.push(n));

    capa.deleteEvidence.and.returnValue(of(undefined));
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    composant.retirer(piece('e-1'));

    expect(vus).toEqual([0]);
  });

  it('dit le poids en unites lisibles', () => {
    monter();
    expect(composant.poids(2048)).toBe('2 Ko');
    expect(composant.poids(2516582)).toBe('2.4 Mo');
  });
});
