import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { ConnectivityService } from '../../../../core/offline/connectivity.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { FiveWhysSharedModule } from '../../../five-whys/five-whys-shared.module';
import { FiveWhysService } from '../../../five-whys/five-whys.service';
import { IshikawaSharedModule } from '../../../ishikawa/ishikawa-shared.module';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { CapaService } from '../../../capa/capa.service';
import { ProductsService } from '../../../products/products.service';
import { NcService } from '../../nc.service';
import { NcBlocker, NcCapaCaseRef, NcResponse, NcStatus } from '../../nc.types';
import {
  NcCauseAnalysisComponent
} from '../../components/nc-cause-analysis/nc-cause-analysis.component';
import { NcDetailComponent } from './nc-detail.component';
import { CapaActionPanelStubModule } from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';
import { CapaEvidencePanelStubModule } from '../../../capa/components/capa-evidence-panel/testing/capa-evidence-panel.stub';

/**
 * LE BANDEAU DIT CE QUI MANQUE, ET OÙ LE LEVER.
 *
 * <p>Il énonçait les motifs et s'arrêtait là. « 2 dossiers CAPA ne sont pas
 * clôturés » laissait chercher lesquels dans un registre qui en compte des
 * centaines : le geste nommé sans son chemin est un travail rendu à
 * l'utilisateur. Et quand le droit manquait, le bouton était simplement
 * absent — rien ne distinguait « il n'y a plus rien à faire » de « ce geste
 * n'est pas le vôtre ».
 *
 * <p>Les dossiers NOMMÉS sont ceux que la fiche porte, donc ceux que ce lecteur
 * a le droit de voir (ADR 0081) ; le décompte de la phrase, lui, les compte
 * tous. Les deux chiffres peuvent différer, et ces bancs le tiennent.
 */
