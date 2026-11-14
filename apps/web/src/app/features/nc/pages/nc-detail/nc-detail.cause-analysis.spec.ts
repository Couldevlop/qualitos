import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
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
import { FiveWhysAnalysis } from '../../../five-whys/five-whys.types';
import { IshikawaSharedModule } from '../../../ishikawa/ishikawa-shared.module';
import { IshikawaService } from '../../../ishikawa/ishikawa.service';
import { IshikawaCauseResponse, IshikawaDiagramResponse } from '../../../ishikawa/ishikawa.types';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';
import {
  NcCauseAnalysisComponent
} from '../../components/nc-cause-analysis/nc-cause-analysis.component';
import { NcDetailComponent } from './nc-detail.component';
import { CapaActionPanelStubModule } from '../../../capa/components/capa-action-panel/testing/capa-action-panel.stub';

function ecart(): NcResponse {
  return {
    id: 'nc-1', reference: 'NC-2026-1001', title: 'Joint hors tolérance',
    category: 'PROCESS', severity: 'MAJOR', status: 'UNDER_ANALYSIS', origin: 'INTERNAL',
    detectedAt: '2026-06-06T00:00:00Z', createdAt: '2026-06-06T00:00:00Z',
    updatedAt: '2026-06-06T00:00:00Z', teamMembers: []
  };
}

function cause(id: string, label: string, parentId?: string): IshikawaCauseResponse {
  return {
    id, diagramId: 'ish-1', category: 'MACHINES', label, parentId, rootCauseScore: 0.8,
    createdAt: '2026-06-06T00:00:00Z', updatedAt: '2026-06-06T00:00:00Z'
  };
}

function diagramme(causes: IshikawaCauseResponse[]): IshikawaDiagramResponse {
  return {
    id: 'ish-1', tenantId: 't', problemStatement: 'Fuite au presse-étoupe', mode: 'SIX_M',
    status: 'DRAFT', ownerId: 'u1', ncId: 'nc-1',
    createdAt: '2026-06-06T00:00:00Z', updatedAt: '2026-06-06T00:00:00Z', causes
  } as IshikawaDiagramResponse;
}

function chaine(overrides: Partial<FiveWhysAnalysis> = {}): FiveWhysAnalysis {
  return {
    id: 'fw-1', ncId: 'nc-1', ncReference: 'NC-2026-1001',
    problem: 'Fuite au presse-étoupe', rootCause: null,
    steps: [
      { id: 's1', position: 1, answer: 'Le joint a cédé', createdAt: '', updatedAt: '' },
      { id: 's2', position: 2, answer: 'Il n’a jamais été remplacé', createdAt: '', updatedAt: '' }
    ],
    createdAt: '2026-06-06T00:00:00Z', updatedAt: '2026-06-06T00:00:00Z', ...overrides
  } as FiveWhysAnalysis;
}

/**
 * Les schémas d'analyse de cause, lus sur la fiche de l'écart.
 *
 * <p>Les boutons de l'en-tête emmènent les dérouler dans leur écran dédié : en
 * lisant la fiche, on ne voyait donc pas si la cause avait été cherchée, ni ce
 * qu'on avait trouvé — alors que c'est la question que pose le D4 du rapport 8D.
 * La fiche <b>montre</b> ; les écrans dédiés modifient.
 */
