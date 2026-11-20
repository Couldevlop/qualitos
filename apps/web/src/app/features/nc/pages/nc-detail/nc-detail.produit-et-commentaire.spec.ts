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
import { ProductResponse } from '../../../products/products.types';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';
import {
  NcCauseAnalysisComponent
} from '../../components/nc-cause-analysis/nc-cause-analysis.component';
import { NcDetailComponent } from './nc-detail.component';
import {
  CapaActionPanelStubModule
} from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';
import { CapaEvidencePanelStubModule } from '../../../capa/components/capa-evidence-panel/testing/capa-evidence-panel.stub';

/**
 * LE PRODUIT AU D2, LA NOTE D'ANALYSE AU D4.
 *
 * <p>Deux champs du modèle de traitement 8D que l'écran ne portait pas.
 *
 * <p>Le produit : la fiche gardait son identifiant depuis toujours et ne
 * l'affichait jamais. On ne savait donc pas, en lisant un écart, sur quel
 * produit il portait — et le rapport remis au client ne le disait pas non plus.
 * Il se CHOISIT dans le référentiel : une référence retapée à la main
 * (« PRD-4821 ») ne rattacherait l'écart à rien, ni au PFMEA du produit, ni à
 * son plan de surveillance.
 *
 * <p>La note du D4 : ce qui ne tient ni dans un diagramme, ni dans une chaîne
 * de pourquoi, ni dans la cause retenue — l'hypothèse écartée, et pourquoi on
 * l'a écartée. C'est ce qu'un auditeur demande quand il conteste la cause, et
 * cela s'écrivait dans la description de l'écart, où plus personne ne le
 * rapprochait de l'analyse.
 */
