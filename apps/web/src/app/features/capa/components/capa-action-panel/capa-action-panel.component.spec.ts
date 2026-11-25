import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Observable, of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { CapaActionPanelModule } from '../../capa-action-panel.module';
import { CapaService } from '../../capa.service';
import {
  CapaActionResponse, CapaActionStatus, CapaActionType, CapaCaseResponse, CapaEvidence,
  CapaSourceType, CapaStatus, OpenCapaRequest
} from '../../capa.types';
import { CapaActionPanelComponent } from './capa-action-panel.component';

function action(overrides: Partial<CapaActionResponse> = {}): CapaActionResponse {
  return {
    id: 'act-1',
    capaId: 'capa-1',
    title: 'Remplacer le joint du presse-étoupe',
    status: 'PENDING',
    actionType: 'CORRECTIVE',
    assigneeId: 'u-pilote',
    assigneeName: 'Marie Durand',
    ...overrides
  };
}

/** La piece d'une action : celle qui atteste que CE geste a ete fait (ADR 0052). */
function preuve(overrides: Partial<CapaEvidence> = {}): CapaEvidence {
  return {
    id: 'evd-1',
    capaId: 'capa-1',
    actionId: 'act-1',
    contentType: 'application/pdf',
    sizeBytes: 2048,
    originalFilename: 'bon-de-travail.pdf',
    createdAt: '2026-11-02T09:00:00Z',
    url: 'https://stockage.example/p?sig=abc',
    ...overrides
  };
}

/** Un evenement de selection de fichier, tel que le gabarit en produit un. */
function fichierChoisi(): Event {
  const file = new File(['%PDF-1.7'], 'bon-de-travail.pdf', { type: 'application/pdf' });
  const input = document.createElement('input');
  Object.defineProperty(input, 'files', { value: [file] });
  return { target: input } as unknown as Event;
}

function dossier(overrides: Partial<CapaCaseResponse> = {}): CapaCaseResponse {
  return {
    id: 'capa-1',
    tenantId: 't-1',
    title: 'Fuite au presse-étoupe',
    type: 'CORRECTIVE',
    criticity: 'HIGH',
    status: 'IN_PROGRESS',
    sourceType: 'NON_CONFORMITY',
    ownerId: 'u-pilote',
    ownerName: 'Marie Durand',
    dueDate: '2026-12-15',
    createdAt: '2026-11-01T08:00:00Z',
    updatedAt: '2026-11-01T08:00:00Z',
    actions: [action()],
    ...overrides
  };
}

@Component({
  template: `<qos-capa-action-panel [capaCaseIds]="ids" [natures]="natures" [team]="equipe"
                                    [source]="source" [ajoutAutorise]="ajoutAutorise"
                                    [colonneNature]="colonneNature"
                                    (changed)="changements = changements + 1"></qos-capa-action-panel>`,
  standalone: false
})
class HoteComponent {
  ids: string[] = ['capa-1'];
  source: { type: CapaSourceType; reference: string } | undefined =
    { type: 'NON_CONFORMITY', reference: 'NC-2026-004' };
  ajoutAutorise = true;
  /** Vide : toutes les actions. Le rapport 8D, lui, restreint par discipline. */
  natures: CapaActionType[] = [];
  /** L'équipe du D1 de l'écart, quand l'écran appelant en a une. */
  equipe: { accountId: string }[] = [];
  /** Vraie : la fiche CAPA mêle les natures. Le rapport 8D la met à faux. */
  colonneNature = true;
  changements = 0;
}

/**
 * L'action du dossier CAPA, lue et avancée hors de la fiche CAPA.
 *
 * <p>Deux promesses tiennent ici. Le dossier porte <b>plusieurs actions</b>
 * (ADR 0115) : le tableau les montre toutes, une ligne s'ouvre pour se corriger,
 * et le bouton d'ajout en ajoute une. Et une <b>panne se dit</b> : un panneau
 * vide passerait pour « aucune action », ce qui est l'information opposée.
 */
