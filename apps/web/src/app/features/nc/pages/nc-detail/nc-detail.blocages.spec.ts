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
import { NcBlocker, NcResponse, NcStatus } from '../../nc.types';
import {
  NcCauseAnalysisComponent
} from '../../components/nc-cause-analysis/nc-cause-analysis.component';
import { NcDetailComponent } from './nc-detail.component';
import { CapaActionPanelStubModule } from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';
import { CapaEvidencePanelStubModule } from '../../../capa/components/capa-evidence-panel/testing/capa-evidence-panel.stub';

/**
 * Ce qui s'oppose à l'avancement de l'écart, DIT avant le clic.
 *
 * <p>Le refus existait et il était juste. Il n'arrivait qu'APRÈS le clic, en
 * anglais, construit par le service : l'utilisateur voyait un bouton éteint
 * sans savoir quoi faire pour l'allumer. Ces bancs tiennent les deux bouts —
 * la phrase est là, et elle nomme le geste qui débloque.
 */
describe('NcDetailComponent — ce qui bloque l’avancement', () => {

  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;

  function ecart(status: NcStatus, blockers: NcBlocker[]): NcResponse {
    return {
      id: 'nc-1', reference: 'NC-2026-1001', title: 'Joint hors tolérance',
      category: 'PROCESS', severity: 'MAJOR', status, origin: 'INTERNAL',
      detectedAt: '2026-11-15T00:00:00Z', createdAt: '2026-11-15T00:00:00Z',
      updatedAt: '2026-11-15T00:00:00Z', teamMembers: [], blockers
    };
  }

  function un(test: string): HTMLElement | null {
    return (fixture.nativeElement as HTMLElement).querySelector('[data-test="' + test + '"]');
  }

  function bouton(test: string): HTMLButtonElement {
    return un(test) as HTMLButtonElement;
  }

  function texte(test: string): string {
    return un(test)?.textContent ?? '';
  }

  function setup(status: NcStatus, blockers: NcBlocker[]): void {
    svc.getNc.and.returnValue(of(ecart(status, blockers)));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<NcService>('NcService', [
      'getEightDReport', 'getNc', 'listPhotos', 'subcategories']);
    svc.listPhotos.and.returnValue(of([]));
    svc.subcategories.and.returnValue(of({ entries: [], defaults: true }));
    // Le D8 lit le rapport 8D pour n'en tirer que les remerciements : un
    // rapport vide suffit ici, la discipline n'est pas le sujet de ces bancs.
    svc.getEightDReport.and.returnValue(of({} as any));
    const ishikawa = jasmine.createSpyObj<IshikawaService>('IshikawaService', ['listForNc', 'createDiagram']);
    ishikawa.listForNc.and.returnValue(of([]));
    const fiveWhys = jasmine.createSpyObj<FiveWhysService>('FiveWhysService', ['listForNc', 'create']);
    fiveWhys.listForNc.and.returnValue(of([]));
    const annuaire = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    annuaire.search.and.returnValue(of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    annuaire.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

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

  // ---------- la phrase est là, et elle dit le geste ----------

  it('dit pourquoi l’écart ne peut pas être résolu, et quoi faire', () => {
    setup('ACTION_DEFINED', [{ code: 'CAPA_ACTIONS_TO_CARRY_OUT', count: 2 }]);

    expect(un('nc-blockers')).not.toBeNull();
    // Le décompte, et le geste : « terminez leurs actions ».
    expect(texte('nc-blockers')).toContain('2');
    expect(texte('nc-blockers')).toContain('Terminez');
    // Le titre nomme l'étape où l'on est.
    expect(texte('nc-blockers')).toContain('résolu');
  });

  it('dit pourquoi l’écart ne peut pas être clôturé, et quoi faire', () => {
    setup('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }]);

    expect(un('nc-blockers')).not.toBeNull();
    expect(texte('nc-blockers')).toContain('clôtur');
    // Le verdict d'efficacité est NOMMÉ : c'est lui qu'on attend (ADR 0108).
    expect(texte('nc-blockers')).toContain('efficacité');
  });

  // Un seul obstacle se dit au singulier. C'est le genre de détail qui fait
  // croire — ou non — que le logiciel a compris la situation.
  it('écrit la phrase au singulier pour un seul dossier', () => {
    setup('ACTION_DEFINED', [{ code: 'CAPA_ACTIONS_TO_CARRY_OUT', count: 1 }]);

    expect(texte('nc-blockers')).toContain('1 dossier CAPA porte');
  });

  /**
   * Un ecart qu'AUCUNE action ne traite dit le geste qui manque.
   *
   * <p>Le bouton etait eteint sans un mot : l'ecart annoncait « action
   * definie » et il n'y en avait pas une seule. La phrase nomme ce qu'il faut
   * faire -- ajouter une action -- au lieu de laisser chercher.
   */
  it('dit d’ajouter au moins une action quand aucune CAPA ne traite l’écart', () => {
    setup('ACTION_DEFINED', [{ code: 'NO_CAPA_CASE', count: 0 }]);

    expect(un('nc-blockers')).not.toBeNull();
    expect(texte('nc-blockers')).toContain('au moins une action');
    expect(bouton('nc-resolve').disabled).toBeTrue();
  });

  // ---------- ce que l'écart enseigne à l'analyse de risque ----------

  /**
   * LE PFMEA QUI ATTEND SA RÉVISION DIT LE GESTE QUI LE LÈVE.
   *
   * <p>Un défaut survenu pour de bon et une analyse de risque qui l'ignore
   * encore se contredisent sur la même page ; l'IATF 16949 en fait une
   * exigence. La phrase nomme le document ET l'issue : accepter la révision,
   * ou corriger la réponse si le PFMEA n'était pas en cause. Sans la seconde
   * moitié, une réponse donnée à tort passerait pour une impasse.
   */
  it('dit quoi faire quand le PFMEA attend sa révision', () => {
    setup('RESOLVED', [{ code: 'PFMEA_REVISION_REQUIRED', count: 1 }]);

    expect(un('nc-blockers')).not.toBeNull();
    expect(texte('nc-blockers')).toContain('PFMEA');
    expect(texte('nc-blockers')).toContain('Acceptez');
    expect(texte('nc-blockers')).toContain('corrigez la réponse');
    expect(bouton('nc-close').disabled).toBeTrue();
  });

  /** Le plan de surveillance porte son propre motif : les deux se révisent à part. */
  it('dit quoi faire quand le plan de surveillance attend sa révision', () => {
    setup('RESOLVED', [{ code: 'CONTROL_PLAN_REVISION_REQUIRED', count: 1 }]);

    expect(texte('nc-blockers')).toContain('plan de surveillance');
    expect(texte('nc-blockers')).toContain('Acceptez');
    expect(bouton('nc-close').disabled).toBeTrue();
  });

  /**
   * Les deux motifs ensemble restent DEUX phrases : fondues en une, elles
   * laisseraient chercher lequel des deux documents retient la clôture.
   */
  it('énonce les deux motifs quand les deux documents attendent', () => {
    setup('RESOLVED', [
      { code: 'PFMEA_REVISION_REQUIRED', count: 1 },
      { code: 'CONTROL_PLAN_REVISION_REQUIRED', count: 1 }
    ]);

    expect(texte('nc-blockers')).toContain('PFMEA');
    expect(texte('nc-blockers')).toContain('plan de surveillance');
  });

  // ---------- le bouton suit la phrase ----------

  it('éteint le bouton que le serveur refuserait', () => {
    setup('ACTION_DEFINED', [{ code: 'CAPA_ACTIONS_TO_CARRY_OUT', count: 1 }]);

    expect(bouton('nc-resolve').disabled).toBeTrue();
  });

  it('laisse le bouton actif quand rien ne s’y oppose', () => {
    setup('ACTION_DEFINED', []);

    expect(un('nc-blockers')).toBeNull();
    expect(bouton('nc-resolve').disabled).toBeFalse();
  });

  it('éteint la clôture tant qu’un dossier n’est pas réglé', () => {
    setup('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 1 }]);

    expect(bouton('nc-close').disabled).toBeTrue();
  });

  // ---------- un serveur plus récent que l'écran ----------

  // Mieux vaut une phrase générique qu'un bouton éteint sans explication : le
  // code inconnu vient d'un serveur déployé avant le front.
  it('reste compréhensible devant un motif qu’il ne connaît pas', () => {
    setup('ACTION_DEFINED', [{ code: 'MOTIF_FUTUR', count: 3 }]);

    expect(un('nc-blockers')).not.toBeNull();
    expect(texte('nc-blockers')).toContain('prérequis');
    expect(bouton('nc-resolve').disabled).toBeTrue();
  });

  // ---------- l'infobulle redit le motif ----------

  it('porte le motif dans l’infobulle du bouton éteint', () => {
    setup('ACTION_DEFINED', [{ code: 'CAPA_ACTIONS_TO_CARRY_OUT', count: 2 }]);

    expect(component.blockersHint(ecart('ACTION_DEFINED',
      [{ code: 'CAPA_ACTIONS_TO_CARRY_OUT', count: 2 }]))).toContain('Terminez');
  });
});
