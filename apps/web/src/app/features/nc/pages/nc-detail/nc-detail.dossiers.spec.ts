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
import { NcCapaCaseRef, NcResponse } from '../../nc.types';
import {
  NcCauseAnalysisComponent
} from '../../components/nc-cause-analysis/nc-cause-analysis.component';
import { NcDetailComponent } from './nc-detail.component';
import { CapaActionPanelStubModule } from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';
import { CapaEvidencePanelStubModule } from '../../../capa/components/capa-evidence-panel/testing/capa-evidence-panel.stub';

/**
 * UN TABLEAU PAR DOSSIER.
 *
 * <p>Un écart peut porter plusieurs dossiers CAPA : le premier ouvert depuis
 * sa fiche, et un second ouvert depuis la liste en citant sa référence, qui ne
 * remplace pas le lien du premier. Sa clôture les attend TOUS — mais la fiche
 * n'en montrait qu'un. On lisait donc « 2 dossiers non réglés » au-dessus d'un
 * tableau qui n'en montrait qu'un, et les actions du second n'existaient nulle
 * part à l'écran : ni dans la fiche, ni dans le rapport 8D.
 */
describe('NcDetailComponent — les dossiers CAPA de l’écart', () => {

  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;
  let capa: jasmine.SpyObj<CapaService>;

  function ecart(capaCases?: NcCapaCaseRef[], capaCaseId?: string): NcResponse {
    return {
      id: 'nc-1', reference: 'NC-2026-1001', title: 'Joint hors tolérance',
      category: 'PROCESS', severity: 'MAJOR', status: 'ACTION_DEFINED', origin: 'INTERNAL',
      detectedAt: '2026-11-15T00:00:00Z', createdAt: '2026-11-15T00:00:00Z',
      updatedAt: '2026-11-15T00:00:00Z', teamMembers: [], blockers: [],
      capaCaseId, capaCases
    };
  }

  function un(test: string): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector('[data-test="' + test + '"]');
  }

  function tous(prefixe: string): HTMLElement[] {
    return Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('[data-test^="' + prefixe + '"]'));
  }

  function setup(nc: NcResponse): void {
    svc.getNc.and.returnValue(of(nc));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<NcService>('NcService', [
      'getEightDReport', 'getNc', 'listPhotos', 'subcategories']);
    capa = jasmine.createSpyObj<CapaService>('CapaService', ['updateCase']);
    capa.updateCase.and.returnValue(of({}) as never);
    svc.listPhotos.and.returnValue(of([]));
    svc.subcategories.and.returnValue(of({ entries: [], defaults: true }));
    // Le D8 lit le rapport 8D pour n'en tirer que les remerciements : un
    // rapport vide suffit ici, la discipline n'est pas le sujet de ces bancs.
    svc.getEightDReport.and.returnValue(of({} as any));
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

    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent, NcCauseAnalysisComponent],
      imports: [SharedModule, UiModule, UserPickerModule, FiveWhysSharedModule,
                IshikawaSharedModule, NoopAnimationsModule, MatDatepickerModule, MatNativeDateModule, CapaActionPanelStubModule, CapaEvidencePanelStubModule],
      providers: [
        provideRouter([]),
        { provide: NcService, useValue: svc },
        { provide: IshikawaService, useValue: ishikawa },
        { provide: CapaService, useValue: capa },
        { provide: ProductsService, useValue: { list: () => of([]) } },
        { provide: FiveWhysService, useValue: fiveWhys },
        { provide: DirectoryService, useValue: annuaire },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1' }) } },
        { provide: ConnectivityService, useValue: { online$: of(true), isOnline: () => true } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'nc-1' }) } }
        }
      ]
    }).compileComponents();
  });

  // ---------- l'invariant : tous les dossiers, pas seulement le premier ----

  it('confie au panneau TOUS les dossiers de l’écart, sans les lier un à un', () => {
    const nc = ecart([
      { id: 'capa-1', title: 'Dossier de la fiche', status: 'IN_PROGRESS' },
      { id: 'capa-2', title: 'Dossier ouvert depuis la liste', status: 'OPEN' }
    ], 'capa-1');
    setup(nc);

    // L'INVARIANT TIENT TOUJOURS -- les actions du second dossier doivent
    // rester visibles -- mais il se lit la ou il vit : le panneau recoit tous
    // les identifiants. Il se lisait avant dans une rangee de liens.
    expect(component.idsDesDossiers(nc))
      .withContext('sinon les actions du second dossier sont invisibles')
      .toEqual(['capa-1', 'capa-2']);

    // LES LIENS SONT PARTIS. Une rangee de six boutons « Voir la CAPA »
    // coiffait un tableau dont chaque ligne EST une CAPA (ADR 0135) : on
    // proposait d'aller voir ailleurs ce qui etait deja sous les yeux.
    expect(tous('capa-dossier-').length)
      .withContext('la ligne du tableau mene deja a son action')
      .toBe(0);
  });

  /**
   * L'INTITULE DU DOSSIER NE COIFFE PLUS LES TABLEAUX D'ACTIONS.
   *
   * <p>Il s'ecrivait au-dessus de chacun des trois : le nom de la premiere
   * action du dossier -- « Action containment 1 » -- se lisait donc comme un
   * titre de section, juste au-dessus d'un tableau qui le reprend sur sa
   * premiere ligne. Deux fois la meme chose, dont une qui ressemblait a
   * l'intitule de la discipline.
   */
  it('ne coiffe plus les tableaux d’actions de l’intitulé du dossier', () => {
    setup(ecart([
      { id: 'capa-1', title: 'Remplacer le joint', status: 'OPEN' },
      { id: 'capa-2', title: 'Revoir la gamme', status: 'OPEN' }
    ], 'capa-1'));

    ['confinement', 'capa-action', 'preventives'].forEach(discipline => {
      const section = un(discipline);
      expect(section).withContext(discipline + ' est rendue').not.toBeNull();
      expect(section!.querySelectorAll('.capa-dossier-titre').length)
        .withContext('aucun intitulé de dossier au-dessus du tableau de ' + discipline)
        .toBe(0);
    });
  });

  /**
   * LE COMPTE EST CELUI DE LA DISCIPLINE, PAS CELUI DES DOSSIERS.
   *
   * <p>Il annoncait « 6 dossiers CAPA traitent cet ecart » au-dessus d'un
   * tableau qui en montrait QUATRE : il comptait tous les dossiers de l'ecart,
   * endiguement et preventif compris, alors que le tableau ne retient que les
   * correctives. Deux nombres pour une meme ligne, dont un faux.
   *
   * <p>Seul le panneau a lu les actions ; le composant ne connait que les
   * dossiers. Le compte lui vient donc par `compter`, comme les coches du rail.
   */
  it('annonce le compte des actions correctives, et seulement au-delà d’une', () => {
    setup(ecart([
      { id: 'capa-1', title: 'Premier', status: 'OPEN' },
      { id: 'capa-2', title: 'Second', status: 'OPEN' }
    ], 'capa-1'));

    // Deux dossiers, mais le panneau n'a pas encore compte : rien a annoncer.
    expect(un('capa-action-compte'))
      .withContext('deux dossiers ne font pas deux actions correctives')
      .toBeNull();

    component.compter('D5', 1);
    fixture.detectChanges();
    expect(un('capa-action-compte'))
      .withContext('« 1 action » serait du bruit')
      .toBeNull();

    component.compter('D5', 4);
    fixture.detectChanges();
    expect(un('capa-action-compte')?.textContent).toContain('4');
  });

  /**
   * Repli : un serveur qui ne renvoie pas encore la liste ne doit pas faire
   * DISPARAÎTRE le tableau que l'écran affichait déjà.
   */
  it('affiche encore le dossier du lien quand la liste n’est pas servie', () => {
    setup(ecart(undefined, 'capa-1'));

    expect(component.idsDesDossiers(ecart(undefined, 'capa-1')))
      .withContext('le dossier du lien reste servi au panneau')
      .toEqual(['capa-1']);
  });

  /**
   * CHAQUE DISCIPLINE OUVRE LE DOSSIER AVEC SA PROPRE NATURE.
   *
   * <p>Le D3 et le D7 renvoyaient au D5 : on y descendait, on cliquait
   * « Nouvelle action », et la fenetre reposait la nature en trois choix avec
   * « Corrective » pre-cochee. L'action de confinement qu'on venait d'ecrire
   * naissait donc corrective -- le registre CAPA l'affichait sous ce faux
   * type, et le D3 restait vide puisqu'il ne montre que l'endiguement.
   */
  it('ouvre le dossier depuis chaque discipline, avec la nature de celle-ci', () => {
    setup(ecart([], undefined));

    // La discipline reste ; c'est son contenu qui change.
    expect(un('capa-action')).not.toBeNull();
    expect(un('capa-action-vide')).not.toBeNull();
    expect(un('d3-ajouter-confinement')).not.toBeNull();
    expect(un('capa-action-ouvrir')).not.toBeNull();
    expect(un('d7-ajouter-preventive')).not.toBeNull();

    const ouvre = spyOn(component, 'openCapa');
    un('d3-ajouter-confinement')!.click();
    un('capa-action-ouvrir')!.click();
    un('d7-ajouter-preventive')!.click();

    expect(ouvre.calls.allArgs().map(args => args[1]))
      .withContext('la nature vient de la discipline, pas d’un champ à corriger')
      .toEqual(['CONTAINMENT', 'CORRECTIVE', 'PREVENTIVE']);
  });

  // ---------- la liste des dossiers, telle que le composant la calcule ----

  it('préfère la liste du serveur au lien de la fiche', () => {
    setup(ecart([
      { id: 'capa-9', title: 'Celui du serveur', status: 'OPEN' }
    ], 'capa-1'));

    expect(component.dossiers(ecart([
      { id: 'capa-9', title: 'Celui du serveur', status: 'OPEN' }
    ], 'capa-1')).map(d => d.id)).toEqual(['capa-9']);
  });
  // ---------- plusieurs dossiers : les preuves s additionnent --------------

  /**
   * Le D6 pose un panneau par dossier. Tous ecrivaient leur compte sous la meme
   * cle : le dernier arrive effacait celui des precedents. Un ecart dont le
   * premier dossier porte des preuves et le dernier aucune se declarait vide.
   */
  it('additionne les preuves des dossiers au lieu de les ecraser', () => {
    const n = ecart([
      { id: 'capa-a', title: 'Premier', status: 'OPEN' },
      { id: 'capa-b', title: 'Second', status: 'OPEN' }
    ], 'capa-a');
    setup(n);

    component.compterPreuves('capa-a', 3);
    component.compterPreuves('capa-b', 0);

    expect(component.etapeFaite('D6', n))
      .withContext('trois preuves sur le premier dossier : le D6 porte quelque chose')
      .toBeTrue();
  });

  it('met a jour le compte d un dossier sans toucher aux autres', () => {
    const n = ecart([
      { id: 'capa-a', title: 'Premier', status: 'OPEN' },
      { id: 'capa-b', title: 'Second', status: 'OPEN' }
    ], 'capa-a');
    setup(n);

    component.compterPreuves('capa-a', 2);
    component.compterPreuves('capa-b', 1);
    // Le premier dossier perd ses pieces : le second garde la sienne.
    component.compterPreuves('capa-a', 0);

    expect(component.etapeFaite('D6', n))
      .withContext('une preuve subsiste sur le second dossier')
      .toBeTrue();

    component.compterPreuves('capa-b', 0);
    expect(component.etapeFaite('D6', n))
      .withContext('plus aucune preuve nulle part, et rien d autre au D6')
      .toBeFalse();
  });

  it('repart de zero quand la fiche est relue', () => {
    const n = ecart([{ id: 'capa-a', title: 'Premier', status: 'OPEN' }], 'capa-a');
    setup(n);

    component.compterPreuves('capa-a', 4);
    expect(component.etapeFaite('D6', n)).toBeTrue();

    // Un dossier retire laisserait sinon son compte dans le total.
    component.reload();
    expect(component.etapeFaite('D6', n))
      .withContext('les panneaux encore a l ecran reannoncent leur compte')
      .toBeFalse();
  });

  // ---------- l endiguement prouve au D3, pas au D6 ------------------------

  /**
   * La preuve d un geste d endiguement se depose sur sa ligne du D3, qui porte
   * une colonne pour cela. Le D6 demontre que le PROBLEME a cesse : un dossier
   * d endiguement seul y reclamait la preuve d une efficacite qu il n a jamais
   * pretendu produire, et la meme piece se serait demandee a deux endroits.
   */
  it('ecarte du D6 les dossiers d endiguement seul', () => {
    const n = ecart([
      { id: 'capa-end', title: 'Bloquer le lot', status: 'OPEN', containmentOnly: true },
      { id: 'capa-cor', title: 'Verrouiller le gabarit', status: 'OPEN', containmentOnly: false }
    ], 'capa-end');
    setup(n);

    expect(component.dossiers(n).map(d => d.id))
      .withContext('le D3 et le D5 continuent de voir tous les dossiers')
      .toEqual(['capa-end', 'capa-cor']);
    expect(component.dossiersAPreuve(n).map(d => d.id))
      .withContext("le D6 ne garde que ce qui peut demontrer que le probleme a cesse")
      .toEqual(['capa-cor']);
  });

  /**
   * Absent veut dire que le serveur n a pas repondu a la question. On ne cache
   * pas un dossier sur une supposition : un `undefined` pris pour un
   * endiguement ferait disparaitre les preuves d un dossier correctif.
   */
  it('garde au D6 un dossier dont le serveur n a rien dit', () => {
    const n = ecart([{ id: 'capa-muet', title: 'Sans nature', status: 'OPEN' }], 'capa-muet');
    setup(n);

    expect(component.dossiersAPreuve(n).map(d => d.id)).toEqual(['capa-muet']);
  });

  it('distingue « aucun dossier » de « que de l endiguement »', () => {
    const sansDossier = ecart([], undefined);
    setup(sansDossier);
    expect(component.queDesEndiguements(sansDossier))
      .withContext('sans dossier, le renvoi doit inviter a en ouvrir un')
      .toBeFalse();

    const queEndiguement = ecart([
      { id: 'capa-end', title: 'Bloquer le lot', status: 'OPEN', containmentOnly: true }
    ], 'capa-end');
    setup(queEndiguement);
    expect(component.queDesEndiguements(queEndiguement))
      .withContext('le renvoi doit pointer le D3, et non faire ouvrir un dossier qui existe')
      .toBeTrue();
  });

  // ---------- la note d'efficacite, au D6, a cote des preuves du dossier ----

  it('enregistre la note du D6 en quittant le champ, et sur le bon dossier', () => {
    setup(ecart([{ id: 'capa-1', title: 'Dossier', status: 'IN_PROGRESS' }], 'capa-1'));
    const d = component.dossiers(ecart([
      { id: 'capa-1', title: 'Dossier', status: 'IN_PROGRESS' }], 'capa-1'))[0];

    component.saisirCommentaire(d, 'Zero retour client sur deux trimestres');
    expect(capa.updateCase)
      .withContext("la saisie n'enregistre rien : une requete par touche serait absurde")
      .not.toHaveBeenCalled();

    component.enregistrerCommentaire(d);
    expect(capa.updateCase).toHaveBeenCalledWith('capa-1',
      { effectivenessComment: 'Zero retour client sur deux trimestres' });
  });

  it("n'enregistre rien quand la note n'a pas change", () => {
    // Quitter le champ sans rien ecrire ne doit pas ecrire sur le dossier.
    setup(ecart([{ id: 'capa-1', title: 'Dossier', status: 'IN_PROGRESS',
                   effectivenessComment: 'Deja note' }], 'capa-1'));
    const d = component.dossiers(ecart([
      { id: 'capa-1', title: 'Dossier', status: 'IN_PROGRESS',
        effectivenessComment: 'Deja note' }], 'capa-1'))[0];

    component.saisirCommentaire(d, 'Deja note');
    component.enregistrerCommentaire(d);

    expect(capa.updateCase).not.toHaveBeenCalled();
  });
});