describe('CapaActionPanelComponent', () => {

  let fixture: ComponentFixture<HoteComponent>;
  let hote: HoteComponent;
  let capa: jasmine.SpyObj<CapaService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  /** Ce que rend la fenêtre en se refermant : un dossier, ou rien si on abandonne. */
  let ferme: unknown;

  const racine = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => racine().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => Array.from(racine().querySelectorAll(`[data-test="${sel}"]`));
  const panneau = (): CapaActionPanelComponent =>
    fixture.debugElement.children[0].componentInstance as CapaActionPanelComponent;

  async function setup(
    opts: {
      dossier?: CapaCaseResponse; droits?: boolean; panne?: boolean;
      preuves?: CapaEvidence[];
    } = {}
  ): Promise<void> {
    TestBed.resetTestingModule();

    capa = jasmine.createSpyObj<CapaService>('CapaService',
      ['getCase', 'openCase', 'updateAction', 'deleteAction',
       'listActionEvidences', 'uploadActionEvidence', 'deleteActionEvidence']);
    capa.openCase.and.returnValue(of(dossier({ id: 'capa-neuve' })));
    capa.getCase.and.returnValue(opts.panne
      ? throwError(() => new HttpErrorResponse({ status: 503 }))
      : of(opts.dossier ?? dossier()));
    capa.updateAction.and.returnValue(of(action({ status: 'IN_PROGRESS' })));
    capa.listActionEvidences.and.returnValue(of(opts.preuves ?? []));
    capa.uploadActionEvidence.and.returnValue(of(preuve()));
    capa.deleteActionEvidence.and.returnValue(of(void 0));
    capa.deleteAction.and.returnValue(of(void 0));

    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    ferme = undefined;
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.callFake((() => ({ afterClosed: () => of(ferme) })) as never);

    const annuaire = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search']);
    annuaire.search.and.returnValue(
      of({ content: [], number: 0, size: 20, totalElements: 0, totalPages: 0 }) as never);

    await TestBed.configureTestingModule({
      declarations: [HoteComponent],
      imports: [CapaActionPanelModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: capa },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: DirectoryService, useValue: annuaire },
        // Tous les droits par défaut : ce banc teste le panneau, pas la matrice (ADR 0078).
        { provide: AuthzService, useValue: { can: () => of(opts.droits ?? true) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HoteComponent);
    hote = fixture.componentInstance;
    fixture.detectChanges();
  }

  // ---------- le filtre de nature : D3, D5, D6 du rapport 8D ----------

  /**
   * Le rapport 8D nomme les trois gestes separement : endiguer (D3), corriger la
   * cause (D5), prevenir la recidive (D6). Chaque discipline recoit donc le
   * tableau de SA nature ; le tableau complet repete sous trois titres
   * laisserait chercher, sous chacun, les lignes qui le concernent.
   */
  it('ne montre que les actions de la nature demandée', async () => {
    await setup({
      dossier: dossier({
        actions: [
          action({ id: 'a-c', title: 'Remplacer le joint', actionType: 'CORRECTIVE' }),
          action({ id: 'a-p', title: 'Former l’équipe de nuit', actionType: 'PREVENTIVE' }),
          action({ id: 'a-e', title: 'Trier le lot', actionType: 'CONTAINMENT' })
        ]
      })
    });

    hote.natures = ['PREVENTIVE'];
    fixture.detectChanges();

    const lignes = tous('capa-action-ligne');
    expect(lignes.length).toBe(1);
    expect(lignes[0].textContent).toContain('Former l’équipe de nuit');
  });

  /**
   * Une action sans nature est corrective — c'est le defaut du serveur. L'exclure
   * du D5 ferait disparaitre du rapport les actions des anciens dossiers.
   */
  it('range l’action sans nature parmi les correctives', async () => {
    await setup({
      dossier: dossier({ actions: [action({ actionType: undefined })] })
    });

    hote.natures = ['CORRECTIVE'];
    fixture.detectChanges();

    expect(tous('capa-action-ligne').length).toBe(1);
  });

  /**
   * Deux absences differentes, et les confondre trompe : un dossier SANS AUCUNE
   * action ne pourra pas etre cloture ; un tableau restreint a une nature peut
   * etre vide alors que le dossier avance tres bien.
   */
  it('ne crie pas à la clôture impossible quand c’est seulement la nature qui manque', async () => {
    await setup();

    hote.natures = ['PREVENTIVE'];
    fixture.detectChanges();

    expect(un('capa-action-vide-nature')).not.toBeNull();
    expect(un('capa-action-vide')!.textContent).not.toContain('clôturé');
  });

  /**
   * Le tableau ne montre qu'une nature : la CAPA qu'on y ouvre est de celle-la,
   * et la case est VERROUILLEE -- choisie dans la fenetre, une action ecrite au
   * D7 pouvait naitre corrective, et ce D7 restait vide (ADR 0130, 0135).
   */
  it('impose la nature du tableau à la CAPA ouverte, et la verrouille', async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    hote.natures = ['PREVENTIVE'];
    fixture.detectChanges();
    ferme = true;

    un('capa-action-definir')!.click();

    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: { prefill?: { type?: string }; lockType?: boolean };
    }).data;
    expect(data.prefill?.type).toBe('PREVENTIVE');
    expect(data.lockType).toBeTrue();
  });

  /**
   * LE PANNEAU PORTE L'OUVERTURE, et ne la laisse pas a la fenetre : c'est lui
   * qui sait par quelle route le dossier se cree en citant l'ecart. La fenetre
   * recoit donc un `open`, qu'elle appellera quand le formulaire sera rempli --
   * sans lui, elle retomberait sur la creation libre du module CAPA et le
   * dossier naitrait sans son ecart (ADR 0121, 0135).
   */
  it('confie a la fenetre la route qui ouvre la CAPA, et en rend l\u2019identifiant',
     async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    hote.natures = ['CONTAINMENT'];
    fixture.detectChanges();
    ferme = true;

    un('capa-action-definir')!.click();

    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: { open?: (r: OpenCapaRequest) => Observable<{ capaId: string }> };
    }).data;
    expect(data.open).toBeDefined();

    let rendu: { capaId: string } | undefined;
    data.open!({ title: 'Isoler le lot' } as OpenCapaRequest)
      .subscribe(o => (rendu = o));

    expect(capa.openCase).toHaveBeenCalledWith(
      jasmine.objectContaining({ title: 'Isoler le lot' }));
    // L'identifiant du dossier ouvert, et lui seul : la fenetre n'a que faire
    // du reste de la reponse.
    expect(rendu).toEqual({ capaId: 'capa-neuve' });
  });

  /**
   * LE BOUTON DIT CE QU'IL AJOUTE.
   *
   * <p>« Ajouter une action » sous le tableau du confinement, puis le meme mot
   * sous celui des correctives et sous celui des preventives : trois boutons
   * identiques pour trois gestes differents, et rien, avant le clic, ne disait
   * lequel on allait ecrire.
   */
  it('nomme la nature dans le bouton d’ajout quand le tableau n’en montre qu’une', async () => {
    await setup({ dossier: dossier({ actions: [] }) });

    hote.natures = ['CONTAINMENT'];
    fixture.detectChanges();
    expect(un('capa-action-definir')!.textContent).toContain('confinement');

    hote.natures = ['CORRECTIVE'];
    fixture.detectChanges();
    expect(un('capa-action-definir')!.textContent).toContain('corrective');

    hote.natures = ['PREVENTIVE'];
    fixture.detectChanges();
    expect(un('capa-action-definir')!.textContent).toContain('préventive');

    // Le tableau complet, lui, n'a aucune nature à nommer.
    hote.natures = [];
    fixture.detectChanges();
    expect(un('capa-action-definir')!.textContent).toContain('Ajouter une action');
  });

  // ---------- lecture ----------

  it('lit le dossier dont on lui donne l’identifiant', async () => {
    await setup();

    expect(capa.getCase).toHaveBeenCalledWith('capa-1');
    expect(tous('capa-action-ligne').length).toBe(1);
    expect(un('capa-action-ligne')!.textContent).toContain('Remplacer le joint');
  });

  it('ne charge rien sans identifiant', async () => {
    await setup();
    capa.getCase.calls.reset();

    hote.ids = [];
    fixture.detectChanges();

    expect(capa.getCase).not.toHaveBeenCalled();
    expect(tous('capa-action-ligne').length).toBe(0);
  });

  /**
   * LE DEFAUT REMONTE DE LA PRODUCTION. Une CAPA nait avec son action : en
   * ecrire une depuis la section CAPA ouvre donc un NOUVEAU dossier sur le meme
   * ecart. Le panneau etant rendu une fois par dossier, cette action paraissait
   * dans un second tableau sous le premier -- « comme si elle refusait de se
   * mettre dans le tableau existant ». Une discipline n'a qu'un tableau.
   */
  it('rassemble en UN seul tableau les actions de plusieurs dossiers', async () => {
    await setup();
    capa.getCase.and.callFake(((id: string) => of(dossier({
      id,
      actions: [action({
        id: 'act-' + id,
        title: id === 'capa-1' ? 'Trier le lot suspect' : 'Bloquer la ligne 3'
      })]
    }))) as never);

    hote.ids = ['capa-1', 'capa-2'];
    fixture.detectChanges();

    expect(tous('capa-actions-table').length).toBe(1);
    expect(tous('capa-action-ligne').length).toBe(2);
    expect(racine().textContent).toContain('Trier le lot suspect');
    expect(racine().textContent).toContain('Bloquer la ligne 3');
  });

  /**
   * Le verrou de cloture s'applique LIGNE PAR LIGNE, puisque le tableau mele
   * des dossiers : les actions d'un dossier clos ne bougent plus, celles de son
   * voisin ouvert continuent. Un verrou applique au tableau entier ferait
   * dependre l'un de l'autre.
   */
  it('verrouille la ligne d\'un dossier clos sans geler celle du dossier ouvert',
    async () => {
      await setup();
      capa.getCase.and.callFake(((id: string) => of(dossier({
        id,
        status: id === 'capa-clos' ? 'CLOSED' : 'IN_PROGRESS',
        actions: [action({ id: 'act-' + id, status: 'PENDING' })]
      }))) as never);

      hote.ids = ['capa-clos', 'capa-ouvert'];
      fixture.detectChanges();

      const avancer = tous('capa-action-avancer') as HTMLButtonElement[];
      expect(avancer.length).toBe(2);
      expect(avancer[0].disabled).toBeTrue();
      expect(avancer[1].disabled).toBeFalse();
    });

  /**
   * LE RAPPORT 8D N'A PAS BESOIN DE LA COLONNE : chacune de ses disciplines ne
   * montre QU'UNE nature -- l'endiguement au D3, le correctif au D5, la
   * prevention au D7 --, et le mot se repetait donc sur toutes les lignes, en
   * tete de tableau, devant l'intitule qu'on vient lire.
   */
  it('retire la colonne des natures quand l\'ecran n’en montre qu’une', async () => {
    await setup();
    hote.colonneNature = false;
    fixture.detectChanges();

    const entetes = Array.from(racine().querySelectorAll('th'))
      .map(th => (th.textContent ?? '').trim());
    expect(entetes).not.toContain("Nature de l'action");
    // L'intitule reste la premiere colonne : c'est ce qu'on vient lire.
    expect(entetes[0]).toBe("Intitulé de l'action");
    const premiere = tous('capa-action-ligne')[0];
    expect(premiere.querySelectorAll('td').length).toBe(entetes.length);
  });

  /** Par defaut elle reste : la fiche CAPA mele les trois natures. */
  it('garde la colonne des natures par défaut', async () => {
    await setup();

    const entetes = Array.from(racine().querySelectorAll('th'))
      .map(th => (th.textContent ?? '').trim());
    expect(entetes).toContain("Nature de l'action");
  });

  it('nomme la nature de l’action en clair, jamais son code', async () => {
    await setup();

    expect(un('capa-action-ligne')!.textContent).toContain('Corrective');
    expect(un('capa-action-ligne')!.textContent).not.toContain('CORRECTIVE');
  });

  // Le tableau porte des COLONNES : chaque action y dit son porteur et son
  // echeance, meme quand ils sont ceux du dossier. Une cellule laissee vide
  // selon la valeur du dossier rendrait la colonne illisible d'une ligne a
  // l'autre, et c'est precisement ce qu'on vient comparer.
  it('donne le responsable et l’échéance de chaque action', async () => {
    await setup({
      dossier: dossier({
        actions: [
          action(),
          action({ id: 'act-2', title: 'Former l’équipe de nuit', actionType: 'PREVENTIVE',
                   assigneeId: 'u-autre', assigneeName: 'Ali Benali', dueDate: '2026-11-30' })
        ]
      })
    });

    const lignes = tous('capa-action-ligne');
    expect(lignes.length).toBe(2);
    expect(lignes[0].textContent).toContain('Marie Durand');
    expect(lignes[1].textContent).toContain('Ali Benali');
    expect(lignes[1].textContent).toContain('Préventive');
    expect(un('capa-actions-table')!.textContent).toContain('Responsable');
  });

  it('affiche un tiret plutôt qu’un identifiant quand le nom du porteur manque', async () => {
    await setup({
      dossier: dossier({ actions: [action({ assigneeId: 'u-autre', assigneeName: undefined })] })
    });

    const texte = un('capa-action-ligne')!.textContent!;
    expect(texte).toContain('—');
    expect(texte).not.toContain('u-autre');
  });

  // ---------- faire avancer ----------

  it('fait avancer l’action d’un cran, statut seul', async () => {
    await setup();

    un('capa-action-avancer')!.click();
    fixture.detectChanges();

    // Seul le statut part : renvoyer le libellé écraserait une correction faite
    // entre-temps par quelqu'un d'autre.
    expect(capa.updateAction).toHaveBeenCalledWith('capa-1', 'act-1', { status: 'IN_PROGRESS' });
    expect(snack.open).toHaveBeenCalled();
  });

  it('propose de terminer une action déjà démarrée', async () => {
    await setup({ dossier: dossier({ actions: [action({ status: 'IN_PROGRESS' })] }) });

    un('capa-action-avancer')!.click();

    expect(capa.updateAction).toHaveBeenCalledWith('capa-1', 'act-1', { status: 'DONE' });
  });

  it('annonce le changement à l’écran qui l’héberge', async () => {
    await setup();

    un('capa-action-avancer')!.click();

    expect(hote.changements).toBe(1);
  });

  it('relit le dossier après avoir avancé', async () => {
    await setup();
    capa.getCase.calls.reset();

    un('capa-action-avancer')!.click();

    expect(capa.getCase).toHaveBeenCalledTimes(1);
  });

  it('dit l’échec d’une mise à jour', async () => {
    await setup();
    capa.updateAction.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));

    panneau().advance(action());

    expect(snack.open).toHaveBeenCalled();
    expect(hote.changements).toBe(0);
    // Le geste se rejoue : un panneau resté « en vol » après une panne se fige.
    expect(panneau().acting$.value).toBeFalse();
  });

  it('n’offre plus de geste sur une action faite', async () => {
    await setup({ dossier: dossier({ actions: [action({ status: 'DONE' })] }) });

    expect(un('capa-action-avancer')).toBeNull();
    expect(panneau().nextStatus('DONE')).toBeNull();
  });

  // Le serveur tient la même garde : proposer le geste serait proposer un refus.
  it('n’offre aucun geste sur un dossier clos ou rejeté', async () => {
    for (const status of ['CLOSED', 'REJECTED'] as CapaStatus[]) {
      await setup({ dossier: dossier({ status }) });

      expect(panneau().canAdvance(action())).withContext(status).toBeFalse();
      expect((un('capa-action-avancer') as HTMLButtonElement).disabled).withContext(status).toBeTrue();
    }
  });

  it('n’offre aucun geste sans le droit de faire avancer', async () => {
    await setup({ droits: false });

    expect(un('capa-action-avancer')).toBeNull();
    expect(tous('capa-action-ligne').length).toBe(1);
  });

  it('ne lance pas deux mises à jour de front', async () => {
    await setup();
    panneau().acting$.next(true);

    panneau().advance(action());

    expect(capa.updateAction).not.toHaveBeenCalled();
  });

  // ---------- ajouter une action, ouvrir une ligne ----------

  it('dit qu’un dossier sans action ne pourra pas être clôturé', async () => {
    await setup({ dossier: dossier({ actions: [] }) });

    expect(un('capa-action-vide')).not.toBeNull();
    expect(un('capa-action-vide')!.textContent).toContain('clôturé');
    expect(un('capa-action-definir')).not.toBeNull();
  });

  /**
   * AJOUTER, C'EST OUVRIR UNE CAPA, et non remplir le dossier voisin : une CAPA
   * porte une action, et une seule (ADR 0135). Ajoutee dans le dossier d'a cote,
   * l'action n'apparaissait pas dans le registre des CAPA -- il montrait son
   * dossier, titre d'une autre action -- et l'aller-retour entre le registre et
   * le 8D ne marchait que dans un sens.
   *
   * <p>La CAPA ouverte CITE L'ECART : c'est ce lien que la fiche de l'ecart et
   * son rapport 8D suivent pour rassembler les dossiers (ADR 0121). Sa criticite
   * est heritee du dossier deja la : c'est celle de l'ecart, et la redecider
   * dossier par dossier ferait diverger deux chiffres sur un seul fait.
   */
  it('ouvre une CAPA citant l’écart, de la criticité du dossier déjà là', async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    ferme = true;

    un('capa-action-definir')!.click();

    expect(dialog.open).toHaveBeenCalled();
    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: {
        caseId?: string;
        source?: { type: string; reference: string };
        prefill?: { criticity?: string };
      };
    }).data;
    expect(data.caseId).toBeUndefined();
    expect(data.source?.reference).toBe('NC-2026-004');
    expect(data.source?.type).toBe('NON_CONFORMITY');
    expect(data.prefill?.criticity).toBe('HIGH');
    expect(hote.changements).toBe(1);
  });

  it('propose d’ajouter une action même quand il y en a déjà une', async () => {
    await setup();

    expect(un('capa-action-ajouter')).not.toBeNull();
    (un('capa-action-ajouter') as HTMLButtonElement).click();

    expect(dialog.open).toHaveBeenCalled();
  });

  it('n’ajoute rien quand la fenêtre est abandonnée', async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    capa.getCase.calls.reset();
    ferme = undefined;

    un('capa-action-definir')!.click();

    expect(hote.changements).toBe(0);
    expect(capa.getCase).not.toHaveBeenCalled();
  });

  /**
   * LE DOSSIER CLOS NE FERME PLUS L'AJOUT : la CAPA qu'on ouvre est NOUVELLE, et
   * celle d'a cote, reglee, ne la concerne pas. Tant qu'ajouter voulait dire une
   * action de plus dans ce dossier, son verrou de cloture faisait office de
   * frein ; il n'a plus rien a dire ici (ADR 0135).
   */
  it('ouvre quand même une CAPA quand le dossier d’à côté est clos', async () => {
    await setup({ dossier: dossier({ actions: [], status: 'CLOSED' }) });
    ferme = true;

    expect((un('capa-action-definir') as HTMLButtonElement).disabled).toBeFalse();
    un('capa-action-definir')!.click();

    expect(dialog.open).toHaveBeenCalled();
  });

  /**
   * LE FREIN VIENT DE L'ECART. Le verrou du dossier voisin ayant cesse d'en
   * tenir lieu, sans cette entree on pourrait decider une action sur un ecart
   * termine -- et la cloture de l'ecart l'attendrait.
   */
  it('n’ouvre aucune CAPA quand l’écart n’en accepte plus', async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    hote.ajoutAutorise = false;
    fixture.detectChanges();

    // ETEINT, et non retire : c'est ce que le panneau faisait deja du verrou de
    // cloture du dossier. Le bouton disparu, l'ecran ne dirait plus qu'on PEUT
    // decider des actions ici -- il se lirait comme un tableau en lecture seule.
    expect((un('capa-action-definir') as HTMLButtonElement).disabled).toBeTrue();
    panneau().addAction();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  /**
   * Sans ecart a citer, pas d'ajout : une CAPA se rattache a l'ecart qu'elle
   * traite, et en ouvrir une sans savoir lequel donnerait un dossier orphelin,
   * que ni la fiche de l'ecart ni son rapport 8D ne verraient.
   */
  it('éteint le bouton d’ajout quand aucun écart n’est cité', async () => {
    await setup({ dossier: dossier({ actions: [] }) });
    hote.source = undefined;
    fixture.detectChanges();

    expect((un('capa-action-definir') as HTMLButtonElement).disabled).toBeTrue();
    panneau().addAction();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  /**
   * Une ligne s'ouvre d'un clic : c'est la que l'action se corrige -- un
   * intitule a reformuler, un porteur parti, une echeance a decaler. Avant, il
   * fallait rouvrir un dossier pour changer une phrase.
   */
  it('ouvre l’action cliquée pour la corriger', async () => {
    await setup();
    ferme = true;

    un('capa-action-ligne')!.click();

    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: { caseId: string; action: { id: string }; readOnly?: boolean };
    }).data;
    expect(data.caseId).toBe('capa-1');
    expect(data.action.id).toBe('act-1');
    expect(data.readOnly).toBeFalse();
    expect(hote.changements).toBe(1);
  });

  /**
   * Un dossier termine s'ouvre EN LECTURE SEULE plutot que de ne pas s'ouvrir :
   * relire ce qui a ete fait est legitime apres la cloture, et une ligne qui ne
   * reagit pas au clic passe pour une panne.
   */
  it('ouvre en lecture seule l’action d’un dossier clos', async () => {
    await setup({ dossier: dossier({ status: 'CLOSED' }) });

    un('capa-action-ligne')!.click();

    const data = (dialog.open.calls.mostRecent().args[1] as {
      data: { readOnly?: boolean };
    }).data;
    expect(data.readOnly).toBeTrue();
  });

  /** Avancer d'un cran et corriger l'action sont deux gestes : le bouton ne doit pas ouvrir la ligne. */
  it('ne confond pas l’avancement d’un cran avec l’ouverture de la ligne', async () => {
    await setup();

    un('capa-action-avancer')!.click();

    expect(capa.updateAction).toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  // ---------- la panne ----------

  it('dit la panne au lieu de laisser croire qu’il n’y a pas d’action', async () => {
    await setup({ panne: true });

    expect(un('capa-action-erreur')).not.toBeNull();
    expect(un('capa-action-vide')).toBeNull();
    expect(tous('capa-action-ligne').length).toBe(0);
  });

  it('se relance après une panne', async () => {
    await setup({ panne: true });
    capa.getCase.and.returnValue(of(dossier()));

    un('capa-action-relancer')!.click();
    fixture.detectChanges();

    expect(un('capa-action-erreur')).toBeNull();
    expect(tous('capa-action-ligne').length).toBe(1);
  });

  // ---------- les anciens dossiers ----------

  // Les dossiers d'avant l'ADR 0088 portent plusieurs actions : on les montre
  // toutes, et on n'en ajoute pas pour autant.
  it('montre toutes les actions d’un ancien dossier', async () => {
    await setup({
      dossier: dossier({
        actions: [action(), action({ id: 'act-2', title: 'Revoir la gamme de montage' })]
      })
    });

    expect(tous('capa-action-ligne').length).toBe(2);
    expect(un('capa-action-definir')).toBeNull();
  });

  it('nomme chaque statut en clair', async () => {
    await setup();

    const libelles = (['PENDING', 'IN_PROGRESS', 'DONE'] as CapaActionStatus[])
      .map(s => panneau().actionStatusLabel(s));
    expect(new Set(libelles).size).toBe(3);
    expect(libelles.every(l => l.length > 0)).toBeTrue();
  });

  // ---------- la colonne Preuve (ADR 0052, ADR 0117) ----------

  /**
   * La piece d'une ACTION atteste que ce geste-la a ete fait. A ne pas
   * confondre avec les preuves du DOSSIER, restees sur la fiche, qui attestent
   * que le probleme a cesse (ISO 9001 §10.2) : un dossier peut tres bien avoir
   * toutes ses actions prouvees et aucune preuve d'efficacite.
   */
  it('range la piece sous son action', async () => {
    await setup({ preuves: [preuve()] });

    const cellule = un('capa-action-preuve');
    expect(cellule).not.toBeNull();
    expect(cellule!.textContent).toContain('bon-de-travail.pdf');
    expect(panneau().preuve('act-1')?.id).toBe('evd-1');
  });

  it('propose de joindre quand l\'action n\'a pas encore sa piece', async () => {
    await setup();

    expect(un('capa-action-preuve')!.textContent).toContain('Joindre');
    expect(panneau().peutJoindre(action())).toBeTrue();
  });

  /**
   * Le selecteur de fichier est UNIQUE pour tout le tableau : sans memoire de
   * l'action visee, le fichier choisi atterrirait sur la derniere ligne rendue
   * plutot que sur celle ou l'on a clique.
   */
  it('depose la piece sur l\'action visee, et pas sur une autre', async () => {
    await setup({
      dossier: dossier({ actions: [action(), action({ id: 'act-2', title: 'Former l\'equipe' })] })
    });
    const input = document.createElement('input');
    spyOn(input, 'click');

    panneau().choisirFichier(input, 'act-2');
    panneau().fichierChoisi(fichierChoisi());

    expect(input.click).toHaveBeenCalled();
    expect(capa.uploadActionEvidence).toHaveBeenCalledWith('capa-1', 'act-2', jasmine.any(File));
  });

  it('ne tente rien quand aucun fichier n\'est choisi', async () => {
    await setup();
    const vide = document.createElement('input');
    Object.defineProperty(vide, 'files', { value: [] });

    panneau().fichierChoisi({ target: vide } as unknown as Event);

    expect(capa.uploadActionEvidence).not.toHaveBeenCalled();
  });

  /**
   * Chaque refus a sa raison : les confondre reviendrait a dire « non » sans
   * dire quoi corriger.
   */
  it('distingue les refus de depot : trop lourd, format, action pourvue, action disparue',
     async () => {
    await setup();
    const vus: string[] = [];
    snack.open.and.callFake(((message: string) => { vus.push(message); }) as never);

    for (const status of [413, 400, 409, 404]) {
      capa.uploadActionEvidence.and.returnValue(
        throwError(() => new HttpErrorResponse({ status })));
      panneau().choisirFichier(document.createElement('input'), 'act-1');
      panneau().fichierChoisi(fichierChoisi());
    }

    expect(vus.length).toBe(4);
    expect(new Set(vus).size).toBe(4);
  });

  /**
   * Stockage coupe sur l'environnement : la colonne le dit en cessant de
   * proposer un depot voue au refus, plutot que de crier a la panne.
   */
  it('annonce le stockage coupe au lieu de proposer un depot voue au refus', async () => {
    await setup();
    capa.uploadActionEvidence.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 503, error: { type: 'urn:qualitos:storage-disabled' } })));

    panneau().choisirFichier(document.createElement('input'), 'act-1');
    panneau().fichierChoisi(fichierChoisi());

    expect(panneau().stockageCoupe).toBeTrue();
    expect(panneau().peutJoindre(action())).toBeFalse();
  });

  it('supprime l\'action apres confirmation, et la garde si on se ravise', async () => {
    await setup();

    ferme = false;
    panneau().supprimerAction(action());
    expect(capa.deleteAction).not.toHaveBeenCalled();

    ferme = true;
    panneau().supprimerAction(action());
    expect(capa.deleteAction).toHaveBeenCalledWith('capa-1', 'act-1');
  });

  it('garde l\'action quand la suppression echoue, et le dit', async () => {
    await setup();
    capa.deleteAction.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })));
    ferme = true;

    panneau().supprimerAction(action());

    expect(snack.open).toHaveBeenCalled();
  });

  /** Un dossier clos est un ecrit arrete : en retirer une action le reecrirait. */
  it('ne supprime rien sur un dossier clos, et n\'offre pas le geste', async () => {
    await setup({ dossier: dossier({ status: 'CLOSED' }) });
    ferme = true;

    expect(panneau().peutSupprimer(action())).toBeFalse();
    panneau().supprimerAction(action());
    expect(capa.deleteAction).not.toHaveBeenCalled();
  });

  it('nomme l\'action dans l\'etiquette de la poubelle', async () => {
    await setup();
    // Huit lignes rendraient huit poubelles indiscernables au lecteur d'ecran.
    expect(panneau().ariaSupprimer(action())).toContain('Remplacer le joint');
  });

  it('retire la piece apres confirmation, et la garde si on se ravise', async () => {
    await setup({ preuves: [preuve()] });

    ferme = false;
    panneau().retirerPreuve(action(), preuve());
    expect(capa.deleteActionEvidence).not.toHaveBeenCalled();

    ferme = true;
    panneau().retirerPreuve(action(), preuve());
    expect(capa.deleteActionEvidence).toHaveBeenCalledWith('capa-1', 'act-1', 'evd-1');
    expect(panneau().preuve('act-1')).toBeUndefined();
  });

  it('garde la piece quand le retrait echoue, et le dit', async () => {
    await setup({ preuves: [preuve()] });
    capa.deleteActionEvidence.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })));
    ferme = true;

    panneau().retirerPreuve(action(), preuve());

    expect(snack.open).toHaveBeenCalled();
    expect(panneau().preuve('act-1')).toBeDefined();
  });

  /** Un dossier clos garde ses preuves lisibles, sans laisser y toucher. */
  it('verrouille la preuve sur un dossier clos, sans la cacher', async () => {
    await setup({ dossier: dossier({ status: 'CLOSED' }), preuves: [preuve()] });

    expect(un('capa-action-preuve')!.textContent).toContain('bon-de-travail.pdf');
    expect(panneau().peutJoindre(action())).toBeFalse();
  });

  /**
   * Une icone seule ou un mot isole ne dit pas SUR QUOI il agit : au lecteur
   * d'ecran, huit lignes rendraient huit boutons « Joindre » indiscernables.
   */
  it('nomme l\'action visee dans les libelles d\'accessibilite', async () => {
    await setup();

    expect(panneau().joindreAria('Remplacer le joint')).toContain('Remplacer le joint');
    expect(panneau().retirerAria('Remplacer le joint')).toContain('Remplacer le joint');
  });

  it('donne une icone par famille de document', async () => {
    await setup();
    const p = panneau();

    expect(p.iconePreuve('application/pdf')).toBe('picture_as_pdf');
    expect(p.iconePreuve('image/png')).toBe('image');
    expect(p.iconePreuve('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'))
      .toBe('table_chart');
    expect(p.iconePreuve('application/vnd.openxmlformats-officedocument.wordprocessingml.document'))
      .toBe('description');
    expect(p.iconePreuve('application/octet-stream')).toBe('attach_file');
  });

  /**
   * Le tableau reste LISIBLE quand les pieces ne se chargent pas : l'avancement
   * des actions ne depend pas du stockage, et une erreur en travers de l'ecran
   * empecherait de travailler pour une colonne.
   */
  it('reste lisible quand les pieces ne se chargent pas', async () => {
    await setup();
    capa.listActionEvidences.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 503, error: { type: 'urn:qualitos:storage-disabled' } })));

    panneau().reload();
    fixture.detectChanges();

    expect(tous('capa-action-ligne').length).toBe(1);
    expect(panneau().stockageCoupe).toBeTrue();
    expect(un('capa-action-erreur')).toBeNull();
  });

  /**
   * LA DATE DE DÉBUT ET L'ÉCHÉANCE, et leur ordre.
   *
   * <p>Le tableau montrait l'échéance seule. Une action dont l'échéance précède
   * son début est presque toujours une faute de frappe, et elle fausse tous les
   * décomptes de retard sans que rien ne l'annonce.
   */

  // ---------- la verification promise action par action (ADR 0132) ----------

  /**
   * Une action « faite » dit que le geste a eu lieu ; elle ne dit pas qu'il a
   * produit son effet. Celle qui a PROMIS une verification (ADR 0131) n'est
   * close que verifiee, et le dossier ne se cloture pas avant : c'est ici,
   * sur la ligne meme ou l'on vient de la terminer, que la promesse se solde.
   */
  describe('juger l’efficacite d’une action', () => {

    const promise = (p: Partial<CapaActionResponse> = {}) => action({
      status: 'DONE', verificationRequired: true,
      effectivenessCriterion: 'Aucun rebut pendant 3 mois', ...p
    });

    it('propose de verifier une action faite qui a promis une verification', async () => {
      await setup({ dossier: dossier({ actions: [promise()] }) });

      expect(panneau().attendVerification(promise())).toBeTrue();
      expect(un('capa-action-verifier')).not.toBeNull();
    });

    it('ne le propose pas quand rien n’a ete promis : le statut « faite » suffit', async () => {
      await setup({ dossier: dossier({ actions: [action({ status: 'DONE' })] }) });

      expect(panneau().attendVerification(action({ status: 'DONE' }))).toBeFalse();
      expect(un('capa-action-verifier')).toBeNull();
    });

    it('ne le propose pas avant que le geste ait eu lieu', async () => {
      const pas_faite = promise({ status: 'IN_PROGRESS' });
      await setup({ dossier: dossier({ actions: [pas_faite] }) });

      expect(panneau().attendVerification(pas_faite)).toBeFalse();
      expect(un('capa-action-verifier')).toBeNull();
    });

    /**
     * Un verdict PARTIEL ou INEFFICACE ne leve pas l'obstacle : l'action a ete
     * regardee, et ce qu'on a vu ne permet pas de clore. C'est exactement ce
     * que le serveur refuse a la cloture.
     */
    it('continue de le proposer apres un verdict insuffisant', async () => {
      const partielle = promise({ verificationVerdict: 'PARTIAL' });
      await setup({ dossier: dossier({ actions: [partielle] }) });

      expect(panneau().attendVerification(partielle)).toBeTrue();
      expect(panneau().efficaciteConstatee(partielle)).toBeFalse();
      expect(un('capa-action-verifier')).not.toBeNull();
    });

    it('affiche l’efficacite constatee, avec qui a juge et quand', async () => {
      const jugee = promise({
        verificationVerdict: 'EFFECTIVE',
        verifiedAt: '2026-11-20T10:00:00Z',
        verifiedByName: 'Amina Dridi'
      });
      await setup({ dossier: dossier({ actions: [jugee] }) });

      expect(panneau().efficaciteConstatee(jugee)).toBeTrue();
      expect(un('capa-action-verifiee')).not.toBeNull();
      expect(un('capa-action-verifier')).toBeNull();
      // Un jugement sans auteur ne vaut rien devant un auditeur.
      expect(panneau().verdictTooltip(jugee)).toContain('Amina Dridi');
      expect(panneau().verdictTooltip(jugee)).toContain('2026-11-20');
    });

    it('ouvre la fenetre avec l’action ET le critere du dossier', async () => {
      const sans_critere = promise({ effectivenessCriterion: undefined });
      await setup({
        dossier: dossier({
          actions: [sans_critere],
          effectivenessCriterion: 'Zero retard sur 620 expeditions'
        })
      });

      panneau().verifier(sans_critere);

      const donnees = (dialog.open.calls.mostRecent().args[1] as { data: Record<string, unknown> }).data;
      expect(donnees['caseId']).toBe('capa-1');
      expect(donnees['action']).toBe(sans_critere);
      // Les actions anterieures aux colonnes n'ont pas le leur : la fenetre
      // doit pouvoir dire a quoi elle confronte le releve.
      expect(donnees['caseCriterion']).toBe('Zero retard sur 620 expeditions');
    });

    it('recharge le tableau quand un verdict a ete enregistre', async () => {
      await setup({ dossier: dossier({ actions: [promise()] }) });
      capa.getCase.calls.reset();
      ferme = true;

      panneau().verifier(promise());

      expect(capa.getCase).toHaveBeenCalled();
    });
  });

  describe('la date de début et l’ordre des deux dates', () => {

    it('montre la date de début de chaque action', async () => {
      await setup({
        dossier: dossier({ actions: [action({ decidedOn: '2026-11-02', dueDate: '2026-12-01' })] })
      });
      expect(un('capa-actions-table')!.textContent).toContain('Nov');
      expect(panneau().echeanceAvantDebut(
        action({ decidedOn: '2026-11-02', dueDate: '2026-12-01' }))).toBeFalse();
    });

    it('signale une échéance antérieure à la date de début, en mots et pas seulement en couleur',
      async () => {
        await setup({
          dossier: dossier({ actions: [action({ decidedOn: '2026-12-01', dueDate: '2026-11-02' })] })
        });
        const cellule = un('capa-actions-table')!
          .querySelector('.actions__echeance') as HTMLElement;
        expect(cellule.classList).toContain('actions__echeance--avant-debut');
        // La couleur ne peut pas porter seule l'information.
        expect(cellule.getAttribute('title')).toBeTruthy();
        expect(cellule.textContent).toContain('antérieure');
      });

    it('compare les jours sans passer par le fuseau du navigateur', async () => {
      await setup({ dossier: dossier({ actions: [action()] }) });
      // Deux dates ÉGALES ne sont pas en désordre. Une comparaison via
      // `new Date()` pourrait les ordonner selon le fuseau.
      expect(panneau().echeanceAvantDebut(
        action({ decidedOn: '2026-11-02', dueDate: '2026-11-02' }))).toBeFalse();
      expect(panneau().echeanceAvantDebut(action({ decidedOn: '2026-11-02' }))).toBeFalse();
      expect(panneau().echeanceAvantDebut(action({ dueDate: '2026-11-02' }))).toBeFalse();
    });
  });

  /**
   * L'ÉQUIPE DE L'ÉCART, proposée au choix du responsable d'une action.
   *
   * <p>Le panneau ne la cherche pas : il sert aussi la fiche d'un dossier CAPA
   * ouvert sans écart, où il n'y a pas d'équipe 8D.
   *
   * <p>Sur la CORRECTION d'une action, seule porte de ce panneau vers le choix
   * d'un responsable : depuis l'ADR 0135, ajouter ouvre une CAPA NOUVELLE, et
   * sa fenêtre de création lit l'annuaire pour elle-même.
   */
  describe('l’équipe transmise à la fenêtre', () => {

    const equipeDeLaFenetre = (): readonly { accountId: string }[] | undefined =>
      (dialog.open.calls.mostRecent().args[1] as {
        data: { team?: readonly { accountId: string }[] };
      }).data.team;

    it('transmet l’équipe et le critère quand on corrige une action', async () => {
      await setup({
        dossier: dossier({ actions: [action()],
                           effectivenessCriterion: 'Aucun rebut pendant 3 mois' })
      });
      hote.equipe = [{ accountId: 'u-alice' }, { accountId: 'u-bob' }];
      fixture.detectChanges();

      (un('capa-action-ligne') as HTMLElement).click();

      expect(equipeDeLaFenetre()).toEqual([{ accountId: 'u-alice' }, { accountId: 'u-bob' }]);
      expect((dialog.open.calls.mostRecent().args[1] as {
        data: { effectivenessCriterion?: string };
      }).data.effectivenessCriterion).toBe('Aucun rebut pendant 3 mois');
    });

    it('ne transmet rien quand l’écran appelant n’a pas d’équipe', async () => {
      await setup({ dossier: dossier({ actions: [action()] }) });
      (un('capa-action-ligne') as HTMLElement).click();
      expect(equipeDeLaFenetre()).toEqual([]);
    });
  });
});
