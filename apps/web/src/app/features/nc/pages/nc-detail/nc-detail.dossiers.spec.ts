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

  it('montre un tableau pour CHAQUE dossier de l’écart', () => {
    setup(ecart([
      { id: 'capa-1', title: 'Dossier de la fiche', status: 'IN_PROGRESS' },
      { id: 'capa-2', title: 'Dossier ouvert depuis la liste', status: 'OPEN' }
    ], 'capa-1'));

    expect(tous('capa-dossier-').length)
      .withContext('un tableau par dossier, sinon les actions du second sont invisibles')
      .toBe(2);
    expect(un('capa-dossier-capa-2')).not.toBeNull();
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
   * C'est la PLURALITÉ qui est inattendue, et c'est elle qu'il faut dire.
   * Annoncer « 1 dossier » sur le cas ordinaire serait du bruit.
   */
  it('annonce le compte seulement quand l’écart porte plusieurs dossiers', () => {
    setup(ecart([{ id: 'capa-1', title: 'Seul dossier', status: 'OPEN' }], 'capa-1'));
    expect(un('capa-action-compte')).toBeNull();

    setup(ecart([
      { id: 'capa-1', title: 'Premier', status: 'OPEN' },
      { id: 'capa-2', title: 'Second', status: 'OPEN' }
    ], 'capa-1'));
    expect(un('capa-action-compte')?.textContent).toContain('2');
  });

  /**
   * Repli : un serveur qui ne renvoie pas encore la liste ne doit pas faire
   * DISPARAÎTRE le tableau que l'écran affichait déjà.
   */
  it('affiche encore le dossier du lien quand la liste n’est pas servie', () => {
    setup(ecart(undefined, 'capa-1'));

    expect(tous('capa-dossier-').length).toBe(1);
    expect(un('capa-dossier-capa-1')).not.toBeNull();
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
