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
 * CE QUI SE CORRIGE SUR LA FICHE.
 *
 * <p>Trois défauts relevés en recette, que le modèle de traitement 8D rendait
 * visibles dès qu'on le posait à côté de l'écran.
 *
 * <p><b>Le constat</b> était figé à la déclaration : une faute de frappe, une
 * quantité fausse ou un détail manquant le restaient pour toujours, sur la
 * pièce même que le client lit. Le serveur l'acceptait déjà ; aucun écran ne
 * l'offrait.
 *
 * <p><b>La date du fait</b> était posée à l'instant de la saisie, sans recours :
 * une réclamation reçue lundi et enregistrée jeudi portait jeudi, alors que
 * c'est elle qui fait courir le délai de réponse dû au client.
 *
 * <p><b>Le valideur de la clôture</b> était offert à qui ne pouvait pas s'en
 * servir : la personne qui a résolu l'écart ne choisit pas qui la relit. Le
 * serveur le refusait déjà, mais après le clic, et par un message générique
 * qui conseillait de recharger la page.
 */
describe('NcDetailComponent — ce qui se corrige sur la fiche', () => {

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

  // ---------- D2 : le constat se corrige ----------

  it('offre le constat à la correction, et le recopie de la fiche', async () => {
    await monter();
    setup(ecart({ description: 'Quatre pompes sans joint torique' }));

    expect(un('constat')).not.toBeNull();
    expect(component.contexteForm.getRawValue().description)
      .toBe('Quatre pompes sans joint torique');
  });

  it('envoie le constat corrigé avec le reste du contexte', async () => {
    await monter();
    setup(ecart({ description: 'Quatre pompes' }));

    component.contexteForm.patchValue({ description: 'Quatre pompes du lot L-2411' });
    component.contexteModifie = true;
    component.enregistrerContexte();

    expect(svc.updateNc).toHaveBeenCalledWith('nc-1',
      jasmine.objectContaining({ description: 'Quatre pompes du lot L-2411' }));
  });

  /**
   * Un écart sans constat ne dit plus ce qui s'est passé, et le vider d'ici
   * contournerait l'obligation posée à la création. Le serveur le refuse ; le
   * formulaire le dit AVANT le clic.
   */
  it('refuse d’effacer le constat', async () => {
    await monter();
    setup(ecart({ description: 'Quatre pompes' }));

    component.contexteForm.patchValue({ description: '   ' });

    expect(component.contexteForm.invalid).toBeTrue();
  });

  // ---------- D2 : la date du fait se corrige ----------

  it('recopie la date du constat dans le champ', async () => {
    await monter();
    setup(ecart({ detectedAt: '2026-11-16T09:30:00Z' }));

    const jour = component.contexteForm.getRawValue().detectedOn as Date;
    expect(jour).not.toBeNull();
    expect(jour.getTime()).toBe(new Date('2026-11-16T09:30:00Z').getTime());
  });

  /**
   * Le sélecteur rend minuit. Garder l'heure déjà enregistrée évite de
   * déplacer le fait de plusieurs heures parce qu'on a corrigé le jour -- et
   * sur un écart détecté à 23 h, ce déplacement changeait la date elle-même
   * dans un autre fuseau.
   */
  it('garde l’heure du constat quand on corrige le jour', async () => {
    await monter();
    const origine = new Date('2026-11-19T12:39:10Z');
    setup(ecart({ detectedAt: origine.toISOString() }));

    const corrige = new Date(origine.getTime());
    corrige.setDate(16);
    corrige.setHours(0, 0, 0, 0);
    component.contexteForm.patchValue({ detectedOn: corrige });
    component.contexteModifie = true;
    component.enregistrerContexte();

    const envoye = svc.updateNc.calls.mostRecent().args[1] as { detectedAt?: string };
    const date = new Date(envoye.detectedAt as string);
    expect(date.getDate()).toBe(16);
    expect(date.getHours()).toBe(origine.getHours());
    expect(date.getMinutes()).toBe(origine.getMinutes());
  });

  // ---------- D1 : qui peut désigner le second regard ----------

  /**
   * LE SECOND REGARD EXISTE, IL N'EST PLUS EXIGE (ADR 0145). Le valideur se
   * designe sur un ecart resolu comme sur un autre : qui a resolu peut clore
   * lui-meme, donc lui interdire de nommer qui pourrait le faire a sa place
   * n'empecherait plus rien.
   */
  it('offre le valideur sur un écart résolu', async () => {
    await monter();
    setup(ecart({ status: 'RESOLVED' }));

    expect(un('valideur')).not.toBeNull();
    expect(un('valideur-interdit')).toBeNull();
  });

  it('laisse désigner le valideur à qui n’a pas résolu l’écart', async () => {
    await monter();
    setup(ecart({ status: 'RESOLVED', blockers: [] }));

    expect(un('valideur')).not.toBeNull();
    expect(un('valideur-interdit')).toBeNull();
  });
});
