import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { EMPTY, of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentsService } from '../../../documents/documents.service';
import { ObjectivesService } from '../../objectives.service';
import {
  ObjectiveProposal,
  ObjectiveReview,
  QualityObjective
} from '../../objectives.types';
import { ObjectivesReviewComponent } from './objectives-review.component';

/**
 * La revue « Objectifs vs KPI réels ».
 *
 * <p>Ce qui est vérifié ici, c'est ce que l'écran DIT : la cible écrite dans le
 * sens du KPI, l'écart signé, pourquoi un objectif reste en alerte, et le fait
 * que les trois gestes du panneau appellent bien trois choses différentes.
 */
describe('ObjectivesReviewComponent — la revue des objectifs (ADR 0133)', () => {

  function objectif(over: Partial<QualityObjective> = {}): QualityObjective {
    return {
      id: 'o-1', title: 'Taux de NC client', kpiId: 'k-1', kpiCode: 'NC-CLI',
      kpiName: 'NC client', kpiUnit: 'ppm', kpiDirection: 'LOWER_IS_BETTER',
      processId: 'p-1', processName: 'Production', isoClause: '6.2',
      periodMonths: 1, targetValue: 500, thresholdWarning: 600, thresholdCritical: 800,
      warningPeriods: 2, criticalPeriods: 1, recoveryPeriods: 3,
      capaOwnerId: 'u-1', capaOwnerName: 'Awa Diallo', rootCauseDueDays: 5,
      lifecycle: 'ACTIVE',
      assessment: {
        status: 'CRITICAL', observedValue: 900, deviationAbsolute: 400,
        deviationPercent: 80, warningBreaches: 3, criticalBreaches: 1,
        onTargetPeriods: 0, triggeredLevel: 'CRITICAL', recoveryAchieved: false,
        heldByOpenCapa: false, periodsConsidered: 6,
        lastPeriodStart: '2026-10-01T00:00:00Z', lastPeriodEnd: '2026-11-01T00:00:00Z'
      },
      createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z',
      ...over
    };
  }

  function proposition(over: Partial<ObjectiveProposal> = {}): ObjectiveProposal {
    return {
      id: 'pr-1', objectiveId: 'o-1', objectiveTitle: 'Taux de NC client',
      level: 'CRITICAL', periodStart: '2026-10-01T00:00:00Z',
      periodEnd: '2026-11-01T00:00:00Z', observedValue: 900, breachPeriods: 1,
      title: 'Objectif critique : Taux de NC client', description: 'Le KPI dépasse…',
      effectivenessCriterion: 'Le KPI « NC client » revient à ou sous 500 ppm…',
      status: 'PENDING', capaCaseId: null, rejectionReason: null,
      decidedBy: null, decidedAt: null, createdAt: '2026-11-01T00:00:00Z',
      ...over
    };
  }

  function revue(over: Partial<ObjectiveReview> = {}): ObjectiveReview {
    return {
      asOf: null,
      counters: { tracked: 1, onTarget: 0, warning: 0, critical: 1 },
      objectives: [objectif()],
      pendingProposals: [proposition()],
      ...over
    };
  }

  let fixture: ComponentFixture<ObjectivesReviewComponent>;
  let component: ObjectivesReviewComponent;
  let svc: jasmine.SpyObj<ObjectivesService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(r: ObjectiveReview = revue(), canManage = true): Promise<void> {
    svc = jasmine.createSpyObj<ObjectivesService>('ObjectivesService',
      ['review', 'accept', 'reject', 'archive', 'evaluateAll']);
    svc.review.and.returnValue(of(r));
    svc.accept.and.returnValue(of(proposition({ status: 'ACCEPTED', capaCaseId: 'c-9' })));
    svc.reject.and.returnValue(of(proposition({ status: 'REJECTED' })));
    svc.archive.and.returnValue(of(undefined));
    svc.evaluateAll.and.returnValue(of([]));

    const docs = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['processes']);
    docs.processes.and.returnValue(of([{ id: 'p-1', name: 'Production', position: 0 }]));

    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['can']);
    authz.can.and.returnValue(of(canManage));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    snack.open.and.returnValue({ onAction: () => EMPTY } as never);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ObjectivesReviewComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: ObjectivesService, useValue: svc },
        { provide: DocumentsService, useValue: docs },
        { provide: AuthzService, useValue: authz },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ObjectivesReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('charge la revue et ouvre la première proposition en attente', async () => {
    await build();
    expect(component.review?.objectives.length).toBe(1);
    expect(component.selected?.id).toBe('pr-1');
  });

  it('signale l\'échec sans laisser une revue à moitié affichée', async () => {
    await build();
    svc.review.and.returnValue(throwError(() => new Error('boom')));
    component.load();
    expect(component.failed).toBeTrue();
    expect(component.review).toBeUndefined();
  });

  /**
   * Le comparateur suit le sens du KPI. Écrire « ≤ » en dur mentirait sur un
   * indicateur qu'on cherche à augmenter — un taux de conformité, par exemple.
   */
  it('écrit la cible dans le sens du KPI, unité comprise', async () => {
    await build();
    expect(component.targetLabel(objectif())).toBe('≤ 500 ppm');
    expect(component.targetLabel(objectif({ kpiDirection: 'HIGHER_IS_BETTER' })))
      .toBe('≥ 500 ppm');
  });

  it('n\'invente pas d\'unité quand le KPI n\'en a pas', async () => {
    await build();
    expect(component.targetLabel(objectif({ kpiUnit: null }))).toBe('≤ 500');
  });

  it('affiche un tiret, et non zéro, quand rien n\'est mesuré', async () => {
    await build();
    const o = objectif({
      assessment: { ...objectif().assessment, observedValue: null, deviationPercent: null }
    });
    expect(component.observedLabel(o)).toBe('—');
    expect(component.deviationLabel(o)).toBe('—');
  });

  it('signe l\'écart et l\'arrondit au dixième', async () => {
    await build();
    expect(component.deviationLabel(objectif())).toBe('+80 %');
    const sous = objectif({
      assessment: { ...objectif().assessment, deviationPercent: -12.34 }
    });
    expect(component.deviationLabel(sous)).toBe('-12.3 %');
  });

  it('met l\'écart absolu en infobulle, avec son unité', async () => {
    await build();
    expect(component.deviationTitle(objectif())).toBe('+400 ppm');
  });

  /**
   * Le fait du panneau doit porter l'unité comme la porte la description du
   * dossier. « 900 » seul, quand la description dit « 900 ppm », se lit comme
   * deux chiffres différents.
   */
  it('cite l\'écart du panneau avec l\'unité du KPI de l\'objectif', async () => {
    await build();
    expect(component.proposalObservedLabel(proposition())).toBe('900 ppm');
  });

  it('n\'invente pas d\'unité dans le panneau quand le KPI n\'en a pas', async () => {
    await build(revue({ objectives: [objectif({ kpiUnit: null })] }));
    expect(component.proposalObservedLabel(proposition())).toBe('900');
  });

  /**
   * Un objectif retenu par une CAPA ouverte doit DIRE ce qu'il attend. Sans
   * cela, l'utilisateur voit un KPI revenu sous la cible et un statut en alerte,
   * et conclut à un défaut d'affichage.
   */
  it('dit combien de périodes il reste avant de repasser « atteint »', async () => {
    await build();
    const o = objectif({
      recoveryPeriods: 3,
      assessment: { ...objectif().assessment, onTargetPeriods: 1, heldByOpenCapa: true }
    });
    expect(component.recoveryLabel(o)).toContain('1');
    expect(component.recoveryLabel(o)).toContain('3');
    expect(component.recoveryLabel(o)).toContain('2');
  });

  it('ne descend jamais sous zéro période restante', async () => {
    await build();
    const o = objectif({
      recoveryPeriods: 1,
      assessment: { ...objectif().assessment, onTargetPeriods: 4, heldByOpenCapa: true }
    });
    expect(component.recoveryLabel(o)).toContain('0');
  });

  it('distingue la CAPA proposée, celle en cours, et l\'absence de dossier', async () => {
    await build();
    expect(component.capaLabel(objectif())).toContain('Proposée');

    await build(revue({ pendingProposals: [] }));
    expect(component.capaLabel(objectif())).toBe('Aucune');

    const retenu = objectif({
      assessment: { ...objectif().assessment, heldByOpenCapa: true }
    });
    expect(component.capaLabel(retenu)).toContain('en cours');
  });

  it('compte les périodes consécutives quand il y en a plus d\'une', async () => {
    await build(revue({ pendingProposals: [proposition({ breachPeriods: 3 })] }));
    expect(component.capaLabel(objectif())).toContain('3');
  });

  it('accepte la proposition et recharge la revue', async () => {
    await build();
    // Une fois la CAPA enregistree, le serveur ne rend plus la proposition
    // tranchee : la revue rechargee referme le panneau.
    svc.review.and.returnValue(of(revue({ pendingProposals: [] })));
    component.accept(proposition());
    expect(svc.accept).toHaveBeenCalledWith('pr-1');
    expect(component.selected).toBeUndefined();
    expect(svc.review).toHaveBeenCalledTimes(2);
  });

  /**
   * « Accepter et ajouter une action » enregistre le dossier ET y mène. Rejeter
   * en bloc laisserait l'écart sans traitement alors que le désaccord ne porte
   * que sur le geste à faire.
   */
  it('accepte puis navigue vers le dossier pour y ajouter une action', async () => {
    await build();
    const router = TestBed.inject(Router);
    const nav = spyOn(router, 'navigate').and.resolveTo(true);
    component.acceptAndAddAction(proposition());
    expect(svc.accept).toHaveBeenCalledWith('pr-1');
    expect(nav).toHaveBeenCalledWith(['/capa', 'c-9']);
  });

  it('ne rejette RIEN si la fenêtre de motif est annulée', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.reject(proposition());
    expect(svc.reject).not.toHaveBeenCalled();
  });

  it('rejette avec le motif saisi, et rien d\'autre', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of('arrêt planifié') } as never);
    component.reject(proposition());
    expect(svc.reject).toHaveBeenCalledWith('pr-1', 'arrêt planifié');
  });

  it('n\'archive qu\'après confirmation', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    component.archive(objectif());
    expect(svc.archive).not.toHaveBeenCalled();

    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    component.archive(objectif());
    expect(svc.archive).toHaveBeenCalledWith('o-1');
  });

  it('dit quand aucun écart n\'atteint la règle, plutôt que de ne rien dire', async () => {
    await build();
    component.evaluateAll();
    expect(snack.open).toHaveBeenCalled();
    expect(svc.review).toHaveBeenCalledTimes(2);
  });

  it('recharge quand on change un filtre, en le transmettant au service', async () => {
    await build();
    component.processId = 'p-9';
    component.asOf = '2026-10-31';
    component.load();
    expect(svc.review).toHaveBeenCalledWith('p-9', '2026-10-31');
  });

  /**
   * La proposition tranchée par quelqu'un d'autre entre deux chargements ne doit
   * pas rester ouverte : le panneau proposerait de décider d'un écart déjà
   * décidé.
   */
  it('referme le panneau si la proposition a disparu de la revue', async () => {
    await build();
    svc.review.and.returnValue(of(revue({ pendingProposals: [] })));
    component.load();
    expect(component.selected).toBeUndefined();
  });

  it('garde ouverte la proposition qu\'on regardait si elle est toujours là', async () => {
    await build();
    svc.review.and.returnValue(of(revue({
      pendingProposals: [proposition({ id: 'pr-2' }), proposition()]
    })));
    component.load();
    expect(component.selected?.id).toBe('pr-1');
  });

  it('ne montre les gestes d\'écriture qu\'à qui peut les faire', async () => {
    await build(revue(), false);
    expect(component.canManage).toBeFalse();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('[data-test="objectifs-nouveau"]')).toBeNull();
    expect(html.querySelector('[data-test="objectifs-accepter"]')).toBeNull();
  });

  it('affiche les quatre tuiles et la ligne de l\'objectif', async () => {
    await build();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelectorAll('[data-test="objectifs-tuiles"] .tuile').length).toBe(4);
    expect(html.querySelector('[data-test="objectif-o-1"]')).not.toBeNull();
    expect(html.querySelector('[data-test="objectifs-critere"]')?.textContent)
      .toContain('NC client');
  });

  it('annonce le vide plutôt que de laisser un tableau nu', async () => {
    await build(revue({ objectives: [], pendingProposals: [], counters:
      { tracked: 0, onTarget: 0, warning: 0, critical: 0 } }));
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('[data-test="objectifs-vide"]')).not.toBeNull();
  });
});
