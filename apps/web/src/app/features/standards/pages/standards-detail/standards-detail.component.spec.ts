import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsExtrasService } from '../../../standards-extras/standards-extras.service';
import { MockAuditReport } from '../../../standards-extras/standards-extras.types';
import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, AlignmentReport, AuditBlancReport, ClauseDetail, RequirementDetail,
  RoadmapSummary, SectionDetail, StandardDetail
} from '../../standards.types';
import { StandardsDetailComponent } from './standards-detail.component';

/**
 * Fiche d'une adoption (§8) — vue de lecture pendant un audit, et, pour les
 * seuls référentiels du tenant, écran de saisie de leur arborescence.
 *
 * L'invariant que cette spec tient : les commandes d'édition n'existent QUE sur
 * un référentiel appartenant au tenant. Les proposer sur une norme livrée
 * promettrait un geste que le serveur refuse (403) — l'utilisateur en
 * conclurait que l'application est cassée, non que la norme est en lecture seule.
 */
describe('StandardsDetailComponent', () => {
  let fixture: ComponentFixture<StandardsDetailComponent>;
  let component: StandardsDetailComponent;
  let svc: jasmine.SpyObj<StandardsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let extras: jasmine.SpyObj<StandardsExtrasService>;
  /** Le vrai Router (les `routerLink` du gabarit en ont besoin), sa navigation espionnée. */
  let navigate: jasmine.Spy;
  /** Les rôles du porteur du jeton, réglables avant `ngOnInit`. */
  let roles: string[] | undefined;
  let el: HTMLElement;

  const ADOPTION = { id: 'ad1', standardId: 's1', standardCode: 'PRO-002' } as AdoptionResponse;

  /** Une exécution d'audit blanc IA, réduite à ce que la fiche en lit. */
  function run(over: Partial<MockAuditReport> = {}): MockAuditReport {
    return {
      id: 'r9', adoptionId: 'ad1', standardId: 's1', standardCode: 'ISO-9001',
      standardName: 'ISO 9001:2015', readiness: 72.4, majorCount: 3, minorCount: 5,
      observationCount: 1, questionCount: 42, questions: [], gaps: [], remediationPlan: [],
      aiProvider: 'mistral', createdByUserId: 'u1', createdAt: '2026-11-10T09:30:00Z', ...over
    } as MockAuditReport;
  }

  const un = (sel: string) => el.querySelector<HTMLElement>(`[data-test="${sel}"]`);

  /**
   * Ouvre l'onglet « Audit blanc » (5e du groupe).
   *
   * <p>Material ne pose le contenu d'un onglet dans le DOM que lorsqu'il
   * devient actif : interroger cet onglet sans l'ouvrir ne trouve rien.
   */
  function ouvrirAuditBlanc(): void {
    fixture.detectChanges();
    component.selectedTab = 4;
    fixture.detectChanges();
  }

  const REQUIREMENT = {
    id: 'r1', code: '1.1.1', text: 'Le programme est revu chaque année',
    obligation: 'MUST', orderIndex: 0
  } as RequirementDetail;

  const CLAUSE = {
    id: 'c1', code: '1.1', title: 'Fréquence', orderIndex: 0, requirements: [REQUIREMENT]
  } as ClauseDetail;

  const SECTION = {
    id: 'sec1', code: '1', title: 'Programmation', orderIndex: 0, clauses: [CLAUSE]
  } as SectionDetail;

  function detail(over: Partial<StandardDetail> = {}): StandardDetail {
    return {
      id: 's1', code: 'PRO-002', fullName: 'Procédure d\'audit interne',
      currentVersion: 'v3', certificationBodyRequired: false, status: 'PUBLISHED',
      sections: [SECTION], owned: true, ...over
    } as StandardDetail;
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<StandardsService>('StandardsService', [
      'getAdoption', 'getStandardDetail', 'listDocumentTemplates', 'listProcessTemplates',
      'listRevisions', 'getAlignment', 'getRoadmap', 'getAuditBlanc', 'listEvidence',
      'addSection', 'updateSection', 'deleteSection',
      'addClause', 'updateClause', 'deleteClause',
      'addRequirement', 'updateRequirement', 'deleteRequirement'
    ]);
    svc.getAdoption.and.returnValue(of(ADOPTION));
    svc.getStandardDetail.and.returnValue(of(detail()));
    svc.listDocumentTemplates.and.returnValue(of([]));
    svc.listProcessTemplates.and.returnValue(of([]));
    svc.listRevisions.and.returnValue(of([]));
    svc.getAlignment.and.returnValue(throwError(() => ({ status: 504 })));
    svc.getRoadmap.and.returnValue(throwError(() => ({ status: 504 })));
    svc.getAuditBlanc.and.returnValue(throwError(() => ({ status: 504 })));
    svc.listEvidence.and.returnValue(of([]));
    [svc.addSection, svc.updateSection, svc.deleteSection,
      svc.addClause, svc.updateClause, svc.deleteClause,
      svc.addRequirement, svc.updateRequirement, svc.deleteRequirement]
      .forEach(spy => spy.and.returnValue(of(undefined)));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    extras = jasmine.createSpyObj<StandardsExtrasService>(
      'StandardsExtrasService', ['mockAuditHistory', 'runMockAudit']);
    extras.mockAuditHistory.and.returnValue(of([]));
    extras.runMockAudit.and.returnValue(of(run()));
    roles = undefined;

    await TestBed.configureTestingModule({
      declarations: [StandardsDetailComponent],
      // FormsModule : la fiche pilote son formulaire de liaison de preuve en
      // ngModel, comme le fait StandardsModule.
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1', roles }) } },
        { provide: StandardsExtrasService, useValue: extras },
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'ad1' } } }
        }
      ]
    }).compileComponents();

    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(StandardsDetailComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement as HTMLElement;
  });

  /** Le dialogue rend `value` ; `undefined` simule une annulation. */
  function dialogReturns(value: unknown): void {
    dialog.open.and.returnValue(
      { afterClosed: () => of(value) } as MatDialogRef<unknown, unknown>);
  }

  it('n\'ouvre l\'édition que sur un référentiel appartenant au tenant', () => {
    // Une norme livrée se maintient par migration : proposer de la modifier
    // promettrait une action que le serveur refusera de toute façon (403).
    svc.getStandardDetail.and.returnValue(of(detail({ owned: false })));
    fixture.detectChanges();
    expect(el.querySelector('.add-section-btn')).toBeNull();
    expect(el.querySelector('.edit-requirement-btn')).toBeNull();

    svc.getStandardDetail.and.returnValue(of(detail({ owned: true })));
    component.ngOnInit();
    fixture.detectChanges();
    expect(el.querySelector('.add-section-btn')).not.toBeNull();
    expect(el.querySelector('.add-clause-btn')).not.toBeNull();
    expect(el.querySelector('.add-requirement-btn')).not.toBeNull();
  });

  it('dit quoi faire devant un référentiel vide', () => {
    // C'est l'état NORMAL à la création : l'écran doit inviter, pas donner
    // l'impression d'un chargement raté.
    svc.getStandardDetail.and.returnValue(of(detail({ sections: [] })));
    fixture.detectChanges();

    expect(el.querySelector('.tree-empty')!.textContent)
      .toContain('saisissez celles de votre procédure');
  });

  it('recharge la fiche après un ajout, sans la reconstruire à la main', () => {
    fixture.detectChanges();
    dialogReturns({ code: '2', title: 'Réalisation' });
    svc.getStandardDetail.calls.reset();

    el.querySelector<HTMLButtonElement>('.add-section-btn')!.click();

    expect(svc.addSection).toHaveBeenCalledWith('s1', { code: '2', title: 'Réalisation' });
    // Reconstruire l'arbre côté client ferait diverger l'écran de la base au
    // premier cas non prévu (ordre, code normalisé par le serveur).
    expect(svc.getStandardDetail).toHaveBeenCalledWith('s1');
  });

  it('n\'écrit rien quand la saisie est abandonnée', () => {
    fixture.detectChanges();
    dialogReturns(undefined);

    component.addSection();
    component.editSection(SECTION);
    component.addClause(SECTION);
    component.editClause(CLAUSE);
    component.addRequirement(CLAUSE);
    component.editRequirement(REQUIREMENT);

    expect(svc.addSection).not.toHaveBeenCalled();
    expect(svc.updateSection).not.toHaveBeenCalled();
    expect(svc.addClause).not.toHaveBeenCalled();
    expect(svc.updateClause).not.toHaveBeenCalled();
    expect(svc.addRequirement).not.toHaveBeenCalled();
    expect(svc.updateRequirement).not.toHaveBeenCalled();
  });

  it('adresse chaque écriture au bon niveau', () => {
    fixture.detectChanges();
    dialogReturns({ code: 'x', title: 'T', text: 'T', obligation: 'MUST' });

    component.editSection(SECTION);
    component.addClause(SECTION);
    component.editClause(CLAUSE);
    component.addRequirement(CLAUSE);
    component.editRequirement(REQUIREMENT);

    expect(svc.updateSection).toHaveBeenCalledWith('s1', 'sec1', jasmine.anything());
    expect(svc.addClause).toHaveBeenCalledWith('s1', 'sec1', jasmine.anything());
    expect(svc.updateClause).toHaveBeenCalledWith('s1', 'c1', jasmine.anything());
    expect(svc.addRequirement).toHaveBeenCalledWith('s1', 'c1', jasmine.anything());
    expect(svc.updateRequirement).toHaveBeenCalledWith('s1', 'r1', jasmine.anything());
  });

  it('dit ce que la suppression emporte avant de l\'exécuter', () => {
    fixture.detectChanges();
    const ask = spyOn(window, 'confirm').and.returnValue(true);

    component.deleteSection(SECTION);

    // La question NOMME ce qui part : « êtes-vous sûr ? » ne se lit plus.
    expect(ask.calls.mostRecent().args[0]).toContain('1');
    expect(svc.deleteSection).toHaveBeenCalledWith('s1', 'sec1');
  });

  it('ne supprime rien si la confirmation est refusée', () => {
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(false);

    component.deleteSection(SECTION);
    component.deleteClause(CLAUSE);
    component.deleteRequirement(REQUIREMENT);

    expect(svc.deleteSection).not.toHaveBeenCalled();
    expect(svc.deleteClause).not.toHaveBeenCalled();
    expect(svc.deleteRequirement).not.toHaveBeenCalled();
  });

  it('supprime clause et exigence quand la confirmation est donnée', () => {
    fixture.detectChanges();
    spyOn(window, 'confirm').and.returnValue(true);

    component.deleteClause(CLAUSE);
    component.deleteRequirement(REQUIREMENT);

    expect(svc.deleteClause).toHaveBeenCalledWith('s1', 'c1');
    expect(svc.deleteRequirement).toHaveBeenCalledWith('s1', 'r1');
  });

  [
    { status: 409, expected: 'déjà pris' },
    { status: 403, expected: 'plateforme' },
    { status: 500, expected: 'impossible' }
  ].forEach(({ status, expected }) => {
    it(`explique le refus ${status} par le geste qu'il appelle`, () => {
      fixture.detectChanges();
      dialogReturns({ code: '1', title: 'Doublon' });
      svc.addSection.and.returnValue(throwError(() => ({ status })));
      svc.getStandardDetail.calls.reset();

      component.addSection();

      expect(snack.open.calls.mostRecent().args[0]).toContain(expected);
      // Rien n'a changé côté serveur : inutile de relire la fiche.
      expect(svc.getStandardDetail).not.toHaveBeenCalled();
    });
  });

  describe('audit blanc', () => {

    it("lance un audit blanc IA depuis la fiche et ouvre son rapport", () => {
      ouvrirAuditBlanc();
      expect(extras.mockAuditHistory).toHaveBeenCalledWith('ad1');
      expect(un('audit-ia-vide')).not.toBeNull();

      un('lancer-audit-ia')!.click();

      expect(extras.runMockAudit).toHaveBeenCalledWith('ad1');
      expect(navigate).toHaveBeenCalledWith(['/standards-ims', 'audit-blanc-ia', 'ad1', 'r9']);
      expect(component.mockAuditRunning).toBeFalse();
    });

    it('relit les audits blancs deja passes, du plus recent au plus ancien', () => {
      extras.mockAuditHistory.and.returnValue(of([run(), run({ id: 'r8', readiness: 51.6 })]));
      ouvrirAuditBlanc();

      expect(un('audit-ia-vide')).toBeNull();
      const lignes = el.querySelectorAll('[data-test="audit-ia-historique"] li');
      expect(lignes.length).toBe(2);
      expect(lignes[0].textContent).toContain('72 %');
      expect(lignes[1].textContent).toContain('52 %');
      expect(lignes[0].textContent).toContain('3');

      (lignes[1].querySelector('button') as HTMLElement).click();
      expect(navigate).toHaveBeenCalledWith(['/standards-ims', 'audit-blanc-ia', 'ad1', 'r8']);
    });

    [
      { status: 409, attendu: 'aucune clause exploitable' },
      { status: 403, attendu: 'pilotage qualité' },
      // 500 : le message commun de la plateforme prend le dessus sur le repli local.
      { status: 500, attendu: 'Erreur serveur' }
    ].forEach(({ status, attendu }) => {
      it(`dit pourquoi l'audit blanc IA est refusé (${status})`, () => {
        extras.runMockAudit.and.returnValue(throwError(() => ({ status })));
        ouvrirAuditBlanc();

        un('lancer-audit-ia')!.click();
        fixture.detectChanges();

        expect(un('audit-ia-erreur')?.textContent).toContain(attendu);
        // Rien n'a ete produit : on ne navigue pas vers un rapport inexistant.
        expect(navigate).not.toHaveBeenCalled();
        expect(component.mockAuditRunning).toBeFalse();
      });
    });

    it("ne propose pas le lancement a qui n'a pas le role, et le serveur reste juge", () => {
      roles = ['user'];
      ouvrirAuditBlanc();
      expect(component.canRunMockAudit).toBeFalse();
      expect(un('lancer-audit-ia')).toBeNull();

      // Sans role lisible (dev, claims absents) on n'ampute pas l'ecran.
      roles = undefined;
      component.ngOnInit();
      ouvrirAuditBlanc();
      expect(component.canRunMockAudit).toBeTrue();
      expect(un('lancer-audit-ia')).not.toBeNull();
    });

    it("propose de relancer l'analyse d'ecarts quand elle a echoue", () => {
      ouvrirAuditBlanc();
      // getAuditBlanc rend 504 par defaut : l'onglet etait muet jusqu'ici.
      expect(component.auditFailed).toBeTrue();
      expect(un('audit-regles-echec')).not.toBeNull();

      const rapport: AuditBlancReport = {
        tenantStandardId: 'ad1', standardId: 's1', standardCode: 'ISO-9001',
        standardName: 'ISO 9001:2015', generatedAt: '2026-11-10T09:30:00Z',
        readinessScore: 80, totalRequirements: 12, coveredRequirements: 9,
        mustTotal: 10, mustCovered: 8, criticalGaps: 0, majorGaps: 1, minorGaps: 1,
        verdict: 'QUASI PRÊT', findings: []
      };
      svc.getAuditBlanc.and.returnValue(of(rapport));
      un('audit-regles-relancer')!.click();
      fixture.detectChanges();

      expect(component.auditFailed).toBeFalse();
      expect(un('audit-regles-echec')).toBeNull();
      expect(svc.getAuditBlanc).toHaveBeenCalledTimes(2);
    });
  });

  describe('definition des indicateurs (regle 18.2 #8)', () => {

    it('dit la formule de chaque indicateur et montre son numerateur', () => {
      const align: AlignmentReport = {
        tenantStandardId: 'ad1', standardId: 's1', standardCode: 'ISO-9001',
        overallScore: 75, totalRequirements: 12, coveredRequirements: 9,
        totalMustRequirements: 10, coveredMustRequirements: 8, sections: []
      };
      svc.getAlignment.and.returnValue(of(align));
      svc.getAuditBlanc.and.returnValue(of({
        tenantStandardId: 'ad1', standardId: 's1', standardCode: 'ISO-9001',
        standardName: 'ISO 9001:2015', generatedAt: '2026-11-10T09:30:00Z',
        readinessScore: 80, totalRequirements: 12, coveredRequirements: 9,
        mustTotal: 10, mustCovered: 8, criticalGaps: 0, majorGaps: 1, minorGaps: 1,
        verdict: 'QUASI PRÊT', findings: []
      } as AuditBlancReport));
      const rm: RoadmapSummary = {
        tenantStandardId: 'ad1', totalStages: 19, doneStages: 6, inProgressStages: 2,
        skippedStages: 3, completionPercent: 37.5, stages: []
      };
      svc.getRoadmap.and.returnValue(of(rm));
      fixture.detectChanges();

      // Preparation : les MUST, et seulement eux.
      expect(un('kpi-preparation')?.textContent).toContain('8/10');
      expect(un('kpi-preparation')?.textContent).toContain('MUST couvertes ÷ MUST au total');
      // Alignement : toutes les exigences, sans ponderation.
      expect(un('kpi-alignement')?.textContent).toContain('9/12');
      expect(un('kpi-alignement')?.textContent).toContain('Chaque exigence pèse pareil');
      // Roadmap : les etapes ecartees sortent du denominateur (19 - 3 = 16).
      expect(un('kpi-roadmap')?.textContent).toContain('6/16');
      expect(un('kpi-roadmap')?.textContent).toContain('étapes applicables');
      // Ecarts : la frontiere critique / majeur / mineur est ecrite.
      expect(un('kpi-ecarts')?.textContent).toContain("risque en cas d'absence est élevé");
      // Et le bareme du verdict, dans l'onglet audit blanc.
      component.selectedTab = 4;
      fixture.detectChanges();
      expect(un('verdict-bareme')?.textContent).toContain('≥ 95 %');
    });

    it('ne promet aucun numerateur quand le rapport ne se lit pas', () => {
      // getAlignment/getAuditBlanc/getRoadmap rendent 504 par defaut.
      fixture.detectChanges();
      expect(un('kpi-preparation')?.textContent).not.toContain('/');
      // La formule, elle, reste dite : elle ne depend pas des donnees.
      expect(un('kpi-preparation')?.textContent).toContain('MUST au total');
    });
  });
});