describe('NcDetailComponent — le bandeau de ce qui manque', () => {

  let fixture: ComponentFixture<NcDetailComponent>;
  let svc: jasmine.SpyObj<NcService>;

  function ecart(status: NcStatus, blockers: NcBlocker[],
                 capaCases: NcCapaCaseRef[] = []): NcResponse {
    return {
      id: 'nc-1', reference: 'NC-2026-1001', title: 'Joint hors tolérance',
      category: 'PROCESS', severity: 'MAJOR', status, origin: 'INTERNAL',
      detectedAt: '2026-11-15T00:00:00Z', createdAt: '2026-11-15T00:00:00Z',
      updatedAt: '2026-11-15T00:00:00Z', teamMembers: [], blockers, capaCases
    };
  }

  function un(test: string): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector('[data-test="' + test + '"]');
  }

  function texte(test: string): string {
    return un(test)?.textContent ?? '';
  }

  /**
   * Monte le banc. `droits` énumère les actions accordées au lecteur ;
   * omis, il a tout.
   */
  async function monter(droits?: string[]): Promise<void> {
    svc = jasmine.createSpyObj<NcService>('NcService', [
      'getEightDReport', 'getNc', 'listPhotos', 'subcategories']);
    svc.listPhotos.and.returnValue(of([]));
    svc.subcategories.and.returnValue(of({ entries: [], defaults: true }));
    svc.getEightDReport.and.returnValue(of({}) as never);
    const ishikawa = jasmine.createSpyObj<IshikawaService>('IshikawaService',
      ['listForNc', 'createDiagram']);
    ishikawa.listForNc.and.returnValue(of([]));
    const fiveWhys = jasmine.createSpyObj<FiveWhysService>('FiveWhysService',
      ['listForNc', 'create']);
    fiveWhys.listForNc.and.returnValue(of([]));
    const annuaire = jasmine.createSpyObj<DirectoryService>('DirectoryService',
      ['search', 'getByAccount']);
    annuaire.search.and.returnValue(
      of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    annuaire.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

    const can = (codes: string | string[]) => {
      if (!droits) {
        return of(true);
      }
      const voulues = Array.isArray(codes) ? codes : [codes];
      return of(voulues.some(c => droits.includes(c)));
    };

    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent, NcCauseAnalysisComponent],
      imports: [SharedModule, UiModule, UserPickerModule, FiveWhysSharedModule,
                IshikawaSharedModule, NoopAnimationsModule, MatDatepickerModule, MatNativeDateModule, CapaActionPanelStubModule,
                CapaEvidencePanelStubModule],
      providers: [
        provideRouter([]),
        { provide: NcService, useValue: svc },
        { provide: IshikawaService, useValue: ishikawa },
        { provide: CapaService, useValue: { updateCase: () => of({}) } },
        { provide: ProductsService, useValue: { list: () => of([]) } },
        { provide: FiveWhysService, useValue: fiveWhys },
        { provide: DirectoryService, useValue: annuaire },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: AuthzService, useValue: { can } },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1' }) } },
        { provide: ConnectivityService, useValue: { online$: of(true), isOnline: () => true } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'nc-1' }) } }
        }
      ]
    }).compileComponents();
  }

  function afficher(nc: NcResponse): void {
    svc.getNc.and.returnValue(of(nc));
    fixture = TestBed.createComponent(NcDetailComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  // ---------- les liens vers les dossiers à régler ------------------------

  it('lie les dossiers qui retiennent la clôture', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 2 }], [
      { id: 'capa-1', title: 'Remplacer le joint', status: 'IN_PROGRESS' },
      { id: 'capa-2', title: 'Revoir la gamme', status: 'RESOLVED' }
    ]));

    const lien1 = un('nc-blocker-lien-capa-1');
    expect(lien1)
      .withContext('sans lien, il faut chercher le dossier dans le registre')
      .not.toBeNull();
    expect(lien1?.textContent).toContain('Remplacer le joint');
    // RESOLVED n'est ni clos ni écarté : il retient la clôture, lui aussi.
    expect(un('nc-blocker-lien-capa-2')).not.toBeNull();
  });

  it('ne lie pas un dossier déjà réglé', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }], [
      { id: 'capa-1', title: 'En cours', status: 'OPEN' },
      { id: 'capa-2', title: 'Déjà clos', status: 'CLOSED' }
    ]));

    expect(un('nc-blocker-lien-capa-1')).not.toBeNull();
    expect(un('nc-blocker-lien-capa-2'))
      .withContext('un dossier clos ne retient rien : le lier enverrait nulle part')
      .toBeNull();
  });

  /**
   * RÉSOUDRE est plus souple que CLORE : un dossier RESOLVED a exécuté ses
   * actions, il ne retient donc pas la résolution de l'écart (ADR 0108).
   */
  it('ne lie que les dossiers dont les actions courent encore, pour résoudre', async () => {
    await monter();
    afficher(ecart('ACTION_DEFINED', [{ code: 'CAPA_ACTIONS_TO_CARRY_OUT', count: 1 }], [
      { id: 'capa-1', title: 'Actions en cours', status: 'IN_PROGRESS' },
      { id: 'capa-2', title: 'Actions faites', status: 'RESOLVED' }
    ]));

    expect(un('nc-blocker-lien-capa-1')).not.toBeNull();
    expect(un('nc-blocker-lien-capa-2')).toBeNull();
  });

  /**
   * SOUS LE MOTIF DE LA RÉSOLUTION, une action terminée depuis le 8D qui
   * attend sa vérification n'est pas nommée : elle retient la CLÔTURE du
   * dossier, pas la résolution de l'écart (ADR 0132). Nommée ici, elle
   * demandait de terminer une action déjà terminée — et le décompte de la
   * ligne contredisait celui du motif.
   */
  it('ne nomme pas, pour résoudre, une action faite qui attend sa vérification', async () => {
    await monter();
    afficher(ecart('ACTION_DEFINED', [{ code: 'CAPA_ACTIONS_TO_CARRY_OUT', count: 1 }], [
      {
        id: 'capa-1', title: 'Le dossier', status: 'IN_PROGRESS', blockingActions: [
          { id: 'act-1', title: 'Reprendre les 12 vannes', reason: 'TO_DO' },
          { id: 'act-2', title: 'Changer le joint', reason: 'TO_VERIFY' }
        ]
      }
    ]));

    const aFaire = un('nc-blocker-action-act-1');
    expect(aFaire).not.toBeNull();
    expect(un('nc-blocker-action-act-2'))
      .withContext('elle est faite : redemander sa clôture est le défaut même')
      .toBeNull();
    expect(texte('nc-blocker-compte-capa-1')).toContain('1 action le retient');

    // LE BLOC NE SE RECONSTRUIT PAS A CHAQUE PASSE. Filtrer en recopiant le
    // dossier donnait une nouvelle identite a chaque detection, `*ngFor`
    // redessinait tout, et le focus pose sur une action sautait.
    fixture.detectChanges();
    expect(un('nc-blocker-action-act-1'))
      .withContext('un bloc redessiné à chaque passe perd le focus du clavier')
      .toBe(aFaire);
  });

  // Le second regard ne se lève pas en ouvrant un dossier : aucun lien n'y mène.
  it('ne met aucun lien sur le motif du second regard', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'SECOND_LOOK_REQUIRED', count: 1 }], [
      { id: 'capa-1', title: 'Un dossier', status: 'IN_PROGRESS' }
    ]));

    expect(un('nc-blocker-SECOND_LOOK_REQUIRED')).not.toBeNull();
    expect(un('nc-blocker-lien-capa-1')).toBeNull();
  });

  /**
   * Le DÉCOMPTE compte tous les dossiers, les liens ne nomment que ceux qu'on
   * voit : un lecteur restreint à ses propres dossiers (ADR 0081) lit « 3 » et
   * n'en voit nommé qu'un. C'est voulu — nommer les deux autres les
   * divulguerait par la fiche d'un écart, là où leur propre fiche les refuse.
   */
  it('garde le décompte du serveur même quand il nomme moins de dossiers', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 3 }], [
      { id: 'capa-1', title: 'Le seul visible', status: 'OPEN' }
    ]));

    expect(texte('nc-blockers')).toContain('3');
    expect(un('nc-blocker-lien-capa-1')).not.toBeNull();
  });

  // ---------- les actions qui retiennent, nommées une à une ---------------

  /**
   * NOMMER LE DOSSIER NE SUFFIT PAS : il porte autant d'actions qu'on veut, et
   * savoir qu'il n'est pas clos ne dit pas laquelle on attend. Il fallait
   * l'ouvrir pour instruire soi-même ce que le serveur savait déjà.
   */
  it('nomme chaque action qui retient la clôture, et RIEN DE PLUS', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }], [
      {
        id: 'capa-1', title: 'Le dossier', status: 'RESOLVED', blockingActions: [
          { id: 'act-1', title: 'Reprendre les 12 vannes', reason: 'TO_DO' },
          { id: 'act-2', title: 'Imprimer la plaque depuis l’ERP', reason: 'TO_VERIFY' }
        ]
      }
    ]));

    const aFaire = un('nc-blocker-action-act-1');
    expect(aFaire)
      .withContext('sans la liste, il faut ouvrir le dossier pour savoir quoi faire')
      .not.toBeNull();
    expect(aFaire?.textContent).toContain('Reprendre les 12 vannes');

    const aVerifier = un('nc-blocker-action-act-2');
    expect(aVerifier?.textContent).toContain('Imprimer la plaque depuis l’ERP');

    // UNE SEULE LIGNE PAR ACTION : son nom, et rien d'autre. L'etat d'attente
    // suivait le nom et chaque action tenait donc deux lignes ; le nom suffit,
    // le lien mene a l'action ou son etat se lit en entier. Sans cette garde,
    // remettre l'etiquette passerait inapercu.
    expect(aFaire?.parentElement?.textContent?.trim())
      .withContext('le nom de l’action, seul, sur une ligne')
      .toBe('Reprendre les 12 vannes');
    expect(aVerifier?.parentElement?.textContent?.trim())
      .toBe('Imprimer la plaque depuis l’ERP');
    // LE DÉCOMPTE du motif compte les DOSSIERS ; celui-ci compte le travail
    // qui reste dedans, et c'est lui qu'on vient lire.
    expect(texte('nc-blocker-compte-capa-1')).toContain('2 actions');
  });

  // « 1 actions » est une faute que personne ne pardonne à un logiciel de
  // qualité : le singulier a sa propre phrase.
  it('écrit le décompte au singulier quand une seule action retient', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }], [
      {
        id: 'capa-1', title: 'Le dossier', status: 'OPEN',
        blockingActions: [{ id: 'act-1', title: 'Une action', reason: 'TO_DO' }]
      }
    ]));

    expect(texte('nc-blocker-compte-capa-1')).toContain('1 action le retient');
  });

  /**
   * LE LIEN MÈNE À L'ACTION, et non à son dossier : elle se traite dans le
   * tableau de sa discipline, plus bas sur cette même fiche — et la fiche du
   * dossier renvoie d'ailleurs ici (ADR 0115). Nommer l'action sans y conduire
   * laisserait chercher dans trois tableaux laquelle est celle-là.
   */
  it('conduit à la ligne de l’action, et lui donne le focus', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }], [
      {
        id: 'capa-1', title: 'Le dossier', status: 'OPEN',
        blockingActions: [{ id: 'act-1', title: 'Une action', reason: 'TO_DO' }]
      }
    ]));

    // La ligne que le panneau d'actions dessine pour cette action-là.
    const ligne = document.createElement('div');
    ligne.id = 'capa-action-act-1';
    ligne.tabIndex = 0;
    const defilement = jasmine.createSpy('scrollIntoView');
    ligne.scrollIntoView = defilement;
    (fixture.nativeElement as HTMLElement).appendChild(ligne);

    un('nc-blocker-action-act-1')!.click();

    expect(defilement).toHaveBeenCalled();
    expect(document.activeElement)
      .withContext('le clavier reprend là où l’œil s’arrête')
      .toBe(ligne);
  });

  // Un panneau d'actions qui charge encore n'a pas dessiné sa ligne : on ne
  // saute pas n'importe où plutôt que de sauter juste.
  it('ne fait rien quand la ligne de l’action n’est pas encore dessinée', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }], [
      {
        id: 'capa-1', title: 'Le dossier', status: 'OPEN',
        blockingActions: [{ id: 'act-1', title: 'Une action', reason: 'TO_DO' }]
      }
    ]));

    expect(() => un('nc-blocker-action-act-1')!.click()).not.toThrow();
  });

  // Un dossier sans action bloquante n'en affiche aucune : la liste ne s'ouvre
  // pas pour rester vide.
  it('n’affiche aucune action quand le dossier n’en retient aucune', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }], [
      { id: 'capa-1', title: 'Le dossier', status: 'RESOLVED', blockingActions: [] }
    ]));

    expect(un('nc-blocker-lien-capa-1')).not.toBeNull();
    expect((fixture.nativeElement as HTMLElement)
      .querySelector('.blockers-actions')).toBeNull();
  });

  // Un code d'attente que cet ecran ne connait pas -- serveur plus recent --
  // ne change plus rien : la liste ne porte que le nom de l'action, et aucun
  // libelle d'etat ne peut donc rester vide.
  it('nomme l’action quel que soit le motif d’attente, même inconnu', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }], [
      {
        id: 'capa-1', title: 'Le dossier', status: 'OPEN',
        blockingActions: [{ id: 'act-9', title: 'Une action', reason: 'TO_SIGN' }]
      }
    ]));

    expect(un('nc-blocker-action-act-9')?.parentElement?.textContent?.trim())
      .toBe('Une action');
  });

  // ---------- le droit qui manque à celui qui lit -------------------------

  it('dit que le droit de clôturer manque, plutôt que de taire le bouton', async () => {
    await monter(['nc.process']);
    afficher(ecart('RESOLVED', []));

    expect(un('nc-blockers'))
      .withContext('sans bandeau, la fiche se lit comme s’il n’y avait plus rien à faire')
      .not.toBeNull();
    expect(texte('nc-blockers')).toContain('droit de clôturer');
    expect(un('nc-close'))
      .withContext('le bouton reste absent : c’est le bandeau qui parle')
      .toBeNull();
  });

  it('dit que le droit de résoudre manque', async () => {
    await monter(['nc.close']);
    afficher(ecart('ACTION_DEFINED', []));

    expect(texte('nc-blockers')).toContain('droit de résoudre');
  });

  it('ne dit rien du droit quand il est accordé', async () => {
    await monter();
    afficher(ecart('RESOLVED', []));

    expect(un('nc-blockers')).toBeNull();
  });

  // Un écart clos n'attend plus rien : annoncer un droit manquant y donnerait à
  // croire qu'il reste un geste à faire.
  it('se tait sur un écart terminé', async () => {
    await monter([]);
    afficher(ecart('CLOSED', []));

    expect(un('nc-blockers')).toBeNull();
  });
});
