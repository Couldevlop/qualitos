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
 * LE SECOND REGARD A DEUX RÉPONSES : clôturer, ou renvoyer en modification.
 *
 * <p>Il n'en avait qu'une. La personne qui valide un écart résolu pouvait le
 * clôturer, et c'est tout : si l'action corrective ne répondait pas à la cause,
 * si une preuve manquait, elle n'avait aucun geste pour le dire — on
 * téléphonait au pilote, et le registre gardait un écart « résolu » que
 * personne ne clôturait, sans qu'aucune fiche ne porte la raison de l'attente.
 *
 * <p>Ces bancs tiennent trois choses : QUAND le geste s'offre, à QUI, et ce que
 * la fiche en dit ensuite.
 */
describe('NcDetailComponent — le renvoi en modification', () => {

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

  // ---------- quand le geste s'offre ----------

  /**
   * SUR UN ECART RESOLU, et la : c'est l'etat ou le second regard est attendu.
   */
  it('offre le renvoi sur un écart résolu', async () => {
    await monter();
    afficher(ecart('RESOLVED', []));

    expect(un('nc-send-back'))
      .withContext('sans ce bouton, valider est la seule reponse possible')
      .not.toBeNull();
  });

  /**
   * LE BOUTON IGNORE LES OBSTACLES, et c'est tout le point. Un dossier CAPA
   * encore en travail eteint la cloture -- et c'est precisement la situation ou
   * le valideur doit pouvoir rendre le traitement. Les lui opposer ici ne lui
   * laisserait aucune reponse du tout.
   */
  it('offre le renvoi même quand un obstacle retient la clôture', async () => {
    await monter();
    afficher(ecart('RESOLVED', [{ code: 'CAPA_CASES_UNSETTLED', count: 2 }]));

    expect(un('nc-send-back')).not.toBeNull();
    expect((un('nc-close') as HTMLButtonElement | null)?.disabled)
      .withContext('la cloture, elle, reste eteinte : le serveur la refuserait')
      .toBeTrue();
  });

  it('ne propose pas le renvoi sur un écart en cours de traitement', async () => {
    await monter();
    afficher(ecart('ACTION_DEFINED', []));

    expect(un('nc-send-back'))
      .withContext("rien n'est encore declare resolu : il n'y a rien a renvoyer")
      .toBeNull();
  });

  it('ne propose pas le renvoi sur un écart clos', async () => {
    await monter();
    afficher(ecart('CLOSED', []));

    expect(un('nc-send-back'))
      .withContext('un ecart clos se REPREND, ce qui est un autre geste')
      .toBeNull();
  });

  // ---------- a qui ----------

  /**
   * QUI A RESOLU PEUT AUSSI RENVOYER (ADR 0145) : clore et renvoyer sont les
   * deux reponses d'un seul acte, et puisque la cloture ne lui est plus
   * refusee, le renvoi ne l'est pas davantage.
   */
  it("offre le renvoi sur tout écart résolu", async () => {
    await monter();
    afficher(ecart('RESOLVED', []));

    expect(un('nc-send-back')).not.toBeNull();
  });

  // ---------- ce que la fiche en dit ----------

  it('dit le renvoi dans le bandeau, avec son message', async () => {
    await monter();
    const n = ecart('ACTION_DEFINED', []);
    n.sentBackAt = '2026-11-20T10:00:00Z';
    n.sentBackReason = "L'action ne répond pas à la cause racine";
    afficher(n);

    expect(texte('bandeau')).toContain('renvoyé au pilote');
    expect(texte('bandeau')).toContain("L'action ne répond pas à la cause racine");
  });

  /**
   * LE FAIT LE PLUS RECENT GAGNE. Un renvoi PRECEDE la cloture qui finit par
   * venir : le laisser en tete ferait porter « renvoye le 20 » a une fiche
   * close le 25.
   */
  it('laisse la clôture postérieure prendre le bandeau', async () => {
    await monter();
    const n = ecart('CLOSED', []);
    n.sentBackAt = '2026-11-20T10:00:00Z';
    n.sentBackReason = 'A revoir';
    n.closedAt = '2026-11-25T09:00:00Z';
    n.closedByName = 'Ali Benali';
    afficher(n);

    expect(texte('bandeau')).toContain('Clôturé');
    expect(texte('bandeau')).not.toContain('renvoyé au pilote');
  });

  /** Et un renvoi POSTERIEUR a une cloture reprise reste, lui, en tete. */
  it('garde le bandeau du renvoi quand il est le fait le plus récent', async () => {
    await monter();
    const n = ecart('ACTION_DEFINED', []);
    n.closedAt = '2026-11-10T09:00:00Z';
    n.reopenedAt = '2026-11-12T09:00:00Z';
    n.sentBackAt = '2026-11-20T10:00:00Z';
    afficher(n);

    expect(texte('bandeau')).toContain('renvoyé au pilote');
  });
});