describe('NcDetailComponent — analyse de cause sur la fiche', () => {

  let fixture: ComponentFixture<NcDetailComponent>;
  let component: NcDetailComponent;
  let svc: jasmine.SpyObj<NcService>;
  let ishikawa: jasmine.SpyObj<IshikawaService>;
  let fiveWhys: jasmine.SpyObj<FiveWhysService>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => Array.from(hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`));

  /**
   * Les figures sont portees par un composant a part : la fiche de l'ecart et le
   * rapport 8D montrent LES MEMES (ADR 0115), et un trace duplique divergerait.
   */
  function figures(): NcCauseAnalysisComponent {
    return fixture.debugElement
      .query(By.directive(NcCauseAnalysisComponent)).componentInstance as NcCauseAnalysisComponent;
  }

  function setup(): void {
    fixture = TestBed.createComponent(NcDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<NcService>('NcService', ['getNc', 'listPhotos', 'subcategories']);
    svc.getNc.and.returnValue(of(ecart()));
    svc.listPhotos.and.returnValue(of([]));
    svc.subcategories.and.returnValue(of({ entries: [], defaults: true }));
    ishikawa = jasmine.createSpyObj<IshikawaService>('IshikawaService', ['listForNc', 'createDiagram']);
    ishikawa.listForNc.and.returnValue(of([]));
    fiveWhys = jasmine.createSpyObj<FiveWhysService>('FiveWhysService', ['listForNc', 'create']);
    fiveWhys.listForNc.and.returnValue(of([]));
    const annuaire = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    annuaire.search.and.returnValue(of({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }));
    annuaire.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

    await TestBed.configureTestingModule({
      declarations: [NcDetailComponent, NcCauseAnalysisComponent],
      imports: [SharedModule, UiModule, UserPickerModule, FiveWhysSharedModule,
                IshikawaSharedModule, NoopAnimationsModule,
                CapaActionPanelStubModule],
      providers: [
        provideRouter([]),
        { provide: NcService, useValue: svc },
        { provide: IshikawaService, useValue: ishikawa },
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

  // ---------- rien à montrer ----------

  // Une carte vide sur un écart dont la cause n'a pas encore été cherchée
  // n'apprendrait rien : les boutons de l'en-tête sont là pour commencer.
  it('ne montre aucune carte quand aucune analyse n’existe', () => {
    setup();
    expect(un('analyse-de-cause')).toBeNull();
  });

  // ---------- les 5 Pourquoi ----------

  it('dessine la cascade de chaque analyse, sans quitter la fiche', () => {
    fiveWhys.listForNc.and.returnValue(of([chaine()]));
    setup();

    expect(un('analyse-de-cause')).not.toBeNull();
    expect(tous('cinq-pourquoi-bloc').length).toBe(1);
    expect(hote().querySelector('qos-five-whys-cascade svg')).not.toBeNull();
    // Les réponses de la chaîne sont dans le dessin.
    expect(hote().querySelector('qos-five-whys-cascade')!.textContent).toContain('Le joint a cédé');
  });

  // La cause racine se lit DANS le schema, dans sa derniere case, et nulle part
  // ailleurs. Elle etait redite en dessous, en une ligne de texte : le meme fait
  // deux fois, et l'oeil quittait le dessin ou se lit le raisonnement.
  it('affiche la cause racine conclue dans le schema, et pas sous lui', () => {
    fiveWhys.listForNc.and.returnValue(of([chaine({ rootCause: 'Absence de plan préventif' })]));
    setup();

    expect(hote().querySelector('qos-five-whys-cascade')!.textContent)
      .toContain('Absence de plan préventif');
    expect(un('cinq-pourquoi-racine')).toBeNull();
  });

  // Une analyse qui vient d'être ouverte n'a pas d'étape : ce n'est pas une
  // panne, et un cadre vide se lirait comme telle.
  it('dit qu’aucun pourquoi n’a encore été posé', () => {
    fiveWhys.listForNc.and.returnValue(of([chaine({ steps: [] })]));
    setup();
    expect(hote().textContent).toContain('Aucun pourquoi');
  });

  it('emmène dérouler l’analyse dans son écran', () => {
    fiveWhys.listForNc.and.returnValue(of([chaine()]));
    setup();
    expect(un('cinq-pourquoi-ouvrir')!.getAttribute('href')).toBe('/five-whys/fw-1');
  });

  // ---------- Ishikawa ----------

  it('dessine l’arête de poisson du diagramme rattaché', () => {
    ishikawa.listForNc.and.returnValue(of([diagramme([cause('c1', 'Joint usé')])]));
    setup();

    expect(tous('ishikawa-bloc').length).toBe(1);
    const figure = hote().querySelector('qos-ishikawa-fishbone');
    expect(figure).not.toBeNull();
    // L'énoncé du problème est porté par la tête du poisson.
    expect(figure!.textContent).toContain('Fuite au presse-étoupe');
    expect(figure!.textContent).toContain('Joint usé');
  });

  it('range les causes dans l’ordre des M, comme l’écran du diagramme', () => {
    ishikawa.listForNc.and.returnValue(of([diagramme([cause('c1', 'Joint usé')])]));
    setup();
    const branches = figures().fishboneBranches(diagramme([cause('c1', 'Joint usé')]));
    expect(branches.map(b => b.key).slice(0, 3)).toEqual(['METHODS', 'MANPOWER', 'MACHINES']);
    expect(branches.find(b => b.key === 'MACHINES')!.causes.length).toBe(1);
  });

  // Le tracé se recalcule à chaque appel : sans mémoire, taper une lettre
  // ailleurs dans la fiche redessinerait l'arête à chaque frappe.
  it('mémorise les branches d’un même diagramme', () => {
    const d = diagramme([cause('c1', 'Joint usé')]);
    ishikawa.listForNc.and.returnValue(of([d]));
    setup();
    expect(figures().fishboneBranches(d)).toBe(figures().fishboneBranches(d));
  });

  it('compte les sous-causes que le dessin masque', () => {
    const d = diagramme([cause('c1', 'Joint usé'), cause('c2', 'Au-delà de 2000 h', 'c1')]);
    ishikawa.listForNc.and.returnValue(of([d]));
    setup();
    const machines = figures().fishboneBranches(d).find(b => b.key === 'MACHINES')!;
    expect(machines.causes.length).toBe(1);
    expect(machines.causes[0].descendants).toBe(1);
  });

  it('dit qu’aucune cause n’a été posée sur un diagramme vide', () => {
    ishikawa.listForNc.and.returnValue(of([diagramme([])]));
    setup();
    expect(hote().textContent).toContain('Aucune cause');
  });

  // La fiche montre, elle ne modifie pas : ajouter une sous-cause demande
  // l'arbre entier, le plan d'actions et les suggestions — l'écran du diagramme.
  it('ouvre le diagramme quand on active une cause du dessin', () => {
    const d = diagramme([cause('c1', 'Joint usé')]);
    ishikawa.listForNc.and.returnValue(of([d]));
    setup();
    const router = TestBed.inject(Router);
    const navigue = spyOn(router, 'navigate').and.resolveTo(true);

    figures().openCause(d);

    expect(navigue).toHaveBeenCalledWith(['/ishikawa', 'ish-1']);
  });

  it('emmène travailler le diagramme dans son écran', () => {
    ishikawa.listForNc.and.returnValue(of([diagramme([])]));
    setup();
    expect(un('ishikawa-ouvrir')!.getAttribute('href')).toBe('/ishikawa/ish-1');
  });

  // ---------- les deux ensemble ----------

  it('montre les deux analyses côte à côte quand l’écart en porte deux', () => {
    fiveWhys.listForNc.and.returnValue(of([chaine()]));
    ishikawa.listForNc.and.returnValue(of([diagramme([cause('c1', 'Joint usé')])]));
    setup();
    expect(tous('cinq-pourquoi-bloc').length).toBe(1);
    expect(tous('ishikawa-bloc').length).toBe(1);
  });

  // L'indisponibilité d'un service d'analyse ne doit pas emporter la fiche.
  it('reste lisible quand les analyses ne se chargent pas', () => {
    fiveWhys.listForNc.and.returnValue(throwError(() => ({ status: 503 })));
    ishikawa.listForNc.and.returnValue(throwError(() => ({ status: 503 })));
    setup();
    expect(un('analyse-de-cause')).toBeNull();
    expect(hote().textContent).toContain('NC-2026-1001');
  });
});