describe('NcDetailComponent — le produit du D2 et la note du D4', () => {

  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;

  const CATALOGUE: ProductResponse[] = [
    {
      id: 'prod-1', code: 'PRD-4821', designation: 'Joint torique 20 mm',
      status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
    },
    {
      id: 'prod-2', code: 'PRD-5002', designation: 'Carter de pompe',
      status: 'ACTIVE', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z'
    }
  ];

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

  function setup(nc: NcResponse): void {
    svc.getNc.and.returnValue(of(nc));
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  /** `produits` = ce que rend le référentiel ; une erreur simule le droit absent. */
  async function monter(produits: 'ok' | 'refuse' = 'ok'): Promise<void> {
    svc = jasmine.createSpyObj<NcService>('NcService', [
      'getEightDReport', 'getNc', 'listPhotos', 'subcategories', 'updateNc']);
    svc.listPhotos.and.returnValue(of([]));
    svc.subcategories.and.returnValue(of({ entries: [], defaults: true }));
    svc.getEightDReport.and.returnValue(of({}) as never);
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
    const dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    const produitsService = {
      list: () => produits === 'ok' ? of(CATALOGUE) : throwError(() => ({ status: 403 }))
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
        { provide: ProductsService, useValue: produitsService },
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
  }

  // ---------- D2 : le produit, choisi dans le référentiel ------------------

  it('offre le choix du produit au D2', async () => {
    await monter();
    setup(ecart());

    expect(un('produit'))
      .withContext('le produit est une donnée du problème : le D2 la demande')
      .not.toBeNull();
  });

  it('recopie le produit de la fiche dans le champ', async () => {
    await monter();
    setup(ecart({ productId: 'prod-2' }));

    expect(component.contexteForm.getRawValue().productId).toBe('prod-2');
  });

  it('nomme le produit, et jamais son identifiant', async () => {
    await monter();
    setup(ecart({ productId: 'prod-1' }));

    const nomme = component.produitLibelle(ecart({ productId: 'prod-1' }));
    expect(nomme).toContain('PRD-4821');
    expect(nomme).toContain('Joint torique 20 mm');
    expect(nomme)
      .withContext('un identifiant technique n’apprend rien à personne')
      .not.toContain('prod-1');
  });

  it('montre le produit en lecture sur un écart terminé', async () => {
    await monter();
    setup(ecart({ status: 'CLOSED', productId: 'prod-1' }));

    expect(un('produit-lecture')?.textContent).toContain('PRD-4821');
  });

  /**
   * Le référentiel ne se lit pas toujours : un lecteur sans ce droit voit la
   * fiche entière, simplement sans le champ. Un identifiant technique à la
   * place du nom n'apprendrait rien, et un champ vide ferait croire que l'écart
   * ne porte sur aucun produit.
   */
  it('se passe du champ quand le référentiel produit n’est pas lisible', async () => {
    await monter('refuse');
    setup(ecart());

    expect(un('produit')).toBeNull();
    expect(un('probleme'))
      .withContext('le reste du D2 doit rester lisible')
      .not.toBeNull();
  });

  it('envoie le produit retenu avec le reste du contexte', async () => {
    await monter();
    setup(ecart());

    component.contexteForm.patchValue({ customerName: 'ACME', productId: 'prod-2' });
    component.enregistrerContexte();

    expect(svc.updateNc).toHaveBeenCalledWith('nc-1', jasmine.objectContaining({
      customerName: 'ACME', productId: 'prod-2'
    }));
  });

  /**
   * Le serveur lit l'identifiant nul comme « inchangé », pas comme « retire le
   * produit » : envoyer une valeur vide n'effacerait rien, et échouerait à la
   * lecture de l'UUID.
   */
  it('n’envoie pas de produit quand aucun n’est retenu', async () => {
    await monter();
    setup(ecart());

    component.contexteForm.patchValue({ customerName: 'ACME' });
    component.enregistrerContexte();

    const envoye = svc.updateNc.calls.mostRecent().args[1] as Record<string, unknown>;
    expect('productId' in envoye).toBeFalse();
  });

  // ---------- D4 : la note libre de l'analyse ------------------------------

  it('offre le champ de commentaires au D4', async () => {
    await monter();
    setup(ecart());

    expect(un('commentaire-cause')).not.toBeNull();
  });

  it('enregistre la note du D4 en quittant le champ', async () => {
    await monter();
    const nc = ecart();
    setup(nc);

    component.saisirCommentaireCause('Piste du fournisseur écartée : lot conforme au contrôle d’entrée');
    expect(svc.updateNc)
      .withContext('une requête par touche serait absurde')
      .not.toHaveBeenCalled();

    component.enregistrerCommentaireCause(nc);
    expect(svc.updateNc).toHaveBeenCalledWith('nc-1', {
      causeAnalysisComment: 'Piste du fournisseur écartée : lot conforme au contrôle d’entrée'
    });
  });

  it('n’enregistre rien quand la note n’a pas changé', async () => {
    await monter();
    const nc = ecart({ causeAnalysisComment: 'Déjà noté' });
    setup(nc);

    component.saisirCommentaireCause('Déjà noté');
    component.enregistrerCommentaireCause(nc);

    expect(svc.updateNc).not.toHaveBeenCalled();
  });

  // Vide EFFACE : une note écrite par erreur doit pouvoir être retirée.
  it('envoie la chaîne vide pour effacer la note', async () => {
    await monter();
    const nc = ecart({ causeAnalysisComment: 'À retirer' });
    setup(nc);

    component.saisirCommentaireCause('');
    component.enregistrerCommentaireCause(nc);

    expect(svc.updateNc).toHaveBeenCalledWith('nc-1', { causeAnalysisComment: '' });
  });

  it('montre la note en lecture sur un écart terminé', async () => {
    await monter();
    setup(ecart({ status: 'CLOSED', causeAnalysisComment: 'Hypothèse outillage écartée' }));

    expect(un('commentaire-cause')).toBeNull();
    expect(un('commentaire-cause-lecture')?.textContent).toContain('outillage');
  });
});
