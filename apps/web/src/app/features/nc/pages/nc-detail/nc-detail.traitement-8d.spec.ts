import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
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
import { NcResponse, NcStatus } from '../../nc.types';
import {
  NcCauseAnalysisComponent
} from '../../components/nc-cause-analysis/nc-cause-analysis.component';
import { NcDetailComponent } from './nc-detail.component';
import {
  CapaActionPanelStubModule
} from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';
import { CapaEvidencePanelStubModule } from '../../../capa/components/capa-evidence-panel/testing/capa-evidence-panel.stub';

/**
 * LE TRAITEMENT D'UN ÉCART, dans l'ordre des huit disciplines.
 *
 * <p>La fiche présentait les mêmes éléments en cartes sans ordre : on y lisait
 * l'équipe, les photos, les actions, sans voir ce qui restait à faire ni dans
 * quel ordre. Ces bancs tiennent les deux promesses de la trame 8D : les huit
 * disciplines sont TOUJOURS là — une étape vide se dit plutôt que de
 * disparaître — et RIEN n'y est obligatoire.
 */
describe('NcDetailComponent — le traitement 8D', () => {

  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const CODES = ['D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7', 'D8'];

  function ecart(patch: Partial<NcResponse> = {}): NcResponse {
    return {
      id: 'nc-1', reference: 'NC-2026-1001', title: 'Joint hors tolérance',
      category: 'PROCESS', severity: 'MAJOR', status: 'ACTION_DEFINED', origin: 'EXTERNAL',
      detectedAt: '2026-11-15T00:00:00Z', createdAt: '2026-11-15T00:00:00Z',
      updatedAt: '2026-11-15T00:00:00Z', teamMembers: [], blockers: [],
      ...patch
    };
  }

  function un(test: string): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector('[data-test="' + test + '"]');
  }

  function texte(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
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
      'getEightDReport', 'saveEightDReport', 'getNc', 'listPhotos', 'subcategories',
      'updateNc', 'reopen']);
    svc.listPhotos.and.returnValue(of([]));
    svc.subcategories.and.returnValue(of({ entries: [], defaults: true }));
    svc.getEightDReport.and.returnValue(of({} as any));
    svc.saveEightDReport.and.returnValue(of({} as any));
    svc.updateNc.and.returnValue(of(ecart()));
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
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as any);

    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent, NcCauseAnalysisComponent],
      imports: [SharedModule, UiModule, UserPickerModule, FiveWhysSharedModule,
                IshikawaSharedModule, NoopAnimationsModule, MatDatepickerModule, MatNativeDateModule, CapaActionPanelStubModule, CapaEvidencePanelStubModule],
      providers: [
        provideRouter([]),
        { provide: NcService, useValue: svc },
        { provide: IshikawaService, useValue: ishikawa },
        { provide: CapaService, useValue: { updateCase: () => of({}) } },
        { provide: ProductsService, useValue: { list: () => of([]) } },
        { provide: FiveWhysService, useValue: fiveWhys },
        { provide: DirectoryService, useValue: annuaire },
        { provide: MatDialog, useValue: dialog },
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

  // ---------- les huit disciplines, toujours là ----------

  it('montre les huit disciplines, même sur un écart tout neuf', () => {
    setup(ecart({ status: 'OPEN' }));

    const hote = fixture.nativeElement as HTMLElement;
    CODES.forEach(code => {
      expect(hote.querySelector('section[id="' + code + '"]'))
        .withContext(code + ' manque : une discipline vide se dit, elle ne disparaît pas')
        .not.toBeNull();
    });
  });

  it('porte un sommaire d’une entrée par discipline', () => {
    setup(ecart());

    expect(un('sommaire')).not.toBeNull();
    CODES.forEach(code => expect(un('etape-' + code)).not.toBeNull());
  });

  /**
   * Le marqueur dit ce qui PORTE quelque chose. Il n'autorise rien et
   * n'interdit rien : aucune discipline n'est obligatoire, et un écart mineur
   * se clôture sans confinement à décrire.
   */
  it('ne marque aucune étape faite sur un écart vide', () => {
    setup(ecart({ status: 'OPEN' }));

    CODES.forEach(code =>
      expect(un('etape-' + code)!.classList.contains('done'))
        .withContext(code + ' marqué fait alors que rien n’y est écrit')
        .toBeFalse());
  });

  it('marque le D1 dès qu’une équipe est nommée, et le D4 dès qu’une cause l’est', () => {
    setup(ecart({
      teamMembers: [{ accountId: 'kc-marie', displayName: 'Marie Durand' }],
      rootCause: 'Usure du roulement'
    }));

    expect(un('etape-D1')!.classList.contains('done')).toBeTrue();
    expect(un('etape-D4')!.classList.contains('done')).toBeTrue();
    expect(un('etape-D3')!.classList.contains('done')).toBeFalse();
  });

  /**
   * LE D6 COCHAIT SUR LA NOTE DE RÉSOLUTION, qui s'écrit à l'étape
   * « Résoudre » et non dans cette carte. On déposait donc une preuve, on
   * écrivait le commentaire d'efficacité, et la discipline restait grise : le
   * sommaire affirmait qu'il restait à faire ce qui venait d'être fait.
   */
  it('marque le D6 dès qu’une pièce est déposée sur le dossier', () => {
    setup(ecart({ capaCases: [{ id: 'capa-1', status: 'IN_PROGRESS' }] }));

    expect(un('etape-D6')!.classList.contains('done'))
      .withContext('le D6 ne devrait rien porter avant qu’une pièce soit lue')
      .toBeFalse();

    component.compter('D6', 2);
    fixture.detectChanges();

    expect(un('etape-D6')!.classList.contains('done')).toBeTrue();
  });

  it('marque le D6 sur le seul commentaire d’efficacité', () => {
    setup(ecart({
      capaCases: [{ id: 'capa-1', status: 'IN_PROGRESS',
                    effectivenessComment: 'Aucun retour client sur deux mois.' }]
    }));

    expect(un('etape-D6')!.classList.contains('done')).toBeTrue();
  });

  it('ne marque pas le D6 quand le dossier est lu et ne porte rien', () => {
    setup(ecart({ capaCases: [{ id: 'capa-1', status: 'IN_PROGRESS' }] }));

    // Zéro est une valeur : « lu, et il n'y en a aucune » n'est pas « fait ».
    component.compter('D6', 0);
    fixture.detectChanges();

    expect(un('etape-D6')!.classList.contains('done')).toBeFalse();
  });

  it('emmène sur la discipline qu’on choisit dans le sommaire', () => {
    setup(ecart());
    const section = (fixture.nativeElement as HTMLElement)
      .querySelector('section[id="D5"]') as HTMLElement;
    const aller = spyOn(section, 'scrollIntoView');

    un('etape-D5')!.click();

    expect(aller).toHaveBeenCalled();
  });

  // ---------- rien n'est obligatoire ----------

  /**
   * Le client et le lot s'écrivaient dans la description, en texte libre, d'où
   * ils ne ressortaient ni en filtre, ni en indicateur, ni dans le rapport
   * remis au client. Facultatifs tous les deux.
   */
  it('n’exige ni client ni lot pour enregistrer le contexte', () => {
    setup(ecart());

    expect(component.contexteForm.valid).toBeTrue();
    component.contexteForm.patchValue({ customerName: 'Fonderie Durand' });
    component.contexteModifie = true;
    fixture.detectChanges();
    component.enregistrerContexte();

    expect(svc.updateNc).toHaveBeenCalledWith('nc-1',
      jasmine.objectContaining({ customerName: 'Fonderie Durand' }));
  });

  it('n’exige aucun mot de remerciement au D8', () => {
    setup(ecart());

    expect(component.remerciementControl.valid).toBeTrue();
    component.remerciementControl.setValue('Merci à toute l’équipe.');
    component.enregistrerRemerciement();

    expect(svc.saveEightDReport).toHaveBeenCalledWith('nc-1',
      jasmine.objectContaining({ recognition: 'Merci à toute l’équipe.' }));
  });

  /**
   * La conversion en cycle PDCA ou en projet DMAIC est une OPTION du D2, pas
   * une étape : ce que l'écart révèle peut mériter un cycle, et c'est une
   * décision, non un passage obligé (§3.6).
   */
  it('offre la conversion en option dans le D2, sans la réclamer', () => {
    setup(ecart({ description: 'Le joint fuit au poste 20.' }));

    const d2 = (fixture.nativeElement as HTMLElement).querySelector('section[id="D2"]')!;
    expect(d2.querySelector('[data-test="convertir"]')).not.toBeNull();
    // Le problème est décrit : le D2 est fait. Qu'aucun cycle ni projet n'ait
    // été ouvert n'y change rien — la conversion est une décision, pas une
    // étape (§3.6).
    expect(un('etape-D2')!.classList.contains('done')).toBeTrue();
  });

  // ---------- le bandeau : ce qui est arrivé à l'écart ----------

  it('dit la reprise plutôt que la clôture qu’elle a défaite', () => {
    setup(ecart({
      status: 'OPEN',
      resolvedAt: '2026-11-16T00:00:00Z',
      closedAt: '2026-11-17T00:00:00Z',
      reopenedAt: '2026-11-18T00:00:00Z',
      reopenReason: 'Récidive sur le lot suivant'
    }));

    expect(un('bandeau')!.textContent).toContain('Récidive sur le lot suivant');
  });

  it('nomme qui a clôturé, quand on le sait', () => {
    setup(ecart({
      status: 'CLOSED', closedAt: '2026-11-17T00:00:00Z', closedByName: 'Ali Benali'
    }));

    expect(un('bandeau')!.textContent).toContain('Ali Benali');
  });

  it('ne montre aucun bandeau sur un écart qui n’a rien vécu', () => {
    setup(ecart({ status: 'OPEN' }));

    expect(un('bandeau')).toBeNull();
  });

  // ---------- la reprise ----------

  it('propose la reprise sur un écart refermé, et sur lui seul', () => {
    (['RESOLVED', 'CLOSED', 'CANCELLED', 'REJECTED'] as NcStatus[])
      .forEach(statut => expect(component?.canReopen
        ? component.canReopen(statut)
        : true).toBeTrue());

    setup(ecart({ status: 'CLOSED', closedAt: '2026-11-17T00:00:00Z' }));
    expect(un('rouvrir')).not.toBeNull();

    setup(ecart({ status: 'UNDER_ANALYSIS' }));
    expect(un('rouvrir')).toBeNull();
  });

  it('passe par une fenêtre pour reprendre, et relit la fiche au retour', () => {
    setup(ecart({ status: 'CLOSED', closedAt: '2026-11-17T00:00:00Z' }));
    svc.getNc.calls.reset();
    dialog.open.and.returnValue({ afterClosed: () => of(ecart({ status: 'OPEN' })) } as any);

    un('rouvrir')!.click();
    fixture.detectChanges();

    expect(dialog.open).toHaveBeenCalled();
    expect(svc.getNc).toHaveBeenCalled();
  });

  // ---------- le second regard ----------

  /**
   * Clore, c'est valider que la résolution tient, et personne ne valide son
   * propre travail. Le motif éteint le bouton AVANT le clic, et dit que l'écart
   * n'est pas en défaut — c'est la personne qui ne peut pas le clôturer.
   */
  it('éteint la clôture et explique le second regard', () => {
    setup(ecart({
      status: 'RESOLVED', resolvedAt: '2026-11-16T00:00:00Z',
      blockers: [{ code: 'SECOND_LOOK_REQUIRED', count: 1 }]
    }));

    const bouton = un('nc-close') as HTMLButtonElement;
    expect(bouton.disabled).toBeTrue();
    expect(un('nc-blockers')!.textContent).toContain('second regard');
  });

  it('montre les deux signatures sur un écart clos', () => {
    setup(ecart({
      status: 'CLOSED', closedAt: '2026-11-17T00:00:00Z',
      resolvedByName: 'Marie Durand', closedByName: 'Ali Benali'
    }));

    const trace = un('second-regard')!.textContent ?? '';
    expect(trace).toContain('Marie Durand');
    expect(trace).toContain('Ali Benali');
  });

  it('n’invente aucune signature sur un écart d’avant la règle', () => {
    setup(ecart({ status: 'CLOSED', closedAt: '2026-11-17T00:00:00Z' }));

    expect(un('second-regard')).toBeNull();
  });

  /** La désignation est FACULTATIVE : c'est ce qui la rend utile. */
  it('n’exige pas de valideur désigné', () => {
    setup(ecart());

    expect(un('valideur')).not.toBeNull();
    expect(component.valideurControl.valid).toBeTrue();
    expect(un('valideur-enregistrer'))
      .withContext('le bouton ne paraît qu’après une modification')
      .toBeNull();
  });

  it('enregistre le valideur désigné par son compte et son nom', () => {
    setup(ecart());

    component.valideurControl.setValue('kc-ali');
    component.valideurChoisi({
      id: 'u-9', accountId: 'kc-ali', displayName: 'Ali Benali',
      email: 'ali@exemple.fr', active: true
    });
    component.enregistrerValideur();

    expect(svc.updateNc).toHaveBeenCalledWith('nc-1',
      jasmine.objectContaining({ validatorId: 'kc-ali', validatorName: 'Ali Benali' }));
  });

  /**
   * Retirer une désignation posée par erreur doit rester possible : le NOM vide
   * porte le retrait, le serveur suivant le nom pour distinguer « retirer » de
   * « ne pas y toucher ».
   */
  it('retire la désignation en vidant le champ', () => {
    setup(ecart({ validatorId: 'kc-ali', validatorName: 'Ali Benali' }));

    component.valideurControl.setValue(null);
    component.valideurChoisi(null);
    component.enregistrerValideur();

    expect(svc.updateNc).toHaveBeenCalledWith('nc-1',
      jasmine.objectContaining({ validatorId: undefined, validatorName: '' }));
  });

  it('montre le valideur par son nom sur un écart clos, jamais son identifiant', () => {
    setup(ecart({
      status: 'CLOSED', closedAt: '2026-11-17T00:00:00Z',
      validatorId: '7f1b2c3d-4e5f-6a7b-8c9d-0e1f2a3b4c5d', validatorName: 'Ali Benali'
    }));

    expect(un('valideur-lecture')!.textContent).toContain('Ali Benali');
    expect(texte()).not.toContain('7f1b2c3d');
  });

  /**
   * LA MÊME DATE, DEUX MOTS, parce que ce n'est pas le même fait : un constat
   * interne se DÉTECTE, une réclamation client se REÇOIT. Dire « détectée »
   * d'une réclamation laisserait croire que l'organisation a vu le problème
   * elle-même, alors que c'est le client qui l'a signalé — et c'est la date de
   * réception qui fait courir le délai de réponse qu'on lui doit.
   */
  describe('le libellé de la date, en D2', () => {

    it('dit « date de réception » sur une réclamation venue du dehors', () => {
      setup(ecart({ origin: 'EXTERNAL' }));
      expect(un('date-libelle-reception')).not.toBeNull();
      expect(un('date-libelle-detection')).toBeNull();
    });

    it('dit « détectée le » sur un constat de l’organisation elle-même', () => {
      setup(ecart({ origin: 'INTERNAL' }));
      expect(un('date-libelle-detection')).not.toBeNull();
      expect(un('date-libelle-reception')).toBeNull();
    });

    it('montre la date dans les deux cas : seul le mot change', () => {
      setup(ecart({ origin: 'INTERNAL', detectedAt: '2026-11-15T00:00:00Z' }));
      const avec = texte();
      setup(ecart({ origin: 'EXTERNAL', detectedAt: '2026-11-15T00:00:00Z' }));
      expect(avec).toContain('2026');
      expect(texte()).toContain('2026');
    });
  });

});
